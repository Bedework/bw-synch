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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** The interface implemented by connectors. This represents the kind of object
 * used to communicate with a particular system or entity. We may implement
 * connectors for files, for exchange for bedework etc.
 *
 * <p>The connector instance carries out global initialization and provides
 * ConncetorInstance objects per subscription.
 *
 * @author Mike Douglass
 *
 * @param <C>
 * @param <N>
 */
public interface Connector<C extends ConnectorInstance,
                           N extends Notification> {
  /** Start the connector. A response of null means no synch available.
   *
   * <p>The callback url is unique to the connector. It will be used as a path
   * prefix to allow the callback service to locate the handler for incoming
   * callback requests.
   *
   * <p>For example, if the callback context is /synchcb/ and the connector id
   * is "bedework" then the callback uri might be /synchcb/bedework/. The
   * connector might append a uid to that path to allow it to locate the
   * active subscription for which the callback is intended.
   *
   * @param connectorId - registered id for the connector
   * @param callbackUri
   * @param syncher
   * @throws SynchException
   */
  void start(String connectorId,
             String callbackUri,
             SynchEngine syncher) throws SynchException;

  /**
   * @return id provided at start
   */
  String getId();

  /** List the information about properties required for subscriptions via this
   * connector.
   *
   * @return
   */
  List<ConnectorPropertyInfo> getPropertyInfo();

  /** Called to obtain a connector instance for a subscription.
   * A response of null means no synch available.
   *
   * @param sub - the subscription
   * @param local - true if this is 'local' end of subscription
   * @return null for no synch else a connector instance.
   * @throws SynchException
   */
  C getConnectorInstance(Subscription sub,
                         boolean local) throws SynchException;

  /** Far end may send a batch of notifications. These should not be batched
   * arbitrarily. One batch per message and response.
   *
   * @param <N>
   */
  static class NotificationBatch<N extends Notification> {
    private List<N> notifications = new ArrayList<N>();

    public NotificationBatch() {
    }

    public NotificationBatch(final N notification) {
      notifications.add(notification);
    }

    public List<N> getNotifications() {
      return notifications;
    }

    public void addNotification(final N notification) {
      notifications.add(notification);
    }
  }

  /** Will create a notification batch object which will be passed to a synchling for
   * processing. When processing is complete respond will be called.
   *
   * <p>The resource URI has been stripped of the context element and the
   * element which identifies the connector. What remains is used by the connector
   * to determine a subscription id allowing retrieval of the subscription from
   * the synch engine.
   *
   * @param req
   * @param resp
   * @param resourceUri - elements of the path with context and connector id removed
   * @return Notification with 1 or more Notification items or null for no action.
   * @throws SynchException
   */
  NotificationBatch<N> handleCallback(HttpServletRequest req,
                                      HttpServletResponse resp,
                                      String[] resourceUri) throws SynchException;

  /** Will respond to a notification.
   *
   * @param resp
   * @param notifications from handleCallback.
   * @throws SynchException
   */
  void respondCallback(HttpServletResponse resp,
                       NotificationBatch<N> notifications) throws SynchException;

  /** Shut down the connector
   * @throws SynchException
   */
  void stop() throws SynchException;
}
