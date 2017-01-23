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

import com.microsoft.schemas.exchange.services._2006.messages.SyncFolderItemsType;
import com.microsoft.schemas.exchange.services._2006.types.BaseFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services._2006.types.SyncFolderItemsScopeType;
import com.microsoft.schemas.exchange.services._2006.types.TargetFolderIdType;

/** Get the items from a folder.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class SyncFolderItemsRequest extends BaseRequest<SyncFolderItemsType> {
  /**
   * @param folderId
   */
  public SyncFolderItemsRequest(final BaseFolderIdType folderId) {
    super();

    request = super.createSyncFolderItemsType();

    /* Say we want all properties returned in the response */
    ItemResponseShapeType irs = new ItemResponseShapeType();
    irs.setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
    request.setItemShape(irs);

    /* Sync folder*/
    TargetFolderIdType tfi = new TargetFolderIdType();
    if (folderId instanceof DistinguishedFolderIdType) {
      tfi.setDistinguishedFolderId((DistinguishedFolderIdType)folderId);
    } else {
      tfi.setFolderId((FolderIdType)folderId);
    }

    request.setSyncFolderId(tfi);

    // Leave synch state null - once only
    // setSyncState(String value)

    // setIgnore(ArrayOfBaseItemIdsType value)
    request.setMaxChangesReturned(100);

    request.setSyncScope(SyncFolderItemsScopeType.NORMAL_AND_ASSOCIATED_ITEMS);
 }
}