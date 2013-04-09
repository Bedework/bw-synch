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
import org.bedework.synch.service.SynchConnConf;

import edu.rpi.cmt.config.ConfigBase;

import java.util.List;
import java.util.SortedSet;

import javax.xml.namespace.QName;

/** This class defines the various properties we need for the synch engine
 *
 * @author Mike Douglass
 */
public class SynchConfig extends ConfigBase<SynchConfig> {
  /** */
  public final static QName confElement = new QName(ns, "synch-confinfo");

  private static final QName synchlingPoolSizeProperty = new QName(ns, "synchlingPoolSize");

  private static final QName synchlingPoolTimeoutProperty = new QName(ns, "synchlingPoolTimeout");

  private static final QName missingTargetRetriesProperty = new QName(ns, "missingTargetRetries");

  private static final QName timezonesURIProperty = new QName(ns, "timezonesURI");

  private static final QName callbackURIProperty = new QName(ns, "callbackURI");

  private static final QName keystoreProperty = new QName(ns, "keystore");

  private static final QName privKeysProperty = new QName(ns, "privKeys");

  private static final QName pubKeysProperty = new QName(ns, "pubKeys");

  private static final QName hibernateProperty = new QName(ns, "hibernateProperty");

  @Override
  public QName getConfElement() {
    return confElement;
  }

  private List<SynchConnConf> connectorConfs;

  private SortedSet<IpAddrInfo> ipInfo;

  /**
   * @param val current size of synchling pool
   */
  public void setSynchlingPoolSize(final int val) {
    setIntegerProperty(synchlingPoolSizeProperty, val);
  }

  /**
   * @return current size of synchling pool
   */
  public int getSynchlingPoolSize() {
    return getIntegerPropertyValue(synchlingPoolSizeProperty);
  }

  /**
   * @param val timeout in millisecs
   */
  public void setSynchlingPoolTimeout(final long val) {
    setLongProperty(synchlingPoolTimeoutProperty, val);
  }

  /**
   * @return timeout in millisecs
   */
  public long getSynchlingPoolTimeout() {
    return getLongPropertyValue(synchlingPoolTimeoutProperty);
  }

  /** How often we retry when a target is missing
   *
   * @param val
   */
  public void setMissingTargetRetries(final int val) {
    setIntegerProperty(missingTargetRetriesProperty, val);
  }

  /**
   * @return How often we retry when a target is missing
   */
  public int getMissingTargetRetries() {
    return getIntegerPropertyValue(missingTargetRetriesProperty);
  }

  /** web service push callback uri - null for no service
   *
   * @param val    String
   */
  public void setCallbackURI(final String val) {
    setProperty(callbackURIProperty, val);
  }

  /** web service push callback uri - null for no service
   *
   * @return String
   */
  public String getCallbackURI() {
    return getPropertyValue(callbackURIProperty);
  }

  /** Timezone server location
   *
   * @param val    String
   */
  public void setTimezonesURI(final String val) {
    setProperty(timezonesURIProperty, val);
  }

  /** Timezone server location
   *
   * @return String
   */
  public String getTimezonesURI() {
    return getPropertyValue(timezonesURIProperty);
  }

  /** Path to keystore - null for use default
   *
   * @param val    String
   */
  public void setKeystore(final String val) {
    setProperty(keystoreProperty, val);
  }

  /** Path to keystore - null for use default
   *
   * @return String
   */
  public String getKeystore() {
    return getPropertyValue(keystoreProperty);
  }

  /**
   *
   * @param val    String
   */
  public void setPrivKeys(final String val) {
    setProperty(privKeysProperty, val);
  }

  /**
   *
   * @return String
   */
  public String getPrivKeys() {
    return getPropertyValue(privKeysProperty);
  }

  /**
   *
   * @param val    String
   */
  public void setPubKeys(final String val) {
    setProperty(pubKeysProperty, val);
  }

  /**
   *
   * @return String
   */
  public String getPubKeys() {
    return getPropertyValue(pubKeysProperty);
  }

  /** Add a hibernate property
   *
   * @param name
   * @param val
   */
  public void addHibernateProperty(final String name,
                                   final String val) {
    addProperty(hibernateProperty, name + "=" + val);
  }

  /** Get a hibernate property
   *
   * @param val
   * @return value or null
   */
  public String getHibernateProperty(final String val) {
    List<String> ps = getHibernateProperties();

    String key = val + "=";
    for (String p: ps) {
      if (p.startsWith(key)) {
        return p.substring(key.length());
      }
    }

    return null;
  }

  /** Remove a hibernate property
   *
   * @param name
   */
  public void removeHibernateProperty(final String name) {
    try {
      String v = getHibernateProperty(name);

      if (v == null) {
        return;
      }

      getConfig().removeProperty(hibernateProperty, name + "=" + v);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /** Set a hibernate property
   *
   * @param name
   * @param val
   */
  public void setHibernateProperty(final String name,
                                   final String val) {
    try {
      removeHibernateProperty(name);
      addHibernateProperty(name, val);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   *
   * @return String val
   */
  public List<String> getHibernateProperties() {
    try {
      return getConfig().getAll(hibernateProperty);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
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
  public void setConnectorConfs(final List<SynchConnConf> val) {
    connectorConfs = val;
  }

  /** Set<ConnectorConfig>
   *
   * @return map
   */
  public List<SynchConnConf> getConnectorConfs() {
    return connectorConfs;
  }
}
