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

import org.bedework.synch.shared.cnctrs.Connector;
import org.bedework.synch.shared.cnctrs.ConnectorInstance;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchDirectionType;
import org.bedework.synch.wsmessages.SynchMasterType;

import java.util.Date;

/** Represents a subscription for the synch engine.
 *
 * <p>A subscription has 2 connections, one for each end of the subscription. We
 * will refer to these as endA and endB.
 *
 * <p>Each connection has a kind which is a name used to retrieve a connector
 * from the synch engine. The retrieved connector implements the Connector
 * interface. This connector object can then be used to retrieve a ConnectionInst
 * implementation which uses information stored in a serializable object to
 * obtain connection specific properties such as id and password.
 *
 * <p>These properties are obtained by presenting the user with a list of
 * required properties and then encrypting and storing the response. The
 * serialized result is stored as a field in the subscription.
 *
 * <p>Connections are either polling or notify. Polling means that
 * the host will be polled to see if anything has changed. Notify means that
 * the subscription will be activated when the system is notified of a change.
 *
 * <p>Connections are also resynch only - that is the far end does not support
 * fetching of individual items but must be completely resynched each time, or
 * the support the full synch feature set.
 *
 * <p>Resynch connections support relatively simple protocols or file synch.
 *
 * <p>The full feature connections are used for bedework, Exchange etc.
 *
 * <h1>Skip Lists</h1>
 * A skip list allows the diffing process to skip properties that are not to be
 * considered, for example lastmod. We create a skip list from 3 lists;<ul>
 * <li>one for each end of the subscription. This marks properties used
 * exclusively by that end, for example x-properties.</li>
 * <li> One for the middle which might skip properties we want to ignore such as
 * alarms</li>
 * </ul>
 *
 * <p>An empty list means exactly that, no skip properties. A null list means
 * the default diff skip list - probably more useful.
 *
 * @author Mike Douglass
 */
public interface Subscription {
  /** Our generated subscriptionId.
   *
   * @param val    String
   */
  void setSubscriptionId(String val);

  /** Our generated subscriptionId.
   *
   * @return String
   */
  String getSubscriptionId();

  /**
   *
   * @param val A UTC dtstamp value
   */
  void setLastRefresh(String val);

  /**
   * @return String lastRefresh
   */
  String getLastRefresh();

  /**
   *
   * @param val int consecutive errors
   */
  void setErrorCt(int val);

  /**
   * @return int consecutive errors
   */
  int getErrorCt();

  /**
   *
   * @param val  True if either target is missing
   */
  void setMissingTarget(boolean val);

  /**
   * @return True if either target is missing
   */
  boolean getMissingTarget();

  /** The owner. This is the (verified) account that set up the subscription.
   * It is either the authenticated user or provided by a proxy that has
   * verified the account. The owner is only needed for subcribing, unsubscribing
   * and display of and updates to the subscription itself.
   *
   * <p>Interactions with the end points use information contained within the
   * subscription.
   *
   * @param val    String
   */
  void setOwner(String val);

  /** Owner
   *
   * @return String
   */
  String getOwner();

  /** Info for the endA connector.
   *
   * @param val SubscriptionConnectorInfo
   */
  void setEndAConnectorInfo(SubscriptionConnectorInfo val);

  /**
   * @return SubscriptionConnectorInfo
   */
  SubscriptionConnectorInfo getEndAConnectorInfo();

  /** Info for the endB connector.
   *
   * @param val    SubscriptionConnectorInfo
   */
  void setEndBConnectorInfo(SubscriptionConnectorInfo val);

  /**
   * @return SubscriptionConnectorInfo
   */
  SubscriptionConnectorInfo getEndBConnectorInfo();

  /** Info for the subscription.
   *
   * @param val    SubscriptionInfo
   */
  void setInfo(SubscriptionInfo val);

  /**
   * @return SubscriptionInfo
   */
  SubscriptionInfo getInfo();

  /**
   *
   * @param val Which way?
   */
  void setDirection(SynchDirectionType val);

  /** Which way?
   *
   * @return direction
   */
  SynchDirectionType getDirection();

  /**
   *
   * @param val Which end is master?
   */
  void setMaster(SynchMasterType val);

  /**
   * @return who's master
   */
  SynchMasterType getMaster();

  /** An outstanding request that requires an unsubscribe to complete first
   *
   * @param val Subscription
   */
  void setOutstandingSubscription(Subscription val);

  /** An outstanding request that requires an unsubscribe to complete first
   *
   * @return Subscription
   */
  Subscription getOutstandingSubscription();

  /**
   *
   * @param val True if subscription deleted
   */
  void setDeleted(boolean val);

  /**
   * @return True if deleted
   */
  boolean getDeleted();

  /**
   * @param val a connection
   */
  void setEndAConn(Connector val);

  /**
   * @return a connection or null
   */
  Connector getEndAConn();

  /**
   * @param val a connection
   */
  void setEndBConn(Connector val);

  /**
   * @return a connection or null
   */
  Connector getEndBConn();

  /**
   * @param val a connection instance
   */
  void setEndAConnInst(ConnectorInstance val);

  /**
   * @return a connection instance or null
   */
  ConnectorInstance getEndAConnInst();

  /**
   * @param val a connection instance
   */
  void setEndBConnInst(ConnectorInstance val);

  /**
   * @return a connection instance or null
   */
  ConnectorInstance getEndBConnInst();

  /**
   * @return true if any connector info changed
   */
  boolean changed();

  /**
   * reset the changed flag.
   */
  void resetChanged();

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @return true if this has to be put on a poll queue
   */
  boolean polling();

  /**
   * @return the delay in millisecs.
   * @throws SynchException on properties error
   */
  long refreshDelay() throws SynchException;

  /** Set the lastRefresh from the current time
   *
   */
  void updateLastRefresh();

  /** Get a next refresh date based on the last refresh value
   *
   * @return date value incremented by delay.
   * @throws SynchException on properties error
   */
  Date nextRefresh() throws SynchException;
}
