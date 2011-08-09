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

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/** Information about a connector for a subscription.
 *
 * <p>Each connector has a kind which is a name used to retrieve a connector
 * from the connector manager. The retrieved connector implements the SynchIntf
 * interface and provides a serializable object to store connection specific
 * properties such as id and password.
 *
 * <p>These properties are obtained by presenting the user with a list of
 * required properties and then encrypting and storing the response.
 *
 * @author Mike Douglass
 */
@SuppressWarnings("rawtypes")
public class SubscriptionConnectorInfo {
  private String connectorId;

  private String connectorProperties;

  /* Following not persisted */

  private Properties props;

  private boolean propsChanged;

  /** null constructor for hibernate
   *
   */
  public SubscriptionConnectorInfo() {
  }

  /** Id of the connector.
   *
   * @param val    String
   */
  public void setConnectorId(final String val) {
	  connectorId = val;
  }

  /**
   * @return String
   */
  public String getConnectorId() {
    return connectorId;
  }

  /** Serialized and encrypted properties
   *
   * @param val    String
   */
  public void setConnectorProperties(final String val) {
	  connectorProperties = val;
  }

  /** Serialized and encrypted properties
   *
   * @return String
   */
  public String getConnectorProperties() {
    return connectorProperties;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Set a property and mark properties as changed
   *
   * @param name
   * @param val
   */
  public void setProperty(final String name, final String val) {
    props.put(name, val);
    propsChanged = true;
  }

  /**
   * @param name
   * @return property value or null
   */
  public String getProperty(final String name) {
    return props.getProperty(name);
  }

  /**
   * @return true of a property waschanged
   */
  public boolean getPropsChanged() {
    return propsChanged;
  }

  /** Reload the properties - usually from the decrypted string properties.
   *
   * @param val
   */
  public void resetProps(final String val) {
    StringReader rdr = new StringReader(val);

    props = new Properties();
    try {
      props.load(rdr);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    propsChanged = false;
  }

  /** Add our stuff to the StringBuilder
   *
   * @param sb    StringBuilder for result
   */
  protected void toStringSegment(final StringBuilder sb,
                                 final String indent) {
    sb.append(",\n");
    sb.append(indent);
    sb.append("connectorId = ");
    sb.append(getConnectorId());

    sb.append(",\n");
    sb.append(indent);
    sb.append("connectorProperties = ");
    sb.append(getConnectorProperties());
  }

  /* ====================================================================
   *                        Object methods
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
  public String toString() {
    StringBuilder sb = new StringBuilder("SubscriptionConnectorInfo{");

    toStringSegment(sb, "  ");

    sb.append("}");
    return sb.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SubscriptionConnectorInfo)) {
      return false;
    }

    SubscriptionConnectorInfo that = (SubscriptionConnectorInfo)o;

    if (!getConnectorId().equals(that.getConnectorId())) {
      return false;
    }

    return getConnectorProperties().equals(that.getConnectorProperties());
  }
}
