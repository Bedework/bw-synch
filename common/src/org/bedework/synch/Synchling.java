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
import org.bedework.synch.Notification.NotificationItem;
import org.bedework.synch.cnctrs.exchange.XmlIcalConvert;
import org.bedework.synch.cnctrs.exchange.responses.ExsynchSubscribeResponse;

import edu.rpi.cmt.calendar.diff.XmlIcalCompare;
import edu.rpi.cmt.timezones.Timezones;
import edu.rpi.sss.util.xml.NsContext;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The synchling handles the processing of a single subscription when there is
 * some activity.
 *
 * <p>A synchling may be started to process a subscription as a the result of a
 * callback notification for example from exchange or because a synch period has
 * elapsed and it's time to refresh.
 *
 * @author Mike Douglass
 */
public class Synchling {
  private final boolean debug;

  protected transient Logger log;

  private final ConnectorInstance localCnctr;

  private final ConnectorInstance remoteCnctr;

  private SynchConfig config;

  /* Max number of items we fetch at a time */
  private final int getItemsBatchSize = 20;

  private Timezones timezones;

  /* Where we keep subscriptions that come in while we are starting */
  private List<Subscription> subsList;

  private SynchDb db;

  /** Constructor
   *
   * @param exintf
   */
  private Synchling() throws SynchException {
    debug = getLogger().isDebugEnabled();

  }

  /** Update or add a subscription.
   *
   * @param sub
   * @return status
   * @throws SynchException
   */
  public StatusType subscribeRequest(final Subscription sub) throws SynchException {
    if (debug) {
      trace("new subscription " + sub);
    }

    synchronized (this) {
      if (starting) {
        if (subsList == null) {
          subsList = new ArrayList<Subscription>();
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
  public void handleNotification(final Subscription sub,
                                 final Notification<NotificationItem> note) throws SynchException {
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

  private void addItem(final Subscription sub,
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

  private void updateItem(final Subscription sub,
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

  private void updateItem(final Subscription sub,
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
   *                        private methods
   * ==================================================================== */

  private StatusType subscribe(final Subscription sub) throws SynchException {
    if (debug) {
      trace("Handle subscription " + sub);
    }

    if (!checkAccess(sub)) {
      info("No access for subscription " + sub);
      return StatusType.NO_ACCESS;
    }

    synchronized (subs) {
      Subscription tsub = subs.get(sub.getCalPath());

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
  public StatusType unsubscribe(final Subscription sub) throws SynchException {
    if (!checkAccess(sub)) {
      info("No access for subscription " + sub);
      return StatusType.NO_ACCESS;
    }

    synchronized (subs) {
      Subscription tsub = subs.get(sub.getCalPath());

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

  private static class SynchInfo {
    /** */
    public ItemInfo itemInfo;

    /* Fields set during the actual synch process */

    /** item needs to be added to remote */
    public boolean addToRemote;

    /** item needs to update remote */
    public boolean updateRemote;

    /** item needs to be added to local */
    public boolean addToLocal;

    /** item needs to update local */
    public boolean updateLocal;

    /** Constructor
     *
     * @param itemInfo
     */
    public SynchInfo(final ItemInfo itemInfo) {
      this.itemInfo = itemInfo;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("SynchInfo{");

      sb.append(itemInfo);

      sb.append("}");

      return sb.toString();
    }
  }

  private StatusType getItems(final Subscription sub) throws SynchException {
    try {
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
       * Get the list of items that already exist in the local and remote
       * calendar collections
       * =================================================================== */
      List<ItemInfo> liis = localCnctr.getItemsInfo();
      if (liis == null) {
        throw new SynchException("Unable to fetch local items info");
      }

      List<ItemInfo> riis = remoteCnctr.getItemsInfo();
      if (riis == null) {
        throw new SynchException("Unable to fetch remote items info");
      }

      /* Items is a table built from the remote calendar */
      Map<String, ItemInfo> items = new HashMap<String, ItemInfo>();

      /* sinfos provides state information about each item */
      Map<String, SynchInfo> sinfos = new HashMap<String, SynchInfo>();

      for (ItemInfo ii: riis) {
        if (debug) {
          trace(ii.toString());
        }
        ii.seen = false;
        items.put(ii.uid, ii);
      }

      List<SynchInfo> toFetch = new ArrayList<SynchInfo>();

      for (ItemInfo lii: liis) {
        ItemInfo rii = items.get(lii.uid);

        if (rii == null) {
          if (debug) {
            trace("Need to add to remote: uid:" + lii.uid);
          }

          SynchInfo si = new SynchInfo(lii);
          si.addToRemote = true;
          toFetch.add(si);
        } else {
          rii.seen = true;

          int cmp = cmpLastMods(rii.lastMod, lii.lastMod);
          if (cmp != 0) {
            SynchInfo si = new SynchInfo(lii);

            if (cmp < 0) {
              if (debug) {
                trace("Need to update remote: uid:" + lii.uid);
              }
              si.updateRemote = true;
            } else {
              if (debug) {
                trace("Need to update local: uid:" + lii.uid);
              }
              si.updateLocal = true;
            }

            toFetch.add(si);
          }
        }
      }

      /* Now go over the remote info and see if we need to fetch any from that
       * end or simply delete them.
       */
      for (ItemInfo ii: riis) {
        if (ii.seen) {
          continue;
        }

        SynchInfo si = new SynchInfo(ii);
        /* If the lastmod is later than the last synch and this is 2 way then
         * this one got added after we synched. Add it to the remote end.
         *
         * If the lastmod is previous to our last synch then this one needs to
         * be deleted.
         */
        si.addToLocal = true;
        toFetch.add(si);
      }

      /* Now update the remote end from the local end.
       */
      if (toFetch.size() > getItemsBatchSize) {
        // Fetch this batch of items and process them.
        updateRemote(sub,
                     fetchItems(toFetch),
                     sinfos);
        toFetch.clear();
      }

      return StatusType.OK;
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  private void updateRemote(final Subscription sub,
                            final List<CalendarItem> cis,
                            final Map<String, SynchInfo> sinfos) throws SynchException {
    XmlIcalConvert cnv = new XmlIcalConvert();

    for (CalendarItem ci: cis) {
      SynchInfo si = sinfos.get(ci.getUID());

      if (si == null) {
        continue;
      }

      if (si.addToRemote) {
        AddItemResponseType air = localCnctr.addItem(ci.getUID(), cnv.toXml(ci));
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

  private boolean checkAccess(final Subscription sub) throws SynchException {
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
