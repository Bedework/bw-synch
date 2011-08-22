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
package org.bedework.synch.cnctrs.file;

import org.bedework.synch.SubscriptionConnectorInfo;
import org.bedework.synch.exception.SynchException;

/** Represents a subscription for the synch engine.
 *
 * <p>A subscription has 2 connectors for each end of the subscription, endA
 * and endB.
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
public class FileSubscriptionInfo {
  private SubscriptionConnectorInfo info;

  private String calPath;

  private String principalHref;

  /**
   * @param info
   * @throws SynchException
   */
  public FileSubscriptionInfo(final SubscriptionConnectorInfo info) throws SynchException {
    this.info = info;
    info.loadProperties();
  }

  /** Constructor
   *
   * @param subscriptionId - null means generate one
   * @param subscribe
   * @param calPath
   * @param principalHref
   */
  public FileSubscriptionInfo(final String subscriptionId,
                              final boolean subscribe,
                              final String calPath,
                              final String principalHref) {
    info = new SubscriptionConnectorInfo();

    setPrincipalHref(principalHref);
    setCalPath(calPath);
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

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  protected void toStringSegment(final StringBuilder sb,
                              final String indent) {
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
  public String toString() {
    StringBuilder sb = new StringBuilder("BedeworkSubscriptionInfo{");

    toStringSegment(sb, "  ");

    sb.append("}");
    return sb.toString();
  }
}
