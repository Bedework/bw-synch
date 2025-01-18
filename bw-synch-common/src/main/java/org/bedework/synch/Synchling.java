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

import org.bedework.synch.filters.Filters;
import org.bedework.synch.shared.BaseSubscriptionInfo.CrudCts;
import org.bedework.synch.shared.Notification;
import org.bedework.synch.shared.Notification.NotificationItem;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.SubscriptionConnectorInfo;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.cnctrs.Connector;
import org.bedework.synch.shared.cnctrs.ConnectorInstance;
import org.bedework.synch.shared.cnctrs.ConnectorInstance.ItemInfo;
import org.bedework.synch.shared.cnctrs.ConnectorInstance.SynchItemsInfo;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.shared.filters.Filter;
import org.bedework.synch.wsmessages.ConnectorInfoType;
import org.bedework.synch.wsmessages.RefreshResponseType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SubscriptionStatusRequestType;
import org.bedework.synch.wsmessages.SubscriptionStatusResponseType;
import org.bedework.synch.wsmessages.SynchDirectionType;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;
import org.bedework.synch.wsmessages.UnsubscribeResponseType;
import org.bedework.util.calendar.diff.XmlIcalCompare;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.base.ToString;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.ComponentSelectionType;
import org.oasis_open.docs.ws_calendar.ns.soap.DeleteItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.ErrorCodeType;
import org.oasis_open.docs.ws_calendar.ns.soap.FetchItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;
import org.oasis_open.docs.ws_calendar.ns.soap.TargetDoesNotExistType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

import static java.lang.String.format;

/** The synchling handles the processing of a single subscription when there is
 * some activity.
 *
 * <p>A synchling may be started to process a subscription as a the result of a
 * callback notification for example from exchange or because a synch period has
 * elapsed and it's time to refresh.
 *
 * @author Mike Douglass
 */
public class Synchling implements Logged {
  private static final Object synchlingIdLock = new Object();

  private static long lastSynchlingId;

  private final long synchlingId;

  private final SynchEngine syncher;

  private XmlIcalCompare diff;

  // Subscription id used when getting the diff object.
  private String diffSubid;

  /* Max number of items we fetch at a time */
  private final int getItemsBatchSize = 20;

  /** Constructor
   *
   * @param syncher the synch engine
   */
  public Synchling(final SynchEngine syncher) {
    this.syncher = syncher;

    synchronized (synchlingIdLock) {
      lastSynchlingId++;
      synchlingId = lastSynchlingId;
    }
  }

  /**
   * @return unique id
   */
  public long getSynchlingId() {
    return synchlingId;
  }

  /**
   * @param note notification
   * @return OK for all handled fine. ERROR - discard. WARN - retry.
   */
  public StatusType handleNotification(final Notification<NotificationItem> note) {
    StatusType st;

    for (final NotificationItem ni: note.getNotifications()) {
      switch (ni.getAction()) {
      case FullSynch:
        if (syncher.subscriptionsOnly()) {
          if (debug()) {
            debug("Skipping: subscriptions only");
          }
          continue;
        }

        st = reSynch(note);
        if (st != StatusType.OK) {
          return st;
        }
        continue;

      case CopiedEvent:
        break;

      case CreatedEvent:
        if (syncher.subscriptionsOnly()) {
          if (debug()) {
            debug("Skipping: subscriptions only");
          }
          continue;
        }

        st = addItem(note, ni);
        if (st != StatusType.OK) {
          return st;
        }
        continue;

      case DeletedEvent:
        break;

      case ModifiedEvent:
        if (syncher.subscriptionsOnly()) {
          if (debug()) {
            debug("Skipping: subscriptions only");
          }
          continue;
        }

        st = updateItem(note, ni);
        if (st != StatusType.OK) {
          return st;
        }
        continue;

      case MovedEvent:
        break;
      case NewMailEvent:
        break;
      case StatusEvent:
        break;

      case NewSubscription:
        ni.getSubResponse().setStatus(subscribe(note, ni));
        if (ni.getSubResponse().getStatus() != StatusType.OK) {
          return ni.getSubResponse().getStatus();
        }

        ni.getSubResponse().setSubscriptionId(note.getSubscriptionId());

        /* Now put it on the queue for first population */
        syncher.setConnectors(note.getSub());

        syncher.reschedule(note.getSub(), true);

        continue;

      case Unsubscribe:
        st = unsubscribe(note, ni);
        if (st != StatusType.OK) {
          return st;
        }
        continue;

      case Refresh:
        st = refresh(note, ni);
        if (st != StatusType.OK) {
          return st;
        }
        continue;

      case SubscriptionStatus:
        st = subStatus(note, ni);
        if (st != StatusType.OK) {
          return st;
        }
        continue;
      }

      return StatusType.ERROR;
    }

    note.getSub().setErrorCt(0);
    return StatusType.OK;
  }

