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
package org.bedework.synch.cnctrs.exchange.responses;

import org.bedework.synch.exception.SynchException;

import java.util.List;

import javax.xml.bind.JAXBElement;

import com.microsoft.schemas.exchange.services._2006.messages.SyncFolderItemsResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.types.BaseNotificationEventType;
import com.microsoft.schemas.exchange.services._2006.types.BaseObjectChangedEventType;
import com.microsoft.schemas.exchange.services._2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.ModifiedEventType;
import com.microsoft.schemas.exchange.services._2006.types.MovedCopiedEventType;
import com.microsoft.schemas.exchange.services._2006.types.SyncFolderItemsChangesType;
import com.microsoft.schemas.exchange.services._2006.types.SyncFolderItemsCreateOrUpdateType;

/** Notification from Exchange.
 *
 */
public class SyncFolderitemsResponse extends ExchangeResponse {
  private String syncState;
  private Boolean includesLastItemInRange;
  private SyncFolderItemsChangesType changes;

  /**
   * @param sfirm
   * @throws SynchException
   */
  public SyncFolderitemsResponse(final SyncFolderItemsResponseMessageType sfirm) throws SynchException {
    super(sfirm);

    syncState = sfirm.getSyncState();
    includesLastItemInRange = sfirm.isIncludesLastItemInRange();

    List<JAXBElement<?>> syncitems = sfirm.getChanges().getCreateOrUpdateOrDelete();

    for (JAXBElement<?> el1: syncitems) {
      String chgType = el1.getName().getLocalPart();

      SyncFolderItemsCreateOrUpdateType s = (SyncFolderItemsCreateOrUpdateType)el1.getValue();

      if (debug) {
        debugMsg("chgType =" + chgType);
      }
    }
  }

  /** Gets the syncState property.
   *
   * @return String
   */
  public String getSyncState() {
    return syncState;
  }

  /** Gets the value of the includesLastItemInRange property.
   *
   * @return Boolean
   */
  public Boolean getIncludesLastItemInRange() {
    return includesLastItemInRange;
  }

  /** Gets the value of the changes property.
   *
   * @return SyncFolderItemsChangesType }
   *
   */
  public SyncFolderItemsChangesType getChanges() {
    return changes;
  }

  /**
   * @author douglm
   */
  public static class NotificationItem extends BaseObjectChangedEventType {
    /**
     * @author douglm
     */
    public enum ActionType {
      /** */
      CopiedEvent,
      /** */
      CreatedEvent,
      /** */
      DeletedEvent,
      /** */
      ModifiedEvent,
      /** */
      MovedEvent,
      /** */
      NewMailEvent,
      /** */
      StatusEvent
    }

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
        BaseObjectChangedEventType boce = (BaseObjectChangedEventType)bne;

        setTimeStamp(boce.getTimeStamp());
        setFolderId(boce.getFolderId());
        setItemId(boce.getItemId());
        setParentFolderId(boce.getParentFolderId());
      }

      if (actionStr.equals("CopiedEvent")) {
        action = ActionType.CopiedEvent;

        MovedCopiedEventType mce = (MovedCopiedEventType)bne;

        oldFolderId = mce.getOldFolderId();
        oldItemId = mce.getOldItemId();
        oldParentFolderId = mce.getOldParentFolderId();

        return;
      }

      if (actionStr.equals("CreatedEvent")) {
        action = ActionType.CreatedEvent;

        return;
      }

      if (actionStr.equals("DeletedEvent")) {
        action = ActionType.DeletedEvent;

        return;
      }

      if (actionStr.equals("ModifiedEvent")) {
        action = ActionType.ModifiedEvent;
        ModifiedEventType met = (ModifiedEventType)bne;

        unreadCount = met.getUnreadCount();

        return;
      }

      if (actionStr.equals("MovedEvent")) {
        action = ActionType.MovedEvent;
        MovedCopiedEventType mce = (MovedCopiedEventType)bne;

        oldFolderId = mce.getOldFolderId();
        oldItemId = mce.getOldItemId();
        oldParentFolderId = mce.getOldParentFolderId();

        return;
      }

      if (actionStr.equals("NewMailEvent")) {
        action = ActionType.NewMailEvent;

        return;
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
      StringBuilder sb = new StringBuilder("NotificationItem{");

      sb.append("watermark=");
      sb.append(getWatermark());

      sb.append(",\n      action=");
      sb.append(getAction());

      sb.append(", timeStamp=");
      sb.append(getTimeStamp());

      sb.append(",\n      folderId=");
      sb.append(getFolderId());

      sb.append(",\n      itemId=");
      sb.append(getItemId());

      sb.append(",\n      parentFolderId=");
      sb.append(getParentFolderId());

      if (getOldFolderId() != null) {
        sb.append(",\n      oldFolderId=");
        sb.append(getOldFolderId());
      }

      if (getOldItemId() != null) {
        sb.append(",\n       oldItemId=");
        sb.append(getOldItemId());
      }

      if (getOldParentFolderId() != null) {
        sb.append(",\n       oldParentFolderId=");
        sb.append(getOldParentFolderId());
      }

      if (getUnreadCount() != null) {
        sb.append(",\n       unreadCount=");
        sb.append(getUnreadCount());
      }

      sb.append("}");

      return sb.toString();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Notification{");

    super.toStringSegment(sb);

    sb.append(",\n   syncState=");
    sb.append(getSyncState());

    sb.append(",\n   includesLastItemInRange=");
    sb.append(getIncludesLastItemInRange());

    /*
    String delim = ",\n   notification items{\n      ";
    for (NotificationItem ni: getNotifications()) {
      sb.append(delim);
      sb.append(ni.toString());

      delim =",\n      ";
    }

    if (getNotifications().size() > 0) {
      sb.append("}");
    }

    sb.append("}");
    */

    return sb.toString();
  }
}
