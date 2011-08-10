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

import java.util.List;

import com.microsoft.schemas.exchange.services._2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services._2006.types.BaseItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.BodyTypeResponseType;
import com.microsoft.schemas.exchange.services._2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services._2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfBaseItemIdsType;

/** Get items by id.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class GetItemsRequest extends BaseRequest<GetItemType> {
  /**
   * @param itemIds
   */
  public GetItemsRequest(final List<BaseItemIdType> itemIds) {
    super();

    request = super.createGetItemType();

    /* Say we want all properties returned in the response */
    ItemResponseShapeType irs = new ItemResponseShapeType();
    irs.setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);

    /* Say we want plain text for the description.
     */
    irs.setBodyType(BodyTypeResponseType.TEXT);

    request.setItemShape(irs);

    NonEmptyArrayOfBaseItemIdsType baseItemIds = new NonEmptyArrayOfBaseItemIdsType();
    request.setItemIds(baseItemIds);
    baseItemIds.getItemIdOrOccurrenceItemIdOrRecurringMasterItemId().addAll(itemIds);
  }
}