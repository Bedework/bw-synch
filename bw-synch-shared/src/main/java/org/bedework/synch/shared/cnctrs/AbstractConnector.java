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
package org.bedework.synch.shared.cnctrs;

import org.bedework.synch.shared.BaseSubscriptionInfo;
import org.bedework.synch.shared.Notification;
import org.bedework.synch.shared.PropertiesInfo;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.SynchDefs;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.conf.ConnectorConfigI;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.ObjectFactory;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.SynchRemoteService;
import org.bedework.synch.wsmessages.SynchRemoteServicePortType;
import org.bedework.util.misc.Logged;

import org.w3c.dom.Document;

import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;

/** A special connector to handle calls to the synch engine via the web context.
 *
 * <p>This is the way to call the system to add subscriptions, to unsubscribe etc.
 *
 * @author Mike Douglass
 *
 * @param <T> Connector subclass
 * @param <TI> Connector instance subclass
 * @param <TN> Notification subclass
 * @param <Tconf> Configuration class
 */
public abstract class AbstractConnector<T,
                                        TI extends AbstractConnectorInstance,
                                        TN extends Notification,
                                        Tconf extends ConnectorConfigI,
                                        InfoT extends BaseSubscriptionInfo>
        extends Logged implements Connector<TI, TN, Tconf> {
  protected Tconf config;

  protected String callbackUri;

  private String connectorId;

  private static final ietf.params.xml.ns.icalendar_2.ObjectFactory icalOf =
      new ietf.params.xml.ns.icalendar_2.ObjectFactory();

  protected SynchEngine syncher;

  protected boolean running;

  protected boolean stopped;

  protected boolean failed;

  // Are these thread safe?
  protected ObjectFactory of = new ObjectFactory();
  protected MessageFactory soapMsgFactory;
  protected JAXBContext jc;

  protected PropertiesInfo propInfo;

  protected AbstractConnector(final PropertiesInfo propInfo) {
    if (propInfo == null) {
      this.propInfo = new PropertiesInfo();
    } else {
      this.propInfo = propInfo;
    }
  }

  private final ConnectorInstanceMap<TI> cinstMap =
          new ConnectorInstanceMap<>();

  /**
   * @return the connector id
   */
  public String getConnectorId() {
    return connectorId;
  }

  @Override
  public void start(final String connectorId,
                    final Tconf conf,
                    final String callbackUri,
                    final SynchEngine syncher) {
    this.connectorId = connectorId;
    this.syncher = syncher;
    this.callbackUri = callbackUri;
    this.config = conf;

    stopped = false;
    running = true;
  }

  @Override
  public String getStatus() {
    final StringBuilder sb = new StringBuilder();

    if (isManager()) {
      sb.append("(Manager): ");
    }

    if (isStarted()) {
      sb.append("Started: ");
    }

    if (isFailed()) {
      sb.append("Failed: ");
    }

    if (isStopped()) {
      sb.append("Stopped: ");
    }

    return sb.toString();
  }

  @Override
  public boolean isStarted() {
    return running;
  }

  @Override
  public boolean isFailed() {
    return failed;
  }

  @Override
  public boolean isStopped() {
    return stopped;
  }

  @Override
  public boolean isManager() {
    return false;
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
  public PropertiesInfo getPropertyInfo() {
    return propInfo;
  }

  @Override
  public List<Object> getSkipList() {
    return null;
  }

  @Override
  public void stop() throws SynchException {
    running = false;
  }

  public abstract TI makeInstance(final Subscription sub,
                                  final SynchEndType end) throws SynchException;

  @Override
  public TI getConnectorInstance(final Subscription sub,
                                 final SynchEndType end) throws SynchException {
    if (!running) {
      return null;
    }

    TI inst = cinstMap.find(sub, end);

    if (inst != null) {
      return inst;
    }

    inst = makeInstance(sub, end);

    cinstMap.add(sub, end, inst);

    return inst;
  }

  class BedeworkNotificationBatch extends NotificationBatch<Notification> {
  }

  @Override
  public NotificationBatch<TN> handleCallback(final HttpServletRequest req,
                                                  final HttpServletResponse resp,
                                                  final List<String> resourceUri) throws SynchException {
    return null;
  }

  @Override
  public void respondCallback(final HttpServletResponse resp,
                              final NotificationBatch<TN> notifications)
          throws SynchException {
  }

  /* ====================================================================
   *                   Protected methods
   * ==================================================================== */

  protected SynchRemoteServicePortType getPort(final String uri) throws SynchException {
    try {
      URL wsURL = new URL(uri);

      SynchRemoteService ers =
        new SynchRemoteService(wsURL,
                               new QName(SynchDefs.synchNamespace,
                                         "SynchRemoteService"));
      SynchRemoteServicePortType port = ers.getSynchRSPort();

      return port;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  protected Object unmarshalBody(final HttpServletRequest req) throws SynchException {
    try {
      SOAPMessage msg = getSoapMsgFactory().createMessage(null, // headers
                                                          req.getInputStream());

      SOAPBody body = msg.getSOAPBody();

      Unmarshaller u = getSynchJAXBContext().createUnmarshaller();

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

  protected void marshal(final Object o,
                         final OutputStream out) throws SynchException {
    try {
      Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      Document doc = dbf.newDocumentBuilder().newDocument();

      SOAPMessage msg = soapMsgFactory.createMessage();
      msg.getSOAPBody().addDocument(doc);

      marshaller.marshal(o,
                         msg.getSOAPBody());

      msg.writeTo(out);
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  protected MessageFactory getSoapMsgFactory() throws SynchException {
    try {
      if (soapMsgFactory == null) {
        soapMsgFactory = MessageFactory.newInstance();
      }

      return soapMsgFactory;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  /* ====================================================================
   *                         Package methods
   * ==================================================================== */

  JAXBContext getSynchJAXBContext() throws SynchException {
    try {
      if (jc == null) {
        jc = JAXBContext.newInstance("org.bedework.synch.wsmessages:" +
                                     "ietf.params.xml.ns.icalendar_2");
      }

      return jc;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }
}
