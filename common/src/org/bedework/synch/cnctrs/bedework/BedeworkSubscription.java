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
package org.bedework.synch.cnctrs.bedework;

import org.bedework.synch.BaseSubscription;

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
public class BedeworkSubscription extends BaseSubscription<BedeworkSubscription> {
  private String calPath;

  private String principalHref;

  /** null constructor for hibernate
   *
   */
  public BedeworkSubscription() {
  }

  /** Constructor
   *
   * @param subscriptionId - null means generate one
   * @param subscribe
   * @param calPath
   * @param principalHref
   */
  public BedeworkSubscription(final String subscriptionId,
                              final boolean subscribe,
                              final String calPath,
                              final String principalHref) {
    super(subscriptionId, subscribe);

    this.principalHref = principalHref;
    this.calPath = calPath;
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
  public void setPrincipalHref(final String val) {
    principalHref = val;
  }

  /** Principal requesting synch service
   *
   * @return String
   */
  public String getPrincipalHref() {
    return principalHref;
  }

  /** equality just checks the path. Look at the rest.
   *
   * @param that
   * @return true if anything changed
   */
  @Override
  public boolean changed(final BedeworkSubscription that) {
    if (super.changed(that)) {
      return true;
    }

    if (!getCalPath().equals(that.getCalPath())) {
      return true;
    }

    if (!getPrincipalHref().equals(that.getPrincipalHref())) {
      return true;
    }

    return false;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  @Override
  protected void toStringSegment(final StringBuilder sb,
                              final String indent) {
    super.toStringSegment(sb, indent);

    sb.append(",\n");
    sb.append(indent);
    sb.append("calPath = ");
    sb.append(getCalPath());
    sb.append("principalHref = ");
    sb.append(getPrincipalHref());
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
  @Override
  public int compareTo(final BedeworkSubscription that) {
    if (this == that) {
      return 0;
    }

    return getSubscriptionId().compareTo(that.getSubscriptionId());
  }

  @Override
  public boolean equals(final Object o) {
    return compareTo((BedeworkSubscription)o) == 0;
  }
}
