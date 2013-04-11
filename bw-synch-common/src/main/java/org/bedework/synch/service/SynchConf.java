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

import org.bedework.synch.Stat;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.conf.SynchConfig;

import edu.rpi.cmt.config.ConfigurationStore;
import edu.rpi.cmt.config.ConfigurationType;
import edu.rpi.cmt.jmx.ConfBase;
import edu.rpi.cmt.jmx.ConfigHolder;
import edu.rpi.cmt.jmx.InfoLines;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.management.ObjectName;

/**
 * @author douglm
 *
 */
public class SynchConf extends ConfBase<SynchConfig> implements SynchConfMBean, ConfigHolder<SynchConfig> {
  private static SynchConfig cfg;

  /* Name of the property holding the location of the config data */
  private static final String datauriPname = "org.bedework.synch.datauri";

  List<String> connectorNames;

  private boolean running;

  private SynchEngine syncher;

  /* Be safe - default to false */
  private boolean export;

  private String schemaOutFile;

  private Configuration hibCfg;

  private class SchemaThread extends Thread {
    InfoLines infoLines = new InfoLines();

    SchemaThread() {
      super("BuildSchema");
    }

    @Override
    public void run() {
      try {
        infoLines.addLn("Started export of schema");

        long startTime = System.currentTimeMillis();

        SchemaExport se = new SchemaExport(getHibConfiguration());

//      if (getDelimiter() != null) {
//        se.setDelimiter(getDelimiter());
//      }

        se.setFormat(true);       // getFormat());
        se.setHaltOnError(false); // getHaltOnError());
        se.setOutputFile(getSchemaOutFile());
        /* There appears to be a bug in the hibernate code. Everybody initialises
        this to /import.sql. Set to null causes an NPE
        Make sure it refers to a non-existant file */
        //se.setImportFile("not-a-file.sql");

        se.execute(false, // script - causes write to System.out if true
                   getExport(),
                   false,   // drop
                   true);   //   getCreate());

        long millis = System.currentTimeMillis() - startTime;
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds -= (minutes * 60);

        infoLines.addLn("Elapsed time: " + minutes + ":" +
                        twoDigits(seconds));
      } catch (Throwable t) {
        error(t);
        infoLines.exceptionMsg(t);
      } finally {
        infoLines.addLn("Schema build completed");
        export = false;
      }
    }
  }

  private SchemaThread buildSchema = new SchemaThread();

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

            syncher = SynchEngine.getSyncher();
            syncher.start();
          }
        } catch (Throwable t) {
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
            Object o = new Object();
            synchronized (o) {
              o.wait (10 * 1000);
            }
          } catch (Throwable t) {
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
    setConfigPname(datauriPname);
    setPathSuffix("conf");

    SynchEngine.setConfigHolder(this);
  }

  @Override
  public ConfigurationType getConfigObject() {
    return getConfig().getConfig();
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

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  @Override
  public void setSynchlingPoolSize(final int val) {
    getConfig().setSynchlingPoolSize(val);
  }

  /**
   * @return current size of synchling pool
   */
  @Override
  public int getSynchlingPoolSize() {
    return getConfig().getSynchlingPoolSize();
  }

  /**
   * @param val timeout in millisecs
   */
  @Override
  public void setSynchlingPoolTimeout(final long val) {
    getConfig().setSynchlingPoolTimeout(val);
  }

  /**
   * @return timeout in millisecs
   */
  @Override
  public long getSynchlingPoolTimeout() {
    return getConfig().getSynchlingPoolTimeout();
  }

  /** How often we retry when a target is missing
   *
   * @param val
   */
  @Override
  public void setMissingTargetRetries(final int val) {
    getConfig().setMissingTargetRetries(val);
  }

  /**
   * @return How often we retry when a target is missing
   */
  @Override
  public int getMissingTargetRetries() {
    return getConfig().getMissingTargetRetries();
  }

  /** web service push callback uri - null for no service
   *
   * @param val    String
   */
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
  public List<Stat> getStats() {
    if (syncher == null) {
      return new ArrayList<Stat>();
    }

    return syncher.getStats();
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  @Override
  public String schema() {
    try {
//      buildSchema = new SchemaThread();

      buildSchema.start();

      return "OK";
    } catch (Throwable t) {
      error(t);

      return "Exception: " + t.getLocalizedMessage();
    }
  }

  @Override
  public synchronized List<String> schemaStatus() {
    if (buildSchema == null) {
      InfoLines infoLines = new InfoLines();

      infoLines.addLn("Schema build has not been started");

      return infoLines;
    }

    return buildSchema.infoLines;
  }

  @Override
  public String listHibernateProperties() {
    StringBuilder res = new StringBuilder();

    List<String> ps = getConfig().getHibernateProperties();

    for (String p: ps) {
      res.append(p);
      res.append("\n");
    }

    return res.toString();
  }

  @Override
  public String displayHibernateProperty(final String name) {
    String val = getConfig().getHibernateProperty(name);

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
    } catch (InterruptedException ie) {
    } catch (Throwable t) {
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

      ConfigurationStore cs = getStore();

      List<String> configNames = cs.getConfigs();

      if (configNames.isEmpty()) {
        error("No configuration on path " + cs.getLocation());
        return "No configuration on path " + cs.getLocation();
      }

      if (configNames.size() != 1) {
        error("1 and only 1 configuration allowed");
        return "1 and only 1 configuration allowed";
      }

      String configName = configNames.iterator().next();

      cfg = getConfigInfo(cs, configName, SynchConfig.class);

      if (cfg == null) {
        error("Unable to read configuration");
        return "Unable to read configuration";
      }

      setConfigName(configName);

      saveConfig(); // Just to ensure we have it for next time

      /* Load up the connectors */

      cs = cs.getStore("connectors");

      connectorNames = cs.getConfigs();

      List<SynchConnConf> sccs = new ArrayList<SynchConnConf>();
      cfg.setConnectorConfs(sccs);

      for (String cn: connectorNames) {
        ObjectName objectName = createObjectName("connector", cn);

        SynchConnConf scc = new SynchConnConf(cs,
                                              objectName.toString(), cn);

        scc.loadConfig();
        sccs.add(scc);
        register("connector", cn, scc);
      }

      return "OK";
    } catch (Throwable t) {
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

  private synchronized Configuration getHibConfiguration() {
    if (hibCfg == null) {
      try {
        hibCfg = new Configuration();

        StringBuilder sb = new StringBuilder();

        List<String> ps = getConfig().getHibernateProperties();

        for (String p: ps) {
          sb.append(p);
          sb.append("\n");
        }

        Properties hprops = new Properties();
        hprops.load(new StringReader(sb.toString()));

        hibCfg.addProperties(hprops).configure();
      } catch (Throwable t) {
        // Always bad.
        error(t);
      }
    }

    return hibCfg;
  }

  /**
   * @param val
   * @return 2 digit val
   */
  private static String twoDigits(final long val) {
    if (val < 10) {
      return "0" + val;
    }

    return String.valueOf(val);
  }

  /* ====================================================================
   *                   Protected methods
   * ==================================================================== */
}