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
package org.bedework.synch.db;

import java.util.Set;
import java.util.SortedSet;

/** This class defines the various properties we need for the synch engine
 *
 * @author Mike Douglass
 */
public class SynchConfig extends DbItem<SynchConfig> {
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

  private Set<ConnectorConfig> connectors;

  private SortedSet<IpAddrInfo> ipInfo;

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

  /** How often we retry when a target is missing
   *
   * @param val
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
   * @param val
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

  /** Map of (name, className)
   *
   * @param val
   */
  public void setConnectors(final Set<ConnectorConfig> val) {
    connectors = val;
  }

  /** Set<ConnectorConfig>
   *
   * @return map
   */
  public Set<ConnectorConfig> getConnectors() {
    return connectors;
  }

  /* ====================================================================
   *                   Object methods
   * We only allow one of these in teh db so any and all are equal.
   * ==================================================================== */

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final SynchConfig o) {
    return 0;
  }

  @Override
  public int hashCode() {
    return 4;
  }
}
