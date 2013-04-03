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

import edu.rpi.cmt.config.ConfigBase;
import edu.rpi.sss.util.ToString;

import javax.xml.namespace.QName;

/** Common connector config properties
 *
 * @author douglm
 */
public class ConnectorConfig extends ConfigBase<ConnectorConfig> implements ConnectorConfigI  {
  /** */
  public final static QName confElement = new QName(ns, "synch-connector");

  private static final QName nameProperty = new QName(ns, "name");

  private static final QName classNameProperty = new QName(ns, "className");

  private static final QName readOnlyProperty = new QName(ns, "readOnly");

  private static final QName trustLastmodProperty = new QName(ns, "trustLastmod");

  private static final QName synchProperty = new QName(ns, "synch-property");

  @Override
  public QName getConfElement() {
    return confElement;
  }

  @Override
  public void setName(final String val) {
    setProperty(nameProperty, val);
  }

  @Override
  public String getName() {
    return getPropertyValue(nameProperty);
  }

  @Override
  public void setClassName(final String val) {
    setProperty(classNameProperty, val);
  }

  @Override
  public String getClassName() {
    return getPropertyValue(classNameProperty);
  }

  @Override
  public void setReadOnly(final boolean val) {
    setBooleanProperty(readOnlyProperty, val);
  }

  @Override
  public boolean getReadOnly() {
    return getBooleanPropertyValue(readOnlyProperty);
  }

  @Override
  public void setTrustLastmod(final boolean val) {
    setBooleanProperty(trustLastmodProperty, val);
  }

  @Override
  public boolean getTrustLastmod() {
    return getBooleanPropertyValue(trustLastmodProperty);
  }

  @Override
  public void toStringSegment(final ToString ts) {
    ts.append("name", getName());
    ts.append("className", getClassName());
    ts.append("readOnly", getReadOnly());
    ts.append("trustLastmod", getTrustLastmod());
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
