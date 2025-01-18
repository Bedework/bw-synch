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
import org.bedework.util.calendar.XcalUtil;
import org.bedework.base.ToString;

import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services._2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.ArrayList;
import java.util.List;

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
      final ToString ts = new ToString(this);

      folderIdToString(ts, "itemId", itemId).newLine();
      return folderIdToString(ts, "parentFolderId", parentFolderId)
              .newLine()
              .indentIn()
              .append("uid", uid)
              .newLine()
              .append("lastMod", lastMod)
              .toString();
    }

    private ToString folderIdToString(final ToString ts,
                                      final String name,
                                      final Object id) {
      ts.delimitersOff()
        .append(name)
        .append("={id=");

      final String iid;
      final String ckey;

      if (id instanceof final FolderIdType fid) {
        iid = fid.getId();
        ckey = fid.getChangeKey();
      } else if (id instanceof final ItemIdType fid) {
        iid = fid.getId();
        ckey = fid.getChangeKey();
      } else {
        iid = "Unhandled class: " + id.getClass();
        ckey = iid;
      }

      ts.append(iid)
        .append(",")
        .newLine()
        .append("        changeKey=")
        .append(ckey)
        .append("}");

      return ts;
    }
  }

  private List<SynchInfo> synchInfo;

  /**
   * @param firm
   * @param synchInfoOnly
   */
  public FinditemsResponse(final FindItemResponseMessageType firm,
                           final boolean synchInfoOnly,
                           final XcalUtil.TzGetter tzGetter) {
    super(firm);

    final FindItemParentType rf = firm.getRootFolder();

    includesLastItemInRange = rf.isIncludesLastItemInRange();

    final XmlIcalConvert cnv = new XmlIcalConvert(tzGetter);

    for (final ItemType item: rf.getItems().getItemOrMessageOrCalendarItem()) {
      if (!(item instanceof final CalendarItemType ci)) {
        continue;
      }

      if (!synchInfoOnly) {
        final IcalendarType ical = cnv.toXml(ci);

        if (icals == null) {
          icals = new ArrayList<IcalendarType>();
        }

        icals.add(ical);
        continue;
      }

      // Synchinfo

      final SynchInfo si = new SynchInfo(ci.getItemId(),
                                         ci.getParentFolderId(),
//                                   ci.getItemClass(),
                                         ci.getUID(),
                                         ci.getLastModifiedTime().toXMLFormat());

      if (synchInfo == null) {
        synchInfo = new ArrayList<>();
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
    final ToString ts = new ToString(this);

    super.toStringSegment(ts);

    ts.append("includesLastItemInRange", getIncludesLastItemInRange());

//    if (icals != null) {
//      for (IcalendarType ical: icals) {
//        sb.append(",\n     ");
//      }
//    }

    ts.append("synchInfo", synchInfo);

    return ts.toString();
  }
}
