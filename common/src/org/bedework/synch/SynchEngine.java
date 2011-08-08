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

import org.bedework.synch.cnctrs.exchange.ExsynchSubscribeResponse;
import org.bedework.synch.cnctrs.exchange.Notification;
import org.bedework.synch.cnctrs.exchange.Notification.NotificationItem;
import org.bedework.synch.wsimpl.BwSynchIntfImpl;

import edu.rpi.cmt.timezones.Timezones;
import edu.rpi.sss.util.OptionsI;

import net.fortuna.ical4j.model.TimeZone;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;

import java.util.ArrayList;
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
 * <p>Shutdown ought to wait for the remote system to ping us for every outstanding
 * subscription. That ought to be fairly quick.
 *
 * @author Mike Douglass
 */
public class SynchEngine {
  private final boolean debug;

  private static String appname = "Exsynch";

  protected transient Logger log;

  /* Map of currently active subscriptions - that is - we have traffic between
   * local and remote systems.
   */
  private final Map<String, BaseSubscription> subs =
    new HashMap<String, BaseSubscription>();

  private final ConnectorInstance exintf;

  private boolean starting;

  private boolean running;

  private boolean stopping;

  private ExsynchConfig config;

  /* Max number of items we fetch at a time */
  private final int getItemsBatchSize = 20;

  private static Object getSyncherLock = new Object();

  private static SynchEngine syncher;

  private Timezones timezones;

  /* Where we keep subscriptions that come in while we are starting */
  private List<BaseSubscription> subsList;

  private SynchDb db;

  /* Calls back from the remote system have resource uris tha are prefixed with
   * this.
   */
  private final String remoteCallbackPathPrefix = "rmtcb/";

  /** Constructor
   *
   * @param exintf
   */
  private SynchEngine(final ConnectorInstance exintf) throws SynchException {
    this.exintf = exintf;
    debug = getLogger().isDebugEnabled();

    System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump",
                       String.valueOf(debug));

