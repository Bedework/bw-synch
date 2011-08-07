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
package org.bedework.synch.wsimpl;

import org.bedework.exsynch.wsmessages.AddItemResponseType;
import org.bedework.exsynch.wsmessages.AddItemType;
import org.bedework.exsynch.wsmessages.AddType;
import org.bedework.exsynch.wsmessages.ArrayOfNamespaces;
import org.bedework.exsynch.wsmessages.ArrayOfUpdates;
import org.bedework.exsynch.wsmessages.BaseUpdateType;
import org.bedework.exsynch.wsmessages.ExsynchRemoteService;
import org.bedework.exsynch.wsmessages.ExsynchRemoteServicePortType;
import org.bedework.exsynch.wsmessages.FetchItemResponseType;
import org.bedework.exsynch.wsmessages.FetchItemType;
import org.bedework.exsynch.wsmessages.GetPropertiesResponseType;
import org.bedework.exsynch.wsmessages.GetPropertiesType;
import org.bedework.exsynch.wsmessages.GetSynchInfoType;
import org.bedework.exsynch.wsmessages.NamespaceType;
import org.bedework.exsynch.wsmessages.NewValueType;
import org.bedework.exsynch.wsmessages.ObjectFactory;
import org.bedework.exsynch.wsmessages.RemoveType;
import org.bedework.exsynch.wsmessages.ReplaceType;
import org.bedework.exsynch.wsmessages.StartServiceNotificationType;
import org.bedework.exsynch.wsmessages.StartServiceResponseType;
import org.bedework.exsynch.wsmessages.StatusType;
import org.bedework.exsynch.wsmessages.SynchInfoResponseType;
import org.bedework.exsynch.wsmessages.SynchInfoResponseType.SynchInfoResponses;
import org.bedework.exsynch.wsmessages.SynchInfoType;
import org.bedework.exsynch.wsmessages.UpdateItemResponseType;
import org.bedework.exsynch.wsmessages.UpdateItemType;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.xri.xrd_1.XRDType;

