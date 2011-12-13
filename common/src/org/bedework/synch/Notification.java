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

import org.bedework.synch.db.Subscription;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SubscriptionStatusRequestType;
import org.bedework.synch.wsmessages.SubscriptionStatusResponseType;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;
import org.bedework.synch.wsmessages.UnsubscribeResponseType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.ArrayList;
import java.util.List;

/** Notification from external system.
 *
 * <p>The system can handle notifications of changes as defined by ActionType
 * below. Connectors set up their remote service to notify the synch engine via
 * the web callback service. These incoming notifications are system specific.
 *
 * <p>On receipt, a connector instance is located and creates a notification
 * which is a subclass of this class. That object will be used to activate a
 * synchling.
 *
 * <p>Some systems will send multiple notifications for the same entity. Each
 * object of this class will contain a list of notification items. Presumably
 * these reflect activity since the last notification.
 *
 * <p>Each notification item defines an action along with a uid and a possible
 * calendar entity. The uid is required as a key as it is the only value which
 * is guaranteed to be available at both ends.
 *
 * <p>We assume that any change to any part of a recurring event master or
 * overrides will result in synching the whole entity.
 *
 * @author douglm
 *
 * @param <NI>
 */
public class Notification<NI extends Notification.NotificationItem> {
  private Subscription sub;

  private String subscriptionId;

  private SynchEndType end;

  private List<NI> notifications = new ArrayList<NI>();

  /** Create a notification for a subscription
   * @param sub
   */
  public Notification(final Subscription sub) {
    this.sub = sub;
    if (sub != null) {
      this.subscriptionId = sub.getSubscriptionId();
    }
  }

