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

import org.bedework.synch.wsmessages.SynchEndType;

import com.microsoft.schemas.exchange.services._2006.messages.SubscribeType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfNotificationEventTypesType;
import com.microsoft.schemas.exchange.services._2006.types.NotificationEventTypeType;
import com.microsoft.schemas.exchange.services._2006.types.PushSubscriptionRequestType;

/** Build a subscription request.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class SubscribeRequest extends BaseRequest<SubscribeType> {
  private NonEmptyArrayOfBaseFolderIdsType folders;
  private DistinguishedFolderIdType fid;

  /**
   * @param subId
   * @param end
   * @param watermark
   * @param callBackUri
   */
  public SubscribeRequest(final String subId,
                          final SynchEndType end,
                          final String watermark,
                          final String callBackUri) {
    super();

    request = super.createSubscribeType();

    PushSubscriptionRequestType psr = types.createPushSubscriptionRequestType();
    request.setPushSubscriptionRequest(psr);

    /* ===== Setup BaseSubscription ====== */

    folders = types.createNonEmptyArrayOfBaseFolderIdsType();
    psr.setFolderIds(folders);
//    sub.fid = types.createFolderIdType();
    fid = types.createDistinguishedFolderIdType();

    NonEmptyArrayOfNotificationEventTypesType etypes = types.createNonEmptyArrayOfNotificationEventTypesType();
    psr.setEventTypes(etypes);

    etypes.getEventType().add(NotificationEventTypeType.COPIED_EVENT);
    etypes.getEventType().add(NotificationEventTypeType.CREATED_EVENT);
    etypes.getEventType().add(NotificationEventTypeType.DELETED_EVENT);
    etypes.getEventType().add(NotificationEventTypeType.MODIFIED_EVENT);
    etypes.getEventType().add(NotificationEventTypeType.MOVED_EVENT);

    /* ===== Setup PushSubscription ====== */

    psr.setStatusFrequency(1);  // 1 minute poll

    psr.setWatermark(watermark);

    StringBuilder uri = new StringBuilder(callBackUri);
    if (!callBackUri.endsWith("/")) {
      uri.append("/");
    }

    uri.append(end.name());
    uri.append(subId);
    uri.append("/");

    psr.setURL(uri.toString());
  }

  /**
   * @param val
   */
  public void setFolderId(final String val) {
    // XXX Need to allow a distinguished id or a folder id
    fid.setId(DistinguishedFolderIdNameType.fromValue(val));
    folders.getFolderIdOrDistinguishedFolderId().add(fid);
  }
}