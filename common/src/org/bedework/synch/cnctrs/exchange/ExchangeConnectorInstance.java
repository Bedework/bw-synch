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

import org.bedework.synch.ConnectorInstance;
import org.bedework.synch.Subscription;
import org.bedework.synch.SynchException;
import org.bedework.synch.cnctrs.exchange.FinditemsResponse.SynchInfo;
import org.bedework.synch.cnctrs.exchange.messages.FindItemsRequest;
import org.bedework.synch.cnctrs.exchange.messages.GetItemsRequest;
import org.bedework.synch.cnctrs.exchange.messages.SubscribeRequest;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import com.microsoft.schemas.exchange.services._2006.messages.ExchangeServicePortType;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeWebService;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SubscribeResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.SubscribeResponseType;
import com.microsoft.schemas.exchange.services._2006.types.BaseItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ExchangeVersionType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemType;
import com.microsoft.schemas.exchange.services._2006.types.MailboxCultureType;
import com.microsoft.schemas.exchange.services._2006.types.RequestServerVersion;
import com.microsoft.schemas.exchange.services._2006.types.ServerVersionInfo;

/** Calls from exchange synch processor to the service.
 *
 * @author Mike Douglass
 */
public class ExchangeConnectorInstance implements ConnectorInstance {
  private ExchangeConnectorConfig config;

  private final ExchangeConnector cnctr;

  private ExchangeSubscriptionInfo info;

  private final Subscription sub;

  private transient Logger log;

  private final boolean debug;

  private final XmlIcalConvert icalConverter = new XmlIcalConvert();

  ExchangeConnectorInstance(final ExchangeConnectorConfig config,
                            final ExchangeConnector cnctr,
                            final Subscription sub,
                            final ExchangeSubscriptionInfo info) {
    this.config = config;
    this.cnctr = cnctr;
    this.sub = sub;
    this.info = info;

    debug = getLogger().isDebugEnabled();
  }

  /** This class is passed back and contans the publicly visible uid and lastmod
   * but also a private BaseItemIdType used to retrieve the item from Exchange.
   *
   * @author douglm
   */
  class ExchangeItemInfo extends ItemInfo {
    private final ItemIdType itemId;

    public ExchangeItemInfo(final String uid,
                            final String lastMod,
                            final ItemIdType itemId) {
      super(uid, lastMod);

      this.itemId = itemId;
    }

    ItemIdType getItemId() {
      return itemId;
    }
  }

  @Override
  public List<ItemInfo> getItemsInfo() throws SynchException {
    DistinguishedFolderIdType fid = new DistinguishedFolderIdType();
    fid.setId(DistinguishedFolderIdNameType.fromValue(info.getExchangeCalendar()));
    FindItemsRequest fir = FindItemsRequest.getSynchInfo(fid);

    Holder<FindItemResponseType> fiResult = new Holder<FindItemResponseType>();

    getPort(info).findItem(fir.getRequest(),
                           // null, // impersonation,
                           getMailboxCulture(),
                           getRequestServerVersion(),
                           // null, // timeZoneContext
                           fiResult,
                           getServerVersionInfoHolder());

    List<JAXBElement<? extends ResponseMessageType>> rms =
      fiResult.value.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

    List<ItemInfo> res = new ArrayList<ItemInfo>();

    for (JAXBElement<? extends ResponseMessageType> jaxbrm: rms) {
      FindItemResponseMessageType firm = (FindItemResponseMessageType)jaxbrm.getValue();

      FinditemsResponse resp = new FinditemsResponse(firm,
                                                     true);

      if (debug) {
        trace(resp.toString());
      }

      for (SynchInfo si: resp.getSynchInfo()) {
        ExchangeItemInfo eii = new ExchangeItemInfo(si.uid,
                                                    si.lastMod,
                                                    si.itemId);

        res.add(eii);
      }
    }

    return res;
  }

  @Override
  public AddItemResponseType addItem(final IcalendarType val) throws SynchException {
    return null;
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
    return null;
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
    return null;
  }