  /** Create a notification for an unsubscribe
   * @param subscriptionId
   */
  public Notification(final String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  /** Create object with a single notification.
   *
   * @param sub
   * @param end
   */
  public Notification(final Subscription sub,
                      final SynchEndType end) {
    this(sub);
    this.end = end;
  }

  /** Create object with a single notification.
   *
   * @param sub
   * @param end
   * @param notificationItem
   */
  public Notification(final Subscription sub,
                      final SynchEndType end,
                      final NI notificationItem) {
    this(sub, end);
    addNotificationItem(notificationItem);
  }

  /** Create a new subscription object
   *
   * @param sub
   * @param response
   */
  @SuppressWarnings("unchecked")
  public Notification(final Subscription sub,
                      final SubscribeResponseType response) {
    this(sub, SynchEndType.NONE);
    addNotificationItem((NI)new NotificationItem(response));
  }

  /** Create a new unsubscription object
   *
   * @param sub
   * @param request
   * @param response
   */
  @SuppressWarnings("unchecked")
  public Notification(final Subscription sub,
                      final UnsubscribeRequestType request,
                      final UnsubscribeResponseType response) {
    this(sub, SynchEndType.NONE);
    addNotificationItem((NI)new NotificationItem(request, response));
  }

  /** Create a new subscription status object
   *
   * @param sub
   * @param request
   * @param response
   */
  @SuppressWarnings("unchecked")
  public Notification(final Subscription sub,
                      final SubscriptionStatusRequestType request,
                      final SubscriptionStatusResponseType response) {
    this(sub, SynchEndType.NONE);
    addNotificationItem((NI)new NotificationItem(request, response));
  }

  /**
   * @param action
   */
  @SuppressWarnings("unchecked")
  public Notification(final NotificationItem.ActionType action) {
    addNotificationItem((NI)new NotificationItem(action));
  }

  /**
   * @return Subscription
   */
  public Subscription getSub() {
    return sub;
  }

  /** Our generated subscriptionId.
   *
   * @return String
   */
  public String getSubscriptionId() {
    return subscriptionId;
  }

  /**
   * @return end designator
   */
  public SynchEndType getEnd() {
    return end;
  }

  /**
   * @return notifications
   */
  public List<NI> getNotifications() {
    return notifications;
  }

  /**
   * @param val
   */
  public void addNotificationItem(final NI val) {
    notifications.add(val);
  }

  /**
   * @author douglm
   */
  public static class NotificationItem {
    /**
     * @author douglm
     */
    public enum ActionType {
      /** */
      FullSynch,
      /** */
      CopiedEvent,
      /** */
      CreatedEvent,
      /** */
      DeletedEvent,
      /** */
      ModifiedEvent,
      /** */
      MovedEvent,
      /** */
      NewMailEvent,
      /** */
      StatusEvent,

      /** */
      NewSubscription,

      /** */
      Unsubscribe,

      /** */
      SubscriptionStatus,

      /** Getting system information */
      GetInfo,
    }

    private ActionType action;

    private IcalendarType ical;

    private String uid;

    private SubscribeResponseType subResponse;

    private UnsubscribeRequestType unsubRequest;
    private UnsubscribeResponseType unsubResponse;

    private SubscriptionStatusRequestType subStatusReq;
    private SubscriptionStatusResponseType subStatusResponse;

    /** Create a notification item for an action.
     *
     * @param action
     */
    public NotificationItem(final ActionType action) {
      this.action = action;
    }

    /** Create a notification item for an action.
     *
     * @param action
     * @param ical - the entity if available
     * @param uid - Uid for the entity if entity not available
     */
    public NotificationItem(final ActionType action,
                            final IcalendarType ical,
                            final String uid) {
      this(action);
      this.ical = ical;
      this.uid = uid;
    }

    /** Create a notification item for a new subscription.
     *
     * @param subResponse
     */
    public NotificationItem(final SubscribeResponseType subResponse) {
      action = ActionType.NewSubscription;
      this.subResponse = subResponse;
    }

    /** Create a notification item for unsubscribe.
     *
     * @param unsubRequest
     * @param unsubResponse
     */
    public NotificationItem(final UnsubscribeRequestType unsubRequest,
                            final UnsubscribeResponseType unsubResponse) {
      action = ActionType.Unsubscribe;
      this.unsubRequest = unsubRequest;
      this.unsubResponse = unsubResponse;
    }

    /** Create a notification item for status.
     *
     * @param subStatusReq
     * @param subStatusResponse
     */
    public NotificationItem(final SubscriptionStatusRequestType subStatusReq,
                            final SubscriptionStatusResponseType subStatusResponse) {
      action = ActionType.SubscriptionStatus;
      this.subStatusReq = subStatusReq;
      this.subStatusResponse = subStatusResponse;
    }

    /**
     * @return the action
     */
    public ActionType getAction() {
      return action;
    }

    /**
     * @return the icalendar entity we were notified about
     */
    public IcalendarType getIcal() {
      return ical;
    }

    /**
     * @return the uid of the icalendar entity we were notified about
     */
    public String getUid() {
      return uid;
    }

    /**
     * @return response to a notification item
     */
    public SubscribeResponseType getSubResponse() {
      return subResponse;
    }

    /**
     * @return request leading to a notification item
     */
    public UnsubscribeRequestType getUnsubRequest() {
      return unsubRequest;
    }

    /**
     * @return response to a notification item
     */
    public UnsubscribeResponseType getUnsubResponse() {
      return unsubResponse;
    }

    /**
     * @return request leading to a notification item
     */
    public SubscriptionStatusRequestType getSubStatusReq() {
      return subStatusReq;
    }

    /**
     * @return response to a notification item
     */
    public SubscriptionStatusResponseType getSubStatusResponse() {
      return subStatusResponse;
    }

    protected void toStringSegment(final StringBuilder sb) {
      sb.append("action=");
      sb.append(getAction());
      sb.append("uid=");
      sb.append(getUid());
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

      toStringSegment(sb);

      sb.append("}");

      return sb.toString();
    }
  }

  protected void toStringSegment(final StringBuilder sb) {
    sb.append("sub=");
    sb.append(getSub());

    sb.append(", end=");
    sb.append(getEnd());

    String delim = ",\n   notification items{\n      ";
    for (NI ni: getNotifications()) {
      sb.append(delim);
      sb.append(ni.toString());

      delim =",\n      ";
    }

    if (getNotifications().size() > 0) {
      sb.append("}");
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    toStringSegment(sb);

    sb.append("}");

    return sb.toString();
  }
}
