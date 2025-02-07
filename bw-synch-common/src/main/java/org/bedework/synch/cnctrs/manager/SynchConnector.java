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

import org.bedework.synch.SubscriptionConnectorInfoImpl;
import org.bedework.synch.SubscriptionInfoImpl;
import org.bedework.synch.db.SubscriptionImpl;
import org.bedework.synch.shared.BaseSubscriptionInfo;
import org.bedework.synch.shared.Notification;
import org.bedework.synch.shared.Notification.NotificationItem;
import org.bedework.synch.shared.Notification.NotificationItem.ActionType;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.SubscriptionConnectorInfo;
import org.bedework.synch.shared.SynchDefs.SynchKind;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.cnctrs.AbstractConnector;
import org.bedework.synch.shared.conf.ConnectorConfig;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.ActiveSubscriptionRequestType;
import org.bedework.synch.wsmessages.AlreadySubscribedType;
import org.bedework.synch.wsmessages.ArrayOfSynchConnectorInfo;
import org.bedework.synch.wsmessages.ArrayOfSynchProperties;
import org.bedework.synch.wsmessages.ArrayOfSynchPropertyInfo;
import org.bedework.synch.wsmessages.ConnectorInfoType;
import org.bedework.synch.wsmessages.GetInfoRequestType;
import org.bedework.synch.wsmessages.GetInfoResponseType;
import org.bedework.synch.wsmessages.RefreshRequestType;
import org.bedework.synch.wsmessages.RefreshResponseType;
import org.bedework.synch.wsmessages.SubscribeRequestType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SubscriptionStatusRequestType;
import org.bedework.synch.wsmessages.SubscriptionStatusResponseType;
import org.bedework.synch.wsmessages.SynchConnectorInfoType;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.SynchInfoType;
import org.bedework.synch.wsmessages.SynchPropertyType;
import org.bedework.synch.wsmessages.UnknownSubscriptionType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;
import org.bedework.synch.wsmessages.UnsubscribeResponseType;

import org.oasis_open.docs.ws_calendar.ns.soap.ErrorResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBElement;

/** A special connector to handle calls to the synch engine via the web context.
 *
 * <p>This is the way to call the system to add subscriptions, to unsubscribe etc.
 *
 * @author Mike Douglass
 */
