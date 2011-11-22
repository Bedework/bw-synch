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
package org.bedework.synch.cnctrs.manager;

import org.bedework.synch.Notification;
import org.bedework.synch.Notification.NotificationItem;
import org.bedework.synch.Notification.NotificationItem.ActionType;
import org.bedework.synch.SynchDefs.SynchKind;
import org.bedework.synch.cnctrs.AbstractConnector;
import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.db.SubscriptionConnectorInfo;
import org.bedework.synch.db.SubscriptionInfo;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.AlreadySubscribedType;
import org.bedework.synch.wsmessages.ArrayOfSynchConnectorInfo;
import org.bedework.synch.wsmessages.ArrayOfSynchProperties;
import org.bedework.synch.wsmessages.ArrayOfSynchPropertyInfo;
import org.bedework.synch.wsmessages.ConnectorInfoType;
import org.bedework.synch.wsmessages.GetInfoRequestType;
import org.bedework.synch.wsmessages.GetInfoResponseType;
import org.bedework.synch.wsmessages.SubscribeRequestType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SynchConnectorInfoType;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.SynchInfoType;
import org.bedework.synch.wsmessages.SynchPropertyType;
import org.bedework.synch.wsmessages.UnknownSubscriptionType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;
import org.bedework.synch.wsmessages.UnsubscribeResponseType;

import org.oasis_open.docs.ns.wscal.calws_soap.ErrorResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;

/** A special connector to handle calls to the synch engine via the web context.
 *
 * <p>This is the way to call the system to add subscriptions, to unsubscribe etc.
 *
 * @author Mike Douglass
 */
