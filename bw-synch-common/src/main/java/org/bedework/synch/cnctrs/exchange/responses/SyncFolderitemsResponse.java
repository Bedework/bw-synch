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

import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.ToString;

import com.microsoft.schemas.exchange.services._2006.messages.SyncFolderItemsResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.types.BaseNotificationEventType;
import com.microsoft.schemas.exchange.services._2006.types.BaseObjectChangedEventType;
import com.microsoft.schemas.exchange.services._2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.ModifiedEventType;
import com.microsoft.schemas.exchange.services._2006.types.MovedCopiedEventType;
import com.microsoft.schemas.exchange.services._2006.types.SyncFolderItemsChangesType;
import com.microsoft.schemas.exchange.services._2006.types.SyncFolderItemsCreateOrUpdateType;

import java.util.List;

import javax.xml.bind.JAXBElement;

/** Notification from Exchange.
 *
 */
public class SyncFolderitemsResponse extends ExchangeResponse
        implements Logged {
  private final String syncState;
  private final Boolean includesLastItemInRange;
  private SyncFolderItemsChangesType changes;

  /**
   * @param sfirm SyncFolderItemsResponseMessageType
   */
  public SyncFolderitemsResponse(final SyncFolderItemsResponseMessageType sfirm) {
    super(sfirm);

    syncState = sfirm.getSyncState();
    includesLastItemInRange = sfirm.isIncludesLastItemInRange();

    final List<JAXBElement<?>> syncitems = sfirm.getChanges().getCreateOrUpdateOrDelete();

    for (final JAXBElement<?> el1: syncitems) {
      final String chgType = el1.getName().getLocalPart();

      final SyncFolderItemsCreateOrUpdateType s = (SyncFolderItemsCreateOrUpdateType)el1.getValue();

      if (debug()) {
        debug("chgType =" + chgType);
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

      if (bne instanceof final BaseObjectChangedEventType boce) {
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
      return new ToString(this)
              .append("watermark", getWatermark())
              .append("action", getAction())
              .append("timeStamp", getTimeStamp())
              .newLine()
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

    ts.append("syncState", getSyncState());

    ts.append("includesLastItemInRange", getIncludesLastItemInRange());

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

    return ts.toString();
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
