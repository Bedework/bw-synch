/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.synch;

import org.bedework.synch.Notification.NotificationItem;
import org.bedework.synch.Notification.NotificationItem.ActionType;
import org.bedework.synch.SynchDefs.SynchEnd;
import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.cnctrs.Connector.NotificationBatch;
import org.bedework.synch.cnctrs.ConnectorInstance;
import org.bedework.synch.exception.SynchException;

import edu.rpi.cmt.security.PwEncryptionIntf;
import edu.rpi.cmt.timezones.Timezones;
import edu.rpi.sss.util.Util;

import net.fortuna.ical4j.model.TimeZone;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Synch processor.
 * <p>The synch processor manages subscriptions made by a subscriber to a target.
 * Such a subscription might be one way or two way.
 *
 * <p>There are two ends to a subscription handled by connectors. The connectors
 * implement a standard interface which provides sufficient information for the
 * synch process.
 *
 * <p>Synchronization is triggered either when a change takes place - through
 * some sort of push-notification or periodically.
 *
 * <p>For example, we might have a one way subscription from bedework to
 * exchange. Exchange will post notifications to the synch engine which will
 * then resynch the modified entity.
 *
 * <p>Alternatively we might have a subscription to a file which we refresh each
 * day at 4am.
 *
 * <p>A subscription may be in the following states:<ul>
 * <li>dormant - that is there is no current activity, for
 * example a file subscription with a periodic update,</li>
 * <li>active - there is some active connection associated with it, for example,
 * an Exchange push subscription waiting for a notification</li>
 * <li>processing - for example, an Exchange push subscription which is
 * processing a notification</li>
 * <li>unsubscribing - the user has asked to unsubscribe but there is some
 * activity we are waiting for<li>
 * </ul>
 *
 * <p>Interactions with the calendars is carried out through an interface which
 * assumes the CalWs-SOAP protocol. Messages and responses are of that form
 * though the actual implementation may not use the protocol if the target does
 * not support it. For example we convert CalWs-SOAP interactions into ExchangeWS.
 *
 * --------------------- ignore below ----------------------------------------
 *
 * <p>This process manages the setting up of push-subscriptions with the exchange
 * service and provides support for the resulting call-back from Exchange. There
 * will be one instance of this object to handle the tables we create and
 * manipulate.
 *
 * <p>There will be multiple threads calling the various methods. Push
 * subscriptions work more or less as follows:<ul>
 * <li>Subscribe to exchange giving a url to call back to. Set a refresh period</li>
 * <li>Exchange calls back even if there is no change - effectively polling us</li>
 * <li>If we don't respond Exchange doubles the wait period and tries again</li>
 * <li>Repeats that a few times then gives up</li>
 * <li>If we do respond will call again at the specified rate</li>
 * <li>No unsubscribe - wait for a ping then respond with unsubscribe</li>
 * </ul>
 *
 * <p>At startup we ask for the back end system to tell us what the subscription
 * are and we spend some time setting those up.
 *
 * <p>We also provide a way for the system to tell us about new (un)subscribe
 * requests. While starting up we need to queue these as they may be unsubscribes
 * for a subscribe in the startup list.
 *
 * <p>Shutdown ought to wait for the remote systems to ping us for every outstanding
 * subscription. That ought to be fairly quick.
 *
 * @author Mike Douglass
 */
public class SynchEngine {
  protected transient Logger log;

  private final boolean debug;

  private static String appname = "Synch";

  private static final String synchConfigPath = "/properties/synch/synch-config.xml";

  /* Config bean name */
  private static String synchConfigName = "synchConfig";

  private ApplicationContext appContext;

  private transient PwEncryptionIntf pwEncrypt;

  /* Map of currently active subscriptions - that is - we have traffic between
   * systems.
   */
  private final Map<String, Subscription> subs =
    new HashMap<String, Subscription>();

  private boolean starting;

  private boolean running;

  private boolean stopping;

  private SynchConfig config;

  private static Object getSyncherLock = new Object();

  private static SynchEngine syncher;

  private Timezones timezones;

  private SynchlingPool synchlingPool;

  /* Where we keep subscriptions that come in while we are starting */
  private List<Subscription> subsList;

  private SynchDb db;

  private Map<String, Connector> connectorMap = new HashMap<String, Connector>();

  /** Constructor
   *
   * @param exintf
   */
  private SynchEngine() throws SynchException {
    debug = getLogger().isDebugEnabled();

    System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump",
                       String.valueOf(debug));

    appContext = new ClassPathXmlApplicationContext(synchConfigPath);

    config = (SynchConfig)appContext.getBean(synchConfigName);

    if ((config.getCallbackURI() != null) &&
        !config.getCallbackURI().endsWith("/")) {
      throw new SynchException("callbackURI MUST end with '/'");
    }

