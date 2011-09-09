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
package org.bedework.synch.cnctrs.exchange;

import org.bedework.synch.cnctrs.ConnectorConfig;

/** Exchange synch connector config
*
* @author douglm
*/
public class ExchangeConnectorConfig extends ConnectorConfig {
  private String exchangeWSDLURI;

  /** Exchange web service WSDL uri
   *
   * @param val    String
   */
  public void setExchangeWSDLURI(final String val) {
    exchangeWSDLURI = val;
  }

  /** Exchange web service WSDL uri
   *
   * @return String
   */
  public String getExchangeWSDLURI() {
    return exchangeWSDLURI;
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
    sb.append("exchangeWSDLURI = ");
    sb.append(getExchangeWSDLURI());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    toStringSegment(sb, "  ");

    sb.append("}");
    return sb.toString();
  }
}
