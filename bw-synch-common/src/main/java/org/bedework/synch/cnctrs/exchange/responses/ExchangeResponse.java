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

import org.bedework.synch.shared.exception.SynchException;
import org.bedework.util.misc.ToString;

import com.microsoft.schemas.exchange.services._2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType.MessageXml;
import com.microsoft.schemas.exchange.services._2006.types.ResponseClassType;
import org.oasis_open.docs.ws_calendar.ns.soap.BaseResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;

/** Base Response from Exchange.
 *
 */
public class ExchangeResponse extends BaseResponseType {
  private ResponseCodeType responseCode;

  private Integer descriptiveLinkKey;

  private MessageXml messageXml;

  /**
   * @param resp
   * @throws SynchException
   */
  public ExchangeResponse(final ResponseMessageType resp) throws SynchException {
    message = resp.getMessageText();

    responseCode = resp.getResponseCode();

    descriptiveLinkKey = resp.getDescriptiveLinkKey();

    messageXml = resp.getMessageXml();

    ResponseClassType rcl = resp.getResponseClass();
    if (rcl.equals(ResponseClassType.ERROR)) {
      status = StatusType.ERROR;
      return;
    }

    if (rcl.equals(ResponseClassType.WARNING)) {
      status = StatusType.WARNING;
      return;
    }

    status = StatusType.OK;
  }

  /**
   * @return - responseCode
   */
  public ResponseCodeType getResponseCode() {
    return responseCode;
  }

  /**
   * @return Integer or null
   */
  public Integer getDescriptiveLinkKey() {
      return descriptiveLinkKey;
  }

  /**
   * @return - message xml
   */
  public MessageXml getMessageXml() {
    return messageXml;
  }

  /**
   * @param ts
   */
  public void toStringSegment(final ToString ts) {
    ts.append("status", getStatus());

    ts.append("responseCode", getResponseCode().toString());

    if (getMessage() != null) {
      ts.append("message", getMessage());
    }

    if (getDescriptiveLinkKey() != null) {
      ts.append("descriptiveLinkKey", getDescriptiveLinkKey());
    }
  }
}
