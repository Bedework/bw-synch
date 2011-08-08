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
package org.bedework.synch.web;

import org.bedework.synch.BaseSubscription;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.SynchException;
import org.bedework.synch.cnctrs.exchange.ExchangeNotificationMessage;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;

import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResultType;
import com.microsoft.schemas.exchange.services._2006.types.SubscriptionStatusType;

/** Handle SOAP interactions with Exchange for exchange synch servlet.
 */
public class EwsSOAPHandler extends SOAPHandler {
  @Override
  public void init(final SynchEngine syncher) throws SynchException {
    super.init(syncher);
    setContextPath("com.microsoft.schemas.exchange.services._2006.messages:" +
                   "com.microsoft.schemas.exchange.services._2006.types");
  }

  @Override
  public void doRequest(final HttpServletRequest req,
                        final HttpServletResponse resp,
                        final String resourceUri) throws SynchException {
    try {
      String id = resourceUri;

      if (id.endsWith("/")) {
        // starts with "/"
        id = id.substring(1, id.length() - 1);
      }
      trace("Notification for id " + id);

      if (!getSyncher().getRunning()) {
        resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        return;
      }

      BaseSubscription sub = getSyncher().getSubscription(id);

      if (sub == null) {
        if (debug) {
          trace("No subscription");
        }

        // Just unsubscribe

        notificationResponse(resp, false);
        return;
      }

      if (debug) {
        trace("Found subscription:" + sub);
      }

      SendNotificationResponseType snr = (SendNotificationResponseType)unmarshalBody(req);

      List<JAXBElement<? extends ResponseMessageType>> responseMessages =
        snr.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

      for (JAXBElement<? extends ResponseMessageType> el: responseMessages) {
        ExchangeNotificationMessage note = new ExchangeNotificationMessage((SendNotificationResponseMessageType)el.getValue());

        if (debug) {
          trace(note.toString());
        }

        getSyncher().handleNotification(sub, note);
      }

      notificationResponse(resp, true);
    } catch (SynchException se) {
      throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  /** ===================================================================
   *                   private methods
   *  =================================================================== */

  private void notificationResponse(final HttpServletResponse resp,
                                    final boolean ok) throws SynchException {
    try {
      ObjectFactory of = new ObjectFactory();
      SendNotificationResultType snr = of.createSendNotificationResultType();

      if (ok) {
        snr.setSubscriptionStatus(SubscriptionStatusType.OK);
      } else {
        snr.setSubscriptionStatus(SubscriptionStatusType.UNSUBSCRIBE);
      }

      marshalBody(resp,
                  snr);
    } catch (SynchException se) {
      throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }
}

