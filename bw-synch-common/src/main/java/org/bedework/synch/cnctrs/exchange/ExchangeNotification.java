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

import org.bedework.synch.Notification;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

/** Notification from Exchange.
 *
 */
public class ExchangeNotification extends
              Notification<ExchangeNotification.NotificationItem> {
  private ExchangeNotificationMessage enm;

  /**
   * @param sub
   * @param end
   * @param enm
   * @throws SynchException
   */
  public ExchangeNotification(final Subscription sub,
                              final SynchEndType end,
                              final ExchangeNotificationMessage enm) throws SynchException {
    super(sub, end);

    this.enm = enm;
  }

  /**
   * @return ExchangeNotificationMessage
   */
  public ExchangeNotificationMessage getEnm() {
    return enm;
  }

  /**
   * @author douglm
   */
  public static class NotificationItem extends Notification.NotificationItem {
    private ExchangeNotificationMessage.NotificationItem ni;

    NotificationItem(final ExchangeNotificationMessage.NotificationItem ni,
                     final IcalendarType ical) {
      super(ni.getAction(), ical, null);
      this.ni = ni;
    }

    @Override
    protected void toStringSegment(final StringBuilder sb) {
      super.toStringSegment(sb);

      sb.append("\n ni=");
      sb.append(ni);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

      toStringSegment(sb);

      sb.append("}");

      return sb.toString();
    }
  }
}
