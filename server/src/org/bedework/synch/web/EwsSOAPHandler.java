/* **********************************************************************
    Copyright 2010 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/

package org.bedework.exchgsynch.web;

import org.bedework.exchgsynch.ExchangeSynch;
import org.bedework.exchgsynch.intf.ExchangeSubscription;
import org.bedework.exchgsynch.intf.SynchException;
import org.bedework.exchgsynch.responses.Notification;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;

import com.microsoft.schemas.exchange.services._2006.messages.ObjectFactory;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResultType;
import com.microsoft.schemas.exchange.services._2006.types.SubscriptionStatusType;

/** Handle SOAP interactions with Exchange for exchange synch servlet.
 */
public class EwsSOAPHandler extends SOAPHandler {
  @Override
  public void init(final ExchangeSynch syncher) throws SynchException {
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

      ExchangeSubscription sub = getSyncher().getSubscription(id);

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
        Notification note = new Notification((SendNotificationResponseMessageType)el.getValue());

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

