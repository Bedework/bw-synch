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

import edu.rpi.cmt.jmx.MBeanInfo;

/** Configure a connector for the Bedework synch engine service
 *
 * @author douglm
 */
public interface SynchConnConfMBean {
  /* ========================================================================
   * Status
   * ======================================================================== */

  /**
   * @return status message
   */
  String getStatus();

  /** Connector name
   *
   * @return String
   */
  @MBeanInfo("Connector name")
  String getName();

  /* ========================================================================
   * Config properties
   * ======================================================================== */

  /** Class name
   *
   * @param val    String
   */
  void setClassName(final String val);

  /** Class name
   *
   * @return String
   */
  String getClassName();

  /** Read only?
   *
   * @param val    int seconds
   */
  void setReadOnly(final boolean val);

  /** Read only?
   *
   * @return int seconds
   */
  boolean getReadOnly();

  /** Can we trust the lastmod from this connector?
   *
   * @param val    boolean
   */
  void setTrustLastmod(final boolean val);

  /** Can we trust the lastmod from this connector?
   *
   * @return boolean
   */
  boolean getTrustLastmod();

  /** Set of properties
   *
   * @return String
   */
  String getProperties();

  /* ========================================================================
   * Operations
   * ======================================================================== */

  /** Add a property
   *
   * @param name
   * @param value
   */
  public void addProperty(String name, String value);

  /** Set or add a property
   *
   * @param name
   * @param value
   * @return completion message
   */
  public String setProperty(String name, String value);

  /** Remove a property
   *
   * @param name
   */
  public void removeProperty(String name);

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