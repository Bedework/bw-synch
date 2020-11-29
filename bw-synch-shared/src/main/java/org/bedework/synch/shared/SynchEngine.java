/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.synch.shared;

import org.bedework.synch.shared.cnctrs.Connector;
import org.bedework.synch.shared.cnctrs.Connector.NotificationBatch;
import org.bedework.synch.shared.cnctrs.ConnectorInstance;
import org.bedework.synch.shared.exception.SynchException;
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
   * @param note
   */
  void handleNotification(Notification<?> note);

  /** When we start up a new subscription we implant a Connector in the object.
   *
   * @param sub
   * @throws SynchException
   */
  void setConnectors(Subscription sub) throws SynchException;

  /** Reschedule a subscription now.
   *
   * @param id the subscription id
   */
  void rescheduleNow(String id) throws SynchException;

  /** Reschedule a subscription for updates.
   *
   * @param sub the subscription
   * @param newSub true for new subscription
   */
  void reschedule(Subscription sub,
                  boolean newSub);

  /** Gets an instance and implants it in the subscription object.
   * @param sub
   * @param end
   * @return ConnectorInstance or throws Exception
   * @throws SynchException
   */
  ConnectorInstance getConnectorInstance(Subscription sub,
                                         SynchEndType end)
          throws SynchException;
  /**
   * @param sub
   * @throws SynchException
   */
  void addSubscription(Subscription sub) throws SynchException;

  /**
   * @param sub
   * @throws SynchException
   */
  void deleteSubscription(Subscription sub) throws SynchException;

  /**
   * @param sub
   * @throws SynchException
   */
  void updateSubscription(Subscription sub) throws SynchException;

  /**
   * @param id
   * @return subscription
   * @throws SynchException
   */
  Subscription getSubscription(String id) throws SynchException;

  /** Find any subscription that matches this one. There can only be one with
   * the same endpoints
   *
   * @param sub
   * @return matching subscriptions
   * @throws SynchException
   */
  Subscription find(final Subscription sub) throws SynchException;

  /**
   * @param val
   * @return decrypted string
   * @throws SynchException
   */
  String decrypt(String val) throws SynchException;

  /** Return a registered connector with the given id.
   *
   * @param id
   * @return connector or null.
   */
  Connector getConnector(String id);

  /**
   * @return registered ids.
   */
  Set<String> getConnectorIds();

  /** Processes a batch of notifications. This must be done in a timely manner
   * as a request is usually hanging on this.
   *
   * @param notes
   * @throws SynchException
   */
  void handleNotifications(
          final NotificationBatch<Notification> notes) throws SynchException;


  /**
   * @return a getter for timezones
   */
  TzGetter getTzGetter();

  /** Start synch process.
   *
   * @throws SynchException
   */
  void start() throws SynchException;

  /** Stop synch process.
   *
   */
  void stop();

  /**
   * @return stats for synch service bean
   */
  List<Stat> getStats();
}
