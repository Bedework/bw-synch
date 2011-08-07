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
package org.bedework.synch.cnctrs.bedework;

import org.bedework.synch.ConnectorInstance;
import org.bedework.synch.SynchException;

import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.BaseParameterType;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/** Calls from exchange synch processor to the service.
 *
 * @author Mike Douglass
 */
public class BedeworkConnectorInstance
      implements ConnectorInstance<BedeworkSubscription> {
  private BedeworkConnector cnctr;

  private BedeworkSubscription sub;

  void init(final BedeworkSubscription sub) {

  }

  @Override
  public List<ItemInfo> getItemsInfo() throws SynchException {
    List<ItemInfo> items = new ArrayList<ItemInfo>();

    GetSynchInfoType gsi = new GetSynchInfoType();

    gsi.setCalendarHref(sub.getCalPath());
    gsi.setPrincipalHref(sub.getprincipalHref());
    gsi.setSynchToken(curToken);

    SynchInfoResponseType sir = cnctr.getPort().getSynchInfo(gsi);

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

  /** Add a calendar component
   *
   * @param sub
   * @param uid of item
   * @param val
   * @return response
   * @throws SynchException
   */
  @Override
  public AddItemResponseType addItem(final String uid,
                                     final IcalendarType val) throws SynchException {
    AddItemType ai = new AddItemType();

    ai.setCalendarHref(sub.getCalPath());
    ai.setPrincipalHref(sub.getprincipalHref());
    ai.setSynchToken(curToken);
    ai.setUid(uid);
    ai.setIcalendar(val);

    return cnctr.getPort().addItem(ai);
  }

  /** Fetch a calendar component
   *
   * @param sub
   * @param uid of item
   * @return response
   * @throws SynchException
   */
  @Override
  public FetchItemResponseType fetchItem(final String uid) throws SynchException {
    FetchItemType fi = new FetchItemType();

    fi.setCalendarHref(sub.getCalPath());
    fi.setPrincipalHref(sub.getPrincipalHref());
    fi.setSynchToken(curToken);
    fi.setUid(uid);

    return cnctr.getPort().fetchItem(fi);
  }

  /** Update a calendar component
   *
   * @param sub
   * @param uid of item
   * @param updates
   * @param nsc - used to set namespaces
   * @return response
   * @throws SynchException
   */
  @Override
  public UpdateItemResponseType updateItem(final String uid,
                                           final UpdateItemType updates) throws SynchException {
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
}