    synchlingPool = new SynchlingPool();
    synchlingPool.start(this,
                        config.getSynchlingPoolSize(),
                        config.getSynchlingPoolTimeout());
  }

  /**
   * @return the syncher
   * @throws SynchException
   */
  public static SynchEngine getSyncher() throws SynchException {
    if (syncher != null) {
      return syncher;
    }

    synchronized (getSyncherLock) {
      if (syncher != null) {
        return syncher;
      }
      syncher = new SynchEngine();
      return syncher;
    }
  }

  /** Set before calling getSyncher
   *
   * @param val
   */
  public static void setAppname(final String val) {
    appname = val;
  }

  /**
   * @return appname
   */
  public static String getAppname() {
    return appname;
  }

  /** Get a timezone object given the id. This will return transient objects
   * registered in the timezone directory
   *
   * @param id
   * @return TimeZone with id or null
   * @throws SynchException
   */
   public static TimeZone getTz(final String id) throws SynchException {
     try {
       return getSyncher().timezones.getTimeZone(id);
     } catch (SynchException se) {
       throw se;
     } catch (Throwable t) {
       throw new SynchException(t);
     }
   }

  /** Start synch process.
   *
   * @throws SynchException
   */
  public void start() throws SynchException {
    try {
      if (starting || running) {
        warn("Start called when already starting or running");
        return;
      }

      synchronized (this) {
        subsList = null;

        starting = true;
      }

      info("**************************************************");
      info("Starting synch");
      info("      callback URI: " + config.getCallbackURI());
      info("**************************************************");

      if (config.getKeystore() != null) {
        System.setProperty("javax.net.ssl.trustStore", config.getKeystore());
        System.setProperty("javax.net.ssl.trustStorePassword", "bedework");
      }

      Map<String, String> connectors = config.getConnectors();

      /* Register the connectors and start them */
      for (String cnctrId: connectors.keySet()) {
        info("Register and start connector " + cnctrId);

        registerConnector(cnctrId, connectors.get(cnctrId));

        Connector conn = getConnector(cnctrId);

        conn.start(cnctrId,
                   config.getCallbackURI() + cnctrId + "/",
                   this);
      }

      /* Get the list of subscriptions from our database and process them.
       * While starting, new subscribe requests get added to the list.
       */

      db = new SynchDb();

      try {
        db.open();
        List<Subscription> startList = db.getAll();
        db.close();

        startup:
        while (starting) {
          if (debug) {
            trace("startList has " + startList.size() + " subscriptions");
          }

          for (Subscription sub: startList) {
            Synchling sl;

            while (true) {
              if (stopping) {
                break startup;
              }

              sl = synchlingPool.getNoException();
              if (sl != null) {
                break;
              }
            }

            NotificationItem ni = new NotificationItem(ActionType.FullSynch,
                                                       null, null);
            Notification<NotificationItem> note = new Notification<NotificationItem>(
                sub, SynchEnd.none, ni);

            sl.handleNotification(note);
          }

          synchronized (this) {
            if (subsList == null) {
              // Nothing came in as we started
              starting = false;
              if (stopping) {
                break startup;
              }
              running = true;
              break;
            }

            startList = subsList;
            subsList = null;
          }
        }
      } finally {
        if ((db != null) && db.isOpen()) {
          db.close();
        }
      }

      info("**************************************************");
      info("Synch started");
      info("**************************************************");
    } catch (SynchException se) {
      error(se);
      starting = false;
      running = false;
      throw se;
    } catch (Throwable t) {
      error(t);
      starting = false;
      running = false;
      throw new SynchException(t);
    }
  }

  /**
   * @return true if we're running
   */
  public boolean getRunning() {
    return running;
  }

  /**
   * @return Spring framework application context.
   */
  public ApplicationContext getAppContext() {
    return appContext;
  }

  /**
   * @return stats for synch service bean
   */
  public List<Stat> getStats() {
    List<Stat> stats = new ArrayList<Stat>();

    stats.addAll(synchlingPool.getStats());

    return stats;
  }

  /** Stop synch process.
   *
   * @throws SynchException
   */
  public void stop() throws SynchException {
    if (stopping) {
      return;
    }

    stopping = true;

    /* Call stop on each connector
     */
    for (Connector conn: getConnectors()) {
      info("Stopping connector " + conn.getId());
      conn.stop();
    }

    info("Connectors stopped");

    long maxWait = 1000 * 90; // 90 seconds - needs to be longer than longest poll interval
    long startTime = System.currentTimeMillis();
    long delay = 1000 * 5; // 5 sec delay

    while (subs.size() > 0) {
      if ((System.currentTimeMillis() - startTime) > maxWait) {
        warn("**************************************************");
        warn("Synch shutdown completed with " +
             subs.size() + " outstanding subscriptions");
        warn("**************************************************");

        break;
      }

      info("**************************************************");
      info("Synch shutdown - " +
           subs.size() + " remaining subscriptions");
      info("**************************************************");

      try {
        wait(delay);
      } catch (InterruptedException ie) {
        maxWait = 0; // Force exit
      }
    }

    info("**************************************************");
    info("Synch shutdown complete");
    info("**************************************************");
  }

  /**
   * @return config object
   */
  public SynchConfig getConfig() {
    return config;
  }

  /**
   * @param val
   * @return decrypted string
   * @throws SynchException
   */
  public String decrypt(final String val) throws SynchException {
    try {
      return getEncrypter().decrypt(val);
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  /**
   * @return en/decryptor
   * @throws SynchException
   */
  public PwEncryptionIntf getEncrypter() throws SynchException {
    if (pwEncrypt != null) {
      return pwEncrypt;
    }

    try {
      String pwEncryptClass = "edu.rpi.cmt.security.PwEncryptionDefault";
      //String pwEncryptClass = getSysparsHandler().get().getPwEncryptClass();

      pwEncrypt = (PwEncryptionIntf)Util.getObject(pwEncryptClass,
                                                   PwEncryptionIntf.class);

      pwEncrypt.init(config.getPrivKeys(), config.getPubKeys());

      return pwEncrypt;
    } catch (Throwable t) {
      t.printStackTrace();
      throw new SynchException(t);
    }
  }

  /** Gets an instance and implants it in the subscription object.
   * @param sub
   * @param end
   * @return ConnectorInstance or throws Exception
   * @throws SynchException
   */
  public ConnectorInstance getConnectorInstance(final Subscription sub,
                                                final SynchEnd end) throws SynchException {
    ConnectorInstance cinst;
    String connectorId;

    if (end == SynchEnd.endA) {
      cinst = sub.getEndAConn();
      connectorId = sub.getEndAConnectorInfo().getConnectorId();
    } else {
      cinst = sub.getEndBConn();
      connectorId = sub.getEndBConnectorInfo().getConnectorId();
    }

    if (cinst != null) {
      return cinst;
    }


    Connector conn = getConnector(connectorId);
    if (conn == null) {
      throw new SynchException("No connector for " + sub + "(" + end + ")");
    }

    cinst = conn.getConnectorInstance(sub, end);
    if (cinst == null) {
      throw new SynchException("No connector instance for " + sub +
                               "(" + end + ")");
    }

    if (end == SynchEnd.endA) {
      sub.setEndAConn(cinst);
    } else {
      sub.setEndBConn(cinst);
    }

    return cinst;
  }

  /** Processes a batch of notifications. This must be done in a timely manner
   * as a request is usually hanging on this.
   *
   * @param notes
   * @throws SynchException
   */
  @SuppressWarnings("unchecked")
  public void handleNotifications(
            final NotificationBatch<Notification> notes) throws SynchException {
    for (Notification note: notes.getNotifications()) {
      db.open();
      try {
        Synchling sl = synchlingPool.get();

        sl.handleNotification(note);
      } finally {
        db.close();
      }
    }

    return;
  }

  /* ====================================================================
   *                        db methods
   * ==================================================================== */

  /**
   * @param id
   * @return subscription
   * @throws SynchException
   */
  public Subscription getSubscription(final String id) throws SynchException {
    db.open();
    try {
      return db.get(id);
    } finally {
      db.close();
    }
  }

  /**
   * @param sub
   * @throws SynchException
   */
  public void addSubscription(final Subscription sub) throws SynchException {
    db.add(sub);
  }

  /**
   * @param sub
   * @throws SynchException
   */
  public void updateSubscription(final Subscription sub) throws SynchException {
    db.open();
    try {
      db.update(sub);
    } finally {
      db.close();
    }
  }

  /**
   * @param sub
   * @throws SynchException
   */
  public void deleteSubscription(final Subscription sub) throws SynchException {
    db.open();
    try {
      db.delete(sub);
    } finally {
      db.close();
    }
  }

  /** Find any subscription that matches this one. There can only be one with
   * the same endpoints
   *
   * @param sub
   * @return matching subscriptions
   * @throws SynchException
   */
  public Subscription find(final Subscription sub) throws SynchException {
    db.open();
    try {
      return db.find(sub);
    } finally {
      db.close();
    }
  }

  /** Return a registered connector witht he given id.
   *
   * @param id
   * @return connector or null.
   */
  public Connector getConnector(final String id) {
    return connectorMap.get(id);
  }

  /* ====================================================================
   *                        private methods
   * ==================================================================== */

  private Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  private void trace(final String msg) {
    getLogger().debug(msg);
  }

  private void warn(final String msg) {
    getLogger().warn(msg);
  }

  private void error(final Throwable t) {
    getLogger().error(this, t);
  }

  private void info(final String msg) {
    getLogger().info(msg);
  }

  private Collection<Connector> getConnectors() {
    return connectorMap.values();
  }

  private void registerConnector(final String id,
                                 final String className) throws SynchException {
    try {
      Class cl = Class.forName(className);

      if (connectorMap.containsKey(id)) {
        throw new SynchException("Connector " + id + " already registered");
      }

      Connector c = (Connector)cl.newInstance();
      connectorMap.put(id, c);
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }
}
