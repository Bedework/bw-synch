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

import org.bedework.synch.BaseSubscriptionInfo;
import org.bedework.synch.Notification;
import org.bedework.synch.PropertiesInfo;
import org.bedework.synch.SynchDefs.SynchKind;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.SynchPropertyInfo;
import org.bedework.synch.cnctrs.AbstractConnector;
import org.bedework.synch.cnctrs.ConnectorInstanceMap;
import org.bedework.synch.db.ConnectorConfig;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** The synch processor connector for subscriptions to files.
 *
 * @author Mike Douglass
 */
public class FileConnector
        extends AbstractConnector<FileConnector,
                                  FileConnectorInstance,
                                  Notification> {
private static PropertiesInfo fPropInfo = new PropertiesInfo();

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

  private ConnectorInstanceMap<FileConnectorInstance> cinstMap =
      new ConnectorInstanceMap<FileConnectorInstance>();

  /**
   */
  public FileConnector() {
    super(fPropInfo);
  }

  @Override
  public void start(final String connectorId,
                    final ConnectorConfig conf,
                    final String callbackUri,
                    final SynchEngine syncher) throws SynchException {
    super.start(connectorId, conf, callbackUri, syncher);

    config = new FileConnectorConfig(conf);

    stopped = false;
    running = true;
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
  public FileConnectorInstance getConnectorInstance(final Subscription sub,
                                                        final SynchEndType end) throws SynchException {
    FileConnectorInstance inst = cinstMap.find(sub, end);

    if (inst != null) {
      return inst;
    }

    //debug = getLogger().isDebugEnabled();
    FileSubscriptionInfo info;

    if (end == SynchEndType.A) {
      info = new FileSubscriptionInfo(sub.getEndAConnectorInfo());
    } else {
      info = new FileSubscriptionInfo(sub.getEndBConnectorInfo());
    }

    String rd = info.getRefreshDelay();
    if (rd == null) {
      info.setRefreshDelay(String.valueOf(((FileConnectorConfig)config).getMinPoll() * 1000));
    }

    inst = new FileConnectorInstance((FileConnectorConfig)config,
                                     this, sub, end, info);
    cinstMap.add(sub, end, inst);

    return inst;
  }

  class BedeworkNotificationBatch extends NotificationBatch<Notification> {
  }

  @Override
  public BedeworkNotificationBatch handleCallback(final HttpServletRequest req,
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
