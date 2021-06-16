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

import org.bedework.synch.SynchEngineImpl;
import org.bedework.synch.conf.SynchConfig;
import org.bedework.synch.shared.Stat;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.conf.ConnectorConfig;
import org.bedework.synch.shared.service.SynchConnConf;
import org.bedework.util.config.ConfigurationStore;
import org.bedework.util.hibernate.HibConfig;
import org.bedework.util.hibernate.SchemaThread;
import org.bedework.util.jmx.ConfBase;
import org.bedework.util.jmx.ConfigHolder;
import org.bedework.util.jmx.InfoLines;

import org.hibernate.cfg.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.management.ObjectName;

/**
 * @author douglm
 *
 */
public class SynchConf extends ConfBase<SynchConfig> implements SynchConfMBean, ConfigHolder<SynchConfig> {
  /* Name of the property holding the location of the config data */
  private static final String confuriPname = "org.bedework.synch.confuri";

  List<String> connectorNames;

  private boolean running;

  private SynchEngine syncher;

  /* Be safe - default to false */
  private boolean export;

  private String schemaOutFile;

  private Configuration hibCfg;

  private class SchemaBuilder extends SchemaThread {

    SchemaBuilder(final String outFile,
                  final boolean export,
                  final Properties hibConfig) {
      super(outFile, export, hibConfig);
    }

    @Override
    public void completed(final String status) {
      if (status.equals(SchemaThread.statusDone)) {
        SynchConf.this.setStatus(ConfBase.statusDone);
      } else {
        SynchConf.this.setStatus(ConfBase.statusFailed);
      }
      setExport(false);
      info("Schema build completed with status " + status);
    }
  }

  private SchemaBuilder buildSchema;

  private class ProcessorThread extends Thread {
    boolean showedTrace;

    /**
     * @param name - for the thread
     */
    public ProcessorThread(final String name) {
      super(name);
    }

    @Override
    public void run() {
      while (running) {
        try {
          if (syncher == null) {
            // Starting the service

            syncher = SynchEngineImpl.getSyncher();
            syncher.start();
          }
        } catch (final Throwable t) {
          if (!showedTrace) {
            error(t);
            showedTrace = true;
          } else {
            error(t.getMessage());
          }
        }

        if (running) {
          // Wait a bit before restarting
          try {
            synchronized (this) {
              this.wait (10 * 1000);
            }
          } catch (final Throwable t) {
            error(t.getMessage());
          }
        }
      }
    }
  }

  private ProcessorThread processor;

  /**
   */
  public SynchConf() {
    super("org.bedework.synch:service=SynchConf");
    setConfigPname(confuriPname);
    setPathSuffix("conf");

    SynchEngineImpl.setConfigHolder(this);
  }

  /* ========================================================================
   * Schema attributes
   * ======================================================================== */

  @Override
  public void setExport(final boolean val) {
    export = val;
  }

  @Override
  public boolean getExport() {
    return export;
  }

  @Override
  public void setSchemaOutFile(final String val) {
    schemaOutFile = val;
  }

  @Override
  public String getSchemaOutFile() {
    return schemaOutFile;
  }

  @Override
  public void setDumpThreshold(final int val) {
    System.setProperty(
            "com.sun.xml.ws.transport.http.HttpAdapter.dumpThreshold",
            String.valueOf(val));
  }

  @Override
  public int getDumpThreshold() {
    try {
      return Integer.parseInt(System.getProperty(
              "com.sun.xml.ws.transport.http.HttpAdapter.dumpThreshold"));
    } catch (final Throwable ignored) {
      return 0;
    }
  }

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  @Override
  public void setSynchlingPoolSize(final int val) {
    getConfig().setSynchlingPoolSize(val);
  }

  @Override
  public int getSynchlingPoolSize() {
    return getConfig().getSynchlingPoolSize();
  }

  @Override
  public void setSynchlingPoolTimeout(final long val) {
    getConfig().setSynchlingPoolTimeout(val);
  }

  @Override
  public long getSynchlingPoolTimeout() {
    return getConfig().getSynchlingPoolTimeout();
  }

  @Override
  public void setMissingTargetRetries(final int val) {
    getConfig().setMissingTargetRetries(val);
  }

  @Override
  public int getMissingTargetRetries() {
    return getConfig().getMissingTargetRetries();
  }

  @Override
  public void setCallbackURI(final String val) {
    getConfig().setCallbackURI(val);
  }

  /** web service push callback uri - null for no service
   *
   * @return String
   */
  @Override
  public String getCallbackURI() {
    return getConfig().getCallbackURI();
  }

  /** Timezone server location
   *
   * @param val    String
   */
  @Override
  public void setTimezonesURI(final String val) {
    getConfig().setTimezonesURI(val);
  }

  /** Timezone server location
   *
   * @return String
   */
  @Override
  public String getTimezonesURI() {
    return getConfig().getTimezonesURI();
  }

  /** Path to keystore - null for use default
   *
   * @param val    String
   */
  @Override
  public void setKeystore(final String val) {
    getConfig().setKeystore(val);
  }

  /** Path to keystore - null for use default
   *
   * @return String
   */
  @Override
  public String getKeystore() {
    return getConfig().getKeystore();
  }

  /**
   *
   * @param val    String
   */
  @Override
  public void setPrivKeys(final String val) {
    getConfig().setPrivKeys(val);
  }

  /**
   *
   * @return String
   */
  @Override
  public String getPrivKeys() {
    return getConfig().getPrivKeys();
  }