import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.BaseParameterType;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/** Implementation of Exchange synch interface using Web service.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class BwSynchIntfImpl implements ConnectorIntf {
  protected boolean debug;

  private Logger log;

  private ExsynchConfig conf;

  private String curToken;

  private XRDType sysInfo;

  /** Constructor
   *
   */
  public BwSynchIntfImpl() {

  }

  public String initExchangeSynch(final ExsynchConfig val,
                                  final String token) throws SynchException {
    conf = val;

    if (sysInfo == null) {
      // Try to get info first
      GetPropertiesType gp = new GetPropertiesType();

      gp.setSystem(new GetPropertiesType.System());

      GetPropertiesResponseType gpr = getPort().getProperties(gp);

      if (gpr != null) {
        sysInfo = gpr.getXRD();
      }
    }
    StartServiceNotificationType ssn = new StartServiceNotificationType();

    /* Set up the call back URL for incoming subscriptions */

    String uri = conf.getExchangeWsPushURI();
    if (!uri.endsWith("/")) {
      uri += "/";
    }

    ssn.setSubscribeUrl(uri + "subscribe/");

    if (token != null) {
      curToken = token;
    } else {
      curToken = UUID.randomUUID().toString();
    }

    ssn.setToken(curToken);

    StartServiceResponseType ssr = getPort().notifyRemoteService(ssn);

    if (ssr.getStatus() != StatusType.OK) {
      warn("Received status " + ssr.getStatus() + " to start notification");
      curToken = null;
      return null;
    }

    if (!curToken.equals(ssr.getToken())) {
      warn("Mismatched tokens in response to start notification");
      curToken = null;
      return null;
    }

    return curToken;
  }

  @Override
  public List<ItemInfo> getItemsInfo(final BaseSubscription sub) throws SynchException {
    List<ItemInfo> items = new ArrayList<ItemInfo>();

    GetSynchInfoType gsi = new GetSynchInfoType();

    gsi.setCalendarHref(sub.getCalPath());
    gsi.setPrincipalHref(sub.getprincipalHref());
    gsi.setSynchToken(curToken);

    SynchInfoResponseType sir = getPort().getSynchInfo(gsi);

    if (!sir.getCalendarHref().equals(sub.getCalPath())) {
      warn("Mismatched calpath in response to GetSycnchInfo: expected '" +
           sub.getCalPath() + "' but received '" +
           sir.getCalendarHref() + "'");
      return null;
    }

    SynchInfoResponses sirs = sir.getSynchInfoResponses();
    if (sirs == null) {
      return items;
    }

    for (SynchInfoType si: sirs.getSynchInfo()) {
      items.add(new ItemInfo(si.getUid(), si.getExchangeLastmod()));
    }

    return items;
  }

  @Override
  public AddItemResponseType addItem(final BaseSubscription sub,
                                 final String uid,
                                 final IcalendarType val) throws SynchException {
    AddItemType ai = new AddItemType();

    ai.setCalendarHref(sub.getCalPath());
    ai.setPrincipalHref(sub.getprincipalHref());
    ai.setSynchToken(curToken);
    ai.setUid(uid);
    ai.setIcalendar(val);

    return getPort().addItem(ai);
  }

  public FetchItemResponseType fetchItem(final BaseSubscription sub,
                                     final String uid) throws SynchException {
    FetchItemType fi = new FetchItemType();

    fi.setCalendarHref(sub.getCalPath());
    fi.setPrincipalHref(sub.getprincipalHref());
    fi.setSynchToken(curToken);
    fi.setUid(uid);

    return getPort().fetchItem(fi);
  }

  @SuppressWarnings("unchecked")
  public UpdateItemResponseType updateItem(final BaseSubscription sub,
                                       final String uid,
                                       final List<XpathUpdate> updates,
                                       final edu.rpi.sss.util.xml.NsContext nsc) throws SynchException {
    UpdateItemType upd = new UpdateItemType();

    ArrayOfNamespaces aon = new ArrayOfNamespaces();
    String defaultNs = nsc.getDefaultNS();

    for (String pfx: nsc.getPrefixes()) {
      String uri = nsc.getNamespaceURI(pfx);

      NamespaceType ns = new NamespaceType();
      if ((defaultNs == null) || !defaultNs.equals(uri)) {
        ns.setPrefix(pfx);
      }

      ns.setUri(uri);

      aon.getNamespace().add(ns);
    }

    upd.setNamespaces(aon);

    upd.setCalendarHref(sub.getCalPath());
    upd.setPrincipalHref(sub.getprincipalHref());
    upd.setSynchToken(curToken);
    upd.setUid(uid);

    ArrayOfUpdates aupd = new ArrayOfUpdates();
    upd.setUpdates(aupd);

    ObjectFactory of = new ObjectFactory();

    for (XpathUpdate xupd: updates) {
      QName name = xupd.getName();

      JAXBElement<? extends BaseUpdateType> jel;

      if (xupd.add) {
        jel = of.createAdd(new AddType());
      } else if (xupd.delete) {
        jel = of.createRemove(new RemoveType());
      } else {
        jel = of.createReplace(new ReplaceType());
      }

      BaseUpdateType but = jel.getValue();

      but.setSel(xupd.getXpath());

      if (but instanceof NewValueType) {
        NewValueType nv = (NewValueType)but;

        if (xupd.getBaseComponent() != null) {
          Object newEntity = xupd.getBaseComponent();

          JAXBElement<? extends BaseComponentType> el = new JAXBElement(name,
                                                                       newEntity.getClass(),
                                                                       newEntity);
          nv.setBaseComponent(el);
        } else if (xupd.getBaseProperty() != null) {
          Object newEntity = xupd.getBaseProperty();

          JAXBElement<? extends BasePropertyType> el = new JAXBElement(name,
                                                                       newEntity.getClass(),
                                                                       newEntity);
          nv.setBaseProperty(el);
        } else if (xupd.getBaseParameter() != null) {
          Object newEntity = xupd.getBaseParameter();

          JAXBElement<? extends BaseParameterType> el = new JAXBElement(name,
                                                                       newEntity.getClass(),
                                                                       newEntity);
          nv.setBaseParameter(el);
        }
      }

      aupd.getBaseUpdate().add(jel);
    }

    return getPort().updateItem(upd);
  }

  /* ====================================================================
   *                         Private methods
   * ==================================================================== */

  private ExsynchRemoteServicePortType getPort() throws SynchException {
    try {
      URL wsURL = new URL(conf.getRemoteWSDLURI());

      ExsynchRemoteService ers =
        new ExsynchRemoteService(wsURL,
                                 new QName("http://www.bedework.org/exsynch/wsmessages",
                                           "ExsynchRemoteService"));
      ExsynchRemoteServicePortType port = ers.getExsynchRSPort();

      return port;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  protected void info(final String msg) {
    getLogger().info(msg);
  }

  protected void warn(final String msg) {
    getLogger().info(msg);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }
}
