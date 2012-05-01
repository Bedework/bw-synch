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


/** Configure the Bedework synch engine service
 *
 * @author douglm
 */
public interface SynchConfMBean {
  /* ========================================================================
   * Config properties
   * ======================================================================== */

  /**
   * @param val current size of synchling pool
   */
  public void setSynchlingPoolSize(final int val);

  /**
   * @return current size of synchling pool
   */
  public int getSynchlingPoolSize();

  /**
   * @param val timeout in millisecs
   */
  public void setSynchlingPoolTimeout(final long val);

  /**
   * @return timeout in millisecs
   */
  public long getSynchlingPoolTimeout();

  /** How often we retry when a target is missing
   *
   * @param val
   */
  public void setMissingTargetRetries(final int val);

  /**
   * @return How often we retry when a target is missing
   */
  public int getMissingTargetRetries();

  /** web service push callback uri - null for no service
   *
   * @param val    String
   */
  public void setCallbackURI(final String val);

  /** web service push callback uri - null for no service
   *
   * @return String
   */
  public String getCallbackURI();

  /** Timezone server location
   *
   * @param val    String
   */
  public void setTimezonesURI(final String val);

  /** Timezone server location
   *
   * @return String
   */
  public String getTimezonesURI();

  /** Path to keystore - null for use default
   *
   * @param val    String
   */
  public void setKeystore(final String val);

  /** Path to keystore - null for use default
   *
   * @return String
   */
  public String getKeystore();

  /**
   *
   * @param val    String
   */
  public void setPrivKeys(final String val);

  /**
   *
   * @return String
   */
  public String getPrivKeys();

  /**
   *
   * @param val    String
   */
  public void setPubKeys(final String val);

  /**
   *
   * @return String
   */
  public String getPubKeys();

  /* *
   * @param val
   * /
  public void setIpInfo(final SortedSet<IpAddrInfo> val) {
    ipInfo = val;
  }

  /**
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
  } */

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  /** Lifecycle
   *
   */
  public void start();

  /** Lifecycle
   *
   */
  public void stop();

  /** Lifecycle
   *
   * @return true if started
   */
  public boolean isStarted();
}
