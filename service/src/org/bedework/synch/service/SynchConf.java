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
package org.bedework.synch.service;

import org.bedework.synch.SynchEngine;
import org.bedework.synch.db.SynchConfig;
import org.bedework.synch.exception.SynchException;

import org.apache.log4j.Logger;
import org.jboss.system.ServiceMBeanSupport;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @author douglm
 *
 */
public class SynchConf extends ServiceMBeanSupport implements SynchConfMBean {
  private transient Logger log;

  /* ========================================================================
   * Conf properties
   * ======================================================================== */

  @Override
  public void setSynchlingPoolSize(final int val) {
    getConf().setSynchlingPoolSize(val);
    update();
  }

  /**
   * @return current size of synchling pool
   */
  @Override
  public int getSynchlingPoolSize() {
    return getConf().getSynchlingPoolSize();
  }

  /**
   * @param val timeout in millisecs
   */
  @Override
  public void setSynchlingPoolTimeout(final long val) {
    getConf().setSynchlingPoolTimeout(val);
    update();
  }

  /**
   * @return timeout in millisecs
   */
  @Override
  public long getSynchlingPoolTimeout() {
    return getConf().getSynchlingPoolTimeout();
  }

  /** How often we retry when a target is missing
   *
   * @param val
   */
  @Override
  public void setMissingTargetRetries(final int val) {
    getConf().setMissingTargetRetries(val);
    update();
  }

  /**
   * @return How often we retry when a target is missing
   */
  @Override
  public int getMissingTargetRetries() {
    return getConf().getMissingTargetRetries();
  }

  /** web service push callback uri - null for no service
   *
   * @param val    String
   */
  @Override
  public void setCallbackURI(final String val) {
    getConf().setCallbackURI(val);
    update();
  }

  /** web service push callback uri - null for no service
   *
   * @return String
   */
  @Override
  public String getCallbackURI() {
    return getConf().getCallbackURI();
  }

  /** Timezone server location
   *
   * @param val    String
   */
  @Override
  public void setTimezonesURI(final String val) {
    getConf().setTimezonesURI(val);
    update();
  }

  /** Timezone server location
   *
   * @return String
   */
  @Override
  public String getTimezonesURI() {
    return getConf().getTimezonesURI();
  }

  /** Path to keystore - null for use default
   *
   * @param val    String
   */
  @Override
  public void setKeystore(final String val) {
    getConf().setKeystore(val);
    update();
  }

  /** Path to keystore - null for use default
   *
   * @return String
   */
  @Override
  public String getKeystore() {
    return getConf().getKeystore();
  }

  /**
   *
   * @param val    String
   */
  @Override
  public void setPrivKeys(final String val) {
    getConf().setPrivKeys(val);
    update();
  }

  /**
   *
   * @return String
   */
  @Override
  public String getPrivKeys() {
    return getConf().getPrivKeys();
  }

  /**
   *
   * @param val    String
   */
  @Override
  public void setPubKeys(final String val) {
    getConf().setPubKeys(val);
    update();
  }

  /**
   *
   * @return String
   */
  @Override
  public String getPubKeys() {
    return getConf().getPubKeys();
  }

  /* *
   * @param val
   * /
  public void setIpInfo(final SortedSet<IpAddrInfo> val) {
    ipInfo = val;
  }

  /* *
   * @return ip info
   * /
  public SortedSet<IpAddrInfo> getIpInfo() {
    return ipInfo;
  }

  /** Map of (name, className)
   *
   * @param val
   * /
  public void setConnectors(final Set<ConnectorConfig> val) {
    connectors = val;
  }

  /** Set<ConnectorConfig>
   *
   * @return map
   * /
  public Set<ConnectorConfig> getConnectors() {
    return connectors;
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  @Override
  protected ObjectName getObjectName(final MBeanServer server,
                                     final ObjectName name)
      throws MalformedObjectNameException {
    if (name == null) {
      return OBJECT_NAME;
    }

    return name;
   }

  @Override
  public void startService() throws Exception {
  }

  @Override
  public void stopService() throws Exception {
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private SynchConfig getConf() {
    try {
      return SynchEngine.getSyncher().getConfig();
    } catch (SynchException se) {
      error(se);
      throw new RuntimeException(se);
    }
  }

  private void update() {
    try {
      SynchEngine.getSyncher().updateConfig();
    } catch (SynchException se) {
      error(se);
      throw new RuntimeException(se);
    }
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

  /* Get a logger for messages
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }
}
