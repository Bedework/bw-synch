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

import org.bedework.synch.cnctrs.exchange.XmlIcalConvert;
import org.bedework.synch.exception.SynchException;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services._2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemType;

/** Response from Exchange after FindItem request.
 *
 */
public class FinditemsResponse extends ExchangeResponse {
  private final Boolean includesLastItemInRange;

  /* If we're fetching all the info */
  private List<IcalendarType> icals;

  /* If we're only fetching enough to synch */
  /**
   * @author douglm
   *
   */
  public static class SynchInfo {
    /** */
    public ItemIdType itemId;

    /** */
    public FolderIdType parentFolderId;

//    public String itemClass;

    /** */
    public String uid;

    /** */
    public String lastMod;

    /** Constructor
     *
     * @param itemId
     * @param parentFolderId
     * @param uid
     * @param lastMod
     */
    public SynchInfo(final ItemIdType itemId,
                     final FolderIdType parentFolderId,
//                     final String itemClass,
                     final String uid,
                     final String lastMod) {
      this.itemId = itemId;
      this.parentFolderId = parentFolderId;
//      this.itemClass = itemClass;
      this.uid = uid;
      this.lastMod = lastMod;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("SynchInfo{");

      folderIdToString(sb, "itemId", itemId);

      sb.append(",\n      ");
      folderIdToString(sb, "parentFolderId", parentFolderId);

 //     sb.append(",\n   itemClass=");
   //   sb.append(itemClass);

      sb.append(",\n      uid=");
      sb.append(uid);

      sb.append(",\n      lastMod=");
      sb.append(lastMod);

      sb.append("}");

      return sb.toString();
    }

    private void folderIdToString(final StringBuilder sb,
                                  final String name,
                                  final Object id) {
      sb.append(name);
      sb.append("={id=");

      String iid;
      String ckey;

      if (id instanceof FolderIdType) {
        FolderIdType fid = (FolderIdType)id;
        iid = fid.getId();
        ckey = fid.getChangeKey();
      } else if (id instanceof ItemIdType) {
        ItemIdType fid = (ItemIdType)id;
        iid = fid.getId();
        ckey = fid.getChangeKey();
      } else {
        iid = "Unhandled class: " + id.getClass();
        ckey = iid;
      }

      sb.append(iid);

      sb.append(",\n        changeKey=");
      sb.append(ckey);
      sb.append("}");
    }
  }

  private List<SynchInfo> synchInfo;

  /**
   * @param firm
   * @param synchInfoOnly
   * @throws SynchException
   */
  public FinditemsResponse(final FindItemResponseMessageType firm,
                           final boolean synchInfoOnly) throws SynchException {
    super(firm);

    FindItemParentType rf = firm.getRootFolder();

    includesLastItemInRange = rf.isIncludesLastItemInRange();

    XmlIcalConvert cnv = new XmlIcalConvert();

    for (ItemType item: rf.getItems().getItemOrMessageOrCalendarItem()) {
      if (!(item instanceof CalendarItemType)) {
        continue;
      }

      CalendarItemType ci = (CalendarItemType)item;

      if (!synchInfoOnly) {
        IcalendarType ical = cnv.toXml(ci);

        if (icals == null) {
          icals = new ArrayList<IcalendarType>();
        }

        icals.add(ical);
        continue;
      }

      // Synchinfo

      SynchInfo si = new SynchInfo(ci.getItemId(),
                                   ci.getParentFolderId(),
//                                   ci.getItemClass(),
                                   ci.getUID(),
                                   ci.getLastModifiedTime().toXMLFormat());

      if (synchInfo == null) {
        synchInfo = new ArrayList<SynchInfo>();
      }

      synchInfo.add(si);
    }
  }

  /** Gets the value of the includesLastItemInRange property.
   *
   * @return Boolean
   */
  public Boolean getIncludesLastItemInRange() {
    return includesLastItemInRange;
  }

  /**
   * @return components or null
   */
  public List<IcalendarType> getIcals() {
    return icals;
  }

  /**
   * @return synchinfo or null
   */
  public List<SynchInfo> getSynchInfo() {
    return synchInfo;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("FinditemsResponse{");

    super.toStringSegment(sb);

    sb.append(",\n   includesLastItemInRange=");
    sb.append(getIncludesLastItemInRange());

//    if (icals != null) {
//      for (IcalendarType ical: icals) {
//        sb.append(",\n     ");
//      }
//    }

    if (synchInfo != null) {
      for (SynchInfo si: synchInfo) {
        sb.append(",\n     ");
        sb.append(si.toString());
      }
    }
    return sb.toString();
  }
}
