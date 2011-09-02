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
package org.bedework.synch.cnctrs;

import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SubscribeResponseType;

import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.BaseResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.List;

/** The interface implemented by connectors. A connector instance is obtained
 * from a connector to handle a specific end of a specific subscription - items of
 * inforamtion handed to the getConnectorInstance method.
 *
 * @author Mike Douglass
 */
public interface ConnectorInstance {
  /** Do whatever is required to set up a subscription to the end point for this
   * connector instance. This is a one time call when a new subscription is
   * created and allows the connector instance to validate the information.
   *
   * <p>This method should set the appropriate status if an error occurs.
   *
   * <p>the open method handles any dynamic creation of a connection to the
   * subscribed-to service.
   *
   * @param sr
   * @return sr
   * @throws SynchException
   */
  SubscribeResponseType subscribe(SubscribeResponseType sr) throws SynchException;

  /** Called when a subscription is activated on synch engine startup or after
   * creation of a new subscription.
   *
   * @return status + messages
   * @throws SynchException
   */
  BaseResponseType open() throws SynchException;

  /** Called before a resynch takes place to determine if the end point has
   * changed and needs resynch. Only the source end of a subscription will be
   * checked. Note that false positives may occur if changes happen outside of
   * the synch time boundaries. For notification driven endpoints this can
   * probably always return false.
   *
   * @return true if a change occurred
   * @throws SynchException
   */
  boolean changed() throws SynchException;

  /** Information used to synch ends A and B
   * This information is only valid in the context of a given subscription.
   */
  public static class ItemInfo {
    /** */
    public String uid;

    /** */
    public String lastMod;

    /** */
    public String lastSynch;

    /** */
    public boolean seen;

    /**
     * @param uid
     * @param lastMod
     * @param lastSynch
     */
    public ItemInfo(final String uid,
                     final String lastMod,
                     final String lastSynch) {
      this.uid = uid;
      this.lastMod = lastMod;
      this.lastSynch = lastSynch;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("ItemInfo{");

      sb.append("uid=");
      sb.append(uid);

      sb.append(",\n   lastMod=");
      sb.append(lastMod);

      sb.append(",\n   lastSynch=");
      sb.append(lastSynch);

      sb.append("}");

      return sb.toString();
    }
  }

  /**
   */
  public class SynchItemsInfo extends BaseResponseType {
    /** the items.
     */
    public List<ItemInfo> items;
  }

  /** Get information about items in the subscribed calendar. Used for initial
   * synch.
   *
   * @return List of items - never null, maybe empty.
   * @throws SynchException
   */
  SynchItemsInfo getItemsInfo() throws SynchException;

  /** Add a calendar component
   *
   * @param val
   * @return response
   * @throws SynchException
   */
  AddItemResponseType addItem(IcalendarType val) throws SynchException;

  /** Fetch a calendar component.  The uid is required as a key as it is the
   * only value which is guaranteed to be available at both ends.
   *
   * @param uid of item
   * @return response
   * @throws SynchException
   */
  FetchItemResponseType fetchItem(String uid) throws SynchException;

  /** Fetch a batch of calendar components. The number and order of the result
   * set must match that of the parameter uids.
   *
   * @param uids of items
   * @return responses
   * @throws SynchException
   */
  List<FetchItemResponseType> fetchItems(List<String> uids) throws SynchException;

  /** Update a calendar component. The updates component has the change token
   * href, and the component selection fields set.
   *
   * @param fir - the currrent state of the entity we are updating.
   * @param updates
   * @return response
   * @throws SynchException
   */
  UpdateItemResponseType updateItem(UpdateItemType updates) throws SynchException;
}
