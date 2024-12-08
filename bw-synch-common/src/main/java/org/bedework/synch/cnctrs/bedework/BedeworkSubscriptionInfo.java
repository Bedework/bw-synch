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

import org.bedework.synch.shared.BaseSubscriptionInfo;
import org.bedework.synch.shared.SubscriptionConnectorInfo;
import org.bedework.synch.shared.exception.SynchException;

/** Stores information about one end of a subscription for connector.
 *
 *
 * @author Mike Douglass
 */
public class BedeworkSubscriptionInfo extends BaseSubscriptionInfo {
  /**
   * @param info the subscription connector info
   */
  public BedeworkSubscriptionInfo(final SubscriptionConnectorInfo info) {
    super(info);
  }
}
