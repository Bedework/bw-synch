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
import org.bedework.synch.Subscription;
import org.bedework.synch.SynchException;
import org.bedework.synch.cnctrs.exchange.ExchangeConnectorConfig;
import org.bedework.synch.wsmessages.GetSynchInfoType;
import org.bedework.synch.wsmessages.SynchIdTokenType;
import org.bedework.synch.wsmessages.SynchInfoResponseType;
import org.bedework.synch.wsmessages.SynchInfoResponseType.SynchInfoResponses;
import org.bedework.synch.wsmessages.SynchInfoType;

import org.apache.log4j.Logger;
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
      implements ConnectorInstance {
  private BedeworkConnectorConfig config;

  private final BedeworkConnector cnctr;

  private BedeworkSubscriptionInfo info;

  private final Subscription sub;

  private transient Logger log;

  private final boolean debug;

  BedeworkConnectorInstance(final BedeworkConnectorConfig config,
                            final BedeworkConnector cnctr,
                            final Subscription sub,
                            final BedeworkSubscriptionInfo info) {
    this.config = config;
    this.cnctr = cnctr;
    this.sub = sub;
    this.info = info;

    debug = getLogger().isDebugEnabled();
  }

  @Override
  public List<ItemInfo> getItemsInfo() throws SynchException {
    List<ItemInfo> items = new ArrayList<ItemInfo>();

    GetSynchInfoType gsi = new GetSynchInfoType();

    gsi.setCalendarHref(info.getCalPath());

    SynchInfoResponseType sir = cnctr.getPort().getSynchInfo(getIdToken(), gsi);

    if (!sir.getCalendarHref().equals(info.getCalPath())) {
      warn("Mismatched calpath in response to GetSycnchInfo: expected '" +
          info.getCalPath() + "' but received '" +
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
  public AddItemResponseType addItem(final IcalendarType val) throws SynchException {
    AddItemType ai = new AddItemType();

    ai.setHref(info.getCalPath());
    ai.setIcalendar(val);

    return cnctr.getPort().addItem(getIdToken(), ai);
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

    fi.setHref(info.getCalPath());
    fi.setUid(uid);

    return cnctr.getPort().fetchItem(getIdToken(), fi);
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

    upd.setHref(info.getCalPath());
    upd.setEtoken(???);

    return cnctr.getPort().updateItem(getIdToken(), upd);
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
   *                   Private methods
   * ==================================================================== */

  private SynchIdTokenType getIdToken() {
    SynchIdTokenType idToken = new SynchIdTokenType();

    idToken.setPrincipalHref(info.getPrincipalHref());
    idToken.setSynchToken(curToken);

    return idToken;
  }
}
