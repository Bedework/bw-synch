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

import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import java.util.ArrayList;
import java.util.List;

/**
 * @author douglm
 *
 */
public class Synch implements SynchMBean {
  private transient Logger log;

  private boolean running;

  private SynchEngine syncher;

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

  /* ========================================================================
   * Dump/restore
   * ======================================================================== */

  private boolean create;

  private String delimiter;

  private boolean drop;

  /* Be safe - default to false */
  private boolean export;

  private boolean format;

  private boolean haltOnError;

  private String schemaOutFile;

  private String sqlIn;

  private String dataIn;

  private String dataOut;

  private String dataOutPrefix;

  private Configuration cfg;

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#getName()
   */
  @Override
  public String getName() {
    /* This apparently must be the same as the name attribute in the
     * jboss service definition
     */
    return "org.bedework:service=Synch";
  }

  @Override
  public void setAppname(final String val) {
	  SynchEngine.setAppname(val);
  }

  @Override
  public String getAppname() {
    return SynchEngine.getAppname();
  }

  /* ========================================================================
   * Dump/restore
   * ======================================================================== */

  @Override
  public void setCreate(final boolean val) {
    create = val;
  }

  @Override
  public boolean getCreate() {
    return create;
  }

  @Override
  public void setDelimiter(final String val) {
    delimiter = val;
  }

  @Override
  public String getDelimiter() {
    return delimiter;
  }

  @Override
  public void setDrop(final boolean val) {
    drop = val;
  }

  @Override
  public boolean getDrop() {
    return drop;
  }

  @Override
  public void setExport(final boolean val) {
    export = val;
  }

  @Override
  public boolean getExport() {
    return export;
  }

  /** Format the output?
   *
   * @param val
   */
  @Override
  public void setFormat(final boolean val) {
    format = val;
  }

  @Override
  public boolean getFormat() {
    return format;
  }

  @Override
  public void setHaltOnError(final boolean val) {
    haltOnError = val;
  }

