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



/** Information about a single connector property. This information will be
 * published by the system allowing clients to determine what properties are
 * needed.
 *
 * @author Mike Douglass
 */
public class ConnectorPropertyInfo {
  private String name;

  private boolean secure;

  private String description;

  /**
   * @param name - name for the property
   * @param secure - true if this property value should be hidden, e.g password
   * @param description - of the property
   */
  public ConnectorPropertyInfo(final String name,
                               final boolean secure,
                               final String description) {
    this.name = name;
    this.secure = secure;
    this.description = description;
  }

  /**
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * @return secure flag
   */
  public boolean getSecure() {
    return secure;
  }

  /**
   * @return description
   */
  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    sb.append("name = ");
    sb.append(getName());

    sb.append(", secure = ");
    sb.append(getSecure());

    sb.append(",\n   description = ");
    sb.append(getDescription());

    sb.append("}");
    return sb.toString();
  }

}