public class SynchConnector
      extends AbstractConnector<SynchConnector,
                                SynchConnectorInstance,
                                Notification<?>,
                                ConnectorConfig,
                                 BaseSubscriptionInfo> {
  /**
   */
  public SynchConnector() {
    super(null);
  }

  @Override
  public void start(final String connectorId,
                    final ConnectorConfig conf,
                    final String callbackUri,
                    final SynchEngine syncher) {
    super.start(connectorId, conf, callbackUri, syncher);

    stopped = false;
    running = true;
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
  public SynchConnectorInstance makeInstance(final Subscription sub,
                                             final SynchEndType end) {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public NotificationBatch handleCallback(final HttpServletRequest req,
                                          final HttpServletResponse resp,
                                          final List<String> resourceUri) {
    try {
      // Resource uri unused for the moment - must be null or zero length (or "/")

      if (!resourceUri.isEmpty()) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return null;
      }

      final Object o = unmarshalBody(req);

      if (o instanceof GetInfoRequestType) {
        return new NotificationBatch<>(
                new Notification<>(NotificationItem.ActionType.GetInfo));
      }

      if (o instanceof final SubscribeRequestType sr) {
        return new NotificationBatch<>(subscribe(resp, sr));
      }

      if (o instanceof final UnsubscribeRequestType ur) {
        return new NotificationBatch<>(unsubscribe(resp, ur));
      }

      if (o instanceof final RefreshRequestType rr) {
        return new NotificationBatch<>(refresh(resp, rr));
      }

      if (o instanceof final SubscriptionStatusRequestType ssr) {
        return new NotificationBatch<>(subStatus(resp, ssr));
      }

      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return null;
    } catch (final SynchException se) {
      throw se;
    } catch(final Throwable t) {
      throw new SynchException(t);
    }
  }

  @Override
  public void respondCallback(final HttpServletResponse resp,
                              final NotificationBatch<Notification<?>> notifications)
                                                    throws SynchException {
    try {
      /* We only expect single notification items in a batch */

      if (notifications.getNotifications().size() != 1) {
        // XXX Error?
        return;
      }

      final Notification<?> note = notifications.getNotifications().get(0);

      // Again one item per notification.

      if (note.getNotifications().size() != 1) {
        // XXX Error?
        return;
      }

      final NotificationItem ni = note.getNotifications().get(0);

      if (ni.getAction() == ActionType.GetInfo) {
        final GetInfoResponseType giresp = new GetInfoResponseType();
        final SynchInfoType sit = new SynchInfoType();

        giresp.setInfo(sit);
        final ArrayOfSynchConnectorInfo asci = new ArrayOfSynchConnectorInfo();
        sit.setConnectors(asci);

        for (final String id: syncher.getConnectorIds()) {
          final var c = syncher.getConnector(id);

          if (c == null) {
            continue;
          }

          final var scit = new SynchConnectorInfoType();

          scit.setName(id);
          scit.setManager(c.isManager());
          scit.setReadOnly(c.isReadOnly());

          final ArrayOfSynchPropertyInfo aspi = new ArrayOfSynchPropertyInfo();
          scit.setProperties(aspi);

          c.getPropertyInfo().addAllToList(aspi.getProperty());

          asci.getConnector().add(scit);
        }

        final JAXBElement<GetInfoResponseType> jax = of.createGetInfoResponse(giresp);

        marshal(jax, resp.getOutputStream());

        return;
      }

      if (ni.getAction() == ActionType.NewSubscription) {
        final SubscribeResponseType sresp = ni.getSubResponse();

        final var jax = of.createSubscribeResponse(sresp);

        marshal(jax, resp.getOutputStream());
      }

      if (ni.getAction() == ActionType.Unsubscribe) {
        final UnsubscribeResponseType usresp = ni.getUnsubResponse();

        final var jax = of.createUnsubscribeResponse(usresp);

        marshal(jax, resp.getOutputStream());
      }

      if (ni.getAction() == ActionType.Refresh) {
        final RefreshResponseType refresp = ni.getRefreshResponse();

        final var jax = of.createRefreshResponse(refresp);

        marshal(jax, resp.getOutputStream());
      }

      if (ni.getAction() == ActionType.SubscriptionStatus) {
        final SubscriptionStatusResponseType ssresp = ni.getSubStatusResponse();

        final JAXBElement<SubscriptionStatusResponseType> jax = of.createSubscriptionStatusResponse(ssresp);

        marshal(jax, resp.getOutputStream());
      }
    } catch (final SynchException se) {
      throw se;
    } catch(final Throwable t) {
      throw new SynchException(t);
    }
  }

  /* ==============================================================
   *                   Private methods
   * ============================================================== */

  private Notification<?> subscribe(final HttpServletResponse resp,
                                 final SubscribeRequestType sr) {
    final Subscription sub = new SubscriptionImpl(null);

    sub.setOwner(sr.getPrincipalHref());
    sub.setDirectionEnum(sr.getDirection());
    sub.setMasterEnum(sr.getMaster());
    sub.setEndAConnectorInfo(makeConnInfo(sr.getEndAConnector()));
    sub.setEndBConnectorInfo(makeConnInfo(sr.getEndBConnector()));

    final ArrayOfSynchProperties info = sr.getInfo();
    if (info != null) {
      final var sinfo = new SubscriptionInfoImpl();

      for (final SynchPropertyType sp: info.getProperty()) {
        sinfo.setProperty(sp.getName(), sp.getValue());
      }
      sub.setInfo(sinfo);
    }

    if (debug()) {
      debug("Handle subscribe " +  sub);
    }

    /* Look for a subscription that matches the 2 end points */

    final Subscription s = syncher.find(sub);

    final SubscribeResponseType sresp = of.createSubscribeResponseType();

    if (s != null) {
      sresp.setStatus(StatusType.ERROR);
      sresp.setErrorResponse(new ErrorResponseType());
      sresp.getErrorResponse().setError(of.createAlreadySubscribed(new AlreadySubscribedType()));
    } else {
      sresp.setStatus(StatusType.OK);
      sresp.setSubscriptionId(sub.getSubscriptionId());
    }

    return new Notification<>(sub, sresp);
  }

  private Notification<?> unsubscribe(
          final HttpServletResponse resp,
          final UnsubscribeRequestType u) {
    if (debug()) {
      debug("Handle unsubscribe " +  u.getSubscriptionId());
    }

    final UnsubscribeResponseType usr = of.createUnsubscribeResponseType();

    final Subscription sub = checkAsr(u);

    if (sub == null) {
      if (debug()) {
        warn("No subscription found for " +  u.getSubscriptionId());
      }
      // No subscription or error - nothing to do
      usr.setStatus(StatusType.ERROR);
      usr.setErrorResponse(new ErrorResponseType());
      usr.getErrorResponse().setError(of.createUnknownSubscription(new UnknownSubscriptionType()));

      return new Notification<>(null, u, usr);
    }

    return new Notification<>(sub, u, usr);
  }

  private Notification<?> refresh(
          final HttpServletResponse resp,
          final RefreshRequestType r) {
    if (debug()) {
      debug("Handle refresh " +  r.getSubscriptionId());
    }

    final RefreshResponseType rr = of.createRefreshResponseType();

    final Subscription sub = checkAsr(r);

    if (sub == null) {
      if (debug()) {
        warn("No subscription found for " +  r.getSubscriptionId());
      }
      // No subscription or error - nothing to do
      rr.setStatus(StatusType.ERROR);
      rr.setErrorResponse(new ErrorResponseType());
      rr.getErrorResponse().setError(of.createUnknownSubscription(new UnknownSubscriptionType()));

      return new Notification<>(null, r, rr);
    }

    return new Notification<>(sub, r, rr);
  }

  private Notification<?> subStatus(final HttpServletResponse resp,
                           final SubscriptionStatusRequestType ss) {
    if (debug()) {
      debug("Handle status " +  ss.getSubscriptionId());
    }

    final SubscriptionStatusResponseType ssr = of.createSubscriptionStatusResponseType();

    final Subscription sub = checkAsr(ss);

    if (sub == null) {
      // No subscription or error - nothing to do
      ssr.setStatus(StatusType.NOT_FOUND);
      ssr.setErrorResponse(new ErrorResponseType());
      ssr.getErrorResponse().setError(of.createUnknownSubscription(new UnknownSubscriptionType()));

      return new Notification<>(null, ss, ssr);
    }

    return new Notification<>(sub, ss, ssr);
  }

  private Subscription checkAsr(final ActiveSubscriptionRequestType asr) {
    final Subscription sub = syncher.getSubscription(asr.getSubscriptionId());

    /* Most errors we'll treat as an unknown subscription */

    if (sub == null) {
      return null;
    }

    // Ensure fields match
    if (!sub.getOwner().equals(asr.getPrincipalHref())) {
      return null;
    }

    // XXX Should check the end info.

    return sub;
  }

  private SubscriptionConnectorInfo<?> makeConnInfo(final ConnectorInfoType cinfo) {
    final SubscriptionConnectorInfo<?> subCinfo =
            new SubscriptionConnectorInfoImpl();

    subCinfo.setConnectorId(cinfo.getConnectorId());

    if (cinfo.getProperties() == null) {
      return subCinfo;
    }

    for (final SynchPropertyType sp: cinfo.getProperties().getProperty()) {
      subCinfo.setProperty(sp.getName(), sp.getValue());
    }

    return subCinfo;
  }
}
