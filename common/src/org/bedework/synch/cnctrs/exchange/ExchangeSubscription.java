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
package org.bedework.synch.cnctrs.exchange;

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
public class ExchangeSubscription implements Comparable<ExchangeSubscription> {
  private long id;

  private int seq;

  private String subscriptionId;

  private String calPath;

  private String principalHref;

  private String exchangeCalendar;

  private String exchangeId;

  private String exchangePw;

  private String exchangeURI;

  private String exchangeSubscriptionId;

  private String exchangeWatermark;

  private String exchangeError;

  /* Following not persisted */

  /* False for unsubscribe */
  private boolean subscribe;

  /* Process outstanding after this */
  private ExchangeSubscription outstandingSubscription;

  /** null constructor for hibernate
   *
   */
  public ExchangeSubscription() {
  }

  /** Constructor
   *
   * @param subscriptionId - null means generate one
   * @param calPath
   * @param principalHref
   * @param exchangeCalendar
   * @param exchangeId
   * @param exchangePw
   * @param exchangeURI
   * @param subscribe
   */
  public ExchangeSubscription(final String subscriptionId,
                              final String calPath,
                              final String principalHref,
                              final String exchangeCalendar,
                              final String exchangeId,
                              final String exchangePw,
                              final String exchangeURI,
                              final boolean subscribe) {
    if (subscriptionId == null) {
      this.subscriptionId = UUID.randomUUID().toString();
    } else {
      this.subscriptionId = subscriptionId;
    }

    this.principalHref = principalHref;
    this.calPath = calPath;
    this.exchangeCalendar = exchangeCalendar;
    this.exchangeId = exchangeId;
    this.exchangePw = exchangePw;
    this.exchangeURI = exchangeURI;
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

  /** Path to this systems calendar collection.
   *
   * @param val    String
   */
  public void setCalPath(final String val) {
    calPath = val;
  }

  /** Path to this systems calendar collection
   *
   * @return String
   */
  public String getCalPath() {
    return calPath;
  }


  /** Principal requesting synch service
   *
   * @param val    String
   */
  public void setprincipalHref(final String val) {
    principalHref = val;
  }

  /** Principal requesting synch service
   *
   * @return String
   */
  public String getprincipalHref() {
    return principalHref;
  }

  /** Exchange Calendar
   *
   * @param val    String
   */
  public void setExchangeCalendar(final String val) {
    exchangeCalendar = val;
  }

  /** Exchange Calendar
   *
   * @return String
   */
  public String getExchangeCalendar() {
    return exchangeCalendar;
  }

  /** Exchange Calendar id
   *
   * @param val    String
   */
  public void setExchangeId(final String val) {
    exchangeId = val;
  }

  /** Exchange Calendar id
   *
   * @return String
   */
  public String getExchangeId() {
    return exchangeId;
  }

  /** Encoded exchange Calendar pw
   *
   * @param val    String
   */
  public void setExchangePw(final String val) {
    exchangePw = val;
  }

  /** Encoded xchange Calendar pw
   *
   * @return String
   */
  public String getExchangePw() {
    return exchangePw;
  }

  /** Exchange web service uri
   *
   * @param val    String
   */
  public void setExchangeURI(final String val) {
    exchangeURI = val;
  }

  /** Exchange web service uri
   *
   * @return String
   */
  public String getExchangeURI() {
    return exchangeURI;
  }

  /** Exchange system subscriptionId.
   *
   * @param val    String
   */
  public void setExchangeSubscriptionId(final String val) {
    exchangeSubscriptionId = val;
  }

  /** Exchange system subscriptionId.
   *
   * @return String
   */
  public String getExchangeSubscriptionId() {
    return exchangeSubscriptionId;
  }

  /** Exchange watermark.
   *
   * @param val    String
   */
  public void setExchangeWatermark(final String val) {
    exchangeWatermark = val;
  }

  /** Exchange watermark.
   *
   * @return String
   */
  public String getExchangeWatermark() {
    return exchangeWatermark;
  }

  /** Exchange error code.
   *
   * @param val    String
   */
  public void setExchangeError(final String val) {
    exchangeError = val;
  }

  /** Exchange error code.
   *
   * @return String
   */
  public String getExchangeError() {
    return exchangeError;
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
  public void setOutstandingSubscription(final ExchangeSubscription val) {
    outstandingSubscription = val;
  }

  /** An outstanding request that requires an unsubscribe to complete first
   *
   * @return ExchangeSubscription
   */
  public ExchangeSubscription getOutstandingSubscription() {
    return outstandingSubscription;
  }

  /** equality just checks the path. Look at the rest.
   *
   * @param that
   * @return true if anything changed
   */
  public boolean changed(final ExchangeSubscription that) {
    if (!equals(that)) {
      return true;
    }

    if (!getprincipalHref().equals(that.getprincipalHref())) {
      return true;
    }

    if (!getExchangeCalendar().equals(that.getExchangeCalendar())) {
      return true;
    }

    if (!getExchangeURI().equals(that.getExchangeURI())) {
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
  public int compareTo(final ExchangeSubscription that) {
    if (this == that) {
      return 0;
    }

    return getSubscriptionId().compareTo(that.getSubscriptionId());
  }

  @Override
  public boolean equals(final Object o) {
    return compareTo((ExchangeSubscription)o) == 0;
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
    sb.append("calPath = ");
    sb.append(getCalPath());

    sb.append(",\n");
    sb.append(indent);
    sb.append("principalHref = ");
    sb.append(getprincipalHref());

    sb.append(",\n");
    sb.append(indent);
    sb.append("exchangeCalendar = ");
    sb.append(getExchangeCalendar());
    sb.append(", exchangeURI = ");
    sb.append(getExchangeURI());

    sb.append(",\n");
    sb.append(indent);
    sb.append("subscribe = ");
    sb.append(getSubscribe());
  }
}