    /* Note that the options factory returns a static object and we should
     * initialise the config once only
     */
    OptionsI opts;
    try {
      opts = SynchOptionsFactory.getOptions();
      config = (ExsynchConfig)opts.getAppProperty(appname);
      if (config == null) {
        config = new ExsynchConfig();
      }

      String tzserverUri = opts.getGlobalStringProperty("timezonesUri");

      if (tzserverUri == null) {
        throw new SynchException("No timezones server URI defined");
      }

      Timezones.initTimezones(tzserverUri);
      timezones = Timezones.getTimezones();
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  /**
   * @return the syncher
   * @throws SynchException
   */
  public static SynchEngine getSyncher() throws SynchException {
    // This needs to use config
    if (syncher != null) {
      return syncher;
    }

    synchronized (getSyncherLock) {
      if (syncher != null) {
        return syncher;
      }
      syncher = new SynchEngine(new BwSynchIntfImpl());
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
      info(" Exchange WSDL URI: " + config.getExchangeWSDLURI());
      info("      callback URI: " + config.getExchangeWsPushURI());
      info("**************************************************");

      if (config.getKeystore() != null) {
        System.setProperty("javax.net.ssl.trustStore", config.getKeystore());
        System.setProperty("javax.net.ssl.trustStorePassword", "bedework");
      }

      /* Get the list of subscriptions from our database and process them.
       * While starting, new subscribe requests get added to the list.
       */

      db = new SynchDb();

      try {
        db.open();
        List<BaseSubscription> startList = db.getAll();
        db.close();

        startup:
        while (starting) {
          if (debug) {
            trace("startList has " + startList.size() + " subscriptions");
          }

          for (BaseSubscription es: startList) {
            if (stopping) {
              break startup;
            }
            if (subscribe(es) != StatusType.OK) {
              // XXX We need to save this subscription somewhere and retry later.
              // Alternatively set its state and retrieve all unstarted for retry.
            }
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

  /** Stop synch process.
   *
   * @throws SynchException
   */
  public void stop() throws SynchException {
    if (stopping) {
      return;
    }

    stopping = true;

    long maxWait = 1000 * 90; // 90 seconds - needs to be longer than longest poll interval
    long startTime = System.currentTimeMillis();
    long delay = 1000 * 5; // 5 sec delay

    while (subs.size() > 0) {
      if ((System.currentTimeMillis() - startTime) > maxWait) {
        warn("**************************************************");
        warn("Exchange synch shutdown completed with " +
             subs.size() + " outstanding subscriptions");
        warn("**************************************************");

        break;
      }

      info("**************************************************");
      info("Exchange synch shutdown - " +
           subs.size() + " remaining subscriptions");
      info("**************************************************");

      try {
        wait(delay);
      } catch (InterruptedException ie) {
        maxWait = 0; // Force exit
      }
    }

    info("**************************************************");
    info("Exchange synch shutdown complete");
    info("**************************************************");
  }

  /**
   * @return config object
   */
  public ExsynchConfig getConfig() {
    return config;
  }

  /** Calls back from the remote system have resource uris that are prefixed with
   * this.
   *
   * @return prefix
   */
  public String getRemoteCallbackPathPrefix() {
    return remoteCallbackPathPrefix;
  }

  /** Update or add a subscription.
   *
   * @param sub
   * @return status
   * @throws SynchException
   */
  public StatusType subscribeRequest(final BaseSubscription sub) throws SynchException {
    if (debug) {
      trace("new subscription " + sub);
    }

    synchronized (this) {
      if (starting) {
        if (subsList == null) {
          subsList = new ArrayList<BaseSubscription>();
        }

        subsList.add(sub);
        return StatusType.OK;
      }
    }

    if (!running) {
      return StatusType.SERVICE_STOPPED;
    }

    return subscribe(sub);
  }

  /**
   * @param sub
   * @param note
   * @throws SynchException
   */
  public void handleNotification(final BaseSubscription sub,
                                 final Notification note) throws SynchException {
    for (NotificationItem ni: note.getNotifications()) {
      if (ni.getItemId() == null) {
        // Folder changes as well as item.
        continue;
      }

      switch (ni.getAction()) {
      case CopiedEvent:
        break;
      case CreatedEvent:
        addItem(sub, ni);
        break;
      case DeletedEvent:
        break;
      case ModifiedEvent:
        updateItem(sub, ni);
        break;
      case MovedEvent:
        break;
      case NewMailEvent:
        break;
      case StatusEvent:
        break;
      }
    }
  }

  /* ====================================================================
   *                        db methods
   * ==================================================================== */

  /**
   * @param id
   * @return subscription
   * @throws SynchException
   */
  public BaseSubscription getSubscription(final String id) throws SynchException {
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
  public void updateSubscription(final BaseSubscription sub) throws SynchException {
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
  public void deleteSubscription(final BaseSubscription sub) throws SynchException {
    db.open();
    try {
      db.delete(sub);
    } finally {
      db.close();
    }
  }

  /** Find subscriptions that match the end points.
   *
   * @param calPath - remote calendar
   * @param exCal - Exchange calendar
   * @param exId - Exchange principal
   * @return matching subscriptions
   * @throws SynchException
   */
  public List<BaseSubscription> find(final String calPath,
                                         final String exCal,
                                         final String exId) throws SynchException {
    db.open();
    try {
      return db.find(calPath, exCal, exId);
    } finally {
      db.close();
    }
  }

  /* ====================================================================
   *                        private methods
   * ==================================================================== */

  private StatusType subscribe(final BaseSubscription sub) throws SynchException {
    if (debug) {
      trace("Handle subscription " + sub);
    }

    if (!checkAccess(sub)) {
      info("No access for subscription " + sub);
      return StatusType.NO_ACCESS;
    }

    synchronized (subs) {
      BaseSubscription tsub = subs.get(sub.getCalPath());

      boolean synchThis = sub.getExchangeWatermark() == null;

      if (tsub != null) {
        return StatusType.ALREADY_SUBSCRIBED;
      }

      ExsynchSubscribeResponse esr = doSubscription(sub);

      if ((esr == null) | !esr.getValid()) {
        return StatusType.ERROR;
      }

      subs.put(sub.getCalPath(), sub);

      if (synchThis) {
        // New subscription - sync
        getItems(sub);
      }
    }

    return StatusType.OK;
  }

  /**
   * @param sub
   * @return status
   * @throws SynchException
   */
  public StatusType unsubscribe(final BaseSubscription sub) throws SynchException {
    if (!checkAccess(sub)) {
      info("No access for subscription " + sub);
      return StatusType.NO_ACCESS;
    }

    synchronized (subs) {
      BaseSubscription tsub = subs.get(sub.getCalPath());

      if (tsub == null) {
        // Nothing active
        if (debug) {
          trace("Nothing active for " + sub);
        }
      } else {
        // Unsubscribe request - set subscribed off and next callback will unsubscribe
        tsub.setOutstandingSubscription(null);
        tsub.setSubscribe(false);
      }

      deleteSubscription(sub);
    }

    return StatusType.OK;
  }

  private boolean checkAccess(final BaseSubscription sub) throws SynchException {
    /* Does this principal have the rights to (un)subscribe? */
    return true;
  }

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
}
