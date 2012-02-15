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

import org.bedework.synch.PropertiesInfo;
import org.bedework.synch.SynchDefs.SynchKind;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.SynchPropertyInfo;
import org.bedework.synch.cnctrs.AbstractConnector;
import org.bedework.synch.cnctrs.ConnectorInstanceMap;
import org.bedework.synch.db.ConnectorConfig;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;

import com.microsoft.schemas.exchange.services._2006.messages.ObjectFactory;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.SendNotificationResultType;
import com.microsoft.schemas.exchange.services._2006.types.SubscriptionStatusType;

/** The synch processor connector for connections to Exchange.
 *
 * @author Mike Douglass
 */
public class ExchangeConnector
      extends AbstractConnector<ExchangeConnector,
                                ExchangeConnectorInstance,
                                ExchangeNotification> {
  /* Information required from the user for an Exchange connection
   *
   * exchange-folder-id
   * exchange-uri
   * exchange-user
   * exchange-pw
   */

  /** */
  public static final String propnameFolderId = "exchange-folder-id";

  private static PropertiesInfo exPropInfo = new PropertiesInfo();

  static {
    exPropInfo.add(propnameFolderId,
                   false,
                   SynchPropertyInfo.typeString,
                   "",
                   true);

    exPropInfo.requiredPrincipal(null);

    exPropInfo.requiredPassword(null);
  }

  private ConnectorInstanceMap<ExchangeConnectorInstance> cinstMap =
      new ConnectorInstanceMap<ExchangeConnectorInstance>();

  // Are these thread safe?
  private JAXBContext ewsjc;

  /**
   */
  public ExchangeConnector() {
    super(exPropInfo);
  }

  @Override
  public void start(final String connectorId,
                    final ConnectorConfig conf,
                    final String callbackUri,
                    final SynchEngine syncher) throws SynchException {
    super.start(connectorId, conf, callbackUri, syncher);

    config = new ExchangeConnectorConfig(conf);

    info("**************************************************");
    info("Starting exchange connector " + connectorId);
    info(" Exchange WSDL URI: " + ((ExchangeConnectorConfig)config).getExchangeWSDLURI());
    info("      callback URI: " + callbackUri);
    info("**************************************************");

    stopped = false;
    running = true;
  }

  @Override
  public boolean isManager() {
    return false;
  }

  @Override
  public SynchKind getKind() {
    return SynchKind.notify;
  }

  @Override
  public boolean isReadOnly() {
    return config.getReadOnly();
  }

  @Override
  public boolean getTrustLastmod() {
    return config.getTrustLastmod();
  }

  @Override
  public List<Object> getSkipList() {
    return null;
  }

  @Override
  public ExchangeConnectorInstance getConnectorInstance(final Subscription sub,
                                                        final SynchEndType end) throws SynchException {
    ExchangeConnectorInstance inst = cinstMap.find(sub, end);

    if (inst != null) {
      return inst;
    }

    //debug = getLogger().isDebugEnabled();
    ExchangeSubscriptionInfo info;

    if (end == SynchEndType.A) {
      info = new ExchangeSubscriptionInfo(sub.getEndAConnectorInfo());
    } else {
      info = new ExchangeSubscriptionInfo(sub.getEndBConnectorInfo());
    }

    inst = new ExchangeConnectorInstance((ExchangeConnectorConfig)config,
                                         this, sub, end, info);
    cinstMap.add(sub, end, inst);

    return inst;
  }

  class ExchangeNotificationBatch extends NotificationBatch<ExchangeNotification> {
  }

  @Override
  public ExchangeNotificationBatch handleCallback(final HttpServletRequest req,
                                     final HttpServletResponse resp,
                                     final List<String> resourceUri) throws SynchException {
    ExchangeNotificationBatch enb = new ExchangeNotificationBatch();

    if (resourceUri.size() != 1) {
      enb.setStatus(StatusType.ERROR);
      return enb;
    }

    String id = resourceUri.get(0);
    SynchEndType end;

    try {
      String endFlag = id.substring(0, 1);
      end = SynchEndType.valueOf(endFlag);
    } catch (Throwable t) {
      enb.setStatus(StatusType.ERROR);
      enb.setMessage("Id not starting with end flag");
      return enb;
    }

    id = id.substring(1);

    Subscription sub = syncher.getSubscription(id);

    /* WRONG - we should register our callback uri along with a connector id.
     *
     */
    ExchangeConnectorInstance cinst = getConnectorInstance(sub, end);
    if (cinst == null) {
      enb.setStatus(StatusType.ERROR);
      enb.setMessage("Unable to get instance for " + sub +
                     " and " + end);
      return enb;
    }

    SendNotificationResponseType snr = (SendNotificationResponseType)unmarshalBody(req);

    List<JAXBElement<? extends ResponseMessageType>> responseMessages =
      snr.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

    for (JAXBElement<? extends ResponseMessageType> el: responseMessages) {
      ExchangeNotificationMessage enm = new ExchangeNotificationMessage((SendNotificationResponseMessageType)el.getValue());

      ExchangeNotification en = new ExchangeNotification(sub, end, enm);

      for (ExchangeNotificationMessage.NotificationItem ni: enm.getNotifications()) {
        IcalendarType ical = cinst.fetchItem(ni.getItemId());

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
                              final NotificationBatch<ExchangeNotification> notifications)
                                                    throws SynchException {
    try {
      ObjectFactory of = new ObjectFactory();
      SendNotificationResultType snr = of.createSendNotificationResultType();

      if (notifications.getStatus() == StatusType.OK) {
        snr.setSubscriptionStatus(SubscriptionStatusType.OK);
      } else {
        snr.setSubscriptionStatus(SubscriptionStatusType.UNSUBSCRIBE);
      }

//      marshalBody(resp,
  //                snr);
 //   } catch (SynchException se) {
   //   throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  @Override
  public void stop() throws SynchException {
    running = false;
  }

  /* ====================================================================
   *                        package methods
   * ==================================================================== */

  JAXBContext getEwsJAXBContext() throws SynchException {
    try {
      if (ewsjc == null) {
        ewsjc = JAXBContext.newInstance(
                     "com.microsoft.schemas.exchange.services._2006.messages:" +
                     "com.microsoft.schemas.exchange.services._2006.types");
      }

      return ewsjc;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  /* ====================================================================
   *                        private methods
   * ==================================================================== */
}
