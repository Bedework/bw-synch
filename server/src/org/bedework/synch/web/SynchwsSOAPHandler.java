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
import org.bedework.exsynch.wsmessages.ObjectFactory;
import org.bedework.exsynch.wsmessages.StatusType;
import org.bedework.exsynch.wsmessages.SubscribeRequestType;
import org.bedework.exsynch.wsmessages.SubscribeResponseType;
import org.bedework.exsynch.wsmessages.UnsubscribeRequestType;
import org.bedework.exsynch.wsmessages.UnsubscribeResponseType;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handle SOAP interactions with the remote service for exchange synch servlet.
 */
public class SynchwsSOAPHandler extends SOAPHandler {
  @Override
  public void init(final ExchangeSynch syncher) throws SynchException {
    super.init(syncher);
    setContextPath("org.bedework.exsynch.wsmessages:" +
                   "ietf.params.xml.ns.icalendar_2");
  }

  @Override
  public void doRequest(final HttpServletRequest req,
                        final HttpServletResponse resp,
                        final String resourceUri) throws SynchException {
    try {
      // Resource uri unused for the moment - must be null or zero length (or "/"

      if (resourceUri.length() > 0) {
        if (!"/".equals(resourceUri)) {
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
      }

      Object o = unmarshalBody(req);

      if (o instanceof SubscribeRequestType) {
        subscribe(resp, (SubscribeRequestType)o);
        return;
      }

      if (o instanceof UnsubscribeRequestType) {
        unsubscribe(resp, (UnsubscribeRequestType)o);
        return;
      }

      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (SynchException se) {
      throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  /** ===================================================================
   *                   private methods
   *  =================================================================== */

  /**
   * @param resp
   * @param sr
   * @throws SynchException
   */
  public void subscribe(final HttpServletResponse resp,
                        final SubscribeRequestType sr) throws SynchException {
    if (debug) {
      trace("Handle subscribe " +  sr.getCalendarHref() +
            " exfolder=" + sr.getExchangeFolderId() +
            "\n       exid=" + sr.getExchangeUser());
    }

    /* Look for a subscription that matches the 2 end points */

    List<ExchangeSubscription> ess = getSyncher().find(sr.getCalendarHref(),
                                                       sr.getExchangeFolderId(),
                                                       sr.getExchangeUser());
    ObjectFactory of = new ObjectFactory();

    SubscribeResponseType sresp = of.createSubscribeResponseType();

    if (!ess.isEmpty()) {
      sresp.setSubscribeStatus(StatusType.ALREADY_SUBSCRIBED);
    } else {
      ExchangeSubscription sub = new ExchangeSubscription(null,
                                                          sr.getCalendarHref(),
                                                          sr.getPrincipalHref(),
                                                          sr.getExchangeFolderId(),
                                                          sr.getExchangeUser(),
                                                          sr.getExchangeEncpw(),
                                                          sr.getExchangeUri(),
                                                          true);
      sresp.setSubscribeStatus(getSyncher().subscribeRequest(sub));
    }

    marshalBody(resp, sresp);
  }

  /**
   * @param resp
   * @param u
   * @throws SynchException
   */
  public void unsubscribe(final HttpServletResponse resp,
                          final UnsubscribeRequestType u) throws SynchException {
    if (debug) {
      trace("Handle unsubscribe " +  u.getSubscriptionId());
    }

    ExchangeSubscription sub;

    sub = getSyncher().getSubscription(u.getSubscriptionId());

    if (sub == null) {
      // No subscription - nothing to do
      return;
    }

    // Ensure fields match
    if (!sub.getprincipalHref().equals(u.getPrincipalHref()) ||
        !sub.getCalPath().equals(u.getCalendarHref())) {
      info("No access for subscription - unmatched parameters " + sub);
      return;
    }

    ObjectFactory of = new ObjectFactory();

    UnsubscribeResponseType usr = of.createUnsubscribeResponseType();

    usr.setSubscribeStatus(getSyncher().unsubscribe(sub));

    marshalBody(resp, usr);
  }
}

