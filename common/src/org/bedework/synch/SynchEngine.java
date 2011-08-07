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

import org.bedework.synch.ConnectorInstance.ItemInfo;
import org.bedework.synch.messages.FindItemsRequest;
import org.bedework.synch.messages.GetItemsRequest;
import org.bedework.synch.messages.SubscribeRequest;
import org.bedework.synch.responses.ExsynchSubscribeResponse;
import org.bedework.synch.responses.FinditemsResponse;
import org.bedework.synch.responses.FinditemsResponse.SynchInfo;
import org.bedework.synch.wsimpl.BwSynchIntfImpl;

import edu.rpi.cmt.calendar.diff.XmlIcalCompare;
import edu.rpi.cmt.timezones.Timezones;
import edu.rpi.sss.util.OptionsI;
import edu.rpi.sss.util.xml.NsContext;

import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.CalendarComponent;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import com.microsoft.schemas.exchange.services._2006.messages.ExchangeServicePortType;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeWebService;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SubscribeResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SubscribeResponseType;
import com.microsoft.schemas.exchange.services._2006.types.BaseItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ExchangeVersionType;
import com.microsoft.schemas.exchange.services._2006.types.ItemType;
import com.microsoft.schemas.exchange.services._2006.types.MailboxCultureType;
import com.microsoft.schemas.exchange.services._2006.types.RequestServerVersion;
import com.microsoft.schemas.exchange.services._2006.types.ServerVersionInfo;

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
 * then resymch the modified entity.
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

  /* If non-null this is the token we currently have for the remote service */
  private String remoteToken;

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

      remoteToken = exintf.initExchangeSynch(config, null);
      if (remoteToken == null) {
        warn("System interface returned null from init. Stopping");
        starting = false;
        return;
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

  /**
   * @throws SynchException
   */
  public void ping() throws SynchException {
    String token = exintf.initExchangeSynch(config,
                                            remoteToken);
    if (token == null) {
      warn("System interface returned null from init. Stopping");
      starting = false;
      running = false;
      stop();
    }
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

    remoteToken = null;

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
   *                        Notification methods
   * ==================================================================== */

  private void addItem(final BaseSubscription sub,
                       final NotificationItem ni) throws SynchException {
    XmlIcalConvert cnv = new XmlIcalConvert();

    CalendarItem ci = fetchItem(sub, ni.getItemId());

    if (ci == null) {
      if (debug) {
        trace("No item found");
      }

      return;
    }

    AddItemResponseType air = exintf.addItem(sub, ci.getUID(), cnv.toXml(ci));
    if (debug) {
      trace("Add: status=" + air.getStatus() +
            " msg=" + air.getMessage());
    }
  }

  private void updateItem(final BaseSubscription sub,
                          final NotificationItem ni) throws SynchException {
    CalendarItem ci = fetchItem(sub, ni.getItemId());

    if (ci == null) {
      if (debug) {
        trace("No item found");
      }

      return;
    }

    updateItem(sub, ci);
  }

  private void updateItem(final BaseSubscription sub,
                          final CalendarItem ci) throws SynchException {
    XmlIcalConvert cnv = new XmlIcalConvert();

    /* Fetch the item from the remote service */
    FetchItemResponseType fir = exintf.fetchItem(sub, ci.getUID());
    if (debug) {
      trace("fetch: status=" + fir.getStatus() +
            " msg=" + fir.getMessage());
    }

    if (fir.getStatus() != StatusType.OK) {
      return;
    }

    IcalendarType exical = cnv.toXml(ci);

    IcalendarType rmtical = fir.getIcalendar();

    /* We expect a single vcalendar for both */

    VcalendarType exvcal = get1vcal(exical);
    VcalendarType rmtvcal = get1vcal(rmtical);

    if ((exvcal == null) || (rmtvcal == null)) {
      return;
    }

    /* Build a diff list from properties and components. */

//    NsContext nsc = new NsContext("urn:ietf:params:xml:ns:icalendar-2.0");
    NsContext nsc = new NsContext(null);
    XmlIcalCompare comp = new XmlIcalCompare(nsc);

//    if (!comp.differ(excomp, rmtcomp)) {
    if (!comp.differ(exvcal, rmtvcal)) {
      return;
    }

    // Use the update list to update the remote end.

    List<XpathUpdate> updates = comp.getUpdates();

    UpdateItemResponseType uir = exintf.updateItem(sub, ci.getUID(), updates, nsc);
    if (debug) {
      trace("Update: status=" + uir.getStatus() +
            " msg=" + uir.getMessage());
    }
  }

  private VcalendarType get1vcal(final IcalendarType ical) {
    List<VcalendarType> vcals = ical.getVcalendar();

    if (vcals.size() != 1) {
      return null;
    }

    return vcals.get(0);
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

  private ExsynchSubscribeResponse doSubscription(final BaseSubscription sub) throws SynchException {
    try {
      /* Send a request for a new subscription to exchange */
      SubscribeRequest s = new SubscribeRequest(sub,
                                                config);

      s.setFolderId(sub.getExchangeCalendar());

      Holder<SubscribeResponseType> subscribeResult = new Holder<SubscribeResponseType>();

      getPort(sub).subscribe(s.getRequest(),
                             // null, // impersonation,
                             getMailboxCulture(),
                             getRequestServerVersion(),
                             subscribeResult,
                             getServerVersionInfoHolder());

      if (debug) {
        trace(subscribeResult.toString());
      }

      List<JAXBElement<? extends ResponseMessageType>> rms =
        subscribeResult.value.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

      if (rms.size() != 1) {
        //
        return null;
      }

      SubscribeResponseMessageType srm = (SubscribeResponseMessageType)rms.iterator().next().getValue();
      ExsynchSubscribeResponse esr = new ExsynchSubscribeResponse(srm);

      if (debug) {
        trace(esr.toString());
      }

      return esr;
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  private ExchangeServicePortType getPort(final BaseSubscription sub) throws SynchException {
    try {
      return getExchangeServicePort(sub.getExchangeId(),
                                    sub.getExchangePw().toCharArray()); // XXX need to en/decrypt
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  private StatusType getItems(final BaseSubscription sub) throws SynchException {
    try {
      /* Trying to use the synch approach
      // XXX Need to allow a distinguished id or a folder id
      DistinguishedFolderIdType fid = new DistinguishedFolderIdType();
      fid.setId(DistinguishedFolderIdNameType.fromValue(sub.getExchangeCalendar()));

      SyncFolderItemsRequest sfir = new SyncFolderItemsRequest(fid);

      Holder<SyncFolderItemsResponseType> syncResult = new Holder<SyncFolderItemsResponseType>();

      getPort(sub).syncFolderItems(sfir.getRequest(),
                                   getMailboxCulture(),
                                   // null, // impersonation,
                                   getRequestServerVersion(),
                                   syncResult,
                                   getServerVersionInfoHolder());

      List<JAXBElement<? extends ResponseMessageType>> rms =
        syncResult.value.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

      for (JAXBElement<? extends ResponseMessageType> jaxbrm: rms) {
        SyncFolderItemsResponseMessageType srm = (SyncFolderItemsResponseMessageType)jaxbrm.getValue();

        SyncFolderitemsResponse sfr = new SyncFolderitemsResponse(srm);
      }
      */

      /* ===================================================================
       * Use FindItem to get list of ids to fetch from Exchange.
       * (Misnamed - it fetches multiple items)
       * =================================================================== */
      DistinguishedFolderIdType fid = new DistinguishedFolderIdType();
      fid.setId(DistinguishedFolderIdNameType.fromValue(sub.getExchangeCalendar()));
      FindItemsRequest fir = FindItemsRequest.getSynchInfo(fid);

      Holder<FindItemResponseType> fiResult = new Holder<FindItemResponseType>();

      getPort(sub).findItem(fir.getRequest(),
                            // null, // impersonation,
                            getMailboxCulture(),
                            getRequestServerVersion(),
                            // null, // timeZoneContext
                            fiResult,
                            getServerVersionInfoHolder());

      List<JAXBElement<? extends ResponseMessageType>> rms =
        fiResult.value.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

      /* The action here depends on which way we are synching. Below we refer
       * to Exchange events. These are signified by particular X-properties we
       * added to the event.
       *
       * For Exchange to Remote
       * If the item does not exist on the remote system then add it.
       * If the lastmod on the remote is prior to the exchange one - update.
       * Otherwise ignore.
       * We should remove all exchange created remote events that have no
       * counterpart on Exchange.
       *
       * For Remote to Exchange
       * Just the reverse of the above.
       *
       * For both ways:
       * One end may the master.
       * A non-exchange event on the remote is copied into exchange (at which
       * point it might become an exchange event)
       * An exchange event not on the remote is copied on to the remote.
       * One on both which differs is copied from the master end if one is
       * nominated, or we try to take the latest.
       */

      // XXX just do Exchange to remote for the moment

      /* ===================================================================
       * Get the list of items that already exist in the remote calendar
       * collection
       * =================================================================== */
      List<ItemInfo> iis = exintf.getItemsInfo(sub);
      if (iis == null) {
        throw new SynchException("Unable to fetch SynchInfo");
      }

      /* Items is a table built from the remote calendar */
      Map<String, ItemInfo> items = new HashMap<String, ItemInfo>();

      /* sinfos provides state information about each item */
      Map<String, SynchInfo> sinfos = new HashMap<String, SynchInfo>();

      for (ItemInfo ii: iis) {
        if (debug) {
          trace(ii.toString());
        }
        ii.seen = false;
        items.put(ii.uid, ii);
      }

      for (JAXBElement<? extends ResponseMessageType> jaxbrm: rms) {
        FindItemResponseMessageType firm = (FindItemResponseMessageType)jaxbrm.getValue();

        FinditemsResponse resp = new FinditemsResponse(firm,
                                                       true);

        if (debug) {
          trace(resp.toString());
        }

        List<BaseItemIdType> toFetch = new ArrayList<BaseItemIdType>();

        for (SynchInfo si: resp.getSynchInfo()) {
          ItemInfo ii = items.get(si.uid);
          sinfos.put(si.uid, si);

          if (ii == null) {
            if (debug) {
              trace("Need to add to remote: uid:" + si.uid);
            }

            si.addToRemote = true;
            toFetch.add(si.itemId);
          } else {
            int cmp = cmpLastMods(ii.lastMod, si.lastMod);
            if (cmp < 0) {
              if (debug) {
                trace("Need to update remote: uid:" + si.uid);
              }

              si.updateRemote = true;
              toFetch.add(si.itemId);
            }
          }

          if (toFetch.size() > getItemsBatchSize) {
            // Fetch this batch of items and process them.
            updateRemote(sub,
                         fetchItems(sub, toFetch),
                         sinfos);
            toFetch.clear();
          }
        }

        if (toFetch.size() > 0) {
          // Fetch the remaining items and process them.
          updateRemote(sub,
                       fetchItems(sub, toFetch),
                       sinfos);
          toFetch.clear();
        }

      }

      return StatusType.OK;
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  private void updateRemote(final BaseSubscription sub,
                            final List<CalendarItem> cis,
                            final Map<String, SynchInfo> sinfos) throws SynchException {
    XmlIcalConvert cnv = new XmlIcalConvert();

    for (CalendarItem ci: cis) {
      SynchInfo si = sinfos.get(ci.getUID());

      if (si == null) {
        continue;
      }

      if (si.addToRemote) {
        AddItemResponseType air = exintf.addItem(sub, ci.getUID(), cnv.toXml(ci));
        if (debug) {
          trace("Add: status=" + air.getStatus() +
                " msg=" + air.getMessage());
        }

        continue;
      }

      // Update the far end.
      updateItem(sub, ci);
    }
  }

  private CalendarItem fetchItem(final BaseSubscription sub,
                                 final BaseItemIdType id) throws SynchException {
    List<BaseItemIdType> toFetch = new ArrayList<BaseItemIdType>();

    toFetch.add(id);

    List<CalendarItem> items = fetchItems(sub, toFetch);

    if (items.size() != 1) {
      return null;
    }

    return items.get(0);
  }

  private List<CalendarItem> fetchItems(final BaseSubscription sub,
                                        final List<BaseItemIdType> toFetch) throws SynchException {
    GetItemsRequest gir = new GetItemsRequest(toFetch);

    Holder<GetItemResponseType> giResult = new Holder<GetItemResponseType>();

    getPort(sub).getItem(gir.getRequest(),
                         // null, // impersonation,
                         getMailboxCulture(),
                         getRequestServerVersion(),
                         // null, // timeZoneContext
                         giResult,
                         getServerVersionInfoHolder());

    List<JAXBElement<? extends ResponseMessageType>> girms =
      giResult.value.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

    List<CalendarItem> items = new ArrayList<CalendarItem>();

    for (JAXBElement<? extends ResponseMessageType> jaxbgirm: girms) {
      Object o = jaxbgirm.getValue();

      if (!(o instanceof ItemInfoResponseMessageType)) {
        continue;
      }

      ItemInfoResponseMessageType iirm = (ItemInfoResponseMessageType)o;

      if (iirm.getItems() == null) {
        continue;
      }

      for (ItemType item: iirm.getItems().getItemOrMessageOrCalendarItem()) {
        if (!(item instanceof CalendarItemType)) {
          continue;
        }

        CalendarItem ci = new CalendarItem((CalendarItemType)item);
        if (debug) {
          CalendarComponent comp = ci.toComp();

          trace(comp.toString());
        }

        items.add(ci);
      }
    }

    return items;
  }

  private int cmpLastMods(final String calLmod, final String exLmod) {
    int exi = 0;
    if (calLmod == null) {
      return -1;
    }

    for (int i = 0; i < 16; i++) {
      char cal = calLmod.charAt(i);
      char ex = exLmod.charAt(exi);

      if (cal < ex) {
        return -1;
      }

      if (cal > ex) {
        return 1;
      }

      exi++;

      // yyyy-mm-ddThh:mm:ssZ
      //     4  7     12 15
      if ((exi == 4) || (exi == 7) || (exi == 12) || (exi == 15)) {
        exi++;
      }
    }

    return 0;
  }

  private boolean checkAccess(final BaseSubscription sub) throws SynchException {
    /* Does this principal have the rights to (un)subscribe? */
    return true;
  }

  private MailboxCultureType getMailboxCulture() {
    MailboxCultureType mbc = new MailboxCultureType();

    mbc.setValue("en-US"); // XXX This probably needs to come from the locale

    return mbc;
  }

  private Holder<ServerVersionInfo> getServerVersionInfoHolder() {
    ServerVersionInfo serverVersionInfo = new ServerVersionInfo();
    Holder<ServerVersionInfo> serverVersion = new Holder<ServerVersionInfo>(serverVersionInfo);

    return serverVersion;
  }

  private RequestServerVersion getRequestServerVersion() {
    RequestServerVersion requestVersion = new RequestServerVersion();

    requestVersion.setVersion(ExchangeVersionType.EXCHANGE_2010);

    return requestVersion;
  }

  private ExchangeServicePortType getExchangeServicePort(final String user,
                                                         final char[] pw) throws SynchException {
    try {
      URL wsdlURL = new URL(config.getExchangeWSDLURI());

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(
                user,
                pw);
        }
    });

      ExchangeWebService ews =
        new ExchangeWebService(wsdlURL,
                               new QName("http://schemas.microsoft.com/exchange/services/2006/messages",
                                         "ExchangeWebService"));
      ExchangeServicePortType port = ews.getExchangeWebPort();

//      Map<String, Object> context = ((BindingProvider)port).getRequestContext();

  //    context.put(BindingProvider.USERNAME_PROPERTY, user);
    //  context.put(BindingProvider.PASSWORD_PROPERTY, new String(pw));

      /*
        $client->__setSoapHeaders(
        new SOAPHeader('http://schemas.microsoft.com/exchange/services/2006/types',
        'RequestServerVersion',
        array("Version"=>"Exchange2007_SP1"))
        );

        $client is the SoapClient Instance.

      */


      return port;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
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
