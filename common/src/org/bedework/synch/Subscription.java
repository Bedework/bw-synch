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

import org.bedework.synch.wsmessages.SynchDirectionType;
import org.bedework.synch.wsmessages.SynchMasterType;

import java.util.UUID;

/** Represents a subscription for the synch engine.
 *
 * <p>A subscription has 2 connections, one for each end of the subscription. We
 * will refer to these as the local and remote ends even though the subscription
 * can be symmetric and either end can physically local or remote.
 *
 * <p>By default the remote end is the 'source' of the information and the local
 * end will track the remote end. However, in a two way synch be either or
 * neither end can nominated the 'master' calendar
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
 * @author Mike Douglass
 */
@SuppressWarnings("rawtypes")
public class Subscription implements Comparable<Subscription> {
  private long id;

  private int seq;

  private String subscriptionId;

  private SubscriptionConnectorInfo localConnectorInfo;

  private SubscriptionConnectorInfo remoteConnectorInfo;

  private SynchDirectionType direction;

  private SynchMasterType master;

  /* Following not persisted */

  /* False for unsubscribe */
  private boolean subscribe;

  /* Process outstanding after this */
  private Subscription outstandingSubscription;

  private ConnectorInstance localConn;

  private ConnectorInstance remoteConn;

  /** null constructor for hibernate
   *
   */
  public Subscription() {
  }

  /** Constructor to create a new subscription.
   *
   * @param subscriptionId - null means generate one
   * @param subscribe
   */
  public Subscription(final String subscriptionId,
                      final boolean subscribe) {
    if (subscriptionId == null) {
      this.subscriptionId = UUID.randomUUID().toString();
    } else {
      this.subscriptionId = subscriptionId;
    }
    this.subscribe = subscribe;
  }

  /**
   * @param val
   */
  public void setId(final Long val) {
    id = val;
  }

  /**
   * @return Long id
   */
  public Long getId() {
    return id;
  }

  /**
   * @return true if this entity is not saved.
   */
  public boolean unsaved() {
    return getId() == null;
  }

  /** Set the seq for this entity
   *
   * @param val    int seq
   */
  public void setSeq(final Integer val) {
    seq = val;
  }

  /** Get the entity seq
   *
   * @return int    the entity seq
   */
  public Integer getSeq() {
    return seq;
  }

  /** Our generated subscriptionId.
   *
   * @param val    String
   */
  public void setSubscriptionId(final String val) {
    subscriptionId = val;
  }

  /** Our generated subscriptionId.
   *
   * @return String
   */
  public String getSubscriptionId() {
    return subscriptionId;
  }

  /** Info for the local connector.
   *
   * @param val SubscriptionConnectorInfo
   */
  public void setLocalConnectorInfo(final SubscriptionConnectorInfo val) {
	  localConnectorInfo = val;
  }

  /**
   * @return SubscriptionConnectorInfo
   */
  public SubscriptionConnectorInfo getLocalConnectorInfo() {
    return localConnectorInfo;
  }

  /** Info for the remote connector.
   *
   * @param val    SubscriptionConnectorInfo
   */
  public void setRemoteConnectorInfo(final SubscriptionConnectorInfo val) {
	  remoteConnectorInfo = val;
  }

  /**
   * @return SubscriptionConnectorInfo
   */
  public SubscriptionConnectorInfo getRemoteConnectorInfo() {
    return remoteConnectorInfo;
  }

  /** Which way?
   *
   * @param val
   */
  public void setDirection(final SynchDirectionType val) {
    direction = val;
  }

  /** Which way?
   *
   * @return direction
   */
  public SynchDirectionType getDirection() {
    return direction;
  }

  /** Which end is master?
   *
   * @param val
   */
  public void setMaster(final SynchMasterType val) {
    master = val;
  }

  /**
   * @return who's master
   */
  public SynchMasterType getMaster() {
    return master;
  }

  /** (un)subscribe?
   *
   * @param val    boolean
   */
  public void setSubscribe(final boolean val) {
    subscribe = val;
  }

  /** (un)subscribe?
   *
   * @return boolean
   */
  public boolean getSubscribe() {
    return subscribe;
  }

  /** An outstanding request that requires an unsubscribe to complete first
   *
   * @param val Subscription
   */
  public void setOutstandingSubscription(final Subscription val) {
    outstandingSubscription = val;
  }

  /** An outstanding request that requires an unsubscribe to complete first
   *
   * @return Subscription
   */
  public Subscription getOutstandingSubscription() {
    return outstandingSubscription;
  }

  /**
   * @param val a connection instance
   */
  public void setLocalConn(final ConnectorInstance val) {
    localConn = val;
  }

  /**
   * @return a connection instance or null
   */
  public ConnectorInstance getLocalConn() {
    return localConn;
  }

  /**
   * @param val a connection instance
   */
  public void setRemoteConn(final ConnectorInstance val) {
    remoteConn = val;
  }

  /**
   * @return a connection instance or null
   */
  public ConnectorInstance getRemoteConn() {
    return remoteConn;
  }

  /** equality just checks the path. Look at the rest.
   *
   * @param that
   * @return true if anything changed
   */
  public boolean changed(final Subscription that) {
    if (!equals(that)) {
      return true;
    }

    if (!getLocalConnectorInfo().equals(that.getLocalConnectorInfo())) {
      return true;
    }

    if (!getRemoteConnectorInfo().equals(that.getRemoteConnectorInfo())) {
      return true;
    }

    if (getSubscribe() != that.getSubscribe()) {
      return true;
    }

    return false;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the StringBuilder
   *
   * @param sb    StringBuilder for result
   */
  protected void toStringSegment(final StringBuilder sb,
                                 final String indent) {
    sb.append("id = ");
    sb.append(getId());
    sb.append(", seq = ");
    sb.append(getSeq());

    sb.append(",\n");
    sb.append(indent);
    sb.append("subscriptionId = ");
    sb.append(getSubscriptionId());

    sb.append(",\n");
    sb.append(indent);
    sb.append("localConnectorInfo = ");
    sb.append(getLocalConnectorInfo());

    sb.append(",\n");
    sb.append(indent);
    sb.append("remoteConnectorInfo = ");
    sb.append(getRemoteConnectorInfo());

    sb.append(",\n");
    sb.append(indent);
    sb.append("subscribe = ");
    sb.append(getSubscribe());

    sb.append(",\n");
    sb.append(indent);
    sb.append("direction = ");
    sb.append(getDirection());

    sb.append(", master = ");
    sb.append(getMaster());
  }

  /* ====================================================================
   *                   Object methods
   * The following are required for a db object.
   * ==================================================================== */

  @Override
  public int hashCode() {
    return getSubscriptionId().hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final Subscription that) {
    if (this == that) {
      return 0;
    }

    return getSubscriptionId().compareTo(that.getSubscriptionId());
  }

  @Override
  public boolean equals(final Object o) {
    return compareTo((Subscription)o) == 0;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    toStringSegment(sb, "  ");

    if (getOutstandingSubscription() != null) {
      sb.append(", \n  OustandingSubscription{");

      toStringSegment(sb, "    ");
      sb.append("  }");
    }

    sb.append("}");
    return sb.toString();
  }
}
