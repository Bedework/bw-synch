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
package org.bedework.synch.cnctrs.exchange;

import org.bedework.synch.shared.PropertiesInfo;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.SynchDefs.SynchKind;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.SynchPropertyInfo;
import org.bedework.synch.shared.cnctrs.AbstractConnector;
import org.bedework.synch.shared.cnctrs.ConnectorInstanceMap;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import com.microsoft.schemas.exchange.services._2006.messages.ObjectFactory;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResponseType;
import com.microsoft.schemas.exchange.services._2006.types.SubscriptionStatusType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;

/** The synch processor connector for connections to Exchange.
 *
 * @author Mike Douglass
 */
public class ExchangeConnector
      extends AbstractConnector<ExchangeConnector,
                                ExchangeConnectorInstance,
                                ExchangeNotification,
                                ExchangeConnectorConfig,
                                ExchangeSubscriptionInfo> {
  /* Information required from the user for an Exchange connection
   *
   * exchange-folder-id
   * exchange-uri
   * exchange-user
   * exchange-pw
   */

  /** */
  public static final String propnameFolderId = "exchange-folder-id";

  private static final PropertiesInfo exPropInfo = new PropertiesInfo();

  static {
    exPropInfo.add(propnameFolderId,
                   false,
                   SynchPropertyInfo.typeString,
                   "",
                   true);

    exPropInfo.requiredPrincipal(null);

    exPropInfo.requiredPassword(null);
  }

  private final ConnectorInstanceMap<ExchangeConnectorInstance> cinstMap =
          new ConnectorInstanceMap<>();

  // Are these thread safe?
  private JAXBContext ewsjc;

  /**
   */
  public ExchangeConnector() {
    super(exPropInfo);
  }

  @Override
  public void start(final String connectorId,
                    final ExchangeConnectorConfig conf,
                    final String callbackUri,
                    final SynchEngine syncher) {
    super.start(connectorId, conf, callbackUri, syncher);

    info("**************************************************");
    info("Starting exchange connector " + connectorId);
    info(" Exchange WSDL URI: " + config.getExchangeWSDLURI());
    info("      callback URI: " + callbackUri);
    info("**************************************************");

    stopped = false;
    running = true;
  }

  @Override
  public SynchKind getKind() {
    return SynchKind.notify;
  }

  @Override
  public ExchangeConnectorInstance makeInstance(final Subscription sub,
                                                final SynchEndType end) {
    final ExchangeSubscriptionInfo info;

    if (end == SynchEndType.A) {
      info = new ExchangeSubscriptionInfo(sub.getEndAConnectorInfo());
    } else {
      info = new ExchangeSubscriptionInfo(sub.getEndBConnectorInfo());
    }

    return new ExchangeConnectorInstance(config,
                                         this, sub, end, info);
  }

  static class ExchangeNotificationBatch extends NotificationBatch<ExchangeNotification> {
  }

  @Override
  public ExchangeNotificationBatch handleCallback(final HttpServletRequest req,
                                     final HttpServletResponse resp,
                                     final List<String> resourceUri) {
    final ExchangeNotificationBatch enb =
            new ExchangeNotificationBatch();

    if (resourceUri.size() != 1) {
      enb.setStatus(StatusType.ERROR);
      return enb;
    }

    String id = resourceUri.get(0);
    final SynchEndType end;

    try {
      final String endFlag = id.substring(0, 1);
      end = SynchEndType.valueOf(endFlag);
    } catch (final Throwable t) {
      enb.setStatus(StatusType.ERROR);
      enb.setMessage("Id not starting with end flag");
      return enb;
    }

    id = id.substring(1);

    final Subscription sub = syncher.getSubscription(id);

    /* WRONG - we should register our callback uri along with a connector id.
     *
     */
    final ExchangeConnectorInstance cinst =
            getConnectorInstance(sub, end);
    if (cinst == null) {
      enb.setStatus(StatusType.ERROR);
      enb.setMessage("Unable to get instance for " + sub +
                     " and " + end);
      return enb;
    }

    final SendNotificationResponseType snr =
            (SendNotificationResponseType)unmarshalBody(req);

    final var responseMessages =
      snr.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

    for (final var el: responseMessages) {
      final ExchangeNotificationMessage enm = new ExchangeNotificationMessage((SendNotificationResponseMessageType)el.getValue());

      final ExchangeNotification en = new ExchangeNotification(sub, end, enm);

      for (final var ni: enm.getNotifications()) {
        final IcalendarType ical = cinst.fetchItem(ni.getItemId());

        en.addNotificationItem(new ExchangeNotification.NotificationItem(ni,
                                                                         ical));
      }

      enb.addNotification(en);
    }

    enb.setStatus(StatusType.OK);
    return enb;
  }

  @Override
  public void respondCallback(final HttpServletResponse resp,
                              final NotificationBatch<ExchangeNotification> notifications) {
    try {
      final ObjectFactory of = new ObjectFactory();
      final var snr = of.createSendNotificationResultType();

      if (notifications.getStatus() == StatusType.OK) {
        snr.setSubscriptionStatus(SubscriptionStatusType.OK);
      } else {
        snr.setSubscriptionStatus(SubscriptionStatusType.UNSUBSCRIBE);
      }

//      marshalBody(resp,
  //                snr);
 //   } catch (final SynchException se) {
   //   throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  /* ==============================================================
   *                        package methods
   * =============================================================== */

  JAXBContext getEwsJAXBContext() {
    try {
      if (ewsjc == null) {
        ewsjc = JAXBContext.newInstance(
                     "com.microsoft.schemas.exchange.services._2006.messages:" +
                     "com.microsoft.schemas.exchange.services._2006.types");
      }

      return ewsjc;
    } catch(final Throwable t) {
      throw new SynchException(t);
    }
  }
}
