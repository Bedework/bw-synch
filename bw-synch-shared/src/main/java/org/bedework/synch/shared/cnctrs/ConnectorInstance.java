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
import org.bedework.synch.shared.BaseSubscriptionInfo.CrudCts;
import org.bedework.synch.wsmessages.ActiveSubscriptionRequestType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;
import org.bedework.synch.wsmessages.UnsubscribeResponseType;
import org.bedework.util.misc.ToString;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.BaseResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.DeleteItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.FetchItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemType;

import java.util.List;

/** The interface implemented by connectors. A connector instance is obtained
 * from a connector to handle a specific end of a specific subscription - items of
 * inforamtion handed to the getConnectorInstance method.
 *
 * @author Mike Douglass
 */
public interface ConnectorInstance<InfoT extends BaseSubscriptionInfo> {
  /** Do whatever is required to set up a subscription to the end point for this
   * connector instance. This is a one time call when a new subscription is
   * created and allows the connector instance to validate the information.
   *
   * <p>This method should set the appropriate status if an error occurs.
   *
   * <p>the open method handles any dynamic creation of a connection to the
   * subscribed-to service.
   *
   * @param sr subscribe response
   * @return false if the subscription fails - status has been set in response
   */
  boolean subscribe(SubscribeResponseType sr);

  /** Check to see if an unsubscribe can go ahead. This method should ensure
   * that the important properties in the request match those set in the
   * subscription, e.g. paths
   *
   * <p>This method should set the appropriate status and return false if an
   * error occurs.
   *
   * @param usreq unsubscribe request
   * @param usresp unsubscribe response
   * @return false if the unsubscribe fails - status has been set in response
   */
  boolean unsubscribe(UnsubscribeRequestType usreq,
                      UnsubscribeResponseType usresp);

  /** Ensure active subscription info matches the subscription
   *
   * @param req http request
   * @param resp http response
   * @param cnctr connector
   * @param info subscription info
   * @return true if all ok
   */
  boolean validateActiveSubInfo(ActiveSubscriptionRequestType req,
                                BaseResponseType resp,
                                Connector<?, ?, ?> cnctr,
                                BaseSubscriptionInfo info);

  /** Called when a subscription is activated on synch engine startup or after
   * creation of a new subscription.
   *
   * @return status + messages
   */
  BaseResponseType open();

  /**
   * @return the connector for this instance
   */
  Connector<?, ?, ?> getConnector();

  /**
   * @return the info for the subscription this instance is handling.
   */
  InfoT getSubInfo();

  /** Called before a resynch takes place to determine if the end point has
   * changed and needs resynch. Only the source end of a subscription will be
   * checked. Note that false positives may occur if changes happen outside of
   * the synch time boundaries. For notification driven endpoints this can
   * probably always return false.
   *
   * @return true if a change occurred
   */
  boolean changed();

  /**
   * @param val counts
   */
  void setLastCrudCts(CrudCts val);

  /**
   * @return cts
   */
  CrudCts getLastCrudCts();

  /**
   * @param val counts
   */
  void setTotalCrudCts(CrudCts val);

  /**
   * @return cts
   */
  public CrudCts getTotalCrudCts();


  /** Information used to synch ends A and B
   * This information is only valid in the context of a given subscription.
   */
  class ItemInfo {
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
      return new ToString(this)
              .append("uid", uid)
              .append("lastMod", lastMod)
              .append("lastSynch", lastSynch)
              .toString();
    }
  }

  /** Status OK and no items is end of batch. batched set true means
   * keep calling until no more items.
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
   */
  SynchItemsInfo getItemsInfo();

  /** Add a calendar component
   *
   * @param val a calendar component
   * @return response
   */
  AddItemResponseType addItem(IcalendarType val);

  /** Fetch a calendar component.  The uid is required as a key as it is the
   * only value which is guaranteed to be available at both ends.
   *
   * @param uid of item
   * @return response
   */
  FetchItemResponseType fetchItem(String uid);

  /** Fetch a batch of calendar components. The number and order of the result
   * set must match that of the parameter uids.
   *
   * @param uids of items
   * @return responses
   */
  List<FetchItemResponseType> fetchItems(List<String> uids);

  /** Update a calendar component.
   *
   * @param updates has the change token, href, and the component selection fields set.
   * @return response
   */
  UpdateItemResponseType updateItem(UpdateItemType updates);

  /** Delete a calendar component.
   *
   * @param uid - of the component to delete.
   * @return response
   */
  DeleteItemResponseType deleteItem(String uid);

  /* Reset subscription so we do a refresh of the data
   */
  void forceRefresh();
}
