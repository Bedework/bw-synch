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

import org.bedework.synch.SubscriptionConnectorInfo;
import org.bedework.synch.exception.SynchException;

/** Stores information about one end of a subscription for connector.
 *
 *
 * @author Mike Douglass
 */
public class BedeworkSubscriptionInfo {
  private SubscriptionConnectorInfo info;

  /**
   * @param info
   * @throws SynchException
   */
  public BedeworkSubscriptionInfo(final SubscriptionConnectorInfo info) throws SynchException {
    this.info = info;
    info.loadProperties();
  }

  /** Constructor
   *
   * @param subscriptionId - null means generate one
   * @param subscribe
   * @param calPath
   * @param principalHref
   * @throws SynchException
   */
  public BedeworkSubscriptionInfo(final String subscriptionId,
                              final boolean subscribe,
                              final String calPath,
                              final String principalHref) throws SynchException {
    info = new SubscriptionConnectorInfo();

    setPrincipalHref(principalHref);
    setCalPath(calPath);
  }

  /** Path to this systems calendar collection.
   *
   * @param val    String
   * @throws SynchException
   */
  public void setCalPath(final String val) throws SynchException {
    info.setProperty(BedeworkConnector.propnameCalendarHref, val);
  }

  /** Path to this systems calendar collection
   *
   * @return String
   * @throws SynchException
   */
  public String getCalPath() throws SynchException {
    return info.getProperty(BedeworkConnector.propnameCalendarHref);
  }


  /** Principal requesting synch service
   *
   * @param val    String
   * @throws SynchException
   */
  public void setPrincipalHref(final String val) throws SynchException {
    info.setProperty(BedeworkConnector.propnamePrincipal, val);
  }

  /** Principal requesting synch service
   *
   * @return String
   * @throws SynchException
   */
  public String getPrincipalHref() throws SynchException {
    return info.getProperty(BedeworkConnector.propnamePrincipal);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  protected void toStringSegment(final StringBuilder sb,
                              final String indent) {
    try {
      sb.append(",\n");
      sb.append(indent);
      sb.append("calPath = ");
      sb.append(getCalPath());
      sb.append("principalHref = ");
      sb.append(getPrincipalHref());
    } catch (Throwable t) {
      sb.append(t.getMessage());
    }
  }

  /* ====================================================================
   *                        Object methods
   * ==================================================================== */

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    toStringSegment(sb, "  ");

    sb.append("}");
    return sb.toString();
  }
}
