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
package org.bedework.synch;

import org.bedework.synch.exception.SynchException;

import edu.rpi.sss.util.Util;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

/** Serializable form of information for a connection to a system via a
 * connector - a connector id and the serialized proeprties.
 *
 * @author douglm
 */
public class SubscriptionConnectorInfo implements Comparable<SubscriptionConnectorInfo> {
  private String connectorId;

  private String connectorProperties;

  /* Loaded from the serialized form */
  private Properties properties;

  private boolean changed;

  /**
   * @param val id
   */
  public void setConnectorId(final String val) {
    connectorId = val;
  }

  /**
   * @return id
   */
  public String getConnectorId() {
    return connectorId;
  }

  /**
   * @param val serialized properties
   */
  public void setConnectorProperties(final String val) {
    connectorProperties = val;
  }

  /**
   * @return serialized properties
   * @throws SynchException
   */
  public String getConnectorProperties() throws SynchException {
    if (changed) {
      try {
        Writer wtr = new StringWriter();

        properties.store(wtr, null);
        connectorProperties = wtr.toString();
      } catch (Throwable t) {
        throw new SynchException(t);
      }
    }
    return connectorProperties;
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

      if (getConnectorProperties() != null) {
          properties.load(new StringReader(getConnectorProperties()));
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

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  @Override
  public int hashCode() {
    try {
      int res = getConnectorId().hashCode();

      if (getConnectorProperties() != null) {
        res *= getConnectorProperties().hashCode();
      }

      return res;
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public int compareTo(final SubscriptionConnectorInfo that) {
    if (this == that) {
      return 0;
    }

    try {
      int res = getConnectorId().compareTo(that.getConnectorId());
      if (res != 0) {
        return res;
      }

      return Util.compareStrings(getConnectorProperties(),
                                 that.getConnectorProperties());
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public boolean equals(final Object o) {
    return compareTo((SubscriptionConnectorInfo)o) == 0;
  }

  @Override
  public String toString() {
    try {
      StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

      sb.append("connectorId= ");
      sb.append(getConnectorId());

      if (getConnectorProperties() != null) {
        sb.append(", connectorProperties = ");
        sb.append(getConnectorProperties());
      }

      sb.append("}");
      return sb.toString();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

}
