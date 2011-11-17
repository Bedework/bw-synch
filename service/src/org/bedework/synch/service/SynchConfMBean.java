/* **********************************************************************
    Copyright 2010 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/
package org.bedework.synch.service;

import org.jboss.mx.util.ObjectNameFactory;
import org.jboss.system.ServiceMBean;

import javax.management.ObjectName;

/** Configure the Bedework synch engine service
 *
 * @author douglm
 */
public interface SynchConfMBean extends ServiceMBean {
  /** The default object name */
  ObjectName OBJECT_NAME = ObjectNameFactory.create("org.bedework:service=SynchConf");

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
   * Operations
   * ======================================================================== */
}
