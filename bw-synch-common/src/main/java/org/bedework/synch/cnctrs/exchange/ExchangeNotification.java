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

import org.bedework.synch.shared.Notification;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.util.misc.ToString;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

/** Notification from Exchange.
 *
 */
public class ExchangeNotification extends
        Notification<ExchangeNotification.NotificationItem> {
  private final ExchangeNotificationMessage enm;

  /**
   * @param sub Subscription
   * @param end indicator
   * @param enm ExchangeNotificationMessage
   */
  public ExchangeNotification(final Subscription sub,
                              final SynchEndType end,
                              final ExchangeNotificationMessage enm) {
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
    private final ExchangeNotificationMessage.NotificationItem ni;

    NotificationItem(final ExchangeNotificationMessage.NotificationItem ni,
                     final IcalendarType ical) {
      super(ni.getAction(), ical, null);
      this.ni = ni;
    }

    @Override
    protected void toStringSegment(final ToString ts) {
      super.toStringSegment(ts);

      ts.append("ni", ni);
    }

    @Override
    public String toString() {
      final var ts = new ToString(this);

      toStringSegment(ts);

      return ts.toString();
    }
  }
}