public class SynchConnector
      extends AbstractConnector<SynchConnector,
                                SynchConnectorInstance,
                                Notification> {
  /**
   */
  public SynchConnector() {
    super(null);
  }

  @Override
  public boolean isManager() {
    return true;
  }

  @Override
  public SynchKind getKind() {
    return SynchKind.notify;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public boolean getTrustLastmod() {
    return false;
  }

  @Override
  public SynchConnectorInstance getConnectorInstance(final Subscription sub,
                                                     final SynchEndType end) throws SynchException {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public NotificationBatch handleCallback(final HttpServletRequest req,
                                          final HttpServletResponse resp,
                                          final List<String> resourceUri) throws SynchException {
    try {
      // Resource uri unused for the moment - must be null or zero length (or "/")

      if (resourceUri.size() > 0) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return null;
      }

      Object o = unmarshalBody(req);

      if (o instanceof GetInfoRequestType) {
        return new NotificationBatch(
            new Notification(NotificationItem.ActionType.GetInfo));
      }

      if (o instanceof SubscribeRequestType) {
        return new NotificationBatch(subscribe(resp, (SubscribeRequestType)o));
      }

      if (o instanceof UnsubscribeRequestType) {
        return new NotificationBatch(unsubscribe(resp, (UnsubscribeRequestType)o));
      }

      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return null;
    } catch (SynchException se) {
      throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  @Override
  public void respondCallback(final HttpServletResponse resp,
                              final NotificationBatch<Notification> notifications)
                                                    throws SynchException {
    try {
      /* We only expect single notification items in a batch */

      if (notifications.getNotifications().size() != 1) {
        // XXX Error?
        return;
      }

      @SuppressWarnings("unchecked")
      Notification<NotificationItem> note = notifications.getNotifications().get(0);

      // Again one item per notification.

      if (note.getNotifications().size() != 1) {
        // XXX Error?
        return;
      }

      NotificationItem ni = note.getNotifications().get(0);

      if (ni.getAction() == ActionType.GetInfo) {
        GetInfoResponseType giresp = new GetInfoResponseType();
        SynchInfoType sit = new SynchInfoType();

        giresp.setInfo(sit);
        ArrayOfSynchConnectorInfo asci = new ArrayOfSynchConnectorInfo();
        sit.setConnectors(asci);

        for (String id: syncher.getConnectorIds()) {
          Connector c = syncher.getConnector(id);

          if (c == null) {
            continue;
          }

          SynchConnectorInfoType scit = new SynchConnectorInfoType();

          scit.setName(id);
          scit.setManager(c.isManager());
          scit.setReadOnly(c.isReadOnly());

          ArrayOfSynchPropertyInfo aspi = new ArrayOfSynchPropertyInfo();
          scit.setProperties(aspi);

          c.getPropertyInfo().addAllToList(aspi.getProperty());

          asci.getConnector().add(scit);
        }

        JAXBElement<GetInfoResponseType> jax = of.createGetInfoResponse(giresp);

        marshal(jax, resp.getOutputStream());

        return;
      }

      if (ni.getAction() == ActionType.NewSubscription) {
        SubscribeResponseType sresp = ni.getSubResponse();

        JAXBElement<SubscribeResponseType> jax = of.createSubscribeResponse(sresp);

        marshal(jax, resp.getOutputStream());
      }

      if (ni.getAction() == ActionType.Unsubscribe) {
        UnsubscribeResponseType usresp = ni.getUnsubResponse();

        JAXBElement<UnsubscribeResponseType> jax = of.createUnsubscribeResponse(usresp);

        marshal(jax, resp.getOutputStream());
      }
    } catch (SynchException se) {
      throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private Notification subscribe(final HttpServletResponse resp,
                                 final SubscribeRequestType sr) throws SynchException {
    Subscription sub = new Subscription(null);

    sub.setOwner(sr.getPrincipalHref());
    sub.setDirection(sr.getDirection());
    sub.setMaster(sr.getMaster());
    sub.setEndAConnectorInfo(makeConnInfo(sr.getEndAConnector()));
    sub.setEndBConnectorInfo(makeConnInfo(sr.getEndBConnector()));

    ArrayOfSynchProperties info = sr.getInfo();
    if (info != null) {
      SubscriptionInfo sinfo = new SubscriptionInfo();

      for (SynchPropertyType sp: info.getProperty()) {
        sinfo.setProperty(sp.getName(), sp.getValue());
      }
      sub.setInfo(sinfo);
    }

    if (debug) {
      trace("Handle subscribe " +  sub);
    }

    /* Look for a subscription that matches the 2 end points */

    Subscription s = syncher.find(sub);

    SubscribeResponseType sresp = of.createSubscribeResponseType();

    if (s != null) {
      sresp.setStatus(StatusType.ERROR);
      sresp.setErrorResponse(new ErrorResponseType());
      sresp.getErrorResponse().setError(of.createAlreadySubscribed(new AlreadySubscribedType()));
    } else {
      sresp.setStatus(StatusType.OK);
      sresp.setSubscriptionId(sub.getSubscriptionId());
    }

    return new Notification(sub, sresp);
  }

  private SubscriptionConnectorInfo makeConnInfo(final ConnectorInfoType cinfo) throws SynchException {
    SubscriptionConnectorInfo subCinfo = new SubscriptionConnectorInfo();

    subCinfo.setConnectorId(cinfo.getConnectorId());

    if (cinfo.getProperties() == null) {
      return subCinfo;
    }

    for (SynchPropertyType sp: cinfo.getProperties().getProperty()) {
      subCinfo.setProperty(sp.getName(), sp.getValue());
    }

    return subCinfo;
  }

  private Notification unsubscribe(final HttpServletResponse resp,
                           final UnsubscribeRequestType u) throws SynchException {
    if (debug) {
      trace("Handle unsubscribe " +  u.getSubscriptionId());
    }

    Subscription sub = syncher.getSubscription(u.getSubscriptionId());

    UnsubscribeResponseType usr = of.createUnsubscribeResponseType();

    /* Most errors we'll treat as an unknown subscription */

    boolean ok = false;

    checkSub: {
      if (sub == null) {
        break checkSub;
      }

      // Ensure fields match
      if (!sub.getOwner().equals(u.getPrincipalHref())) {
        break checkSub;
      }

      ok = true;
    } // checkSub

    if (!ok) {
      // No subscription or error - nothing to do
      usr.setStatus(StatusType.ERROR);
      usr.setErrorResponse(new ErrorResponseType());
      usr.getErrorResponse().setError(of.createUnknownSubscription(new UnknownSubscriptionType()));

      return new Notification(sub, u, usr);
    }

    //usr.setSubscribeStatus(syncher.unsubscribe(sub));

    return new Notification(sub, u, usr);
  }
}
