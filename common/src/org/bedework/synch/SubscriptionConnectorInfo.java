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

import edu.rpi.sss.util.Util;

import java.io.StringReader;
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
   */
  public String getConnectorProperties() {
    return connectorProperties;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Load the properties from the serialized form.
   *
   * @throws SynchException
   */
  public void loadProperties() throws SynchException {
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

  /** Add a property to the internal properties
   * @param name
   * @param val
   */
  public void addProperty(final String name, final String val) {
    if (properties == null) {
      properties = new Properties();
    }

    properties.setProperty(name, val);
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  @Override
  public int hashCode() {
    int res = getConnectorId().hashCode();

    if (getConnectorProperties() != null) {
      res *= getConnectorProperties().hashCode();
    }

    return res;
  }
  @Override
  public int compareTo(final SubscriptionConnectorInfo that) {
    if (this == that) {
      return 0;
    }

    int res = getConnectorId().compareTo(that.getConnectorId());
    if (res != 0) {
      return res;
    }

    return Util.compareStrings(getConnectorProperties(),
                               that.getConnectorProperties());
  }

  @Override
  public boolean equals(final Object o) {
    return compareTo((SubscriptionConnectorInfo)o) == 0;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    sb.append("connectorId= ");
    sb.append(getConnectorId());

    if (getConnectorProperties() != null) {
      sb.append(", onnectorProperties = ");
      sb.append(getConnectorProperties());
    }

    sb.append("}");
    return sb.toString();
  }

}
