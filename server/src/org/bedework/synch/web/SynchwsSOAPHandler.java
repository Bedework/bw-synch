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

import org.bedework.synch.Subscription;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.SynchException;
import org.bedework.synch.wsmessages.SubscribeRequestType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handle SOAP interactions with the remote service for exchange synch servlet.
 */
public class SynchwsSOAPHandler extends SOAPHandler {
  @Override
  public void init(final SynchEngine syncher) throws SynchException {
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

    List<Subscription> ess = getSyncher().find(sr.getCalendarHref(),
                                                       sr.getExchangeFolderId(),
                                                       sr.getExchangeUser());
    ObjectFactory of = new ObjectFactory();

    SubscribeResponseType sresp = of.createSubscribeResponseType();

    if (!ess.isEmpty()) {
      sresp.setSubscribeStatus(StatusType.ALREADY_SUBSCRIBED);
    } else {
      Subscription sub = new Subscription(null,
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

    Subscription sub;

    sub = getSyncher().getSubscription(u.getSubscriptionId());

    if (sub == null) {
      // No subscription - nothing to do
      return;
    }

    // Ensure fields match
    if (!sub.getPrincipalHref().equals(u.getPrincipalHref()) ||
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

