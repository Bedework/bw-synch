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
package org.bedework.synch.cnctrs.exchange.messages;

import com.microsoft.schemas.exchange.services._2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services._2006.types.BaseFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services._2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services._2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfPathsToElementType;
import com.microsoft.schemas.exchange.services._2006.types.UnindexedFieldURIType;

/** Get the items from a folder.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class FindItemsRequest extends BaseRequest<FindItemType> {
  /** Get enough information out of Exchange to allowus to figure out what we
   * need to fetch to (re)synch the target system with Exchange
   *
   * @param parentId
   * @return FindItemsRequest object ready for call.
   */
  public static FindItemsRequest getSynchInfo(final BaseFolderIdType parentId) {
    FindItemsRequest fir = new FindItemsRequest(parentId);

    /* Specify the actual properties */
    ItemResponseShapeType irs = new ItemResponseShapeType();
    irs.setBaseShape(DefaultShapeNamesType.ID_ONLY);

    /* Say we want the UID and the lastmod. */

    NonEmptyArrayOfPathsToElementType additionalProperties =
      new NonEmptyArrayOfPathsToElementType();

    fir.addShapeProp(additionalProperties, UnindexedFieldURIType.CALENDAR_UID);
    fir.addShapeProp(additionalProperties, UnindexedFieldURIType.ITEM_LAST_MODIFIED_TIME);
    fir.addShapeProp(additionalProperties, UnindexedFieldURIType.ITEM_PARENT_FOLDER_ID);

    irs.setAdditionalProperties(additionalProperties);

    fir.request.setItemShape(irs);

    return fir;
  }

  /**
   * @param parentId
   */
  public FindItemsRequest(final BaseFolderIdType parentId) {
    super();

    request = super.createFindItemType();

    /* Say we want all properties returned in the response */
//    ItemResponseShapeType irs = new ItemResponseShapeType();
  //  irs.setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
    //request.setItemShape(irs);

    /* Here we can set one or none of
     *    indexedPageItemView   (for paged requests?)
     *    fractionalPageItemView (send me a quarter?)
     *    calendarView         (date ranged)
     *    contactsView         (limit alpahebetically)
     *
     *    none I guess returns all
     */

    // setGroupBy(GroupByType value)
    // setDistinguishedGroupBy(DistinguishedGroupByType value)
    // setRestriction(RestrictionType value)
    // setSortOrder(NonEmptyArrayOfFieldOrdersType value)

    /* Parent folder ids */
    NonEmptyArrayOfBaseFolderIdsType fids = new NonEmptyArrayOfBaseFolderIdsType();
    fids.getFolderIdOrDistinguishedFolderId().add(parentId);
    request.setParentFolderIds(fids);

    // setQueryString(String value)

    request.setTraversal(ItemQueryTraversalType.SHALLOW);
  }
}