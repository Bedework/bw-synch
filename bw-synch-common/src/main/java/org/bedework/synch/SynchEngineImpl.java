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

import org.bedework.synch.conf.SynchConfig;
import org.bedework.synch.db.SynchDb;
import org.bedework.synch.shared.Notification;
import org.bedework.synch.shared.Stat;
import org.bedework.synch.shared.StatLong;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.cnctrs.Connector;
import org.bedework.synch.shared.cnctrs.Connector.NotificationBatch;
import org.bedework.synch.shared.cnctrs.ConnectorInstance;
import org.bedework.synch.shared.conf.ConnectorConfig;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.shared.service.SynchConnConf;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.util.calendar.XcalUtil.TzGetter;
import org.bedework.util.jmx.ConfigHolder;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;
import org.bedework.util.security.PwEncryptionIntf;
import org.bedework.util.timezones.Timezones;
import org.bedework.util.timezones.TimezonesImpl;

import net.fortuna.ical4j.model.TimeZone;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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
public class SynchEngineImpl
        implements Logged, SynchEngine, TzGetter {
  //private static String appname = "Synch";
  static ConfigHolder<SynchConfig> cfgHolder;

  private transient PwEncryptionIntf pwEncrypt;

  /* Map of currently active notification subscriptions. These are subscriptions
   * for which we get change messages from the remote system(s).
   */
  private final Map<String, Subscription> activeSubs =
      new HashMap<>();

  private boolean starting;

  private boolean running;

  private boolean stopping;

  //private Configurator config;

  private static final Object getSyncherLock = new Object();

  private static SynchEngine syncher;

  private Timezones timezones;

  static TzGetter tzgetter;

  private SynchlingPool synchlingPool;

  private SynchTimer synchTimer;

  private BlockingQueue<Notification> notificationInQueue;

  /* Where we keep subscriptions that come in while we are starting */
  private List<Subscription> subsList;

  private SynchDb db;

  private final Map<String, Connector> connectorMap = new HashMap<>();

  /* Some counts */

  private final StatLong notificationsCt =
          new StatLong("notifications");

  private final StatLong notificationsAddWt =
          new StatLong("notifications add wait");

  /** This process handles startup notifications and (un)subscriptions.
   *
   */
  private class NotificationInThread extends Thread {
    long lastTrace;

    /**
     */
    public NotificationInThread() {
      super("NotifyIn");
    }

    @Override
    public void run() {
      while (true) {
        if (debug()) {
          debug("About to wait for notification");
        }

        try {
          final Notification<?> note = notificationInQueue.take();
          if (note == null) {
            continue;
          }

          if (debug()) {
            debug("Received notification");
          }

          if ((note.getSub() != null) && note.getSub().getDeleted()) {
            // Drop it

            if (debug()) {
              debug("Dropping deleted notification");
            }

            continue;
          }

          notificationsCt.inc();
          Synchling sl = null;

          try {
            /* Get a synchling from the pool */
            while (true) {
              if (stopping) {
                return;
              }

              sl = synchlingPool.getNoException();
              if (sl != null) {
                break;
              }
            }

            /* The synchling needs to be running it's own thread. */
            final StatusType st = handleNotification(sl, note);

            if (st == StatusType.WARNING) {
              /* Back on the queue - these need to be flagged so we don't get an
               * endless loop - perhaps we need a delay queue
               */

              notificationInQueue.put(note);
            }
          } finally {
            synchlingPool.add(sl);
          }

          /* If this is a poll kind then we should add it to a poll queue
           */
          // XXX Add it to poll queue
        } catch (final InterruptedException ie) {
          warn("Notification handler shutting down");
          break;
        } catch (final Throwable t) {
          if (debug()) {
            error(t);
          } else {
            // Try not to flood the log with error traces
            final long now = System.currentTimeMillis();
            if ((now - lastTrace) > (30 * 1000)) {
              error(t);
              lastTrace = now;
            } else if (t.getMessage() == null) {
              error(t);
            } else {
              error(t.getMessage());
            }
          }
        }
      }
    }
  }

  private static NotificationInThread notifyInHandler;

  /** Constructor
   *
   */
  private SynchEngineImpl() {
    System.setProperty(
            "com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump",
            String.valueOf(debug()));
    System.setProperty(
            "net.fortuna.ical4j.timezone.cache.impl",
            "net.fortuna.ical4j.util.MapTimeZoneCache");
  }

  /**
   * @return the syncher
   */
  public static SynchEngine getSyncher() {
    if (syncher != null) {
      return syncher;
    }

    synchronized (getSyncherLock) {
      if (syncher != null) {
        return syncher;
      }
      syncher = new SynchEngineImpl();
      return syncher;
    }
  }

  @Override
  public Timezones getTimezones() {
    return timezones;
  }

  @Override
  public boolean subscriptionsOnly() {
    return getConfig().getSubscriptionsOnly();
  }

  @Override
  public void handleNotification(final Notification<?> note) {
    try {
      while (true) {
        if (stopping) {
          return;
        }

        if (notificationInQueue.offer(note, 5, TimeUnit.SECONDS)) {
          break;
        }
      }
    } catch (final InterruptedException ignored) {
    }
  }

  @Override
  public void setConnectors(final Subscription sub) throws SynchException {
    String connectorId = sub.getEndAConnectorInfo().getConnectorId();

    Connector<?, ?, ?> conn = getConnector(connectorId);
    if (conn == null) {
      throw new SynchException("No connector for " + sub + "(" +
                                       SynchEndType.A + ")");
    }

    sub.setEndAConn(conn);

    connectorId = sub.getEndBConnectorInfo().getConnectorId();

    conn = getConnector(connectorId);
    if (conn == null) {
      throw new SynchException("No connector for " + sub + "(" +
                                       SynchEndType.B + ")");
    }

    sub.setEndBConn(conn);
  }

  @Override
  public void rescheduleNow(final String id) throws SynchException {
    if (debug()) {
      debug("reschedule now for subscription id " + id);
    }

    final Subscription sub = getSubscription(id);

    if (sub == null) {
      if (debug()) {
        debug("No subscription");
      }
      return;
    }

    setConnectors(sub);

    sub.setErrorCt(0);
    synchTimer.schedule(sub, new Date());
  }

  @Override
  public void reschedule(final Subscription sub,
                         final boolean newSub) {
    if (debug()) {
      debug("reschedule subscription " + sub);
    }

    if (sub.polling()) {
      Date when = null;
      try {
        when = sub.nextRefresh();
      } catch (final Throwable t) {
        error(t);
      }
      synchTimer.schedule(sub, when);
      return;
    }

    // XXX start up the add to active subs

    activeSubs.put(sub.getSubscriptionId(), sub);
  }

  @Override
  public ConnectorInstance getConnectorInstance(final Subscription sub,
                                                final SynchEndType end) throws SynchException {
    ConnectorInstance cinst;
    final Connector conn;

    if (end == SynchEndType.A) {
      cinst = sub.getEndAConnInst();
      conn = sub.getEndAConn();
    } else {
      cinst = sub.getEndBConnInst();
      conn = sub.getEndBConn();
    }

    if (cinst != null) {
      return cinst;
    }

    if (conn == null) {
      throw new SynchException("No connector for " + sub + "(" + end + ")");
    }

    cinst = conn.getConnectorInstance(sub, end);
    if (cinst == null) {
      throw new SynchException("No connector instance for " + sub +
                                       "(" + end + ")");
    }

    if (end == SynchEndType.A) {
      sub.setEndAConnInst(cinst);
    } else {
      sub.setEndBConnInst(cinst);
    }

    return cinst;
  }

  @Override
  public void addSubscription(final Subscription sub) throws SynchException {
    db.add(sub);
    sub.resetChanged();
  }

  @Override
  public void deleteSubscription(final Subscription sub) throws SynchException {
    db.delete(sub);
  }

  @Override
  public void updateSubscription(final Subscription sub) throws SynchException {
    final boolean opened = db.open();

    try {
      db.update(sub);
      sub.resetChanged();
    } finally {
      if (opened) {
        // It's a one-shot
        db.close();
      }
    }
  }

  @Override
  public Subscription getSubscription(final String id) throws SynchException {
    final boolean opened = db.open();

    try {
      return db.get(id);
    } finally {
      if (opened) {
        // It's a one-shot
        db.close();
      }
    }
  }

  @Override
  public Subscription find(final Subscription sub) throws SynchException {
    final boolean opened = db.open();

    try {
      return db.find(sub);
    } finally {
      if (opened) {
        // It's a one-shot
        db.close();
      }
    }
  }

  @Override
  public Connector getConnector(final String id) {
    return connectorMap.get(id);
  }

  @Override
  public Set<String> getConnectorIds() {
    return connectorMap.keySet();
  }

  @Override
  public void handleNotifications(
          final NotificationBatch<Notification> notes) throws SynchException {
    for (final Notification note: notes.getNotifications()) {
      db.open();
      Synchling sl = null;

      try {
        if (note.getSub() != null) {
          sl = synchlingPool.get();

          handleNotification(sl, note);
        }
      } finally {
        db.close();
        if (sl != null) {
          synchlingPool.add(sl);
        }
      }
    }
  }

  @Override
  public TzGetter getTzGetter() {
    return tzgetter;
  }

  @Override
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

      final var cfg = getConfig();

      db = new SynchDb(cfg);

      if (cfg.getTimezonesURI() == null) {
        throw new SynchException(
                "Timezones URI must be set in configuration");
      }

      timezones = new TimezonesImpl();
      timezones.init(cfg.getTimezonesURI());

      tzgetter = this;

      synchlingPool = new SynchlingPool();
      synchlingPool.start(this,
                          cfg.getSynchlingPoolSize(),
                          cfg.getSynchlingPoolTimeout());

      notificationInQueue = new ArrayBlockingQueue<>(100);

      info("**************************************************");
      info("Starting synch");
      info("      callback URI: " + cfg.getCallbackURI());
      info("**************************************************");

      if (cfg.getKeystore() != null) {
        System.setProperty("javax.net.ssl.trustStore", cfg.getKeystore());
        System.setProperty("javax.net.ssl.trustStorePassword", "bedework");
      }

      final List<SynchConnConf<?>> connectorConfs =
              getConfig().getConnectorConfs();
      final String callbackUriBase = getConfig().getCallbackURI();

      /* Register the connectors and start them */
      for (final SynchConnConf<?> scc: connectorConfs) {
        final ConnectorConfig conf = scc.getConfig();
        final String cnctrId = conf.getName();
        info("Register and start connector " + cnctrId);

        registerConnector(cnctrId, conf);

        final var conn = getConnector(cnctrId);
        scc.setConnector(conn);

        conn.start(cnctrId,
                   conf,
                   callbackUriBase + cnctrId + "/",
                   this);

        while (!conn.isStarted()) {
          /* Wait for it to start */
          synchronized (this) {
            this.wait(250);
          }

          if (conn.isFailed()) {
            error("Connector " + cnctrId + " failed to start");
            break;
          }
        }
      }

      synchTimer = new SynchTimer(this);

      /* Get the list of subscriptions from our database and process them.
       * While starting, new subscribe requests get added to the list.
       */

      notifyInHandler = new NotificationInThread();
      notifyInHandler.start();

      try {
        db.open();
        List<Subscription> startList = db.getAll();
        db.close();

        startup:
        while (starting) {
          if (debug()) {
            debug("startList has " + startList.size() + " subscriptions");
          }

          for (final Subscription sub: startList) {
            setConnectors(sub);

            reschedule(sub, false);
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
    } catch (final SynchException se) {
      error(se);
      starting = false;
      running = false;
      throw se;
    } catch (final Throwable t) {
      error(t);
      starting = false;
      running = false;
      throw new SynchException(t);
    }
  }

  @Override
  public List<Stat> getStats() {
    final List<Stat> stats = new ArrayList<>();

    stats.addAll(synchlingPool.getStats());
    stats.addAll(synchTimer.getStats());
    stats.add(notificationsCt);
    stats.add(notificationsAddWt);

    return stats;
  }

  @Override
  public void stop() {
    if (stopping) {
      return;
    }

    stopping = true;

    /* Call stop on each connector
     */
    for (final Connector conn: getConnectors()) {
      info("Stopping connector " + conn.getId());
      try {
        conn.stop();
      } catch (final Throwable t) {
        if (debug()) {
          error(t);
        } else if (t.getMessage() == null) {
          error(t);
        } else {
          error(t.getMessage());
        }
      }
    }

    info("Connectors stopped");

    if (synchlingPool != null) {
      synchlingPool.stop();
    }

    syncher = null;

    info("**************************************************");
    info("Synch shutdown complete");
    info("**************************************************");
  }

  /* ------ */

  /**
   * @return true if we're running
   */
  public boolean getRunning() {
    return running;
  }

  /**
   * @param val
   */
  public static void setConfigHolder(final ConfigHolder<SynchConfig> val) {
    cfgHolder = val;
  }

  /**
   * @return current state of the configuration
   */
  public static SynchConfig getConfig() {
    if (cfgHolder == null) {
      throw new RuntimeException("No configuration available");
    }

    return cfgHolder.getConfig();
  }

  /**
   */
  public void updateConfig() {
    if (cfgHolder != null) {
      cfgHolder.putConfig();
    }
  }

  /** Get a timezone object given the id. This will return transient objects
   * registered in the timezone directory
   *
   * @param id
   * @return TimeZone with id or null
   * @throws Throwable
   */
  @Override
  public TimeZone getTz(final String id) throws Throwable {
    return getSyncher().getTimezones().getTimeZone(id);
  }

  /**
   * @param val possibly null password
   * @return decrypted string
   * @throws SynchException on decryption failure
   */
  public String decrypt(final String val) throws SynchException {
    if (val == null) {
      return null;
    }

    try {
      return getEncrypter().decrypt(val);
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
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
      final String pwEncryptClass =
              "org.bedework.util.security.PwEncryptionDefault";
      //String pwEncryptClass = getSysparsHandler().get().getPwEncryptClass();

      pwEncrypt = (PwEncryptionIntf)Util.getObject(pwEncryptClass,
                                                   PwEncryptionIntf.class);

      pwEncrypt.init(getConfig().getPrivKeys(),
                     getConfig().getPubKeys());

      return pwEncrypt;
    } catch (final Throwable t) {
      t.printStackTrace();
      throw new SynchException(t);
    }
  }

  private Collection<Connector> getConnectors() {
    return connectorMap.values();
  }

  private void registerConnector(final String id,
                                 final ConnectorConfig conf) throws SynchException {
    try {
      final Class<?> cl = Class.forName(conf.getConnectorClassName());

      if (connectorMap.containsKey(id)) {
        throw new SynchException("Connector " + id + " already registered");
      }

      final Connector c = (Connector)cl.newInstance();
      connectorMap.put(id, c);
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  @SuppressWarnings("unchecked")
  private StatusType handleNotification(final Synchling sl,
                                        final Notification note) throws SynchException {
    final StatusType st = sl.handleNotification(note);

    final Subscription sub = note.getSub();
    if (sub.getDeleted() || !sub.getMissingTarget()) {
      return st;
    }

    if (sub.getErrorCt() > getConfig().getMissingTargetRetries()) {
      deleteSubscription(sub);
      info("Subscription deleted after missing target retries exhausted: " + sub);
    }

    return st;
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