  /* =============================================================
   *                        Notification methods
   * ============================================================= */


  private StatusType subscribe(final Notification<?> note,
                         final NotificationItem ni) {
    if (debug()) {
      debug("new subscription " + note.getSub());
    }

    syncher.setConnectors(note.getSub());

    /* Try to subscribe to both ends */
    ConnectorInstance<?> cinst =
            syncher.getConnectorInstance(note.getSub(),
                                         SynchEndType.A);

    final SubscribeResponseType sr = ni.getSubResponse();

    if (!cinst.subscribe(sr)) {
      return sr.getStatus();
    }

    cinst = syncher.getConnectorInstance(note.getSub(),
                                         SynchEndType.B);
    if (!cinst.subscribe(sr)) {
      return sr.getStatus();
    }

    syncher.addSubscription(note.getSub());
    return StatusType.OK;
  }

  private StatusType addItem(final Notification<?> note,
                             final NotificationItem ni) {
    final IcalendarType ical = ni.getIcal();

    if (ical == null) {
      if (debug()) {
        debug("No item found");
      }

      return StatusType.ERROR;
    }

    final AddItemResponseType air = getOtherCinst(note).addItem(ical);
    if (debug()) {
      debug("Add: status=" + air.getStatus() +
            " msg=" + air.getMessage());
    }

    return air.getStatus();
  }

  private StatusType updateItem(final Notification<?> note,
                                final NotificationItem ni) {
    final IcalendarType ical = ni.getIcal();

    if (ical == null) {
      if (debug()) {
        debug("No item found");
      }

      return StatusType.ERROR;
    }

    final ConnectorInstance<?> cinst = getOtherCinst(note);

    final FetchItemResponseType fresp = cinst.fetchItem(ni.getUid());
    if (debug()) {
      debug("Update: status=" + fresp.getStatus() +
            " msg=" + fresp.getMessage());
    }

    if (fresp.getStatus() != StatusType.OK) {
      return fresp.getStatus();
    }

    final IcalendarType targetIcal = fresp.getIcalendar();

    final Subscription sub = note.getSub();

    final ResynchInfo ainfo = new ResynchInfo(sub,
                                              SynchEndType.A,
                                              syncher);
    final ResynchInfo binfo = new ResynchInfo(sub,
                                              SynchEndType.B,
                                              syncher);
    final ResynchInfo toInfo;
    final ResynchInfo fromInfo;
    if (note.getEnd() == SynchEndType.A) {
      toInfo = binfo;
      fromInfo = ainfo;
    } else {
      toInfo = ainfo;
      fromInfo = binfo;
    }

    final ComponentSelectionType cst = getDiffer(note,
                                                 fromInfo,
                                                 toInfo).diff(ical, targetIcal);

    if (cst == null) {
      if (debug()) {
        debug("No update needed for " + ni.getUid());
      }

      return StatusType.OK;
    }

    final UpdateItemType ui = new UpdateItemType();

    ui.setHref(fresp.getHref());
    ui.setChangeToken(fresp.getChangeToken());
    ui.getSelect().add(cst);

    final UpdateItemResponseType uir = cinst.updateItem(ui);
    if (debug()) {
      debug("Update: status=" + uir.getStatus() +
            " msg=" + uir.getMessage());
    }
    return uir.getStatus();
  }

  private ConnectorInstance<?> getOtherCinst(final Notification<?> note) {
    final SynchEndType otherEnd;
    if (note.getEnd() == SynchEndType.A) {
      otherEnd = SynchEndType.B;
    } else {
      otherEnd = SynchEndType.A;
    }

    return syncher.getConnectorInstance(note.getSub(),
                                        otherEnd);
  }

