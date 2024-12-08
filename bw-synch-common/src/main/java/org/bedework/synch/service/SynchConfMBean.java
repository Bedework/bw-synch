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

import org.bedework.synch.shared.Stat;
import org.bedework.util.jmx.ConfBaseMBean;
import org.bedework.util.jmx.MBeanInfo;

import java.util.List;

/** Configure the Bedework synch engine service
 *
 * @author douglm
 */
public interface SynchConfMBean extends ConfBaseMBean {
  /* ==============================================================
   * Config properties
   * ============================================================== */

  /**
   * @param val current size of synchling pool
   */
  void setSynchlingPoolSize(int val);

  /**
   * @return current size of synchling pool
   */
  @MBeanInfo("size of synchling pool.")
  int getSynchlingPoolSize();

  /**
   * @param val timeout in millisecs
   */
  void setSynchlingPoolTimeout(long val);

  /**
   * @return timeout in millisecs
   */
  @MBeanInfo("timeout in millisecs.")
  long getSynchlingPoolTimeout();

  /**
   *
   * @param val How often we retry when a target is missing
   */
  void setMissingTargetRetries(int val);

  /**
   * @return How often we retry when a target is missing
   */
  @MBeanInfo("How often we retry when a target is missing.")
  int getMissingTargetRetries();

  /** web service push callback uri - null for no service
   *
   * @param val    String
   */
  void setCallbackURI(String val);

  /** web service push callback uri - null for no service
   *
   * @return String
   */
  @MBeanInfo("web service push callback uri - null for no service.")
  String getCallbackURI();

  /** Timezone server location
   *
   * @param val    String
   */
  void setTimezonesURI(String val);

  /** Timezone server location
   *
   * @return String
   */
  @MBeanInfo("Timezone server location.")
  String getTimezonesURI();

  /** Path to keystore - null for use default
   *
   * @param val    String
   */
  void setKeystore(String val);

  /** Path to keystore - null for use default
   *
   * @return String
   */
  @MBeanInfo("Path to keystore - null for use default.")
  String getKeystore();

  /**
   *
   * @param val    String
   */
  void setPrivKeys(String val);

  /**
   *
   * @return String
   */
  @MBeanInfo("Name of private keys file.")
  String getPrivKeys();

  /**
   *
   * @param val    String
   */
  void setPubKeys(String val);

  /**
   *
   * @return String
   */
  @MBeanInfo("Name of public keys file.")
  String getPubKeys();

  /** List connector names
   *
   * @return list of names
   */
  @MBeanInfo("List the connector names.")
  List<String> getConnectorNames();

  /**
   *
   * @param val true if this instance only handles subscriptions
   */
  void setSubscriptionsOnly(boolean val);

  /**
   *
   * @return true if this instance only handles subscriptions
   */
  @MBeanInfo("True if this instance only handles subscriptions.")
  boolean getSubscriptionsOnly();

  /** Get the current stats
   *
   * @return List of Stat
   */
  @MBeanInfo("Get the current stats.")
  List<Stat> getStats();

  /**
   *
   * @param val Export schema to database?
   */
  void setExport(boolean val);

  /**
   * @return true for export schema
   */
  @MBeanInfo("Export (write) schema to database?")
  boolean getExport();

  /**
   *
   * @param val Output file name - full path
   */
  void setSchemaOutFile(String val);

  /**
   * @return Output file name - full path
   */
  @MBeanInfo("Full path of schema output file")
  String getSchemaOutFile();

  /**
   *
   * @param val Dump threshold
   */
  void setDumpThreshold(int val);

  /**
   * @return Dump threshold
   */
  @MBeanInfo("Dump threshold")
  int getDumpThreshold();

  /* ========================================================================
   * Operations
   * ======================================================================== */

  /** Create or dump new schema. If export and drop set will try to drop tables.
   * Export and create will create a schema in the db and export, drop, create
   * will drop tables, and try to create a new schema.
   * <p>
   * The export and drop flags will all be reset to false after this,
   * whatever the result. This avoids accidental damage to the db.
   *
   * @return Completion message
   */
  @MBeanInfo("Start build of the database schema. Set export flag to write to db.")
  String schema();

  /** Returns status of the schema build.
   *
   * @return Completion messages
   */
  @MBeanInfo("Status of the database schema build.")
  List<String> schemaStatus();

  /** Reschedule the subscription now
   *
   * @param id of subscription
   * @return status
   */
  @MBeanInfo("Reschedule the subscription now")
  String rescheduleNow(@MBeanInfo("id") String id);

  /**
   * @param value the hibernate dialect class
   */
  @MBeanInfo("Set the hibernate dialect")
  void setHibernateDialect(@MBeanInfo("value: a valid hibernate dialect class") String value);

  /**
   * @return Completion messages
   */
  @MBeanInfo("Get the hibernate dialect")
  String getHibernateDialect();

  /** List the hibernate properties
   *
   * @return properties
   */
  @MBeanInfo("List the hibernate properties")
  String listHibernateProperties();

  /** Display the named property
   *
   * @param name of property
   * @return value
   */
  @MBeanInfo("Display the named hibernate property")
  String displayHibernateProperty(@MBeanInfo("name") String name);

  /** Remove the named property
   *
   * @param name of property
   */
  @MBeanInfo("Remove the named hibernate property")
  void removeHibernateProperty(@MBeanInfo("name") String name);

  /**
   * @param name of property
   * @param value of property
   */
  @MBeanInfo("Add a hibernate property")
  void addHibernateProperty(@MBeanInfo("name") String name,
                              @MBeanInfo("value") String value);

  /**
   * @param name of property
   * @param value of property
   */
  @MBeanInfo("Set a hibernate property")
  void setHibernateProperty(@MBeanInfo("name") String name,
                            @MBeanInfo("value") String value);

  /* ==============================================================
   * Lifecycle
   * ============================================================== */

  /** Lifecycle
   *
   */
  void start();

  /** Lifecycle
   *
   */
  void stop();

  /** Lifecycle
   *
   * @return true if started
   */
  boolean isStarted();

  /** (Re)load the configuration
   *
   * @return status
   */
  @MBeanInfo("(Re)load the configuration")
  String loadConfig();
}
