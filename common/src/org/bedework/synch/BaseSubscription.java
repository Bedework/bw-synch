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

import java.util.UUID;

/** Represents a subscription for the synch engine.
 * 
 * <p>A subscription has 2 connectors for each end of the subscription. We will
 * refer to these as the local and remote ends even though the subscription can
 * be symmetric and either end can be nominated the 'master' calendar.
 * 
 * <p>Each connector has a kind which is a name used to retrieve a connector
 * from the connector manager. The retrieved connector implements the SynchIntf
 * interface and provides a serializable object to store connection specific
 * properties such as id and password.
 * 
 * <p>These properties are obtained by presenting the user with a list of 
 * required properties and then encrypting and storing the response.
 *
 * @author Mike Douglass
 */
public class BaseSubscription implements Comparable<BaseSubscription> {
  private long id;

  private int seq;

  private String subscriptionId;

  private String localConnectorId;

  private String localConnectorProperties;

  private String remoteConnectorId;

  private String remoteConnectorProperties;

  /* Following not persisted */

  /* False for unsubscribe */
  private boolean subscribe;

  /* Process outstanding after this */
  private BaseSubscription outstandingSubscription;

  /** null constructor for hibernate
   *
   */
  public BaseSubscription() {
  }

  /** Constructor to create a new subscription.
   *
   * @param subscriptionId - null means generate one
   */
  public BaseSubscription(final String subscriptionId) {
    if (subscriptionId == null) {
      this.subscriptionId = UUID.randomUUID().toString();
    } else {
      this.subscriptionId = subscriptionId;
    }
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

  /** Id of the local connector.
   *
   * @param val    String
   */
  public void setLocalConnectorId(final String val) {
	  localConnectorId = val;
  }

  /** 
   * @return String
   */
  public String getLocalConnectorId() {
    return localConnectorId;
  }

  /** Serialized and encrypted properties
   *
   * @param val    String
   */
  public void setLocalConnectorProperties(final String val) {
	  localConnectorProperties = val;
  }

  /** Serialized and encrypted properties
   *
   * @return String
   */
  public String getLocalConnectorProperties() {
    return localConnectorProperties;
  }

  /** Id of the remote connector.
   *
   * @param val    String
   */
  public void setRemoteConnectorId(final String val) {
	  remoteConnectorId = val;
  }

  /** 
   * @return String
   */
  public String getRemoteConnectorId() {
    return remoteConnectorId;
  }

  /** Serialized and encrypted properties
   *
   * @param val    String
   */
  public void setRemoteConnectorProperties(final String val) {
	  remoteConnectorProperties = val;
  }

  /** Serialized and encrypted properties
   *
   * @return String
   */
  public String getRemoteConnectorProperties() {
    return remoteConnectorProperties;
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
   * @param val    ExchangeSubscription
   */
  public void setOutstandingSubscription(final BaseSubscription val) {
    outstandingSubscription = val;
  }

  /** An outstanding request that requires an unsubscribe to complete first
   *
   * @return ExchangeSubscription
   */
  public BaseSubscription getOutstandingSubscription() {
    return outstandingSubscription;
  }

  /** equality just checks the path. Look at the rest.
   *
   * @param that
   * @return true if anything changed
   */
  public boolean changed(final BaseSubscription that) {
    if (!equals(that)) {
      return true;
    }

    if (!getLocalConnectorId().equals(that.getLocalConnectorId())) {
      return true;
    }

    if (!getLocalConnectorProperties().equals(that.getLocalConnectorProperties())) {
      return true;
    }

    if (!getRemoteConnectorId().equals(that.getRemoteConnectorId())) {
      return true;
    }

    if (!getRemoteConnectorProperties().equals(that.getRemoteConnectorProperties())) {
      return true;
    }

    if (getSubscribe() != that.getSubscribe()) {
      return true;
    }

    return false;
  }

  /* ====================================================================
   *                        Object methods
   * ==================================================================== */

  @Override
  public int hashCode() {
    return getSubscriptionId().hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ExchangeSubscription{");

    toStringSegment(sb, "  ");

    if (getOutstandingSubscription() != null) {
      sb.append(", \n  OustandingSubscription{");

      toStringSegment(sb, "    ");
      sb.append("  }");
    }

    sb.append("}");
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(final BaseSubscription that) {
    if (this == that) {
      return 0;
    }

    return getSubscriptionId().compareTo(that.getSubscriptionId());
  }

  @Override
  public boolean equals(final Object o) {
    return compareTo((BaseSubscription)o) == 0;
  }

  /* ====================================================================
   *                        Private methods
   * ==================================================================== */

  private void toStringSegment(final StringBuilder sb,
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
    sb.append("localConnectorId = ");
    sb.append(getLocalConnectorId());

    sb.append(",\n");
    sb.append(indent);
    sb.append("localConnectorProperties = ");
    sb.append(getLocalConnectorProperties());


    sb.append(",\n");
    sb.append(indent);
    sb.append("remoteConnectorId = ");
    sb.append(getRemoteConnectorId());

    sb.append(",\n");
    sb.append(indent);
    sb.append("remoteConnectorProperties = ");
    sb.append(getRemoteConnectorProperties());

    sb.append(",\n");
    sb.append(indent);
    sb.append("subscribe = ");
    sb.append(getSubscribe());
  }
}