  /* ==========================================================
   *                       private methods
   * ========================================================== */

  /**
   * @param note the notification
   * @return status
   */
  private StatusType unsubscribe(final Notification<?> note,
                                 final NotificationItem ni) {
    final Subscription sub = note.getSub();
    if (sub == null){
      return StatusType.ERROR;
    }

    if (!checkAccess(sub)) {
      info("No access for subscription " + sub);
      return StatusType.NO_ACCESS;
    }

    syncher.setConnectors(sub);

    /* See if it's OK by the connector instances */

    ConnectorInstance<?> cinst = syncher.getConnectorInstance(sub,
                                                           SynchEndType.A);

    final UnsubscribeRequestType usreq = ni.getUnsubRequest();
    final UnsubscribeResponseType usr = ni.getUnsubResponse();

    if (!cinst.unsubscribe(usreq, usr)) {
      warn("Unsubscribe end " + SynchEndType.A +
                   " returned false for " + sub);
      //return usr.getStatus();
    }

    cinst = syncher.getConnectorInstance(note.getSub(),
                                         SynchEndType.B);
    if (!cinst.unsubscribe(usreq, usr)) {
      warn("Unsubscribe end " + SynchEndType.B +
                   " returned false for " + sub);
      //return usr.getStatus();
    }

    // Unsubscribe request - call connector instance to carry out any required
    // action
    sub.setOutstandingSubscription(null);
    sub.setDeleted(true);

    if (debug()) {
      debug("Attempt to delete " + sub);
    }

    syncher.deleteSubscription(sub);

    if (debug()) {
      debug("Deleted");
    }

    return StatusType.OK;
  }

  /**
   * @param note the notification
   * @return status
   */
  private StatusType refresh(final Notification<?> note,
                             final NotificationItem ni) {
    final Subscription sub = note.getSub();
    if (sub == null){
      return StatusType.ERROR;
    }

    if (!checkAccess(sub)) {
      info("No access for subscription " + sub);
      return StatusType.NO_ACCESS;
    }

    syncher.setConnectors(sub);

    /* See if it's OK by the connector instances */

    ConnectorInstance<?> cinst =
            syncher.getConnectorInstance(sub, SynchEndType.A);

    final RefreshResponseType resp = ni.getRefreshResponse();

    cinst.forceRefresh();

    cinst = syncher.getConnectorInstance(note.getSub(),
                                         SynchEndType.B);
    cinst.forceRefresh();

    sub.setLastRefresh(null);
    syncher.reschedule(sub, false);
    resp.setStatus(StatusType.OK);

    return StatusType.OK;
  }

  /**
   * @param note the notification
   * @return status
   */
  private StatusType subStatus(final Notification<?> note,
                               final NotificationItem ni) {
    final Subscription sub = note.getSub();
    if (sub == null){
      return StatusType.ERROR;
    }

    if (!checkAccess(sub)) {
      info("No access for subscription " + sub);
      return StatusType.NO_ACCESS;
    }

    syncher.setConnectors(sub);

    /* See if it's OK by the connector instances */

    final ConnectorInstance<?> cinst =
            syncher.getConnectorInstance(sub,
                                         SynchEndType.A);

    final SubscriptionStatusRequestType ssreq = ni.getSubStatusReq();
    final SubscriptionStatusResponseType ssr = ni.getSubStatusResponse();

    ssr.setSubscriptionId(sub.getSubscriptionId());
    ssr.setPrincipalHref(sub.getOwner());
    ssr.setDirection(sub.getDirectionEnum());
    ssr.setLastRefresh(sub.getLastRefresh());
    ssr.setErrorCt(new BigInteger(String.valueOf(sub.getErrorCt())));

    ssr.setEndAConnector(getConnectorInfo(sub.getEndAConnectorInfo()));
    ssr.setEndBConnector(getConnectorInfo(sub.getEndBConnectorInfo()));

    if (!cinst.validateActiveSubInfo(ssreq, ssr,
                                     cinst.getConnector(),
                                     cinst.getSubInfo())) {
      return ssr.getStatus();
    }

    return StatusType.OK;
  }

