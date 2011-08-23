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
import org.bedework.synch.SynchDefs.SynchEnd;
import org.bedework.synch.cnctrs.ConnectorInstance;
import org.bedework.synch.cnctrs.ConnectorInstance.ItemInfo;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SubscribeResponseType;

import edu.rpi.cmt.calendar.diff.XmlIcalCompare;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.ComponentSelectionType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

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
  private boolean debug;

  protected transient Logger log;

  private ConnectorInstance endACnctr;

  private ConnectorInstance endBCnctr;

  private SynchEngine syncher;

  private XmlIcalCompare differ = new XmlIcalCompare();

  /* Max number of items we fetch at a time */
  private final int getItemsBatchSize = 20;

  /** Constructor
   *
   * @param syncher
   * @throws SynchException
   */
  public Synchling(final SynchEngine syncher) throws SynchException {
    debug = getLogger().isDebugEnabled();

    this.syncher = syncher;
  }

  /**
   * @param sub
   * @param note
   * @throws SynchException
   */
  public void handleNotification(final Notification<NotificationItem> note) throws SynchException {
    for (NotificationItem ni: note.getNotifications()) {

      switch (ni.getAction()) {
      case FullSynch:
        reSynch(note);
        break;
      case CopiedEvent:
        break;
      case CreatedEvent:
        addItem(note, ni);
        break;
      case DeletedEvent:
        break;
      case ModifiedEvent:
        updateItem(note, ni);
        break;
      case MovedEvent:
        break;
      case NewMailEvent:
        break;
      case StatusEvent:
        break;

      case NewSubscription:
        subscribe(note, ni);
        break;

      case Unsubscribe:
        break;
      }
    }
  }

  /* ====================================================================
   *                        Notification methods
   * ==================================================================== */


  private void subscribe(final Notification note,
                         final NotificationItem ni) throws SynchException {
    if (debug) {
      trace("new subscription " + note.getSub());
    }

    /* Try to subscribe to both ends */
    ConnectorInstance cinst = syncher.getConnectorInstance(note.getSub(),
                                                           SynchEnd.endA);

    SubscribeResponseType sr = cinst.subscribe(ni.getSubResponse());

    if (sr.getStatus() != StatusType.OK) {
      return;
    }

    cinst = syncher.getConnectorInstance(note.getSub(),
                                         SynchEnd.endB);
    sr = cinst.subscribe(ni.getSubResponse());

    if (sr.getStatus() != StatusType.OK) {
      return;
    }

    syncher.addSubscription(note.getSub());
  }

  private void addItem(final Notification note,
                       final NotificationItem ni) throws SynchException {
    IcalendarType ical = ni.getIcal();

    if (ical == null) {
      if (debug) {
        trace("No item found");
      }

      return;
    }

    AddItemResponseType air = getOtherCinst(note).addItem(ical);
    if (debug) {
      trace("Add: status=" + air.getStatus() +
            " msg=" + air.getMessage());
    }
  }

  private void updateItem(final Notification note,
                          final NotificationItem ni) throws SynchException {
    IcalendarType ical = ni.getIcal();

    if (ical == null) {
      if (debug) {
        trace("No item found");
      }

      return;
    }

    ConnectorInstance cinst = getOtherCinst(note);

    FetchItemResponseType fresp = cinst.fetchItem(ni.getUid());
    if (debug) {
      trace("Update: status=" + fresp.getStatus() +
            " msg=" + fresp.getMessage());
    }

    if (fresp.getStatus() != StatusType.OK) {
      return;
    }

    IcalendarType targetIcal = fresp.getIcalendar();

    XmlIcalCompare comp = new XmlIcalCompare();

    ComponentSelectionType cst = comp.diff(ical, targetIcal);

    UpdateItemType ui = new UpdateItemType();

    ui.setHref(fresp.getHref());
    ui.setEtoken(fresp.getEtoken());
    ui.getSelect().add(cst);

    UpdateItemResponseType uir = cinst.updateItem(ui);
    if (debug) {
      trace("Update: status=" + uir.getStatus() +
            " msg=" + uir.getMessage());
    }
  }

  private ConnectorInstance getOtherCinst(final Notification note) throws SynchException {
    SynchEnd otherEnd;
    if (note.getEnd() == SynchEnd.endA) {
      otherEnd = SynchEnd.endB;
    } else {
      otherEnd = SynchEnd.endA;
    }

    return syncher.getConnectorInstance(note.getSub(),
                                        otherEnd);
  }

  /* ====================================================================
   *                        private methods
   * ==================================================================== */

  /**
   * @param note
   * @return status
   * @throws SynchException
   */
  public StatusType unsubscribe(final Notification note) throws SynchException {
    Subscription sub = syncher.getSubscription(note.getSubscriptionId());
    if (sub == null){
      return StatusType.ERROR;
    }

    if (!checkAccess(sub)) {
      info("No access for subscription " + sub);
      return StatusType.NO_ACCESS;
    }

    // Unsubscribe request - set subscribed off and next callback will unsubscribe
    sub.setOutstandingSubscription(null);
    sub.setSubscribe(false);

    syncher.deleteSubscription(sub);

    return StatusType.OK;
  }

  private static class SynchInfo {
    /** */
    public ItemInfo itemInfo;

    /* Fields set during the actual synch process */

    /** add to none, A or B */
    public SynchEnd addTo = SynchEnd.none;

    /** Update none, A or B */
    public SynchEnd updateEnd = SynchEnd.none;

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

  private StatusType reSynch(final Notification note) throws SynchException {
    try {
      /* The action here depends on which way we are synching.
       *
       * For A to B
       * If the item does not exist on the B system then add it to B.
       * If the lastmod on B is prior to the A one - update.
       * Otherwise ignore.
       * We should remove all B events that have no counterpart on A. (though
       * this may be a parameter)
       *
       * For B to A
       * Just the reverse of the above.
       *
       * For both ways:
       * This is essentially do both the above. The intent is to ensure that
       * we add appropriate events to either end and possibly remove from either
       * end till we are left with the full overlapping set. That's the easy bit.
       *
       * Updates are easy as long as the set of updated events from both ends
       * does not overlap. For the non-overlapping event just update the older
       * event from the newer event.
       *
       * For the rest, if one end is designated master, then  update the non-master
       * from the master. Otherwise we probably need to flag the event.
       *
       * Note that we need to have a last-synched stamp in each event to
       * determine if the event has changed since last synched. For conflict
       * detection we also need a reliable lastmod which we store in the
       * last synched.
       */

      endACnctr = syncher.getConnectorInstance(note.getSub(), SynchEnd.endA);
      endBCnctr = syncher.getConnectorInstance(note.getSub(), SynchEnd.endB);

      // XXX 2 way not fully done
      /* ===================================================================
       * Get the list of items that already exist in the calendar collections
       * =================================================================== */
      List<ItemInfo> aiis = endACnctr.getItemsInfo();
      if (aiis == null) {
        throw new SynchException("Unable to fetch endA items info");
      }

      Map<String, ItemInfo> bitems = getItemsMap(endBCnctr);

      /* toFetch is a list of those items we have to fetch from end A to
       * resynch that item
       */
      List<SynchInfo> toFetch = new ArrayList<SynchInfo>();

      for (ItemInfo aii: aiis) {
        ItemInfo bii = bitems.get(aii.uid);

        if (bii == null) {
          /* It's not in the B list - add to list to fetch from A */
          if (debug) {
            trace("Need to add to end B: uid:" + aii.uid);
          }

          SynchInfo si = new SynchInfo(aii);
          si.addTo = SynchEnd.endB;
          toFetch.add(si);
          continue;
        }

        /* It is at the A end - mark as seen then compare to see if
         * we need to update
         */
        bii.seen = true;

        int cmp = cmpLastMods(bii.lastMod, aii.lastMod);
        if (cmp == 0) {
          /* No update needed */
          continue;
        }

        /* We need to update one end from the other */
        SynchInfo si = new SynchInfo(aii);

        if (cmp < 0) {
          if (debug) {
            trace("Need to update end B: uid:" + aii.uid);
          }
          si.updateEnd = SynchEnd.endB;
        } else {
          if (debug) {
            trace("Need to update end A: uid:" + aii.uid);
          }
          si.updateEnd = SynchEnd.endA;
        }

        toFetch.add(si);
      }

      /* Now go over the end B info and see if we need to fetch any from that
       * end or simply delete them.
       */
      for (ItemInfo ii: bitems.values()) {
        if (ii.seen) {
          continue;
        }

        SynchInfo si = new SynchInfo(ii);
        /* If the lastmod is later than the last synch and this is 2 way then
         * this one got added after we synched. Add it to end B.
         *
         * If the lastmod is previous to our last synch then this one needs to
         * be deleted.
         */
        si.addTo = SynchEnd.endA;
        toFetch.add(si);
      }

      Holder<List<SynchInfo>> unprocessedRes = new Holder<List<SynchInfo>>();

      /* Now update end A from end B.
       */
      while ((toFetch.size() > 0) &&
             processFetchList(note, toFetch, unprocessedRes, SynchEnd.endA)) {
        toFetch = unprocessedRes.value;
      }

      /* Now update end B from end A.
       */
      while ((toFetch.size() > 0) &&
             processFetchList(note, toFetch, unprocessedRes, SynchEnd.endB)) {
        toFetch = unprocessedRes.value;
      }

      return StatusType.OK;
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  private Map<String, ItemInfo> getItemsMap(final ConnectorInstance cinst) throws SynchException {
    /* Items is a table built from the endB calendar */
    Map<String, ItemInfo> items = new HashMap<String, ItemInfo>();

    List<ItemInfo> iis = cinst.getItemsInfo();
    if (iis == null) {
      throw new SynchException("Unable to fetch items info");
    }

    for (ItemInfo ii: iis) {
      if (debug) {
        trace(ii.toString());
      }
      ii.seen = false;
      items.put(ii.uid, ii);
    }

    return items;
  }

  /**
   * @param note
   * @param toFetch
   * @param unprocessedRes
   * @param from
   * @return true if there are unprocessed entries for this end
   * @throws SynchException
   */
  private boolean processFetchList(final Notification note,
                                   final List<SynchInfo> toFetch,
                                   final Holder<List<SynchInfo>> unprocessedRes,
                                   final SynchEnd dest) throws SynchException {
    boolean callAgain = true;
    List<SynchInfo> unProcessed = new ArrayList<SynchInfo>();
    unprocessedRes.value = unProcessed;

    List<String> uids = new ArrayList<String>();
    List<SynchInfo> sis = new ArrayList<SynchInfo>();

    /* First fetch a batch of items */
    int i = 0;
    while ((uids.size() < getItemsBatchSize) && (i < toFetch.size())) {
      SynchInfo si = toFetch.get(i);

      if ((si.addTo != dest) && (si.updateEnd != dest)) {
        unProcessed.add(si);
        continue;
      }

      uids.add(si.itemInfo.uid);
      sis.add(si);
    }

    if (uids.size() == 0) {
      return false;
    }

    ConnectorInstance fromInst;
    ConnectorInstance toInst;

    if (dest == SynchEnd.endA) {
      toInst = endACnctr;
      fromInst = endBCnctr;
    } else {
      toInst = endBCnctr;
      fromInst = endACnctr;
    }

    List<FetchItemResponseType> firs = fromInst.fetchItems(uids);

    Iterator<SynchInfo> siit = sis.iterator();
    for (FetchItemResponseType fir: firs) {
      SynchInfo si = siit.next();

      if (si.addTo == dest) {
        AddItemResponseType air = toInst.addItem(fir.getIcalendar());
        if (debug) {
          trace("Add: status=" + air.getStatus() +
                " msg=" + air.getMessage());
        }

        continue;
      }

      if (si.updateEnd == dest) {
        // Update the instance
        FetchItemResponseType toFir = toInst.fetchItem(si.itemInfo.uid);

        if (toFir.getStatus() != StatusType.OK) {
          warn("Unable to fetch destination entity for update");
          continue;
        }

        ComponentSelectionType cst = differ.diff(fir.getIcalendar(),
                                                 toFir.getIcalendar());

        UpdateItemType ui = new UpdateItemType();

        ui.setHref(toFir.getHref());
        ui.setEtoken(toFir.getEtoken());
        ui.getSelect().add(cst);

        UpdateItemResponseType uir = toInst.updateItem(ui);

        if (uir.getStatus() != StatusType.OK) {
          warn("Unable to update destination entity");
        }

        continue;
      }

      warn("Should not get here");
    }

    return callAgain;
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

  @SuppressWarnings("unused")
  private void error(final Throwable t) {
    getLogger().error(this, t);
  }

  private void info(final String msg) {
    getLogger().info(msg);
  }
}
