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

import org.bedework.synch.shared.BaseSubscriptionInfo;
import org.bedework.synch.shared.Notification;
import org.bedework.synch.shared.PropertiesInfo;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.SynchDefs.SynchKind;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.SynchPropertyInfo;
import org.bedework.synch.shared.cnctrs.AbstractConnector;
import org.bedework.synch.shared.cnctrs.ConnectorInstanceMap;
import org.bedework.synch.wsmessages.SynchEndType;

/** The synch processor connector for subscriptions to files.
 *
 * @author Mike Douglass
 */
public class FileConnector
        extends AbstractConnector<FileConnector,
        FileConnectorInstance,
        Notification<?>,
        FileConnectorConfig,
        FileSubscriptionInfo> {
  private static final PropertiesInfo fPropInfo = new PropertiesInfo();

  static {
    fPropInfo.requiredUri(null);


    fPropInfo.optionalPrincipal(null);

    fPropInfo.optionalPassword(null);

    fPropInfo.add(BaseSubscriptionInfo.propnameRefreshDelay,
                  false,
                  SynchPropertyInfo.typeInteger,
                  "",
                  false);
  }

  private final ConnectorInstanceMap<FileConnectorInstance> cinstMap =
      new ConnectorInstanceMap<>();

  /**
   */
  public FileConnector() {
    super(fPropInfo);
  }

  @Override
  public void start(final String connectorId,
                    final FileConnectorConfig conf,
                    final String callbackUri,
                    final SynchEngine syncher) {
    super.start(connectorId, conf, callbackUri, syncher);
  }

  @Override
  public SynchKind getKind() {
    return SynchKind.poll;
  }

  @Override
  public FileConnectorInstance makeInstance(final Subscription sub,
                                            final SynchEndType end) {
    final FileSubscriptionInfo info;

    if (end == SynchEndType.A) {
      info = new FileSubscriptionInfo(sub.getEndAConnectorInfo());
    } else {
      info = new FileSubscriptionInfo(sub.getEndBConnectorInfo());
    }

    return new FileConnectorInstance(config,
                                         this, sub, end, info);
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */
}
