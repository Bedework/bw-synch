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
package org.bedework.synch.conf;

import org.bedework.synch.db.IpAddrInfo;
import org.bedework.synch.shared.service.SynchConnConf;
import org.bedework.util.config.ConfInfo;
import org.bedework.util.config.OrmConfigBase;

import java.util.List;
import java.util.SortedSet;

/** This class defines the various properties we need for the synch engine
 *
 * @author Mike Douglass
 */
@ConfInfo(elementName = "synch-confinfo")
public class SynchConfig extends OrmConfigBase<SynchConfig> {
  /* Size of synchling pool */
  private int synchlingPoolSize;

  /* millisecs */
  private long synchlingPoolTimeout;

  /* How often we retry when a target is missing */
  private int missingTargetRetries;

  /* web service push callback uri - null for no service */
  private String callbackURI;

  /* Timezone server location */
  private String timezonesURI;

  /* Path to keystore - null for use default */
  private String keystore;

  /* Path to keystores  */
  private String privKeys;
  /* Path to keystores  */
  private String pubKeys;

  private List<SynchConnConf<?>> connectorConfs;

  private SortedSet<IpAddrInfo> ipInfo;

  private boolean subscriptionsOnly;

  /**
   * @param val current size of synchling pool
   */
  public void setSynchlingPoolSize(final int val) {
    synchlingPoolSize = val;
  }

  /**
   * @return current size of synchling pool
   */
  public int getSynchlingPoolSize() {
    return synchlingPoolSize;
  }

  /**
   * @param val timeout in millisecs
   */
  public void setSynchlingPoolTimeout(final long val) {
    synchlingPoolTimeout = val;
  }

  /**
   * @return timeout in millisecs
   */
  public long getSynchlingPoolTimeout() {
    return synchlingPoolTimeout;
  }

  /**
   *
   * @param val How often we retry when a target is missing
   */
  public void setMissingTargetRetries(final int val) {
    missingTargetRetries = val;
  }

  /**
   * @return How often we retry when a target is missing
   */
  public int getMissingTargetRetries() {
    return missingTargetRetries;
  }

  /** web service push callback uri - null for no service
   *
   * @param val    String
   */
  public void setCallbackURI(final String val) {
    callbackURI = val;
  }

  /** web service push callback uri - null for no service
   *
   * @return String
   */
  public String getCallbackURI() {
    return callbackURI;
  }

  /** Timezone server location
   *
   * @param val    String
   */
  public void setTimezonesURI(final String val) {
    timezonesURI = val;
  }

  /** Timezone server location
   *
   * @return String
   */
  public String getTimezonesURI() {
    return timezonesURI;
  }

  /** Path to keystore - null for use default
   *
   * @param val    String
   */
  public void setKeystore(final String val) {
    keystore = val;
  }

  /** Path to keystore - null for use default
   *
   * @return String
   */
  public String getKeystore() {
    return keystore;
  }

  /**
   *
   * @param val    String
   */
  public void setPrivKeys(final String val) {
    privKeys = val;
  }

  /**
   *
   * @return String
   */
  public String getPrivKeys() {
    return privKeys;
  }

  /**
   *
   * @param val    String
   */
  public void setPubKeys(final String val) {
    pubKeys = val;
  }

  /**
   *
   * @return String
   */
  public String getPubKeys() {
    return pubKeys;
  }

  /**
   * @param val information about ip
   */
  public void setIpInfo(final SortedSet<IpAddrInfo> val) {
    ipInfo = val;
  }

  /**
   * @return ip info
   */
  public SortedSet<IpAddrInfo> getIpInfo() {
    return ipInfo;
  }

  /**
   *
   * @param val Map of (name, className)
   */
  public void setConnectorConfs(final List<SynchConnConf<?>> val) {
    connectorConfs = val;
  }

  /** Set<ConnectorConfig>
   *
   * @return map
   */
  @ConfInfo(dontSave = true)
  public List<SynchConnConf<?>> getConnectorConfs() {
    return connectorConfs;
  }

  /** This flag alllows us to run  synch engine on multiple machines.
   * For the moment only one of those instances should handle the
   * actual synch process. All others shoudl have this flag set to
   * true so that they only handle the creation and deletion of
   * subscriptions.
   *
   * @param val true if this instance only handles subscriptions
   */
  public void setSubscriptionsOnly(final boolean val) {
    subscriptionsOnly = val;
  }

  /**
   *
   * @return true if this instance only handles subscriptions
   */
  public boolean getSubscriptionsOnly() {
    return subscriptionsOnly;
  }
}
