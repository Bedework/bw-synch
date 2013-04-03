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
import org.bedework.synch.wsmessages.ArrayOfSynchProperties;
import org.bedework.synch.wsmessages.SynchPropertyType;

import edu.rpi.sss.util.Util;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

/** Serializable form of information for a connection to a system via a
 * connector - a connector id and the serialized proeprties.
 *
 * @param <T>
 */
public class SerializableProperties<T> implements Comparable<T> {
  private String synchProperties;

  /* Loaded from the serialized form */
  private Properties properties;

  private boolean changed;

  /**
   *
   */
  public SerializableProperties() {
  }

  /** Build from an array of properties
   * @param props
   * @throws SynchException
   */
  public SerializableProperties(final ArrayOfSynchProperties props) throws SynchException {
    if (props == null) {
      return;
    }

    for (SynchPropertyType prop: props.getProperty()) {
      setProperty(prop.getName(), prop.getValue());
    }
  }

  /**
   * @param val serialized properties
   */
  public void setSynchProperties(final String val) {
    synchProperties = val;
  }

  /**
   * @return serialized properties
   * @throws SynchException
   */
  public String getSynchProperties() throws SynchException {
    if (changed) {
      try {
        Writer wtr = new StringWriter();

        properties.store(wtr, null);
        synchProperties = wtr.toString();
      } catch (Throwable t) {
        throw new SynchException(t);
      }
    }
    return synchProperties;
  }

  /** Set the changed flag
   *
   * @param val
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
   * @throws SynchException
   */
  public synchronized void loadProperties() throws SynchException {
    try {
      if (properties == null) {
        properties = new Properties();
      }

      if (getSynchProperties() != null) {
          properties.load(new StringReader(getSynchProperties()));
      }
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  /** Set a property in the internal properties - loading them from the
   * external value first if necessary.
   *
   * @param name
   * @param val
   * @throws SynchException
   */
  public void setProperty(final String name,
                          final String val) throws SynchException {
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
   * @param name
   * @return val
   * @throws SynchException
   */
  public synchronized String getProperty(final String name) throws SynchException {
    if (properties == null) {
      loadProperties();
    }

    return properties.getProperty(name);
  }
  protected void toStringSegment(final StringBuilder sb,
                                 final String indent) {
    try {
      if (getSynchProperties() != null) {
        sb.append(", synchProperties = ");
        sb.append(getSynchProperties());
      }
    } catch (Throwable t) {
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
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public int compareTo(final T that) {
    throw new RuntimeException("CompareTo not implemented");
  }

  /**
   * @param that
   * @return int
   */
  public int compareTo(final SerializableProperties that) {
    if (this == that) {
      return 0;
    }

    try {
      return Util.compareStrings(getSynchProperties(),
                                 that.getSynchProperties());
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public boolean equals(final Object o) {
    return compareTo((SerializableProperties)o) == 0;
  }

  @Override
  public String toString() {
    try {
      StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

      toStringSegment(sb, "");

      sb.append("}");
      return sb.toString();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

}
