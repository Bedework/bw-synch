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

import org.bedework.synch.cnctrs.exchange.responses.ExchangeResponse;
import org.bedework.synch.shared.Notification.NotificationItem.ActionType;
import org.bedework.base.ToString;

import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.types.BaseNotificationEventType;
import com.microsoft.schemas.exchange.services._2006.types.BaseObjectChangedEventType;
import com.microsoft.schemas.exchange.services._2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.ModifiedEventType;
import com.microsoft.schemas.exchange.services._2006.types.MovedCopiedEventType;
import com.microsoft.schemas.exchange.services._2006.types.NotificationType;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBElement;

/** Notification from Exchange.
 *
 */
public class ExchangeNotificationMessage extends ExchangeResponse {
  private final String subscriptionId;
  private final String previousWatermark;

  private final List<NotificationItem> notifications = new ArrayList<>();

  /**
   * @param snrm SendNotificationResponseMessageType
   */
  public ExchangeNotificationMessage(final SendNotificationResponseMessageType snrm) {
    super(snrm);

    final NotificationType nt = snrm.getNotification();
    subscriptionId = nt.getSubscriptionId();
    previousWatermark = nt.getPreviousWatermark();

    final var bnes =
      nt.getCopiedEventOrCreatedEventOrDeletedEvent();

    for (final JAXBElement<? extends BaseNotificationEventType> el1: bnes) {
      notifications.add(new NotificationItem(el1.getName().getLocalPart(),
                                             el1.getValue()));
    }
  }

  /**
   * @return String
   */
  public String getSubscriptionId() {
    return subscriptionId;
  }

  /**
   * @return String
   */
  public String getPreviousWatermark() {
    return previousWatermark;
  }

  /**
   * @return notifications
   */
  public List<NotificationItem> getNotifications() {
    return notifications;
  }


  /**
   * @author douglm
   */
  public static class NotificationItem extends BaseObjectChangedEventType {
    private ActionType action;

    // Moved or copied fields
    private FolderIdType oldFolderId;
    private ItemIdType oldItemId;
    private FolderIdType oldParentFolderId;

    // Modified
    private Integer unreadCount;

    private NotificationItem(final String actionStr,
                             final BaseNotificationEventType bne) {
      setWatermark(bne.getWatermark());

      if (actionStr.equals("StatusEvent")) {
        action = ActionType.StatusEvent;

        return;
      }

      if (bne instanceof BaseObjectChangedEventType) {
        final var boce = (BaseObjectChangedEventType)bne;

        setTimeStamp(boce.getTimeStamp());
        setFolderId(boce.getFolderId());
        setItemId(boce.getItemId());
        setParentFolderId(boce.getParentFolderId());
      }

      switch (actionStr) {
        case "CopiedEvent" -> {
          action = ActionType.CopiedEvent;

          final MovedCopiedEventType mce = (MovedCopiedEventType)bne;

          oldFolderId = mce.getOldFolderId();
          oldItemId = mce.getOldItemId();
          oldParentFolderId = mce.getOldParentFolderId();

          return;
        }
        case "CreatedEvent" -> {
          action = ActionType.CreatedEvent;

          return;
        }
        case "DeletedEvent" -> {
          action = ActionType.DeletedEvent;

          return;
        }
        case "ModifiedEvent" -> {
          action = ActionType.ModifiedEvent;
          final ModifiedEventType met = (ModifiedEventType)bne;

          unreadCount = met.getUnreadCount();

          return;
        }
        case "MovedEvent" -> {
          action = ActionType.MovedEvent;
          final MovedCopiedEventType mce = (MovedCopiedEventType)bne;

          oldFolderId = mce.getOldFolderId();
          oldItemId = mce.getOldItemId();
          oldParentFolderId = mce.getOldParentFolderId();

          return;
        }
        case "NewMailEvent" -> {
          action = ActionType.NewMailEvent;

          return;
        }
      }

    }

    /** Common to all
     *
     * @return String
     */
    @Override
    public String getWatermark() {
      return watermark;
    }

    /**
     * @return the action
     */
    public ActionType getAction() {
      return action;
    }

    /** Gets the value of the oldFolderId property.
     *
     * @return FolderIdType
     */
    public FolderIdType getOldFolderId() {
      return oldFolderId;
    }

    /** Gets the value of the oldItemId property.
     *
     * @return FolderIdType
     */
    public ItemIdType getOldItemId() {
      return oldItemId;
    }

    /** Gets the value of the oldParentFolderId property.
     *
     * @return FolderIdType
     */
    public FolderIdType getOldParentFolderId() {
      return oldParentFolderId;
    }

    /** Gets the value of the unreadCount property.
     *
     * @return Integer
     */
    public Integer getUnreadCount() {
      return unreadCount;
    }

    @Override
    public String toString() {
      return new ToString(this).append("watermark", getWatermark())
                               .append("action", getAction())
                               .append("timeStamp", getTimeStamp())
                               .append("folderId", getFolderId())
                               .append("itemId", getItemId())
                               .append("parentFolderId", getParentFolderId())
                               .appendNotNull("oldFolderId", getOldFolderId())
                               .appendNotNull("oldItemId", getOldItemId())
                               .appendNotNull("oldParentFolderId", getOldParentFolderId())
                               .appendNotNull("unreadCount", getUnreadCount())
                               .toString();
    }
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    super.toStringSegment(ts);

    ts.append("subscriptionId", getSubscriptionId())
      .append("previousWatermark", getPreviousWatermark())
      .append("notification items", getNotifications());

    return ts.toString();
  }
}
