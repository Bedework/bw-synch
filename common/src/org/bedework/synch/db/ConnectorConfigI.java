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


import org.bedework.synch.exception.SynchException;

import java.util.Set;

/** Common connector config properties
 *
 * @author douglm
 */
public interface ConnectorConfigI {
  /** Connector name - unique key
   *
   * @param val    String
   */
  void setName(final String val);

  /** Connector name - unique key
   *
   * @return String
   */
  String getName();

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

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /**
   * @param val
   */
  void setProperties(final Set<SynchProperty> val);

  /**
   * @return properties
   */
  Set<SynchProperty> getProperties();

  /**
   * @param name
   * @return properties with given name
   */
  Set<SynchProperty> getProperties(final String name);

  /**
   * @param name
   * @return single value of valued property with given name
   * @throws SynchException if more than one property with given name
   */
  String getPropertyValue(final String name) throws SynchException;

  /**
   * @param name
   * @return single value of valued property with given name
   * @throws SynchException if more than one property with given name
   */
  Integer getIntPropertyValue(final String name) throws SynchException;

  /**
   * @param name
   * @return single value of valued property with given name
   * @throws SynchException if more than one property with given name
   */
  Long getLongPropertyValue(final String name) throws SynchException;

  /** Remove all with given name
   *
   * @param name
   */
  void removeProperties(final String name);

  /**
   * @return int
   */
  int getNumProperties();

  /**
   * @param name
   * @return property or null
   */
  SynchProperty findProperty(final String name);

  /** Set the single valued property
   *
   * @param name
   * @param value
   * @throws SynchException if more than one property with given name
   */
  void setProperty(final String name, final String value) throws SynchException;

  /**
   * @param val
   */
  void addProperty(final SynchProperty val);

  /**
   * @param val
   * @return boolean
   */
  boolean removeProperty(final SynchProperty val);

  /**
   * @return set of SynchProperty
   */
  Set<SynchProperty> copyProperties();

  /**
   * @return set of SynchProperty
   */
  Set<SynchProperty> cloneProperties();
}
