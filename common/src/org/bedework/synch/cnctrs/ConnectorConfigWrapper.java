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
package org.bedework.synch.cnctrs;

import org.bedework.synch.db.ConnectorConfig;
import org.bedework.synch.db.ConnectorConfigI;
import org.bedework.synch.db.SynchProperty;
import org.bedework.synch.exception.SynchException;

import java.util.Set;

/** Common connector config properties
 *
 * @author douglm
 *
 * @param <T>
 */
public class ConnectorConfigWrapper<T extends ConnectorConfigWrapper>
      implements Comparable<T>, ConnectorConfigI {
  ConnectorConfig conf;

  /**
   * @param conf
   */
  public ConnectorConfigWrapper(final ConnectorConfig conf) {
    this.conf = conf;
  }

  /**
   * @return the wrapped object
   */
  public ConnectorConfig unwrap() {
    return conf;
  }

  /** Connector name - unique key
   *
   * @param val    String
   */
  @Override
  public void setName(final String val) {
    conf.setName(val);
  }

  /** Connector name - unique key
   *
   * @return String
   */
  @Override
  public String getName() {
    return conf.getName();
  }

  /** Class name
   *
   * @param val    String
   */
  @Override
  public void setClassName(final String val) {
    conf.setClassName(val);
  }

  /** Class name
   *
   * @return String
   */
  @Override
  public String getClassName() {
    return conf.getClassName();
  }

  /** Read only?
   *
   * @param val    int seconds
   */
  @Override
  public void setReadOnly(final boolean val) {
    conf.setReadOnly(val);
  }

  /** Read only?
   *
   * @return int seconds
   */
  @Override
  public boolean getReadOnly() {
    return conf.getReadOnly();
  }

  /** Can we trust the lastmod from this connector?
   *
   * @param val    boolean
   */
  @Override
  public void setTrustLastmod(final boolean val) {
    conf.setTrustLastmod(val);
  }

  /** Can we trust the lastmod from this connector?
   *
   * @return boolean
   */
  @Override
  public boolean getTrustLastmod() {
    return conf.getTrustLastmod();
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /**
   * @param val
   */
  @Override
  public void setProperties(final Set<SynchProperty> val) {
    conf.setProperties(val);
  }

  /**
   * @return properties
   */
  @Override
  public Set<SynchProperty> getProperties() {
    return conf.getProperties();
  }

  /**
   * @param name
   * @return properties with given name
   */
  @Override
  public Set<SynchProperty> getProperties(final String name) {
    return conf.getProperties(name);
  }

  /** Remove all with given name
   *
   * @param name
   */
  @Override
  public void removeProperties(final String name) {
    conf.removeProperties(name);
  }

  /**
   * @return int
   */
  @Override
  public int getNumProperties() {
    return conf.getNumProperties();
  }

  /**
   * @param name
   * @return property or null
   */
  @Override
  public SynchProperty findProperty(final String name) {
    return conf.findProperty(name);
  }

  @Override
  public void setProperty(final String name,
                          final String value) throws SynchException {
    conf.setProperty(name, value);
  }

  @Override
  public String getPropertyValue(final String name) throws SynchException {
    return  conf.getPropertyValue(name);
  }

  @Override
  public Integer getIntPropertyValue(final String name) throws SynchException {
    return conf.getIntPropertyValue(name);
  }

  @Override
  public Long getLongPropertyValue(final String name) throws SynchException {
    return conf.getLongPropertyValue(name);
  }

  /**
   * @param val
   */
  @Override
  public void addProperty(final SynchProperty val) {
    conf.addProperty(val);
  }

  /**
   * @param val
   * @return boolean
   */
  @Override
  public boolean removeProperty(final SynchProperty val) {
    return conf.removeProperty(val);
  }

  /**
   * @return set of SynchProperty
   */
  @Override
  public Set<SynchProperty> copyProperties() {
    return conf.copyProperties();
  }

  /**
   * @return set of SynchProperty
   */
  @Override
  public Set<SynchProperty> cloneProperties() {
    return conf.cloneProperties();
  }

  /** Add our stuff to the StringBuilder
   *
   * @param sb    StringBuilder for result
   */
  protected void toStringSegment(final StringBuilder sb,
                                 final String indent) {
    conf.toStringSegment(sb, indent);
  }

  /* ====================================================================
   *                   Object methods
   * We only allow one of these in teh db so any and all are equal.
   * ==================================================================== */

  @Override
  public int compareTo(final T that) {
    return unwrap().compareTo(that.unwrap());
  }

  @Override
  public int hashCode() {
    return unwrap().hashCode();
  }

  @Override
  public String toString() {
    return unwrap().toString();
  }
}
