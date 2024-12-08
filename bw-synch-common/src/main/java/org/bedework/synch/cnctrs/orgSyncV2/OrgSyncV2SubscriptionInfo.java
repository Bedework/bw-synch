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
package org.bedework.synch.cnctrs.orgSyncV2;

import org.bedework.synch.shared.BaseSubscriptionInfo;
import org.bedework.synch.shared.SubscriptionConnectorInfo;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.util.misc.ToString;

/** Represents connection information for an orgsync v2 connector instance.
 *
 * @author Mike Douglass
 */
public class OrgSyncV2SubscriptionInfo extends BaseSubscriptionInfo {

  /**
   * @param info the subscription info
   * @throws SynchException on load error
   */
  public OrgSyncV2SubscriptionInfo(final SubscriptionConnectorInfo<?> info) {
    super(info);
  }

  /**
   * @param val OrgSyncPublicOnly
   * @throws SynchException on property error
   */
  public void setOrgSyncPublicOnly(final boolean val) {
    setProperty(propnameOrgSyncPublicOnly, String.valueOf(val));
  }

  /**
   * @return boolean OrgSyncPublicOnly
   * @throws SynchException on property error
   */
  public boolean getOrgSyncPublicOnly() {
    return Boolean.parseBoolean(getProperty(propnameOrgSyncPublicOnly));
  }

  /**
   * @return String location key
   * @throws SynchException on property error
   */
  public String getLocationKey() {
    return getProperty(propnameLocKey);
  }

  protected void toStringSegment(final ToString ts) {
    try {
      ts.append("uri", getUri())
        .newLine()
        .append("orgSyncPublicOnly", getOrgSyncPublicOnly());
    } catch (final Throwable t) {
      ts.append(t.getMessage());
    }
  }
}