  private ConnectorInfoType getConnectorInfo(
          final SubscriptionConnectorInfo<?> sci) {
    final ConnectorInfoType ci = new ConnectorInfoType();

    ci.setConnectorId(sci.getConnectorId());

    ci.setProperties(sci.getAllSynchProperties());

    return ci;
  }

  private static class SynchInfo {
    /** */
    public ItemInfo itemInfo;

    /* Fields set during the actual synch process */

    /** add to none, A or B */
    public SynchEndType addTo = SynchEndType.NONE;

    /** Update none, A or B */
    public SynchEndType updateEnd = SynchEndType.NONE;

    /** delete none, A or B */
    public SynchEndType deleteFrom = SynchEndType.NONE;

    /** both ends changed since last synch */
    @SuppressWarnings("UnusedDeclaration")
    public boolean conflict;

    /** Constructor
     *
     * @param itemInfo the item info
     */
    public SynchInfo(final ItemInfo itemInfo) {
      this.itemInfo = itemInfo;
    }

    @Override
    public String toString() {
      final ToString ts = new ToString(this);

      ts.append(itemInfo);

      return ts.toString();
    }
  }

  /** Information and objects needed to process one end of a resynch
   */
  private static class ResynchInfo {
    Subscription sub;
    SynchEndType end;
    boolean trustLastmod;
    ConnectorInstance<?> inst;
    Map<String, ItemInfo> items;
    CrudCts lastCts;
    CrudCts totalCts;

    final SubscriptionConnectorInfo<?> connInfo;
    List<Filter> inFilters;
    List<Filter> outFilters;

    // True if our target is missing.
    boolean missingTarget;

    ResynchInfo(final Subscription sub,
                final SynchEndType end,
                final SynchEngine syncher) {
      this.sub = sub;
      this.end = end;
      final Connector<?, ?, ?> c;
      if (end == SynchEndType.A) {
        c = sub.getEndAConn();
        connInfo = sub.getEndAConnectorInfo();
      } else {
        c = sub.getEndBConn();
        connInfo = sub.getEndBConnectorInfo();
      }

      trustLastmod = c.getTrustLastmod();
      inst = syncher.getConnectorInstance(sub, end);

      lastCts = new CrudCts();
      inst.setLastCrudCts(lastCts);
      totalCts = inst.getTotalCrudCts();
    }

    void updateCts() {
      inst.setLastCrudCts(lastCts);
      inst.setTotalCrudCts(totalCts);
    }

    List<Filter> getInFilters() {
      if (inFilters == null) {
        inFilters = connInfo.getInputFilters(sub);
      }

      return inFilters;
    }

    List<Filter> getOutFilters() {
      if (outFilters == null) {
        outFilters = connInfo.getOutputFilters(sub);
      }

      return outFilters;
    }
  }