  /**
   *
   * @param val    String
   */
  @Override
  public void setPubKeys(final String val) {
    getConfig().setPubKeys(val);
  }

  /**
   *
   * @return String
   */
  @Override
  public String getPubKeys() {
    return getConfig().getPubKeys();
  }

  @Override
  public List<String> getConnectorNames() {
    return connectorNames;
  }

  @Override
  public void setSubscriptionsOnly(final boolean val) {
    getConfig().setSubscriptionsOnly(val);
  }

  @Override
  public boolean getSubscriptionsOnly() {
    return getConfig().getSubscriptionsOnly();
  }

  @Override
  public List<Stat> getStats() {
    if (syncher == null) {
      return new ArrayList<>();
    }

    return syncher.getStats();
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  @Override
  public String schema() {
    try {
      final HibConfig hc = new HibConfig(getConfig());

      buildSchema = new SchemaBuilder(getSchemaOutFile(),
                                      getExport(),
                                      hc.getHibConfiguration().getProperties());

      setStatus(statusStopped);

      buildSchema.start();

      return "OK";
    } catch (final Throwable t) {
      error(t);

      return "Exception: " + t.getLocalizedMessage();
    }
  }

  @Override
  public synchronized List<String> schemaStatus() {
    if (buildSchema == null) {
      final InfoLines infoLines = new InfoLines();

      infoLines.addLn("Schema build has not been started");

      return infoLines;
    }

    return buildSchema.infoLines;
  }

  @Override
  public String rescheduleNow(final String id) {
    try {
      syncher.rescheduleNow(id);

      return "ok";
    } catch (final Throwable t) {
      error(t);
      return "error";
    }
  }


  @Override
  public void setHibernateDialect(final String value) {
    getConfig().setHibernateDialect(value);
  }

  @Override
  public String getHibernateDialect() {
    return getConfig().getHibernateDialect();
  }

  @Override
  public String listHibernateProperties() {
    final StringBuilder res = new StringBuilder();

    final List<String> ps = getConfig().getHibernateProperties();

    for (final String p: ps) {
      res.append(p);
      res.append("\n");
    }

    return res.toString();
  }

  @Override
  public String displayHibernateProperty(final String name) {
    final String val = getConfig().getHibernateProperty(name);

    if (val != null) {
      return val;
    }

    return "Not found";
  }

  @Override
  public void removeHibernateProperty(final String name) {
    getConfig().removeHibernateProperty(name);
  }

  @Override
  public void addHibernateProperty(final String name,
                                   final String value) {
    getConfig().addHibernateProperty(name, value);
  }

  @Override
  public void setHibernateProperty(final String name,
                                   final String value) {
    getConfig().setHibernateProperty(name, value);
  }

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  @Override
  public void start() {
    if (processor != null) {
      error("Already started");
      return;
    }

    info("************************************************************");
    info(" * Starting syncher");
    info("************************************************************");

    running = true;

    processor = new ProcessorThread(getServiceName());
    processor.start();
  }

  @Override
  public void stop() {
    if (processor == null) {
      error("Already stopped");
      return;
    }

    info("************************************************************");
    info(" * Stopping syncher");
    info("************************************************************");

    running = false;

    syncher.stop();

    processor.interrupt();
    try {
      processor.join(20 * 1000);
    } catch (final InterruptedException ignored) {
    } catch (final Throwable t) {
      error("Error waiting for processor termination");
      error(t);
    }

    processor = null;

    syncher = null;

    info("************************************************************");
    info(" * Syncher terminated");
    info("************************************************************");
  }

  @Override
  public boolean isStarted() {
    return true;
  }

  @Override
  public String loadConfig() {
    try {
      /* Load up the config */

      final String res = loadOnlyConfig(SynchConfig.class);

      if (res != null) {
        return res;
      }

      /* Load up the connectors */

      final ConfigurationStore cs = getStore().getStore("connectors");

      connectorNames = cs.getConfigs();

      final List<SynchConnConf<?>> sccs = new ArrayList<>();
      cfg.setConnectorConfs(sccs);

      for (final String cn: connectorNames) {
        final ObjectName objectName = createObjectName("connector", cn);

        /* Read the config so we can get the mbean class name. */

        final ConnectorConfig connCfg =
                (ConnectorConfig)cs.getConfig(cn);

        if (connCfg == null) {
          error("Unable to read connector configuration " + cn);
          continue;
        }

        String mbeanClassName = connCfg.getMbeanClassName();

        if (connCfg.getMbeanClassName() == null) {
          error("Must set the mbean class name for connector " + cn);
          error("Falling back to base class for " + cn);

          mbeanClassName = SynchConnConf.class.getCanonicalName();
        }

        @SuppressWarnings("unchecked")
        final SynchConnConf<ConnectorConfig> scc =
                (SynchConnConf<ConnectorConfig>)makeObject(mbeanClassName);

        if (scc == null) {
          error("Unable to create mbean class: " + mbeanClassName);
          error("Skipping");
          continue;
        }

        scc.init(cs, objectName.toString(), connCfg);

        sccs.add(scc);
        register("connector", cn, scc);
      }

      return "OK";
    } catch (final Throwable t) {
      error("Failed to start management context: " + t.getLocalizedMessage());
      error(t);
      return "failed";
    }
  }

  @Override
  public SynchConfig getConfig() {
    return cfg;
  }

  /** Save the configuration.
   *
   */
  @Override
  public void putConfig() {
    saveConfig();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /* ====================================================================
   *                   Protected methods
   * ==================================================================== */
}
