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
import org.bedework.synch.Subscription;
import org.bedework.synch.SubscriptionConnectorInfo;
import org.bedework.synch.SubscriptionInfo;
import org.bedework.synch.SynchDefs;
import org.bedework.synch.SynchDefs.SynchEnd;
import org.bedework.synch.SynchDefs.SynchKind;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.SynchPropertyInfo;
import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.cnctrs.bedework.BedeworkConnectorConfig;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.AlreadySubscribedType;
import org.bedework.synch.wsmessages.ArrayOfSynchConnectorInfo;
import org.bedework.synch.wsmessages.ArrayOfSynchProperties;
import org.bedework.synch.wsmessages.ArrayOfSynchPropertyInfo;
import org.bedework.synch.wsmessages.ConnectorInfoType;
import org.bedework.synch.wsmessages.GetInfoRequestType;
import org.bedework.synch.wsmessages.GetInfoResponseType;
import org.bedework.synch.wsmessages.ObjectFactory;
import org.bedework.synch.wsmessages.SubscribeRequestType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SynchConnectorInfoType;
import org.bedework.synch.wsmessages.SynchInfoType;
import org.bedework.synch.wsmessages.SynchPropertyType;
import org.bedework.synch.wsmessages.SynchRemoteService;
import org.bedework.synch.wsmessages.SynchRemoteServicePortType;
import org.bedework.synch.wsmessages.UnknownSubscriptionType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;
import org.bedework.synch.wsmessages.UnsubscribeResponseType;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.ErrorResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;
import org.w3c.dom.Document;

import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
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
 */
public class SynchConnector implements Connector<SynchConnectorInstance,
                                                 Notification> {
  private BedeworkConnectorConfig config;

  private String callbackUri;

  private String connectorId;

  private SynchEngine syncher;

  private transient Logger log;

  private static ietf.params.xml.ns.icalendar_2.ObjectFactory icalOf =
      new ietf.params.xml.ns.icalendar_2.ObjectFactory();

  private boolean debug;

  private boolean running;

  // Are these thread safe?
  private ObjectFactory of = new ObjectFactory();
  private MessageFactory soapMsgFactory;
  private JAXBContext jc;

  private static List<SynchPropertyInfo> propInfo =
      new ArrayList<SynchPropertyInfo>();

  @Override
  public void start(final String connectorId,
                    final String callbackUri,
                    final SynchEngine syncher) throws SynchException {
    this.connectorId = connectorId;
    this.syncher = syncher;
    this.callbackUri = callbackUri;

    debug = getLogger().isDebugEnabled();
    running = true;
  }

  @Override
  public boolean isManager() {
    return true;
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
    return true;
  }

  @Override
  public boolean getTrustLastmod() {
    return false;
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
  public SynchConnectorInstance getConnectorInstance(final Subscription sub,
                                                     final SynchEnd end) throws SynchException {
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

          @SuppressWarnings("unchecked")
          List<SynchPropertyInfo> l = c.getPropertyInfo();
          for (SynchPropertyInfo spit: l) {
            aspi.getProperty().add(spit);
          }

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

  @Override
  public void stop() throws SynchException {
    running = false;
  }

  /* ====================================================================
   *                   Protected methods
   * ==================================================================== */

  protected void info(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  protected void warn(final String msg) {
    getLogger().warn(msg);
  }

  /* Get a logger for messages
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  /* ====================================================================
   *                         Package methods
   * ==================================================================== */

  SynchRemoteServicePortType getPort() throws SynchException {
    try {
      URL wsURL = new URL(config.getBwWSDLURI());

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


  Object unmarshalBody(final HttpServletRequest req) throws SynchException {
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

      // Check with the connector to see if this is a valid match
      //    !sub.getCalPath().equals(u.getCalendarHref())) {
      //  info("No access for subscription - unmatched parameters " + sub);
      //  return;
      //}

      ok = true;
    } // checkSub

    if (!ok) {
      // No subscription or error - nothing to do
      usr.setStatus(StatusType.ERROR);
      usr.setErrorResponse(new ErrorResponseType());
      usr.getErrorResponse().setError(of.createUnknownSubscription(new UnknownSubscriptionType()));

      return new Notification(sub, usr);
    }

    //usr.setSubscribeStatus(syncher.unsubscribe(sub));

    return new Notification(sub, usr);
  }
}
