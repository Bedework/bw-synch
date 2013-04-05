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

import java.io.StringWriter;
import java.util.List;

import javax.xml.namespace.QName;

import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.conf.ConnectorConfig;

import edu.rpi.cmt.config.ConfigBase;
import edu.rpi.cmt.config.ConfigurationElementType;
import edu.rpi.cmt.config.ConfigurationFileStore;
import edu.rpi.cmt.config.ConfigurationStore;
import edu.rpi.cmt.config.ConfigurationType;
import edu.rpi.cmt.jmx.ConfBase;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlEmit.NameSpace;
import edu.rpi.sss.util.xml.tagdefs.BedeworkServerTags;

/**
 * @author douglm
 *
 */
public class SynchConnConf extends ConfBase<ConnectorConfig> implements SynchConnConfMBean {
  private String cname;

  private ConnectorConfig cfg;

  private Connector connector;

  /**
   * @param configStore
   * @param serviceName
   * @param cname - connector name
   */
  public SynchConnConf(final ConfigurationStore configStore,
                       final String serviceName,
                       final String cname) {
    super(serviceName);
    setStore(configStore);

    this.cname = cname;
  }

  @Override
  public ConfigurationType getConfigObject() {
    return getConfig().getConfig();
  }

  /** Embed the connector
   *
   * @param val
   */
  public void setConnector(final Connector val) {
    connector = val;
  }

  /**
   * @return the connector
   */
  public Connector getConnector() {
    return connector;
  }

  /**
   * @return status message
   */
  @Override
  public String getStatus() {
    return connector.getStatus();
  }

  @Override
  public String getName() {
    return cname;
  }

  /**
   * @return the configuration object
   */
  public ConnectorConfig getConfig() {
    return cfg;
  }

  /**
   * @throws Throwable
   */
  public void loadConfig() throws Throwable {
    /* Load up the config */

    ConfigurationStore cfs = new ConfigurationFileStore(getConfigUri());

    cfg = getConfigInfo(cfs, getName(), ConnectorConfig.class);

    if (cfg == null) {
      throw new Exception("Unable to read configuration " + getName());
    }

    setConfigName(getName());

    saveConfig(); // Just to ensure we have it for next time
  }

  /* ========================================================================
   * Conf properties
   * ======================================================================== */

  /** Class name
   *
   * @param val    String
   */
  @Override
  public void setClassName(final String val) {
    cfg.setClassName(val);
  }

  /** Class name
   *
   * @return String
   */
  @Override
  public String getClassName() {
    return cfg.getClassName();
  }

  /** Read only?
   *
   * @param val    int seconds
   */
  @Override
  public void setReadOnly(final boolean val) {
    cfg.setReadOnly(val);
  }

  /** Read only?
   *
   * @return int seconds
   */
  @Override
  public boolean getReadOnly() {
    return cfg.getReadOnly();
  }

  /** Can we trust the lastmod from this connector?
   *
   * @param val    boolean
   */
  @Override
  public void setTrustLastmod(final boolean val) {
    cfg.setTrustLastmod(val);
  }

  /** Can we trust the lastmod from this connector?
   *
   * @return boolean
   */
  @Override
  public boolean getTrustLastmod() {
    return cfg.getTrustLastmod();
  }

  @Override
  public String getProperties() {
    String plist = "";

    List<ConfigurationElementType> ps = cfg.getProperties();
    if (ps == null) {
      return plist;
    }

    for (ConfigurationElementType ce: ps) {
      try {
        StringWriter str = new StringWriter();
        XmlEmit xml = new XmlEmit();

        xml.addNs(new NameSpace(BedeworkServerTags.bedeworkSystemNamespace, "BWS"), true);

        xml.startEmit(str);
        ce.toXml(xml);

        plist += str.toString() + "\n";
      } catch (Throwable t) {
        plist += t.getLocalizedMessage() + "\n";
      }
    }

    return plist;
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  @Override
  public void addProperty(final String name, final String value) {
    cfg.addProperty(new QName(ConfigBase.ns, name), value);
  }

  @Override
  public String setProperty(final String name, final String value) {
    if (cfg == null) {
      return "No current connector";
    }

    cfg.setProperty(new QName(ConfigBase.ns, name), value);
    return "ok";
  }

  @Override
  public void removeProperty(final String name) {
    QName qn = new QName(ConfigBase.ns, name);

    String val = cfg.getPropertyValue(qn);
    cfg.removeProperty(qn, val);
  }

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }

  @Override
  public boolean isStarted() {
    return true;
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /* ====================================================================
   *                   Protected methods
   * ==================================================================== */
}
