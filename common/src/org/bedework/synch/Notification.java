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

import org.bedework.synch.wsmessages.SubscribeRequestType;

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

  private boolean local;

  private List<NI> notifications = new ArrayList<NI>();

  /** Create a notification for a subscription
   * @param sub
   * @param local
   */
  public Notification(final Subscription sub,
                      final boolean local) {
    this.sub = sub;
    this.local = local;
  }

  /** Create object with a single notification.
   *
   * @param sub
   * @param local
   * @param notificationItem
   */
  public Notification(final Subscription sub,
                      final boolean local,
                      final NI notificationItem) {
    this(sub, local);
    addNotificationItem(notificationItem);
  }

  /**
   * @return Subscription
   */
  public Subscription getSub() {
    return sub;
  }

  /**
   * @return boolean local flag
   */
  public boolean getLocal() {
    return local;
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
    }

    private ActionType action;

    private IcalendarType ical;

    private String uid;

    private SubscribeRequestType subscribeRequest;

    /** Create a notification item for an action.
     *
     * @param action
     * @param ical - the entity if available
     * @param uid - Uid for the entity
     */
    public NotificationItem(final ActionType action,
                            final IcalendarType ical,
                            final String uid) {
      this.action = action;
      this.ical = ical;
      this.uid = uid;
    }

    /** Create a notification item for a subscription.
     *
     * @param subscribeRequest
     */
    public NotificationItem(final SubscribeRequestType subscribeRequest) {
      action = ActionType.NewSubscription;
      this.subscribeRequest = subscribeRequest;
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

    sb.append(", local=");
    sb.append(getLocal());

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
