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

import org.bedework.synch.BaseSubscriptionInfo;
import org.bedework.synch.db.SubscriptionConnectorInfo;
import org.bedework.synch.exception.SynchException;

/** The deserialized information for an Exchange connection.
 *
 * @author Mike Douglass
 */
public class ExchangeSubscriptionInfo extends BaseSubscriptionInfo {
  /** Name of the calendar
   */
  public static final String propnameExchangeCalendar = "exchgCalendar";

  /** The exchange subscription id
   */
  public static final String propnameExchangeSubscriptionId = "exchgSubid";

  /** The exchange watermark
   */
  public static final String propnameExchangeWatermark = "exchgWatermark";

  /**
   *
   * @param info
   * @throws SynchException
   */
  public ExchangeSubscriptionInfo(final SubscriptionConnectorInfo info) throws SynchException {
    super(info);
  }

  /** Exchange Calendar
   *
   * @param val    String
   * @throws SynchException
   */
  public void setExchangeCalendar(final String val) throws SynchException {
    setProperty(propnameExchangeCalendar, val);
  }

  /** Exchange Calendar
   *
   * @return String
   * @throws SynchException
   */
  public String getExchangeCalendar() throws SynchException {
    return getProperty(propnameExchangeCalendar);
  }

  /** Exchange system subscriptionId.
   *
   * @param val    String
   * @throws SynchException
   */
  public void setExchangeSubscriptionId(final String val) throws SynchException {
    setProperty(propnameExchangeSubscriptionId, val);
  }

  /** Exchange system subscriptionId.
   *
   * @return String
   * @throws SynchException
   */
  public String getExchangeSubscriptionId() throws SynchException {
    return getProperty(propnameExchangeSubscriptionId);
  }

  /** Exchange watermark.
   *
   * @param val    String
   * @throws SynchException
   */
  public void setExchangeWatermark(final String val) throws SynchException {
    setProperty(propnameExchangeWatermark, val);
  }

  /** Exchange watermark.
   *
   * @return String
   * @throws SynchException
   */
  public String getExchangeWatermark() throws SynchException {
    return getProperty(propnameExchangeWatermark);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  @Override
  protected void toStringSegment(final StringBuilder sb,
                              final String indent) {
    super.toStringSegment(sb, indent);

    try {
      sb.append(",\n");
      sb.append(indent);
      sb.append("exchangeCalendar = ");
      sb.append(getExchangeCalendar());
      sb.append(", exchangeSubscriptionId = ");
      sb.append(getExchangeSubscriptionId());
      sb.append(", exchangeWatermark = ");
      sb.append(getExchangeWatermark());
    } catch (Throwable t) {
      sb.append(t.getMessage());
    }
  }
}
