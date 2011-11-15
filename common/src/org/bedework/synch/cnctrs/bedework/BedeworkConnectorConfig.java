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
package org.bedework.synch.cnctrs.bedework;

import org.bedework.synch.db.ConnectorConfig;

/** Bedework synch connector config
 *
 * @author douglm
 */
public class BedeworkConnectorConfig extends ConnectorConfig {
  /* WSDL for remote service */
  private String bwWSDLURI;

  private int retryInterval;

  private int keepAliveInterval;

  /** bedework web service WSDL uri
   *
   * @param val    String
   */
  public void setBwWSDLURI(final String val) {
    bwWSDLURI = val;
  }

  /** Bedework web service WSDL uri
   *
   * @return String
   */
  public String getBwWSDLURI() {
    return bwWSDLURI;
  }

  /** retryInterval - seconds
   *
   * @param val    int seconds
   */
  public void setRetryInterval(final int val) {
    retryInterval = val;
  }

  /** retryInterval - seconds
   *
   * @return int seconds
   */
  public int getRetryInterval() {
    return retryInterval;
  }

  /** KeepAliveInterval - seconds
   *
   * @param val    int seconds
   */
  public void setKeepAliveInterval(final int val) {
    keepAliveInterval = val;
  }

  /** KeepAliveInterval - seconds
   *
   * @return int seconds
   */
  public int getKeepAliveInterval() {
    return keepAliveInterval;
  }

  /** Add our stuff to the StringBuilder
   *
   * @param sb    StringBuilder for result
   */
  @Override
  protected void toStringSegment(final StringBuilder sb,
                                 final String indent) {
    super.toStringSegment(sb, indent);

    sb.append(",");
    sb.append(indent);
    sb.append("bwWSDLURI = ");
    sb.append(getBwWSDLURI());

    sb.append("retryInterval = ");
    sb.append(getRetryInterval());

    sb.append("keepAliveInterval = ");
    sb.append(getKeepAliveInterval());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    toStringSegment(sb, "  ");

    sb.append("}");
    return sb.toString();
  }
}
