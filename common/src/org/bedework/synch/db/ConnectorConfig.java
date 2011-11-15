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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/** Common connector config properties
 *
 * @author douglm
 */
public class ConnectorConfig extends DbItem<ConnectorConfig> {
  private String name;

  private String className;

  private boolean readOnly;

  private boolean trustLastmod;

  private Set<SynchProperty> properties;

  /** Connector name - unique key
   *
   * @param val    String
   */
  public void setName(final String val) {
    name = val;
  }

  /** Connector name - unique key
   *
   * @return String
   */
  public String getName() {
    return name;
  }

  /** Class name
   *
   * @param val    String
   */
  public void setClassName(final String val) {
    className = val;
  }

  /** Class name
   *
   * @return String
   */
  public String getClassName() {
    return className;
  }

  /** Read only?
   *
   * @param val    int seconds
   */
  public void setReadOnly(final boolean val) {
    readOnly = val;
  }

  /** Read only?
   *
   * @return int seconds
   */
  public boolean getReadOnly() {
    return readOnly;
  }

  /** Can we trust the lastmod from this connector?
   *
   * @param val    boolean
   */
  public void setTrustLastmod(final boolean val) {
    trustLastmod = val;
  }

  /** Can we trust the lastmod from this connector?
   *
   * @return boolean
   */
  public boolean getTrustLastmod() {
    return trustLastmod;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /**
   * @param val
   */
  public void setProperties(final Set<SynchProperty> val) {
    properties = val;
  }

  /**
   * @return properties
   */
  public Set<SynchProperty> getProperties() {
    return properties;
  }

  /**
   * @param name
   * @return properties with given name
   */
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
  public int getNumProperties() {
    Collection<SynchProperty> c = getProperties();
    if (c == null) {
      return 0;
    }

    return c.size();
  }

  /**
   * @param name
   * @return property or null
   */
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

  /**
   * @param val
   */
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
   */
  protected void toStringSegment(final StringBuilder sb,
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
