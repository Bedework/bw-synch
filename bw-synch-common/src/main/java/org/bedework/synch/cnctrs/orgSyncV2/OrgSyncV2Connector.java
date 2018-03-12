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
import org.bedework.synch.shared.Notification;
import org.bedework.synch.shared.PropertiesInfo;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.SynchDefs.SynchKind;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.SynchPropertyInfo;
import org.bedework.synch.shared.cnctrs.AbstractConnector;
import org.bedework.synch.shared.cnctrs.ConnectorInstanceMap;
import org.bedework.synch.shared.conf.ConnectorConfigI;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** The synch processor connector for subscriptions to orgsync.
 *
 * @author Mike Douglass
 */
public class OrgSyncV2Connector
        extends AbstractConnector<OrgSyncV2Connector,
                                  OrgSyncV2ConnectorInstance,
                                  Notification,
                                  OrgSyncV2ConnectorConfig> {
  private final static PropertiesInfo propInfo = new PropertiesInfo();

  static {
    propInfo.add(BaseSubscriptionInfo.propnameRefreshDelay,
                 false,
                 SynchPropertyInfo.typeInteger,
                 "",
                 false);

    propInfo.add(BaseSubscriptionInfo.propnameOrgSyncPublicOnly,
                 false,
                 SynchPropertyInfo.typeBoolean,
                 "Allow only public entities",
                 false);

    propInfo.add(BaseSubscriptionInfo.propnameLocKey,
                 false,
                 SynchPropertyInfo.typeString,
                 "Location key name for matching",
                 false);
  }

  private final ConnectorInstanceMap<OrgSyncV2ConnectorInstance> cinstMap =
      new ConnectorInstanceMap<>();

  private boolean failed;

  /**
   */
  public OrgSyncV2Connector() {
    super(propInfo);
  }

  @Override
  public void start(final String connectorId,
                    final ConnectorConfigI conf,
                    final String callbackUri,
                    final SynchEngine syncher) {
    super.start(connectorId, conf, callbackUri, syncher);

    config = (OrgSyncV2ConnectorConfig)conf;

    if (config.getUidPrefix() == null) {
      error("Must supply uid prefix in config");
      failed = true;
      return;
    }

    stopped = false;
    running = true;
  }

  public boolean isFailed() {
    return failed;
  }

  @Override
  public boolean isManager() {
    return false;
  }

  @Override
  public SynchKind getKind() {
    return SynchKind.poll;
  }

  @Override
  public boolean isReadOnly() {
    return config.getReadOnly();
  }

  @Override
  public boolean getTrustLastmod() {
    return config.getTrustLastmod();
  }

  @Override
  public List<Object> getSkipList() {
    return null;
  }

  @Override
  public OrgSyncV2ConnectorInstance getConnectorInstance(final Subscription sub,
                                                         final SynchEndType end) throws SynchException {
    OrgSyncV2ConnectorInstance inst = cinstMap.find(sub, end);

    if (inst != null) {
      return inst;
    }

    //debug = getLogger().isDebugEnabled();
    final OrgSyncV2SubscriptionInfo info;

    if (end == SynchEndType.A) {
      info = new OrgSyncV2SubscriptionInfo(sub.getEndAConnectorInfo());
    } else {
      info = new OrgSyncV2SubscriptionInfo(sub.getEndBConnectorInfo());
    }

    final String rd = info.getRefreshDelay();
    if (rd == null) {
      info.setRefreshDelay(String.valueOf(config.getMinPoll() * 1000));
    }

    inst = new OrgSyncV2ConnectorInstance(config,
                                          this, sub, end, info);
    cinstMap.add(sub, end, inst);

    return inst;
  }

  class BedeworkNotificationBatch extends NotificationBatch<Notification> {
  }

  @Override
  public BedeworkNotificationBatch handleCallback(
          final HttpServletRequest req,
          final HttpServletResponse resp,
          final List<String> resourceUri) throws SynchException {
    return null;
  }

  @Override
  public void respondCallback(final HttpServletResponse resp,
                              final NotificationBatch<Notification> notifications)
                                                    throws SynchException {
  }

  @Override
  public void stop() throws SynchException {
    running = false;
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */
}