  private MailboxCultureType getMailboxCulture() {
    MailboxCultureType mbc = new MailboxCultureType();

    mbc.setValue("en-US"); // XXX This probably needs to come from the locale

    return mbc;
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

  ExchangeServicePortType getPort(final ExchangeSubscriptionInfo sub) throws SynchException {
    try {
      return getExchangeServicePort(sub.getExchangeId(),
                                    sub.getExchangePw().toCharArray()); // XXX need to en/decrypt
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private ExchangeServicePortType getExchangeServicePort(final String user,
                                                         final char[] pw) throws SynchException {
    try {
      URL wsdlURL = new URL(config.getExchangeWSDLURI());

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(
                user,
                pw);
        }
    });

      ExchangeWebService ews =
        new ExchangeWebService(wsdlURL,
                               new QName("http://schemas.microsoft.com/exchange/services/2006/messages",
                                         "ExchangeWebService"));
      ExchangeServicePortType port = ews.getExchangeWebPort();

//      Map<String, Object> context = ((BindingProvider)port).getRequestContext();

  //    context.put(BindingProvider.USERNAME_PROPERTY, user);
    //  context.put(BindingProvider.PASSWORD_PROPERTY, new String(pw));

      /*
        $client->__setSoapHeaders(
        new SOAPHeader('http://schemas.microsoft.com/exchange/services/2006/types',
        'RequestServerVersion',
        array("Version"=>"Exchange2007_SP1"))
        );

        $client is the SoapClient Instance.

      */


      return port;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  private IcalendarType fetchItem(final BaseItemIdType id) throws SynchException {
    List<BaseItemIdType> toFetch = new ArrayList<BaseItemIdType>();

    toFetch.add(id);

    List<IcalendarType> items = fetchItems(toFetch);

    if (items.size() != 1) {
      return null;
    }

    return items.get(0);
  }

  private ExsynchSubscribeResponse doSubscription() throws SynchException {
    try {
      /* Send a request for a new subscription to exchange */
      SubscribeRequest s = new SubscribeRequest(sub,
                                                config);

      s.setFolderId(info.getExchangeCalendar());

      Holder<SubscribeResponseType> subscribeResult = new Holder<SubscribeResponseType>();

      getPort(info).subscribe(s.getRequest(),
                              // null, // impersonation,
                              getMailboxCulture(),
                              getRequestServerVersion(),
                              subscribeResult,
                              getServerVersionInfoHolder());

      if (debug) {
        trace(subscribeResult.toString());
      }

      List<JAXBElement<? extends ResponseMessageType>> rms =
        subscribeResult.value.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

      if (rms.size() != 1) {
        //
        return null;
      }

      SubscribeResponseMessageType srm = (SubscribeResponseMessageType)rms.iterator().next().getValue();
      ExsynchSubscribeResponse esr = new ExsynchSubscribeResponse(srm);

      if (debug) {
        trace(esr.toString());
      }

      return esr;
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  private List<IcalendarType> fetchItems(final List<BaseItemIdType> toFetch) throws SynchException {
    GetItemsRequest gir = new GetItemsRequest(toFetch);

    Holder<GetItemResponseType> giResult = new Holder<GetItemResponseType>();

    getPort(info).getItem(gir.getRequest(),
                          // null, // impersonation,
                          getMailboxCulture(),
                          getRequestServerVersion(),
                          // null, // timeZoneContext
                          giResult,
                          getServerVersionInfoHolder());

    List<JAXBElement<? extends ResponseMessageType>> girms =
      giResult.value.getResponseMessages().getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();

    List<IcalendarType> items = new ArrayList<IcalendarType>();

    for (JAXBElement<? extends ResponseMessageType> jaxbgirm: girms) {
      Object o = jaxbgirm.getValue();

      if (!(o instanceof ItemInfoResponseMessageType)) {
        continue;
      }

      ItemInfoResponseMessageType iirm = (ItemInfoResponseMessageType)o;

      if (iirm.getItems() == null) {
        continue;
      }

      for (ItemType item: iirm.getItems().getItemOrMessageOrCalendarItem()) {
        if (!(item instanceof CalendarItemType)) {
          continue;
        }

        IcalendarType ical = icalConverter.toXml((CalendarItemType)item);
        if (debug) {
          // serialize and print
          //trace(comp.toString());
        }

        items.add(ical);
      }
    }

    return items;
  }

  private Holder<ServerVersionInfo> getServerVersionInfoHolder() {
    ServerVersionInfo serverVersionInfo = new ServerVersionInfo();
    Holder<ServerVersionInfo> serverVersion = new Holder<ServerVersionInfo>(serverVersionInfo);

    return serverVersion;
  }

  private RequestServerVersion getRequestServerVersion() {
    RequestServerVersion requestVersion = new RequestServerVersion();

    requestVersion.setVersion(ExchangeVersionType.EXCHANGE_2010);

    return requestVersion;
  }
}
