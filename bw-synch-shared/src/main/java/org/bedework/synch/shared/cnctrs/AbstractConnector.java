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
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;

import org.w3c.dom.Document;

import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPMessage;

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
                                        TI extends AbstractConnectorInstance<?, ?, ?>,
                                        TN extends Notification<?>,
                                        Tconf extends ConnectorConfigI,
                                        InfoT extends BaseSubscriptionInfo>
        implements Logged, Connector<TI, TN, Tconf> {
  protected Tconf config;

  protected String callbackUri;

  private String connectorId;

  private final Map<String, SynchRemoteService> services =
          new HashMap<>();

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
    this.propInfo =
            Objects.requireNonNullElseGet(propInfo,
                                          PropertiesInfo::new);
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
  public void stop() {
    running = false;
  }

  public abstract TI makeInstance(Subscription sub,
                                  SynchEndType end);

  @Override
  public TI getConnectorInstance(final Subscription sub,
                                 final SynchEndType end) {
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

  static class BedeworkNotificationBatch
          extends NotificationBatch<Notification<?>> {
  }

  @Override
  public NotificationBatch<TN> handleCallback(final HttpServletRequest req,
                                                  final HttpServletResponse resp,
                                                  final List<String> resourceUri) {
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

  protected SynchRemoteServicePortType getPort() {
    throw new SynchException("Not implemented");
  }

  protected SynchRemoteServicePortType getPort(final String uri) {
    return getRemoteService(uri).getSynchRSPort();
  }

  private SynchRemoteService getRemoteService(final String uri) {
    SynchRemoteService ers = services.get(uri);
    if (ers != null) {
      return ers;
    }

    try {
      ers = new SynchRemoteService(
              new URL(uri),
              new QName(SynchDefs.synchNamespace,
                        "SynchRemoteService"));
      services.put(uri, ers);
      return ers;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  protected Object unmarshalBody(final HttpServletRequest req) {
    try {
      final SOAPMessage msg =
              getSoapMsgFactory().createMessage(null,// headers
                                                req.getInputStream());

      final SOAPBody body = msg.getSOAPBody();

      final Unmarshaller u = getSynchJAXBContext().createUnmarshaller();

      Object o = u.unmarshal(body.getFirstChild());

      if (o instanceof JAXBElement) {
        // Some of them get wrapped.
        o = ((JAXBElement<?>)o).getValue();
      }

      return o;
    } catch (final SynchException se) {
      throw se;
    } catch(final Throwable t) {
      throw new SynchException(t);
    }
  }

  protected void marshal(final Object o,
                         final OutputStream out) {
    try {
      final Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final Document doc = dbf.newDocumentBuilder().newDocument();

      final SOAPMessage msg = soapMsgFactory.createMessage();
      msg.getSOAPBody().addDocument(doc);

      marshaller.marshal(o,
                         msg.getSOAPBody());

      msg.writeTo(out);
    } catch(final Throwable t) {
      throw new SynchException(t);
    }
  }

  protected MessageFactory getSoapMsgFactory() {
    try {
      if (soapMsgFactory == null) {
        soapMsgFactory = MessageFactory.newInstance();
      }

      return soapMsgFactory;
    } catch(final Throwable t) {
      throw new SynchException(t);
    }
  }

  /* ==============================================================
   *                         Package methods
   * ============================================================== */

  JAXBContext getSynchJAXBContext() {
    try {
      if (jc == null) {
        jc = JAXBContext.newInstance("org.bedework.synch.wsmessages:" +
                                     "ietf.params.xml.ns.icalendar_2");
      }

      return jc;
    } catch(final Throwable t) {
      throw new SynchException(t);
    }
  }

  /* ==============================================================
   *                   Logged methods
   * ============================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
