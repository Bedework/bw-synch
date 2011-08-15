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

import org.bedework.synch.Connector;
import org.bedework.synch.ConnectorInstanceMap;
import org.bedework.synch.ConnectorPropertyInfo;
import org.bedework.synch.Subscription;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.SynchException;

import org.apache.log4j.Logger;

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

/** Calls from exchange synch processor to the service.
 *
 * @author Mike Douglass
 */
public class ExchangeConnector
      implements Connector<ExchangeConnectorInstance,
                           ExchangeNotification> {
  protected transient Logger log;

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

  private static List<ConnectorPropertyInfo> propInfo =
      new ArrayList<ConnectorPropertyInfo>();

  static {
    propInfo.add(new ConnectorPropertyInfo(propnameFolderId,
                                           false,
                                           ""));

    propInfo.add(new ConnectorPropertyInfo(propnameAccount,
                                           false,
                                           ""));

    propInfo.add(new ConnectorPropertyInfo(propnamePw,
                                           true,
                                           ""));
  }

  private SynchEngine syncher;

  private ExchangeConnectorConfig config;

  private String callbackUri;

  private String connectorId;

  private ConnectorInstanceMap<ExchangeConnectorInstance> cinstMap =
      new ConnectorInstanceMap<ExchangeConnectorInstance>();

  // Are these thread safe?
  private MessageFactory soapMsgFactory;
  private JAXBContext ewsjc;

  @Override
  public void start(final String connectorId,
                    final String callbackUri,
                    final SynchEngine syncher) throws SynchException {
    try {
      this.connectorId = connectorId;
      this.syncher = syncher;
      this.callbackUri = callbackUri;

      config = (ExchangeConnectorConfig)syncher.getAppContext().getBean(connectorId + "ExchangeConfig");

      info("**************************************************");
      info("Starting exchange connector " + connectorId);
      info(" Exchange WSDL URI: " + config.getExchangeWSDLURI());
      info("      callback URI: " + callbackUri);
      info("**************************************************");
    } catch (Throwable t) {
      error(t);
      throw new SynchException(t);
    }
  }

  @Override
  public String getId() {
    return connectorId;
  }

  @Override
  public List<ConnectorPropertyInfo> getPropertyInfo() {
    return propInfo;
  }

  @Override
  public ExchangeConnectorInstance getConnectorInstance(final Subscription sub,
                                                        final boolean local) throws SynchException {
    ExchangeConnectorInstance inst = cinstMap.find(sub, local);

    if (inst != null) {
      return inst;
    }

    //debug = getLogger().isDebugEnabled();
    ExchangeSubscriptionInfo info;

    if (local) {
      info = new ExchangeSubscriptionInfo(sub.getLocalConnectorInfo());
    } else {
      info = new ExchangeSubscriptionInfo(sub.getRemoteConnectorInfo());
    }

    inst = new ExchangeConnectorInstance(config, this, sub, local, info);
    cinstMap.add(sub, local, inst);

    return inst;
  }

  class ExchangeNotificationBatch extends NotificationBatch<ExchangeNotification> {
  }

  @Override
  public ExchangeNotificationBatch handleCallback(final HttpServletRequest req,
                                     final HttpServletResponse resp,
                                     final String[] resourceUri) throws SynchException {
    if (resourceUri.length != 1) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return null;
    }

    String id = resourceUri[0];

    boolean local;

    if (id.startsWith("L")) {
      local = true;
    } else if (id.startsWith("R")) {
      local = false;
    } else {
      throw new SynchException("Id not starting with L or R");
    }

    id = id.substring(1);

    Subscription sub = syncher.getSubscription(id);

    /* WRONG - we should register our callback uri along with a connector id.
     *
     */

    SendNotificationResponseType snr = (SendNotificationResponseType)unmarshalBody(req);

    ExchangeNotificationBatch enb = new ExchangeNotificationBatch();

    List<JAXBElement<? extends ResponseMessageType>> responseMessages =
      snr.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

    for (JAXBElement<? extends ResponseMessageType> el: responseMessages) {
      ExchangeNotificationMessage note = new ExchangeNotificationMessage((SendNotificationResponseMessageType)el.getValue());

      ExchangeNotification en = new ExchangeNotification(sub, local, note);

      // XXX fetch the event and put into notification.

      enb.addNotification(en);
      syncher.handleNotification(sub, note);
    }

    return enb;
  }

  @Override
  public void respondCallback(final HttpServletResponse resp,
                              final NotificationBatch<ExchangeNotification> notifications)
                                                    throws SynchException {
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

  @Override
  public void stop() throws SynchException {

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

  private void trace(final String msg) {
    getLogger().debug(msg);
  }

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
