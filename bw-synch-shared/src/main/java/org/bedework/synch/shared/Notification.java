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
package org.bedework.synch.shared;

import org.bedework.synch.wsmessages.RefreshRequestType;
import org.bedework.synch.wsmessages.RefreshResponseType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SubscriptionStatusRequestType;
import org.bedework.synch.wsmessages.SubscriptionStatusResponseType;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;
import org.bedework.synch.wsmessages.UnsubscribeResponseType;
import org.bedework.base.ToString;

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

  private final List<NI> notifications = new ArrayList<>();

  /** Create a notification for a subscription
   * @param sub subscription
   */
  public Notification(final Subscription sub) {
    this.sub = sub;
    if (sub != null) {
      this.subscriptionId = sub.getSubscriptionId();
    }
  }

  /** Create a notification for an unsubscribe
   *
   * @param subscriptionId id to unsubscribe
   */
  public Notification(final String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  /** Create object with a single notification.
   *
   * @param sub subscription
   * @param end which end
   */
  public Notification(final Subscription sub,
                      final SynchEndType end) {
    this(sub);
    this.end = end;
  }

  /** Create object with a single notification.
   *
   * @param sub subscription
   * @param end which end
   * @param notificationItem the notification
   */
  public Notification(final Subscription sub,
                      final SynchEndType end,
                      final NI notificationItem) {
    this(sub, end);
    addNotificationItem(notificationItem);
  }

  /** Create a new subscription object
   *
   * @param sub subscription
   * @param response and the response
   */
  @SuppressWarnings("unchecked")
  public Notification(final Subscription sub,
                      final SubscribeResponseType response) {
    this(sub, SynchEndType.NONE);
    addNotificationItem((NI)new NotificationItem(response));
  }

  /** Create a new unsubscription object
   *
   * @param sub subscription
   * @param request unsubscribe
   * @param response and the response
   */
  @SuppressWarnings("unchecked")
  public Notification(final Subscription sub,
                      final UnsubscribeRequestType request,
                      final UnsubscribeResponseType response) {
    this(sub, SynchEndType.NONE);
    addNotificationItem((NI)new NotificationItem(request, response));
  }

  /** Create a new refresh object
   *
   * @param sub subscription
   * @param request refresh
   * @param response and the response
   */
  @SuppressWarnings("unchecked")
  public Notification(final Subscription sub,
                      final RefreshRequestType request,
                      final RefreshResponseType response) {
    this(sub, SynchEndType.NONE);
    addNotificationItem((NI)new NotificationItem(request, response));
  }

  /** Create a new subscription status object
   *
   * @param sub subscription
   * @param request for status
   * @param response and the response
   */
  @SuppressWarnings("unchecked")
  public Notification(final Subscription sub,
                      final SubscriptionStatusRequestType request,
                      final SubscriptionStatusResponseType response) {
    this(sub, SynchEndType.NONE);
    addNotificationItem((NI)new NotificationItem(request, response));
  }

  /**
   * @param action for notification
   */
  @SuppressWarnings("unchecked")
  public Notification(final NotificationItem.ActionType action) {
    addNotificationItem((NI)new NotificationItem(action));
  }

  /**
   *
   * @param sub possibly updated subscription
   */
  public void setSub(final Subscription sub) {
    this.sub = sub;
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
   * @param val notification
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
      Refresh,

      /** */
      SubscriptionStatus,

      /** Getting system information */
      GetInfo,
    }

    private final ActionType action;

    private IcalendarType ical;

    private String uid;

    private SubscribeResponseType subResponse;

    private UnsubscribeRequestType unsubRequest;
    private UnsubscribeResponseType unsubResponse;

    private RefreshRequestType refreshRequest;
    private RefreshResponseType refreshResponse;

    private SubscriptionStatusRequestType subStatusReq;
    private SubscriptionStatusResponseType subStatusResponse;

    /** Create a notification item for an action.
     *
     * @param action for notification
     */
    public NotificationItem(final ActionType action) {
      this.action = action;
    }

    /** Create a notification item for an action.
     *
     * @param action for notification
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
     * @param subResponse to the subscribe
     */
    public NotificationItem(final SubscribeResponseType subResponse) {
      action = ActionType.NewSubscription;
      this.subResponse = subResponse;
    }

    /** Create a notification item for unsubscribe.
     *
     * @param unsubRequest to unsubscribe
     * @param unsubResponse to the unsubscribe
     */
    public NotificationItem(final UnsubscribeRequestType unsubRequest,
                            final UnsubscribeResponseType unsubResponse) {
      action = ActionType.Unsubscribe;
      this.unsubRequest = unsubRequest;
      this.unsubResponse = unsubResponse;
    }

    /** Create a notification item for refresh.
     *
     * @param refreshRequest to refresh
     * @param refreshResponse to the refresh
     */
    public NotificationItem(final RefreshRequestType refreshRequest,
                            final RefreshResponseType refreshResponse) {
      action = ActionType.Refresh;
      this.refreshRequest = refreshRequest;
      this.refreshResponse = refreshResponse;
    }

    /** Create a notification item for status.
     *
     * @param subStatusReq for status
     * @param subStatusResponse to the status request
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
    public RefreshRequestType getRefreshRequest() {
      return refreshRequest;
    }

    /**
     * @return response to a notification item
     */
    public RefreshResponseType getRefreshResponse() {
      return refreshResponse;
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

    protected void toStringSegment(final ToString ts) {
      ts.append("action", getAction())
        .append("uid", getUid());
    }

    @Override
    public String toString() {
      final var ts = new ToString(this);

      toStringSegment(ts);

      return ts.toString();
    }
  }

  protected void toStringSegment(final ToString ts) {
    ts.append("sub", getSub())
      .append("end", getEnd())
      .delimitersOff();

    String delim = ",\n   notification items{\n      ";
    for (final NI ni: getNotifications()) {
      ts.append(delim)
        .append(ni.toString());

      delim =",\n      ";
    }

    if (!getNotifications().isEmpty()) {
      ts.append("}");
    }
  }

  @Override
  public String toString() {
    final var ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
