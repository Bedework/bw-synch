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

import org.bedework.synch.cnctrs.ConnectorConfigWrapper;
import org.bedework.synch.conf.ConnectorConfig;

import edu.rpi.sss.util.ToString;

import javax.xml.namespace.QName;

/** Bedework synch connector config
 *
 * @author douglm
 */
public class BedeworkConnectorConfig
    extends ConnectorConfigWrapper<BedeworkConnectorConfig> {
  /** WSDL for remote service */
  private static final QName propBwWSDLURI = new QName(ns, "bwWSDLURI");

  /** seconds before retry on failure  */
  private static final QName propRetryInterval = new QName(ns, "retryInterval");

  /** seconds before we ping just to say we're still around  */
  private static final QName propKeepAliveInterval = new QName(ns, "keepAliveInterval");

  /**
   * @param conf
   */
  public BedeworkConnectorConfig(final ConnectorConfig conf) {
    super(conf);
  }

  /** bedework web service WSDL uri
   *
   * @param val    String
   */
  public void setBwWSDLURI(final String val) {
    unwrap().setProperty(propBwWSDLURI, val);
  }

  /** Bedework web service WSDL uri
   *
   * @return String
   */
  public String getBwWSDLURI() {
    return unwrap().getPropertyValue(propBwWSDLURI);
  }

  /** retryInterval - seconds
   *
   * @param val    int seconds
   */
  public void setRetryInterval(final int val) {
    unwrap().setIntegerProperty(propRetryInterval, val);
  }

  /** retryInterval - seconds
   *
   * @return int seconds
   */
  public int getRetryInterval() {
    return unwrap().getIntegerPropertyValue(propRetryInterval);
  }

  /** KeepAliveInterval - seconds
   *
   * @param val    int seconds
   */
  public void setKeepAliveInterval(final int val) {
    unwrap().setIntegerProperty(propKeepAliveInterval, val);
  }

  /** KeepAliveInterval - seconds
   *
   * @return int seconds
   */
  public int getKeepAliveInterval() {
    return unwrap().getIntegerPropertyValue(propKeepAliveInterval);
  }

  @Override
  public void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);

    ts.append("bwWSDLURI", getBwWSDLURI()).
      append("retryInterval", getRetryInterval()).
      append("keepAliveInterval", getKeepAliveInterval());
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
