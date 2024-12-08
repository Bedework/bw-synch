/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.synch.shared;

import org.bedework.synch.shared.cnctrs.Connector;
import org.bedework.synch.shared.cnctrs.Connector.NotificationBatch;
import org.bedework.synch.shared.cnctrs.ConnectorInstance;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.util.calendar.XcalUtil.TzGetter;
import org.bedework.util.timezones.Timezones;

import java.util.List;
import java.util.Set;

/**
 * User: mike Date: 3/11/18 Time: 00:26
 */
public interface SynchEngine {
  Timezones getTimezones();

  /**
   * @return true if this instance is only handling subscriptions
   */
  boolean subscriptionsOnly();

  /**
   * @param note Notification
   */
  void handleNotification(Notification<Notification.NotificationItem> note);

  /** When we start up a new subscription we implant a Connector in the object.
   *
   * @param sub Subscription
   */
  void setConnectors(Subscription sub);

  /** Reschedule a subscription now.
   *
   * @param id the subscription id
   */
  void rescheduleNow(String id);

  /** Reschedule a subscription for updates.
   *
   * @param sub the subscription
   * @param newSub true for new subscription
   */
  void reschedule(Subscription sub,
                  boolean newSub);

  /** Gets an instance and implants it in the subscription object.
   * @param sub Subscription
   * @param end end indicator
   * @return ConnectorInstance or throws Exception
   */
  ConnectorInstance<?> getConnectorInstance(Subscription sub,
                                            SynchEndType end);
  /**
   * @param sub Subscription
   */
  void addSubscription(Subscription sub);

  /**
   * @param sub Subscription
   */
  void deleteSubscription(Subscription sub);

  /**
   * @param sub Subscription
   */
  void updateSubscription(Subscription sub);

  /**
   * @param id for Subscription
   * @return subscription
   */
  Subscription getSubscription(String id);

  /** Find any subscription that matches this one. There can only be one with
   * the same endpoints
   *
   * @param sub Subscription
   * @return matching subscriptions
   */
  Subscription find(final Subscription sub);

  /**
   * @param val to decrypt
   * @return decrypted string
   */
  String decrypt(String val);

  /** Return a registered connector with the given id.
   *
   * @param id of connector
   * @return connector or null.
   */
  Connector<?, ?, ?> getConnector(String id);

  /**
   * @return registered ids.
   */
  Set<String> getConnectorIds();

  /** Processes a batch of notifications. This must be done in a timely manner
   * as a request is usually hanging on this.
   *
   * @param notes Notifications
   */
  void handleNotifications(
          final NotificationBatch<Notification> notes);


  /**
   * @return a getter for timezones
   */
  TzGetter getTzGetter();

  /** Start synch process.
   *
   */
  void start();

  /** Stop synch process.
   *
   */
  void stop();

  /**
   * @return stats for synch service bean
   */
  List<Stat> getStats();
}