  private StatusType reSynch(final Notification<?> note) {
    final Subscription sub = note.getSub();

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

      final var dir = sub.getDirectionEnum();
      final boolean bothWays =
              dir == SynchDirectionType.BOTH_WAYS;

      final ResynchInfo ainfo = new ResynchInfo(sub,
                                                SynchEndType.A,
                                                syncher);
      final ResynchInfo binfo = new ResynchInfo(sub,
                                                SynchEndType.B,
                                                syncher);

      boolean aChanged = false;
      boolean bChanged = false;

      if ((dir == SynchDirectionType.A_TO_B) || bothWays) {
        aChanged = ainfo.inst.changed();
      }

      if ((dir == SynchDirectionType.B_TO_A) || bothWays) {
        bChanged = binfo.inst.changed();
      }

      if (!aChanged && !bChanged) {
        // Nothing to do. last refresh updated on the way out.
        return StatusType.OK;
      }

      sub.setMissingTarget(false);

      /* Build maps of the items we believe we will need to check more
       * fully. We query the target for enough information to hopefully
       * eliminate checks on most of the entries, e.g. we use lastmods
       */

      ainfo.items = getItemsMap(ainfo);
      if (ainfo.items == null) {
        if (ainfo.missingTarget) {
          sub.setMissingTarget(true);
        }
        return StatusType.ERROR;
      }

      binfo.items = getItemsMap(binfo);
      if (binfo.items == null) {
        if (binfo.missingTarget) {
          sub.setMissingTarget(true);
        }
        return StatusType.ERROR;
      }

      /* updateInfo is a list of changes we need to apply to one or both ends
       */
      List<SynchInfo> updateInfo = new ArrayList<>();

      /* First see what we need to transfer from A to B */
      if ((dir == SynchDirectionType.A_TO_B) || bothWays) {
        getResynchs(updateInfo, ainfo, binfo);
      }

      /* Now B to A */
      if ((dir == SynchDirectionType.B_TO_A) || bothWays) {
        getResynchs(updateInfo, binfo, ainfo);
      }

      if ((dir == SynchDirectionType.A_TO_B) || bothWays) {
        checkDeletes(updateInfo, binfo);
      }

      if ((dir == SynchDirectionType.B_TO_A) || bothWays) {
        checkDeletes(updateInfo, ainfo);
      }

      if (debug()) {
        debug("---------------- update set ----------------");
        for (final SynchInfo si: updateInfo) {
          debug(si.toString());
        }
        debug("---------------- end update set ----------------");
      }

      if (!updateInfo.isEmpty()) {
        final Holder<List<SynchInfo>> unprocessedRes = new Holder<>();

        /* Now update end A from end B.
         */
        if ((dir == SynchDirectionType.B_TO_A) || bothWays) {
          while ((!updateInfo.isEmpty()) &&
                 processUpdates(note, updateInfo, unprocessedRes,
                                binfo, ainfo)) {
            updateInfo = unprocessedRes.value;
          }

          ainfo.updateCts();
        }

        /* Now update end B from end A.
         */
        if ((dir == SynchDirectionType.A_TO_B) || bothWays) {
          while ((!updateInfo.isEmpty()) &&
                 processUpdates(note, updateInfo, unprocessedRes,
                                ainfo, binfo)) {
            updateInfo = unprocessedRes.value;
          }

          binfo.updateCts();
        }
      }

      /* -------------------- Deletions ------------------------ */

      if (!sub.getInfo().getDeletionsSuppressed()) {
        if (((!updateInfo.isEmpty()) &&
                     (dir == SynchDirectionType.B_TO_A)) || bothWays) {
          processDeletes(note, updateInfo, ainfo);
        }

        if (((!updateInfo.isEmpty()) &&
                     (dir == SynchDirectionType.A_TO_B)) || bothWays) {
          processDeletes(note, updateInfo, binfo);
        }
      }

      sub.setErrorCt(0);

      return StatusType.OK;
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    } finally {
      sub.updateLastRefresh();
      syncher.updateSubscription(sub);
      syncher.reschedule(sub, false);
    }
  }

  private void getResynchs(final List<SynchInfo> updateInfo,
                           final ResynchInfo fromInfo,
                           final ResynchInfo toInfo) {
    final boolean useLastmods = fromInfo.trustLastmod && toInfo.trustLastmod;

    for (final ItemInfo fromIi: fromInfo.items.values()) {
      final ItemInfo toIi = toInfo.items.get(fromIi.uid);

      if (toIi == null) {
        /* It's not in the to list - add to list to fetch from the from end */
        if (debug()) {
          debug("Need to add to end " + toInfo.end + ": uid:" + fromIi.uid);
        }

        final SynchInfo si = new SynchInfo(fromIi);
        si.addTo = toInfo.end;
        updateInfo.add(si);
        continue;
      }

      /* It is at the to end - mark as seen then compare to see if
       * we need to update
       */
      toIi.seen = true;

      boolean update = true;

      if (useLastmods) {
        update = cmpLastMods(toIi.lastMod, fromIi.lastMod) < 0;
      }

      if (!update) {
        if (debug()) {
          debug("No need to update end " + toInfo.end + ": uid:" + fromIi.uid);
        }
      } else if (debug()) {
        debug("Need to update end " + toInfo.end + ": uid:" + fromIi.uid);
      }

      final SynchInfo si = new SynchInfo(fromIi);

      si.updateEnd = toInfo.end;
      updateInfo.add(si);
    }
  }

  private void checkDeletes(final List<SynchInfo> updateInfo,
                            final ResynchInfo toInfo) {
    for (final ItemInfo ii: toInfo.items.values()) {
      if (ii.seen) {
        continue;
      }

      final SynchInfo si = new SynchInfo(ii);
      /* If the lastmod is later than the last synch and this is 2 way then
       * this one got added after we synched. Add it to end B.
       *
       * If the lastmod is previous to our last synch then this one needs to
       * be deleted.
       */
      si.deleteFrom = toInfo.end;
      updateInfo.add(si);
    }
  }

  /** Uses the connector instance to fetch a map of info for items to be
   * synchronised from the target designated by the information.
   *
   * <p>The information identifies the record (by uid at the moment)
   * and provides enough data to determine if the entry should be examined
   * e.g lastmod</p>
   *
   * @param rinfo resynchinfo
   * @return map or null for error
   */
  private Map<String, ItemInfo> getItemsMap(final ResynchInfo rinfo) {
    /* Items is a table built from the target calendar */
    final Map<String, ItemInfo> items = new HashMap<>();

    final SynchItemsInfo sii = rinfo.inst.getItemsInfo();
    if (sii.getStatus() != StatusType.OK) {
      if ((sii.getErrorResponse() != null) &&
          (sii.getErrorResponse().getError() != null)) {
        // More information
        final ErrorCodeType ecode = sii.getErrorResponse().getError().getValue();
        if (ecode instanceof TargetDoesNotExistType) {
          // The target we are addressing is no longer available.
          rinfo.missingTarget = true;
        }
      }
      rinfo.sub.setErrorCt(rinfo.sub.getErrorCt() + 1);
      return null;
    }

    for (final ItemInfo ii: sii.items) {
      if (debug()) {
        debug(ii.toString());
      }

      ii.seen = false;
      items.put(ii.uid, ii);
    }

    return items;
  }

  /** Do the adds and updates for the end specified by toInfo.
   *
   * @param note the notification
   * @param updateInfo list of synchinfo
   * @param unprocessedRes holder of list of unprocessed
   * @param fromInfo resynch info
   * @param toInfo resynch info
   * @return true if there are unprocessed entries for this end
   */
  private boolean processUpdates(final Notification<?> note,
                                 final List<SynchInfo> updateInfo,
                                 final Holder<List<SynchInfo>> unprocessedRes,
                                 final ResynchInfo fromInfo,
                                 final ResynchInfo toInfo) {
    boolean callAgain = false;
    final List<SynchInfo> unProcessed = new ArrayList<>();
    unprocessedRes.value = unProcessed;

    final List<String> uids = new ArrayList<>();
    final List<SynchInfo> sis = new ArrayList<>();

    int i = 0;
    /* First make a batch of items to fetch */

    while (i < updateInfo.size()) {
      final SynchInfo si = updateInfo.get(i);
      i++;

      // Add to unprocessed if it's not one of ours
      if ((si.addTo != toInfo.end) && (si.updateEnd != toInfo.end)) {
        unProcessed.add(si);
        continue;
      }

      // Add to unprocessed if the batch is big enough
      if (uids.size() == getItemsBatchSize) {
        unProcessed.add(si);
        callAgain = true;
        continue;
      }

      uids.add(si.itemInfo.uid);
      sis.add(si);
    }

    if (uids.isEmpty()) {
      // Nothing left to do
      return false;
    }

    /* Fetch the batch of items.
     *
     * Each item will be run through input filters which may remove,
     * alter or add properties to the item.
     *
     * If the item is to be added, it will be added to the list.
     *
     * If the item is to be updated we fetch the target, which is also
     * run through the input filters for its connection. We then compare
     * the two, filtered, items and update the target if necessary.
     *
     */

    final List<FetchItemResponseType> firs = fromInfo.inst.fetchItems(uids);

    final Iterator<SynchInfo> siit = sis.iterator();
    for (final FetchItemResponseType fir: firs) {
      final SynchInfo si = siit.next();

      if (si.addTo == toInfo.end) {
        IcalendarType filtered = Filters.doFilters(fir.getIcalendar(),
                                                   fromInfo.getInFilters());

        if (filtered != null) {
          filtered = Filters.doFilters(filtered,
                                       toInfo.getOutFilters());
        }
        final AddItemResponseType air = toInfo.inst.addItem(filtered);

        toInfo.lastCts.created++;
        toInfo.totalCts.created++;

        if (debug()) {
          debug("Add: status=" + air.getStatus() +
                " msg=" + air.getMessage());
        }

        continue;
      }

      if (si.updateEnd == toInfo.end) {
        // Update the instance
        final FetchItemResponseType toFir = toInfo.inst.fetchItem(si.itemInfo.uid);

        if (toFir.getStatus() != StatusType.OK) {
          warn("Unable to fetch destination entity for update: message was " +
               toFir.getMessage());
          continue;
        }

        IcalendarType filtered = Filters.doFilters(fir.getIcalendar(),
                                                   fromInfo.getInFilters());

        if (filtered != null) {
          filtered = Filters.doFilters(filtered,
                                       toInfo.getOutFilters());
        }

        if (filtered == null) {
          if (debug()) {
            debug("Filter removed everything for " + si.itemInfo.uid);
          }

          continue;
        }

        final IcalendarType toFiltered =
                Filters.doFilters(toFir.getIcalendar(),
                                  toInfo.getInFilters());

        final ComponentSelectionType cst =
                getDiffer(note,
                          fromInfo,
                          toInfo).diff(filtered,
                                       toFiltered);

        if (cst == null) {
          if (debug()) {
            debug("No update needed for " + si.itemInfo.uid);
          }

          continue;
        }

        if (debug()) {
          debug("Update needed for " + si.itemInfo.uid);
        }

        final UpdateItemType ui = new UpdateItemType();

        ui.setHref(toFir.getHref());
        ui.setChangeToken(toFir.getChangeToken());
        ui.getSelect().add(cst);

        final UpdateItemResponseType uir = toInfo.inst.updateItem(ui);

        if (uir.getStatus() != StatusType.OK) {
          error(format("Unable to update destination entity. " +
                               "Message %s", uir.getMessage()));
          continue;
        }

        toInfo.lastCts.updated++;
        toInfo.totalCts.updated++;

        continue;
      }

      warn("Should not get here");
    }

    return callAgain;
  }

  /**
   * @param note the notification
   * @param updateInfo list of update info
   * @param toInfo resynch info
   */
  private void processDeletes(@SuppressWarnings("UnusedParameters")
                              final Notification<?> note,
                              final List<SynchInfo> updateInfo,
                              final ResynchInfo toInfo) {
    for (final SynchInfo si: updateInfo) {
      // Add to unprocessed if it's not one of ours
      if (si.deleteFrom != toInfo.end) {
        continue;
      }

      final DeleteItemResponseType dir =
              toInfo.inst.deleteItem(si.itemInfo.uid);
      final var status = dir.getStatus();
      if (!status.equals(StatusType.OK)) {
        error(format("Failed to delete %s, status was %s",
                     si.itemInfo.uid, status));
      }
    }
  }

  private int cmpLastMods(final String calLmod, final String exLmod) {
    int exi = 0;
    if (calLmod == null) {
      return -1;
    }

    for (int i = 0; i < 16; i++) {
      final char cal = calLmod.charAt(i);
      final char ex = exLmod.charAt(exi);

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

  @SuppressWarnings("UnusedParameters")
  private boolean checkAccess(final Subscription sub) {
    /* Does this principal have the rights to (un)subscribe? */
    return true;
  }

  private XmlIcalCompare getDiffer(final Notification<?> note,
                                   final ResynchInfo fromInfo,
                                   final ResynchInfo toInfo) {
    final Subscription sub = note.getSub();

    if ((diff != null) &&
        (diffSubid != null) &&
        diffSubid.equals(sub.getSubscriptionId())) {
      return diff;
    }

    /* Make up diff list */

    /* First the defaults */
    final List<Object> skipList = new ArrayList<>(XmlIcalCompare.defaultSkipList);

    Filters.addDifferSkipItems(skipList, fromInfo.getInFilters());
    Filters.addDifferSkipItems(skipList, toInfo.getOutFilters());

    diffSubid = sub.getSubscriptionId();
    diff = new XmlIcalCompare(skipList, syncher.getTzGetter());
    return diff;
  }

  /* =============================================================
   *                   Logged methods
   * ============================================================= */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
