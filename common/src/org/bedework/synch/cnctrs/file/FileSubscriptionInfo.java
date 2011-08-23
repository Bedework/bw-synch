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

  /* properties saved by connector instance */

  /** */
  public static final String propnameEtag = "etag";

  /** */
  public static final String propnameLastRefreshStatus = "last-refresh-status";

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
   * @param uri
   * @param principalHref
   * @param password
   * @throws SynchException
   */
  public FileSubscriptionInfo(final String subscriptionId,
                              final boolean subscribe,
                              final String uri,
                              final String principalHref,
                              final String password) throws SynchException {
    info = new SubscriptionConnectorInfo();

    setUri(uri);
    setPrincipalHref(principalHref);
    setPassword(password);
  }

  /** Path to the calendar collection.
   *
   * @param val    String
   * @throws SynchException
   */
  public void setUri(final String val) throws SynchException {
    info.setProperty(FileConnector.propnameUri, val);
  }

  /** Path to the calendar collection
   *
   * @return String
   * @throws SynchException
   */
  public String getUri() throws SynchException {
    return info.getProperty(FileConnector.propnameUri);
  }

  /** Principal requesting synch service
   *
   * @param val    String
   * @throws SynchException
   */
  public void setPrincipalHref(final String val) throws SynchException {
    info.setProperty(FileConnector.propnamePrincipal, val);
  }

  /** Principal requesting synch service
   *
   * @return String
   * @throws SynchException
   */
  public String getPrincipalHref() throws SynchException {
    return info.getProperty(FileConnector.propnamePrincipal);
  }

  /** Principals password
   *
   * @param val    String
   * @throws SynchException
   */
  public void setPassword(final String val) throws SynchException {
    info.setProperty(FileConnector.propnamePassword, val);
  }

  /** Principal password
   *
   * @return String
   * @throws SynchException
   */
  public String getPassword() throws SynchException {
    return info.getProperty(FileConnector.propnamePassword);
  }

  /** etag
   *
   * @param val    String
   * @throws SynchException
   */
  public void setEtag(final String val) throws SynchException {
    info.setProperty(propnameEtag, val);
  }

  /** Etag
   *
   * @return String
   * @throws SynchException
   */
  public String getEtag() throws SynchException {
    return info.getProperty(propnameEtag);
  }

  /** HTTP status or other appropriate value
   * @param val
   * @throws SynchException
   */
  public void setLastRefreshStatus(final String val) throws SynchException {
    info.setProperty(propnameLastRefreshStatus, val);
  }

  /**
   * @return String lastRefreshStatus
   * @throws SynchException
   */
  public String getLastRefreshStatus() throws SynchException {
    return info.getProperty(propnameLastRefreshStatus);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  protected void toStringSegment(final StringBuilder sb,
                              final String indent) {
    try {
      sb.append(",\n");
      sb.append(indent);
      sb.append("uri = ");
      sb.append(getUri());
      sb.append("principalHref = ");
      sb.append(getPrincipalHref());
      sb.append("password = ");
      sb.append(getPassword());
      sb.append("etag = ");
      sb.append(getEtag());
      sb.append("lastRefreshStatus = ");
      sb.append(getLastRefreshStatus());
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
