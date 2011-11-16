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
import org.bedework.synch.db.ConnectorConfig;
import org.bedework.synch.exception.SynchException;

import edu.rpi.sss.util.ToString;

/** Bedework synch connector config
 *
 * @author douglm
 */
public class BedeworkConnectorConfig
    extends ConnectorConfigWrapper<BedeworkConnectorConfig> {
  /** WSDL for remote service */
  private static final String propBwWSDLURI = "bwWSDLURI";

  /** seconds before retry on failure  */
  private static final String propRetryInterval = "retryInterval";

  /** seconds before we ping just to say we're still around  */
  private static final String propKeepAliveInterval = "keepAliveInterval";

  /**
   * @param conf
   */
  public BedeworkConnectorConfig(final ConnectorConfig conf) {
    super(conf);
  }

  /** bedework web service WSDL uri
   *
   * @param val    String
   * @throws SynchException
   */
  public void setBwWSDLURI(final String val) throws SynchException {
    setProperty(propBwWSDLURI, val);
  }

  /** Bedework web service WSDL uri
   *
   * @return String
   * @throws SynchException
   */
  public String getBwWSDLURI() throws SynchException {
    return getPropertyValue(propBwWSDLURI);
  }

  /** retryInterval - seconds
   *
   * @param val    int seconds
   * @throws SynchException
   */
  public void setRetryInterval(final int val) throws SynchException {
    setProperty(propRetryInterval, String.valueOf(val));
  }

  /** retryInterval - seconds
   *
   * @return int seconds
   * @throws SynchException
   */
  public int getRetryInterval() throws SynchException {
    Integer i = getIntPropertyValue(propRetryInterval);

    if (i == null) {
      return 0;
    }

    return i.intValue();
  }

  /** KeepAliveInterval - seconds
   *
   * @param val    int seconds
   * @throws SynchException
   */
  public void setKeepAliveInterval(final int val) throws SynchException {
    setProperty(propKeepAliveInterval, String.valueOf(val));
  }

  /** KeepAliveInterval - seconds
   *
   * @return int seconds
   * @throws SynchException
   */
  public int getKeepAliveInterval() throws SynchException {
    Integer i = getIntPropertyValue(propKeepAliveInterval);

    if (i == null) {
      return 0;
    }

    return i.intValue();
  }

  /** Add our stuff to the StringBuilder
   *
   * @param ts    ToString for result
   */
  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts.getSb(), "  ");

    try {
      ts.append("bwWSDLURI", getBwWSDLURI()).
        append("retryInterval", getRetryInterval()).
        append("keepAliveInterval", getKeepAliveInterval());
    } catch (SynchException e) {
      ts.append(e);
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