  @Override
  public boolean getHaltOnError() {
    return haltOnError;
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
  public void setSqlIn(final String val) {
    sqlIn = val;
  }

  @Override
  public String getSqlIn() {
    return sqlIn;
  }

  @Override
  public void setDataIn(final String val) {
    dataIn = val;
  }

  @Override
  public String getDataIn() {
    return dataIn;
  }

  @Override
  public void setDataOut(final String val) {
    dataOut = val;
  }

  @Override
  public String getDataOut() {
    return dataOut;
  }

  @Override
  public void setDataOutPrefix(final String val) {
    dataOutPrefix = val;
  }

  @Override
  public String getDataOutPrefix() {
    return dataOutPrefix;
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  @Override
  public boolean testSchemaValid() {
    return true;
  }

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#schema()
   */
  @Override
  public String schema() {
    String result = "Export complete: check logs";

    try {
      SchemaExport se = new SchemaExport(getConfiguration());

      if (getDelimiter() != null) {
        se.setDelimiter(getDelimiter());
      }

      se.setFormat(getFormat());
      se.setHaltOnError(getHaltOnError());
      se.setOutputFile(getSchemaOutFile());
      se.setImportFile(getSqlIn());

      se.execute(false, // script - causes write to System.out if true
                 getExport(),
                 getDrop(),
                 getCreate());
    } catch (Throwable t) {
      error(t);
      result = "Exception: " + t.getLocalizedMessage();
    } finally {
      create = false;
      drop = false;
      export = false;
    }

    return result;
  }

  @Override
  public synchronized List<String> restoreData() {
    List<String> infoLines = new ArrayList<String>();

    try {
      /*
      if (!disableIndexer()) {
        infoLines.add("***********************************\n");
        infoLines.add("********* Unable to disable indexer\n");
        infoLines.add("***********************************\n");
      }

      long startTime = System.currentTimeMillis();

      Restore r = new Restore(getConfiguration());

      String[] args = new String[] {"-appname",
                                    appname
      };

      r.getConfigProperties(new Args(args));

      r.setFilename(getDataIn());

      r.setNoIndexes(true);

      r.open();

      r.doRestore();

      r.close();

      r.stats(infoLines);

      long millis = System.currentTimeMillis() - startTime;
      long seconds = millis / 1000;
      long minutes = seconds / 60;
      seconds -= (minutes * 60);

      infoLines.add("Elapsed time: " + minutes + ":" +
                    Restore.twoDigits(seconds) + "\n");

      infoLines.add("Restore complete" + "\n");
      */
      infoLines.add("************************Restore unimplemented *************************" + "\n");
    } catch (Throwable t) {
      error(t);
      infoLines.add("Exception - check logs: " + t.getMessage() + "\n");
    } finally {
      /*
      try {
        if (!reindex()) {
          infoLines.add("***********************************");
          infoLines.add("********* Unable to disable indexer");
          infoLines.add("***********************************");
        }
      } catch (Throwable t) {
        error(t);
        infoLines.add("Exception - check logs: " + t.getMessage() + "\n");
      }
        */
    }

    return infoLines;
  }

  @Override
  public List<String> dumpData() {
    List<String> infoLines = new ArrayList<String>();

    try {
      /*
      long startTime = System.currentTimeMillis();

      Dump d = new Dump(getConfiguration());

      String[] args = new String[] {"-appname",
                                    appname
      };

      d.getConfigProperties(args);

      StringBuilder fname = new StringBuilder(getDataOut());
      if (!getDataOut().endsWith("/")) {
        fname.append("/");
      }

      fname.append(getDataOutPrefix());

      /* append "yyyyMMddTHHmmss" * /
      fname.append(DateTimeUtil.isoDateTime());
      fname.append(".xml");

      d.setFilename(fname.toString());

      d.open();

      d.doDump();

      d.close();

      d.stats(infoLines);

      long millis = System.currentTimeMillis() - startTime;
      long seconds = millis / 1000;
      long minutes = seconds / 60;
      seconds -= (minutes * 60);

      infoLines.add("Elapsed time: " + minutes + ":" +
                    Restore.twoDigits(seconds) + "\n");

      infoLines.add("Dump complete" + "\n");
      */
      infoLines.add("************************Dump unimplemented *************************" + "\n");
    } catch (Throwable t) {
      error(t);
      infoLines.add("Exception - check logs: " + t.getMessage() + "\n");
    }

    return infoLines;
  }

  @Override
  public String dropTables() {
    return "Not implemented";
  }

  @Override
  public List<Stat> getStats() {
    if (syncher == null) {
      return new ArrayList<Stat>();
    }

    return syncher.getStats();
  }

  /* an example say's we need this  - we should probably implement some system
   * independent jmx support which will build this using introspection and/or lists
  public MBeanInfo getMBeanInfo() throws Exception {
    InitialContext ic = new InitialContext();
    RMIAdaptor server = (RMIAdaptor) ic.lookup("jmx/rmi/RMIAdaptor");

    ObjectName name = new ObjectName(MBEAN_OBJ_NAME);

    // Get the MBeanInfo for this MBean
    MBeanInfo info = server.getMBeanInfo(name);
    return info;
  }
  */

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#create()
   */
  @Override
  public void create() {
    // An opportunity to initialise
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#start()
   */
  @Override
  public void start() {
    if (processor != null) {
      error("Already started");
      return;
    }

    running = true;

    processor = new ProcessorThread(getName());
    processor.start();
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#stop()
   */
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

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#isStarted()
   */
  @Override
  public boolean isStarted() {
    return (processor != null) && processor.isAlive();
  }

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#destroy()
   */
  @Override
  public void destroy() {
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private synchronized Configuration getConfiguration() {
    if (cfg == null) {
      cfg = new Configuration().configure();
    }

    return cfg;
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
