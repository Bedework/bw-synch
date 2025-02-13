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

import org.bedework.base.ToString;

import com.microsoft.schemas.exchange.services._2006.messages.SubscribeResponseMessageType;

/** Response from a subscription.
 *
 */
public class ExsynchSubscribeResponse extends ExchangeResponse {
  private final String subscriptionId;
  private final String watermark;

  /**
   * @param srm SubscribeResponseMessageType
   */
  public ExsynchSubscribeResponse(final SubscribeResponseMessageType srm) {
    /* Successful looks something like
     * <?xml version="1.0" encoding="utf-8"?>
     * <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
     *   <s:Header>
     *     <h:ServerVersionInfo MajorVersion="14" MinorVersion="0"
     *                          MajorBuildNumber="639" MinorBuildNumber="21"
     *                          Version="Exchange2010"
     *          xmlns:h="http://schemas.microsoft.com/exchange/services/2006/types"
     *          xmlns="http://schemas.microsoft.com/exchange/services/2006/types"
     *          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *          xmlns:xsd="http://www.w3.org/2001/XMLSchema"/>
     *   </s:Header>
     *   <s:Body xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *           xmlns:xsd="http://www.w3.org/2001/XMLSchema">
     *     <m:SubscribeResponse xmlns:m="http://schemas.microsoft.com/exchange/services/2006/messages"
     *                          xmlns:t="http://schemas.microsoft.com/exchange/services/2006/types">
     *       <m:ResponseMessages>
     *         <m:SubscribeResponseMessage ResponseClass="Success">
     *           <m:ResponseCode>NoError</m:ResponseCode>
     *           <m:SubscriptionId>HQB0b290bGVzLWZlMS5uZXZlcmxhbmQucnBpLmVkdRAAAAB6doL7rLBaRJpD6SPqdeo6E2rIWt0xzQg=</m:SubscriptionId>
     *           <m:Watermark>AQAAAA9RN9h99EZMiSH6g0jBK/hThQAAAAAAAAA=</m:Watermark>
     *         </m:SubscribeResponseMessage>
     *       </m:ResponseMessages>
     *     </m:SubscribeResponse>
     *   </s:Body>
     * </s:Envelope>
     * ----------------------------------------------------------------------
     * Failure:
     * <?xml version="1.0" encoding="utf-8"?>
     * <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
     *   <s:Header>
     *     <h:ServerVersionInfo MajorVersion="14" MinorVersion="0" MajorBuildNumber="639" MinorBuildNumber="21" Version="Exchange2010"
     *                          xmlns:h="http://schemas.microsoft.com/exchange/services/2006/types"
     *                          xmlns="http://schemas.microsoft.com/exchange/services/2006/types" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *                          xmlns:xsd="http://www.w3.org/2001/XMLSchema"/>
     *   </s:Header>
     *   <s:Body xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
     *     <m:SubscribeResponse xmlns:m="http://schemas.microsoft.com/exchange/services/2006/messages"
     *                          xmlns:t="http://schemas.microsoft.com/exchange/services/2006/types">
     *       <m:ResponseMessages>
     *         <m:SubscribeResponseMessage ResponseClass="Error">
     *           <m:MessageText>Id is malformed.</m:MessageText>
     *           <m:ResponseCode>ErrorInvalidIdMalformed</m:ResponseCode>
     *           <m:DescriptiveLinkKey>0</m:DescriptiveLinkKey>
     *         </m:SubscribeResponseMessage>
     *       </m:ResponseMessages>
     *     </m:SubscribeResponse>
     *   </s:Body>
     * </s:Envelope>
     */
    super(srm);

    subscriptionId = srm.getSubscriptionId();
    watermark = srm.getWatermark();
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
  public String getWatermark() {
    return watermark;
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    super.toStringSegment(ts);

    ts.append("subscriptionId", getSubscriptionId());
    ts.append("watermark", getWatermark());

    return ts.toString();
  }
}
