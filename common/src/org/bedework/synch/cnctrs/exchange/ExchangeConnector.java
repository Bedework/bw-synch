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

import org.bedework.synch.SynchDefs.SynchKind;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.SynchPropertyInfo;
import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.cnctrs.ConnectorInstanceMap;
import org.bedework.synch.db.ConnectorConfig;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;

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
      implements Connector<ExchangeConnectorInstance,
                           ExchangeNotification> {
  protected transient Logger log;

  private static ietf.params.xml.ns.icalendar_2.ObjectFactory icalOf =
      new ietf.params.xml.ns.icalendar_2.ObjectFactory();

  /* Information required from the user for an Exchange connection
   *
   * exchange-folder-id
   * exchange-uri
   * exchange-user
   * exchange-pw
   */

  /** */
  public static final String propnameFolderId = "exchange-folder-id";

  /** */
  public static final String propnameAccount = "account";

  /** */
  public static final String propnamePw = "password";

  private static List<SynchPropertyInfo> propInfo =
      new ArrayList<SynchPropertyInfo>();

  static {
    propInfo.add(new SynchPropertyInfo(propnameFolderId,
                                       false,
                                       SynchPropertyInfo.typeString,
                                       "",
                                       true));

    propInfo.add(new SynchPropertyInfo(propnameAccount,
                                       false,
                                       SynchPropertyInfo.typeString,
                                       "",
                                       true));

    propInfo.add(new SynchPropertyInfo(propnamePw,
                                       true,
                                       SynchPropertyInfo.typePassword,
                                       "",
                                       true));
  }

  private SynchEngine syncher;

  private ExchangeConnectorConfig config;

  private String callbackUri;

  private String connectorId;

  private boolean running;

  private ConnectorInstanceMap<ExchangeConnectorInstance> cinstMap =
      new ConnectorInstanceMap<ExchangeConnectorInstance>();

  // Are these thread safe?
  private MessageFactory soapMsgFactory;
  private JAXBContext ewsjc;

  @Override
  public void start(final String connectorId,
                    final ConnectorConfig conf,
                    final String callbackUri,
                    final SynchEngine syncher) throws SynchException {
    try {
      this.connectorId = connectorId;
      this.syncher = syncher;
      this.callbackUri = callbackUri;

      config = new ExchangeConnectorConfig(conf);

      info("**************************************************");
      info("Starting exchange connector " + connectorId);
      info(" Exchange WSDL URI: " + config.getExchangeWSDLURI());
      info("      callback URI: " + callbackUri);
      info("**************************************************");
      running = true;
    } catch (Throwable t) {
      error(t);
      throw new SynchException(t);
    }
  }

  @Override
  public boolean isManager() {
    return false;
  }

  @Override
  public boolean isStarted() {
    return running;
  }

  @Override
  public boolean isFailed() {
    return false;
  }

  @Override
  public boolean isStopped() {
    return !running;
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
  public String getId() {
    return connectorId;
  }

  @Override
  public String getCallbackUri() {
    return callbackUri;
  }

  @Override
  public SynchEngine getSyncher() {
    return syncher;
  }

  @Override
  public ietf.params.xml.ns.icalendar_2.ObjectFactory getIcalObjectFactory() {
    return icalOf;
  }

  @Override
  public List<SynchPropertyInfo> getPropertyInfo() {
    return propInfo;
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

    inst = new ExchangeConnectorInstance(config, this, sub, end, info);
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

  Object unmarshalBody(final HttpServletRequest req) throws SynchException {
    try {
      SOAPMessage msg = getSoapMsgFactory().createMessage(null, // headers
                                                          req.getInputStream());

      SOAPBody body = msg.getSOAPBody();

      Unmarshaller u = getEwsJAXBContext().createUnmarshaller();

      Object o = u.unmarshal(body.getFirstChild());

      if (o instanceof JAXBElement) {
        // Some of them get wrapped.
        o = ((JAXBElement)o).getValue();
      }

      return o;
    } catch (SynchException se) {
      throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  MessageFactory getSoapMsgFactory() throws SynchException {
    try {
      if (soapMsgFactory == null) {
        soapMsgFactory = MessageFactory.newInstance();
      }

      return soapMsgFactory;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

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

  private Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  @SuppressWarnings("unused")
  private void trace(final String msg) {
    getLogger().debug(msg);
  }

  @SuppressWarnings("unused")
  private void warn(final String msg) {
    getLogger().warn(msg);
  }

  private void error(final Throwable t) {
    getLogger().error(this, t);
  }

  private void info(final String msg) {
    getLogger().info(msg);
  }
}
