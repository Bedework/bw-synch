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

import org.bedework.synch.shared.SerializableProperties;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.ArrayOfSynchProperties;
import org.bedework.synch.wsmessages.SynchPropertyType;
import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

/** Serializable form of information for a connection to a system via a
 * connector - a connector id and the serialized proeprties.
 *
 * @param <T>
 */
public abstract class SerializablePropertiesImpl<T>
        implements SerializableProperties<T> {
  private String synchProperties;

  /* Loaded from the serialized form */
  private Properties properties;

  private boolean changed;

  /**
   *
   */
  public SerializablePropertiesImpl() {
  }

  /** Build from an array of properties
   * @param props an array of properties
   */
  public SerializablePropertiesImpl(final ArrayOfSynchProperties props) {
    if (props == null) {
      return;
    }

    for (final SynchPropertyType prop: props.getProperty()) {
      setProperty(prop.getName(), prop.getValue());
    }
  }

  public ArrayOfSynchProperties getAllSynchProperties() {
    loadProperties();

    final ArrayOfSynchProperties asp = new ArrayOfSynchProperties();
    final List<SynchPropertyType> l = asp.getProperty();

    for (final String s: properties.stringPropertyNames()) {
      final SynchPropertyType prop = new SynchPropertyType();
      prop.setName(s);
      prop.setValue(properties.getProperty(s));
      l.add(prop);
    }

    return asp;
  }

  /**
   * @param val serialized properties
   */
  public void setSynchProperties(final String val) {
    synchProperties = val;
  }

  /**
   * @return serialized properties
   */
  public String getSynchProperties() {
    if (changed) {
      try {
        final Writer wtr = new StringWriter();

        properties.store(wtr, null);
        synchProperties = wtr.toString();
      } catch (final Throwable t) {
        throw new SynchException(t);
      }
    }
    return synchProperties;
  }

  /** Set the changed flag
   *
   * @param val the changed flag
   */
  public void setChanged(final boolean val) {
    changed = val;
  }

  /**
   * @return changed flag.
   */
  public boolean getChanged() {
    return changed;
  }

  /**
   * reset the changed flag.
   */
  public void resetChanged() {
    if (!changed) {
      return;
    }

    properties = null;
    changed = false;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Load the properties from the serialized form.
   *
   */
  public synchronized void loadProperties() {
    try {
      if (properties == null) {
        properties = new Properties();
      }

      if (getSynchProperties() != null) {
        properties.load(new StringReader(getSynchProperties()));
      }
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  /** Set a property in the internal properties - loading them from the
   * external value first if necessary.
   *
   * @param name of property
   * @param val its value
   */
  public void setProperty(final String name,
                          final String val) {
    if (properties == null) {
      loadProperties();
    }

    if (val == null) {
      properties.remove(name);
    } else {
      properties.setProperty(name, val);
    }
    changed = true;
  }

  /** Get a property from the internal properties - loading them from the
   * external value first if necessary.
   *
   * @param name of property
   * @return val
   */
  public synchronized String getProperty(final String name) {
    if (properties == null) {
      loadProperties();
    }

    return properties.getProperty(name);
  }

  protected void toStringSegment(final ToString ts) {
    try {
      if (getSynchProperties() != null) {
        ts.append("synchProperties", getSynchProperties());
      }
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  @Override
  public int hashCode() {
    try {
      int res = 1;

      if (getSynchProperties() != null) {
        res *= getSynchProperties().hashCode();
      }

      return res;
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   * @param that SerializablePropertiesImpl
   * @return int
   */
  public int doCompare(final SerializablePropertiesImpl<?> that) {
    if (this == that) {
      return 0;
    }

    try {
      return Util.compareStrings(getSynchProperties(),
                                 that.getSynchProperties());
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SerializablePropertiesImpl<?>)) {
      return false;
    }
    return compareTo((T)o) == 0;
  }

  @Override
  public String toString() {
    try {
      final ToString ts = new ToString(this);

      toStringSegment(ts);

      return ts.toString();
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

}
