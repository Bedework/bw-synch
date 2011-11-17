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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/** Common connector config properties
 *
 * @author douglm
 */
public class ConnectorConfig
      extends DbItem<ConnectorConfig> implements ConnectorConfigI {
  private String name;

  private String className;

  private boolean readOnly;

  private boolean trustLastmod;

  private Set<SynchProperty> properties;

  /** Connector name - unique key
   *
   * @param val    String
   */
  @Override
  public void setName(final String val) {
    name = val;
  }

  /** Connector name - unique key
   *
   * @return String
   */
  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setClassName(final String val) {
    className = val;
  }

  @Override
  public String getClassName() {
    return className;
  }

  @Override
  public void setReadOnly(final boolean val) {
    readOnly = val;
  }

  @Override
  public boolean getReadOnly() {
    return readOnly;
  }

  @Override
  public void setTrustLastmod(final boolean val) {
    trustLastmod = val;
  }

  @Override
  public boolean getTrustLastmod() {
    return trustLastmod;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /**
   * @param val
   */
  @Override
  public void setProperties(final Set<SynchProperty> val) {
    properties = val;
  }

  /**
   * @return properties
   */
  @Override
  public Set<SynchProperty> getProperties() {
    return properties;
  }

  /**
   * @param name
   * @return properties with given name
   */
  @Override
  public Set<SynchProperty> getProperties(final String name) {
    TreeSet<SynchProperty> ps = new TreeSet<SynchProperty>();

    if (getNumProperties() == 0) {
      return null;
    }

    for (SynchProperty p: getProperties()) {
      if (p.getName().equals(name)) {
        ps.add(p);
      }
    }

    return ps;
  }

  /** Remove all with given name
   *
   * @param name
   */
  @Override
  public void removeProperties(final String name) {
    Set<SynchProperty> ps = getProperties(name);

    if (ps == null) {
      return;
    }

    for (SynchProperty p: ps) {
      removeProperty(p);
    }
  }

  /**
   * @return int
   */
  @Override
  public int getNumProperties() {
    Collection<SynchProperty> c = getProperties();
    if (c == null) {
      return 0;
    }

    return c.size();
  }

  @Override
  public SynchProperty findProperty(final String name) {
    Collection<SynchProperty> props = getProperties();

    if (props == null) {
      return null;
    }

    for (SynchProperty prop: props) {
      if (name.equals(prop.getName())) {
        return prop;
      }
    }

    return null;
  }

  @Override
  public void setProperty(final String name,
                          final String value) throws SynchException {
    Set<SynchProperty> ps = getProperties(name);

    if (ps.size() == 0) {
      addProperty(new SynchProperty(name, value));
      return;
    }

    if (ps.size() > 1) {
      throw new SynchException("Multiple values for single valued property " + name);
    }

    SynchProperty p = ps.iterator().next();

    if (!p.getValue().equals(value)) {
      p.setValue(value);
    }
  }

  @Override
  public String getPropertyValue(final String name) throws SynchException {
    Set<SynchProperty> ps = getProperties(name);

    if (ps.size() == 0) {
      return null;
    }

    if (ps.size() > 1) {
      throw new SynchException("Multiple values for single valued property " + name);
    }

    return ps.iterator().next().getValue();
  }

  @Override
  public Integer getIntPropertyValue(final String name) throws SynchException {
    String s = getPropertyValue(name);

    if (s == null) {
      return null;
    }

    return Integer.valueOf(s);
  }

  @Override
  public Long getLongPropertyValue(final String name) throws SynchException {
    String s = getPropertyValue(name);

    if (s == null) {
      return null;
    }

    return Long.valueOf(s);
  }

  @Override
  public void addProperty(final SynchProperty val) {
    Set<SynchProperty> c = getProperties();
    if (c == null) {
      c = new TreeSet<SynchProperty>();
      setProperties(c);
    }

    if (!c.contains(val)) {
      c.add(val);
    }
  }

  /**
   * @param val
   * @return boolean
   */
  @Override
  public boolean removeProperty(final SynchProperty val) {
    Set<SynchProperty> c = getProperties();
    if (c == null) {
      return false;
    }

    return c.remove(val);
  }

  /**
   * @return set of SynchProperty
   */
  @Override
  public Set<SynchProperty> copyProperties() {
    if (getNumProperties() == 0) {
      return null;
    }
    TreeSet<SynchProperty> ts = new TreeSet<SynchProperty>();

    for (SynchProperty p: getProperties()) {
      ts.add(p);
    }

    return ts;
  }

  /**
   * @return set of SynchProperty
   */
  @Override
  public Set<SynchProperty> cloneProperties() {
    if (getNumProperties() == 0) {
      return null;
    }
    TreeSet<SynchProperty> ts = new TreeSet<SynchProperty>();

    for (SynchProperty p: getProperties()) {
      ts.add((SynchProperty)p.clone());
    }

    return ts;
  }

  /** Add our stuff to the StringBuilder
   *
   * @param sb    StringBuilder for result
   * @param indent
   */
  public void toStringSegment(final StringBuilder sb,
                                 final String indent) {
    sb.append("name = ");
    sb.append(getName());

    sb.append(", className = ");
    sb.append(getClassName());

    sb.append(",\n");
    sb.append(indent);
    sb.append("readOnly = ");
    sb.append(getReadOnly());

    sb.append(", trustLastmod = ");
    sb.append(getTrustLastmod());
  }

  /* ====================================================================
   *                   Object methods
   * We only allow one of these in teh db so any and all are equal.
   * ==================================================================== */

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final ConnectorConfig that) {
    return getName().compareTo(that.getName());
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    toStringSegment(sb, "  ");

    sb.append("}");
    return sb.toString();
  }
}
