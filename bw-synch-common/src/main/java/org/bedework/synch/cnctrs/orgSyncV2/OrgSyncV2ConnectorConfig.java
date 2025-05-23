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

import org.bedework.synch.shared.conf.ConnectorConfig;
import org.bedework.util.config.ConfInfo;
import org.bedework.base.ToString;

/** File synch connector config
 *
 * @author douglm
 */
@ConfInfo(elementName = "synch-connector")
public class OrgSyncV2ConnectorConfig extends ConnectorConfig {
  /** Min polling interval - seconds */
  private int minPoll;

  private String uidPrefix;

  /** Min poll - seconds
   *
   * @param val    int seconds
   */
  public void setMinPoll(final int val) {
    minPoll = val;
  }

  /** Min poll - seconds
   *
   * @return int seconds
   */
  public int getMinPoll() {
    return minPoll;
  }

  /**
   *
   * @param val   uid prefix
   */
  public void setUidPrefix(final String val) {
    uidPrefix = val;
  }

  /**
   *
   * @return uid prefix
   */
  public String getUidPrefix() {
    return uidPrefix;
  }

  @Override
  public void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);

    ts.append("minPoll", getMinPoll());
    ts.append("uidPrefix", getUidPrefix());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
