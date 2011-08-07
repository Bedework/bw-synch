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
import org.bedework.synch.SynchException;

import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.List;

/** Calls from exchange synch processor to the service.
 *
 * @author Mike Douglass
 */
public class ExchangeConnectorInstance implements ConnectorInstance<ExchangeSubscription> {
  /** Called to initialize the exchange synch process. A response of null means
   * no exchange synch. Note that users can synchronize with exchange systems in
   * other domains, so even if your site doesn't run exchange you may want to to
   * run the synch process,
   *
   * The return value is a random uid which is used to validate incoming
   * requests from the remote server.
   *
   * @param conf
   * @param token - null for new connection - current token for ping
   * @return null for no synch else a random uid.
   * @throws SynchException
   */
  @Override
  public String init(final ExchangeSubscription sub,
                     final String callbackUri,
                     final String token) throws SynchException {
    return null;
  }

  @Override
  public List<ItemInfo> getItemsInfo() throws SynchException {
    return null;
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
}
