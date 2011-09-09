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

/** Common connector config properties
 *
 * @author douglm
 */
public class ConnectorConfig {
  private boolean readOnly;

  private boolean trustLastmod;

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

  /** Add our stuff to the StringBuilder
   *
   * @param sb    StringBuilder for result
   */
  protected void toStringSegment(final StringBuilder sb,
                                 final String indent) {
    sb.append("readOnly = ");
    sb.append(getReadOnly());

    sb.append(", trustLastmod = ");
    sb.append(getTrustLastmod());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    toStringSegment(sb, "  ");

    sb.append("}");
    return sb.toString();
  }
}
