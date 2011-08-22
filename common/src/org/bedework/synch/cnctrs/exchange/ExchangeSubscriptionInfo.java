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

import org.bedework.synch.SubscriptionConnectorInfo;
import org.bedework.synch.exception.SynchException;


/** The deserialized information for an Exchange connection.
 *
 * @author Mike Douglass
 */
public class ExchangeSubscriptionInfo {
  private SubscriptionConnectorInfo info;

  private String exchangeCalendar;

  private String exchangeId;

  private String exchangePw;

  private String exchangeURI;

  private String exchangeSubscriptionId;

  private String exchangeWatermark;

  private String exchangeError;

  /**
   *
   * @param info
   * @throws SynchException
   */
  public ExchangeSubscriptionInfo(final SubscriptionConnectorInfo info) throws SynchException {
    this.info = info;
    info.loadProperties();
  }

  /** Constructor
   *
   * @param exchangeCalendar
   * @param exchangeId
   * @param exchangePw
   * @param exchangeURI
   */
  public ExchangeSubscriptionInfo(final String exchangeCalendar,
                                  final String exchangeId,
                                  final String exchangePw,
                                  final String exchangeURI) {
    info = new SubscriptionConnectorInfo();

    setExchangeCalendar(exchangeCalendar);
    setExchangeId(exchangeId);
    setExchangePw(exchangePw);
    setExchangeURI(exchangeURI);
  }

  /** Exchange Calendar
   *
   * @param val    String
   */
  public void setExchangeCalendar(final String val) {
    exchangeCalendar = val;
  }

  /** Exchange Calendar
   *
   * @return String
   */
  public String getExchangeCalendar() {
    return exchangeCalendar;
  }

  /** Exchange Calendar id
   *
   * @param val    String
   */
  public void setExchangeId(final String val) {
    exchangeId = val;
  }

  /** Exchange Calendar id
   *
   * @return String
   */
  public String getExchangeId() {
    return exchangeId;
  }

  /** Encoded exchange Calendar pw
   *
   * @param val    String
   */
  public void setExchangePw(final String val) {
    exchangePw = val;
  }

  /** Encoded xchange Calendar pw
   *
   * @return String
   */
  public String getExchangePw() {
    return exchangePw;
  }

  /** Exchange web service uri
   *
   * @param val    String
   */
  public void setExchangeURI(final String val) {
    exchangeURI = val;
  }

  /** Exchange web service uri
   *
   * @return String
   */
  public String getExchangeURI() {
    return exchangeURI;
  }

  /** Exchange system subscriptionId.
   *
   * @param val    String
   */
  public void setExchangeSubscriptionId(final String val) {
    exchangeSubscriptionId = val;
  }

  /** Exchange system subscriptionId.
   *
   * @return String
   */
  public String getExchangeSubscriptionId() {
    return exchangeSubscriptionId;
  }

  /** Exchange watermark.
   *
   * @param val    String
   */
  public void setExchangeWatermark(final String val) {
    exchangeWatermark = val;
  }

  /** Exchange watermark.
   *
   * @return String
   */
  public String getExchangeWatermark() {
    return exchangeWatermark;
  }

  /** Exchange error code.
   *
   * @param val    String
   */
  public void setExchangeError(final String val) {
    exchangeError = val;
  }

  /** Exchange error code.
   *
   * @return String
   */
  public String getExchangeError() {
    return exchangeError;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  protected void toStringSegment(final StringBuilder sb,
                              final String indent) {
    sb.append(",\n");
    sb.append(indent);
    sb.append("exchangeCalendar = ");
    sb.append(getExchangeCalendar());
    sb.append(", exchangeURI = ");
    sb.append(getExchangeURI());
  }

  /* ====================================================================
   *                        Object methods
   * ==================================================================== */

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ExchangeSubscription{");

    toStringSegment(sb, "  ");

    sb.append("}");
    return sb.toString();
  }
}
