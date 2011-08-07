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

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.List;

import org.bedework.synch.SynchException;
import org.bedework.synch.intf.Defs;

import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponseType;

/** Calls from exchange synch processor to the service.
 *
 * @author Mike Douglass
 */
public interface ExchangeSynchIntf extends Defs {
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
  String initExchangeSynch(ExsynchConfig conf,
                           String token) throws SynchException;

  /** Information used to synch remote with Exchange
   * This information is only valid in the context of a given subscription.
   */
  public static class ItemInfo {
    /** */
    public String uid;

    /** */
    public String lastMod;

    /** */
    public boolean seen;

    /**
     * @param uid
     * @param lastMod
     */
    public ItemInfo(final String uid,
                     final String lastMod) {
      this.uid = uid;
      this.lastMod = lastMod;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("ItemInfo{");

      sb.append("uid=");
      sb.append(uid);

      sb.append(",\n   lastMod=");
      sb.append(lastMod);

      sb.append("}");

      return sb.toString();
    }
  }

  /** Get information about items in the subscribed calendar. Used for initial
   * synch.
   *
   * @param sub
   * @return List of items - never null, maybe empty.
   * @throws SynchException
   */
  List<ItemInfo> getItemsInfo(ExchangeSubscription sub) throws SynchException;

  /** Add a calendar component
   *
   * @param sub
   * @param uid of item
   * @param val
   * @return response
   * @throws SynchException
   */
  AddItemResponseType addItem(ExchangeSubscription sub,
                          String uid,
                          IcalendarType val) throws SynchException;

  /** Fetch a calendar component
   *
   * @param sub
   * @param uid of item
   * @return response
   * @throws SynchException
   */
  FetchItemResponseType fetchItem(ExchangeSubscription sub,
                                  String uid) throws SynchException;

  /** Update a calendar component
   *
   * @param sub
   * @param uid of item
   * @param updates
   * @param nsc - used to set namespaces
   * @return response
   * @throws SynchException
   */
  UpdateItemResponseType updateItem(ExchangeSubscription sub,
                                String uid,
                                List<XpathUpdate> updates,
                                edu.rpi.sss.util.xml.NsContext nsc) throws SynchException;
}
