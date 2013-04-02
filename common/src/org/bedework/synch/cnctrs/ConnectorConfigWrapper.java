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

import org.bedework.synch.conf.ConnectorConfig;
import org.bedework.synch.conf.ConnectorConfigI;

import edu.rpi.cmt.config.ConfigBase;

import javax.xml.namespace.QName;

/** Common connector config properties
 *
 * @author douglm
 *
 * @param <T>
 */
public class ConnectorConfigWrapper<T extends ConnectorConfigWrapper>
extends ConfigBase<T> implements Comparable<T>, ConnectorConfigI {
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

  @Override
  public QName getConfElement() {
    return conf.getConfElement();
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
