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
import org.bedework.synch.SynchDefs.SynchKind;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.SynchPropertyInfo;
import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.cnctrs.ConnectorInstanceMap;
import org.bedework.synch.db.ConnectorConfig;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** The synch processor connector for subscriptions to files.
 *
 * @author Mike Douglass
 */
public class FileConnector
      implements Connector<FileConnectorInstance,
                           Notification> {
  private transient Logger log;

  private static ietf.params.xml.ns.icalendar_2.ObjectFactory icalOf =
      new ietf.params.xml.ns.icalendar_2.ObjectFactory();

  private static List<SynchPropertyInfo> propInfo =
      new ArrayList<SynchPropertyInfo>();

  static {
    propInfo.add(new SynchPropertyInfo(BaseSubscriptionInfo.propnameUri,
                                       false,
                                       SynchPropertyInfo.typeUri,
                                       "",
                                       true));

    propInfo.add(new SynchPropertyInfo(BaseSubscriptionInfo.propnamePrincipal,
                                       false,
                                       SynchPropertyInfo.typeString,
                                       "",
                                       false));

    propInfo.add(new SynchPropertyInfo(BaseSubscriptionInfo.propnamePassword,
                                       true,
                                       SynchPropertyInfo.typePassword,
                                       "",
                                       false));

    propInfo.add(new SynchPropertyInfo(BaseSubscriptionInfo.propnameRefreshDelay,
                                       false,
                                       SynchPropertyInfo.typeInteger,
                                       "",
                                       false));
  }

  private SynchEngine syncher;

  private FileConnectorConfig config;

  private String callbackUri;

  private String connectorId;

  private boolean running;

  private ConnectorInstanceMap<FileConnectorInstance> cinstMap =
      new ConnectorInstanceMap<FileConnectorInstance>();

  @Override
  public void start(final String connectorId,
                    final ConnectorConfig conf,
                    final String callbackUri,
                    final SynchEngine syncher) throws SynchException {
    this.connectorId = connectorId;
    this.syncher = syncher;
    this.callbackUri = callbackUri;

    config = new FileConnectorConfig(conf);

    this.syncher = syncher;
    running = true;
  }

  @Override
  public boolean isManager() {
    return false;
  }

  @Override
  public boolean isStarted() {
    return running;
  }

  @Override
  public boolean isFailed() {
    return false;
  }

  @Override
  public boolean isStopped() {
    return !running;
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
  public String getId() {
    return connectorId;
  }

  @Override
  public String getCallbackUri() {
    return callbackUri;
  }

  @Override
  public SynchEngine getSyncher() {
    return syncher;
  }

  @Override
  public ietf.params.xml.ns.icalendar_2.ObjectFactory getIcalObjectFactory() {
    return icalOf;
  }

  @Override
  public List<SynchPropertyInfo> getPropertyInfo() {
    return propInfo;
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
      info.setRefreshDelay(String.valueOf(config.getMinPoll() * 1000));
    }

    inst = new FileConnectorInstance(config, this, sub, end, info);
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
   *                   Protected methods
   * ==================================================================== */

  protected void info(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  protected void warn(final String msg) {
    getLogger().warn(msg);
  }

  /* Get a logger for messages
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  /* ====================================================================
   *                         Package methods
   * ==================================================================== */

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */
}
