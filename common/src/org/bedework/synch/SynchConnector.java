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
package org.bedework.synch;

import org.bedework.synch.SynchDefs.SynchEnd;
import org.bedework.synch.SynchDefs.SynchKind;
import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.cnctrs.ConnectorInstance;
import org.bedework.synch.cnctrs.ConnectorPropertyInfo;
import org.bedework.synch.cnctrs.bedework.BedeworkConnectorConfig;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.AlreadySubscribedType;
import org.bedework.synch.wsmessages.ConnectorInfoType;
import org.bedework.synch.wsmessages.ObjectFactory;
import org.bedework.synch.wsmessages.SubscribeRequestType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SynchPropertyType;
import org.bedework.synch.wsmessages.SynchRemoteService;
import org.bedework.synch.wsmessages.SynchRemoteServicePortType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.BaseResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.ErrorResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;

/** A special connector to handle calls to the synch engine via the web context.
 *
 * <p>This is the way to call the system to add subscriptions, to unsubscribe etc.
 *
 * @author Mike Douglass
 */
public class SynchConnector
      implements Connector<SynchConnector.SynchConnectorInstance,
                           Notification> {
  private BedeworkConnectorConfig config;

  private String callbackUri;

  private String connectorId;

  private SynchEngine syncher;

  private transient Logger log;

  private boolean debug;

  private boolean running;

  // Are these thread safe?
  private MessageFactory soapMsgFactory;
  private JAXBContext jc;

  private static List<ConnectorPropertyInfo> propInfo =
      new ArrayList<ConnectorPropertyInfo>();

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
  public List<ConnectorPropertyInfo> getPropertyInfo() {
    return propInfo;
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

      if (o instanceof SubscribeRequestType) {
        return new NotificationBatch(subscribe(resp, (SubscribeRequestType)o));
      }

      if (o instanceof UnsubscribeRequestType) {
//        unsubscribe(resp, (UnsubscribeRequestType)o);
        return null;
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

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private Notification subscribe(final HttpServletResponse resp,
                                 final SubscribeRequestType sr) throws SynchException {
    Subscription sub = new Subscription(null, true);

    sub.setDirection(sr.getDirection());
    sub.setMaster(sr.getMaster());
    sub.setEndAConnectorInfo(makeConnInfo(sr.getEndAConnector()));
    sub.setEndBConnectorInfo(makeConnInfo(sr.getEndBConnector()));

    if (debug) {
      trace("Handle subscribe " +  sub);
    }

    /* Look for a subscription that matches the 2 end points */

    Subscription s = syncher.find(sub);
    ObjectFactory of = new ObjectFactory();

    SubscribeResponseType sresp = of.createSubscribeResponseType();

    if (s != null) {
      sresp.setStatus(StatusType.ERROR);
      sresp.setErrorResponse(new ErrorResponseType());
      sresp.getErrorResponse().setError(of.createAlreadySubscribed(new AlreadySubscribedType()));
    } else {
      sresp.setStatus(StatusType.OK);
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

  /**
   * @param resp
   * @param u
   * @throws SynchException
   */
  private void unsubscribe(final HttpServletResponse resp,
                           final UnsubscribeRequestType u) throws SynchException {
    if (debug) {
      trace("Handle unsubscribe " +  u.getSubscriptionId());
    }
/*
    Subscription sub;

    sub = syncher.getSubscription(u.getSubscriptionId());

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

    usr.setSubscribeStatus(syncher.unsubscribe(sub));

    marshalBody(resp, usr);
    */
  }

  /* Null class to do nothing except fail. */
  static class SynchConnectorInstance implements ConnectorInstance {
    @Override
    public SubscribeResponseType subscribe(final SubscribeResponseType val) throws SynchException {
      return val;
    }

    @Override
    public BaseResponseType open() throws SynchException {
      return null;
    }

    @Override
    public boolean changed() throws SynchException {
      return false;
    }

    @Override
    public List<ItemInfo> getItemsInfo() throws SynchException {
      throw new SynchException("Uncallable");
    }

    @Override
    public AddItemResponseType addItem(final IcalendarType val) throws SynchException {
      throw new SynchException("Uncallable");
    }

    @Override
    public FetchItemResponseType fetchItem(final String uid) throws SynchException {
      throw new SynchException("Uncallable");
    }

    @Override
    public List<FetchItemResponseType> fetchItems(final List<String> uids) throws SynchException {
      return null;
    }

    @Override
    public UpdateItemResponseType updateItem(final UpdateItemType updates) throws SynchException {
      throw new SynchException("Uncallable");
    }
  }
}
