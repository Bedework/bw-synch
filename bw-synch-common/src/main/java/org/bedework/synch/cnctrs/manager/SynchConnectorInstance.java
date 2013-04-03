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

import org.bedework.synch.BaseSubscriptionInfo;
import org.bedework.synch.cnctrs.AbstractConnectorInstance;
import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.exception.SynchException;

import org.oasis_open.docs.ws_calendar.ns.soap.AddItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.BaseResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.FetchItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.List;

/** A null connector instance
 *
 * @author Mike Douglass
 */
public class SynchConnectorInstance extends AbstractConnectorInstance {
  private SynchConnector cnctr;

  SynchConnectorInstance(final SynchConnector cnctr){
    super(null, null, null);

    this.cnctr = cnctr;
  }

  @Override
  public Connector getConnector() {
    return cnctr;
  }

  @Override
  public BaseSubscriptionInfo getSubInfo() {
    return null;
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
  public SynchItemsInfo getItemsInfo() throws SynchException {
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
