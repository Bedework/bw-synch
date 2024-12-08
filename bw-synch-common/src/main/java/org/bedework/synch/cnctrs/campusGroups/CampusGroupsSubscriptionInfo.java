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
package org.bedework.synch.cnctrs.campusGroups;

import org.bedework.synch.cnctrs.file.FileSubscriptionInfo;
import org.bedework.synch.shared.SubscriptionConnectorInfo;
import org.bedework.synch.shared.exception.SynchException;

/** Represents connection information for a CampusGroups connector
 * instance.
 *
 * @author Mike Douglass
 */
public class CampusGroupsSubscriptionInfo
        extends FileSubscriptionInfo {

  /**
   * @param info
   */
  public CampusGroupsSubscriptionInfo(final SubscriptionConnectorInfo info) {
    super(info);
  }
}
