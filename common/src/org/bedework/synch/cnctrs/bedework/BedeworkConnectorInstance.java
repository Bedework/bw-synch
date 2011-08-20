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
import org.bedework.synch.SynchDefs.SynchEnd;
import org.bedework.synch.SynchException;
import org.bedework.synch.wsmessages.GetSynchInfoType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SynchIdTokenType;
import org.bedework.synch.wsmessages.SynchInfoResponseType;
import org.bedework.synch.wsmessages.SynchInfoResponseType.SynchInfoResponses;
import org.bedework.synch.wsmessages.SynchInfoType;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemType;
import org.oasis_open.docs.ns.wscal.calws_soap.AllpropType;
import org.oasis_open.docs.ns.wscal.calws_soap.BaseResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.CalendarDataResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.CalendarQueryResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.CalendarQueryType;
import org.oasis_open.docs.ns.wscal.calws_soap.CompFilterType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FilterType;
import org.oasis_open.docs.ns.wscal.calws_soap.MultistatResponseElementType;
import org.oasis_open.docs.ns.wscal.calws_soap.PropFilterType;
import org.oasis_open.docs.ns.wscal.calws_soap.PropstatType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;
import org.oasis_open.docs.ns.wscal.calws_soap.TextMatchType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.ArrayList;
import java.util.List;

/** Calls from exchange synch processor to the service.
 *
 * @author Mike Douglass
 */
public class BedeworkConnectorInstance implements ConnectorInstance {
  private transient Logger log;

  private final boolean debug;

  private BedeworkConnectorConfig config;

  private final BedeworkConnector cnctr;

  private BedeworkSubscriptionInfo info;

  private final Subscription sub;

  private SynchEnd end;

  BedeworkConnectorInstance(final BedeworkConnectorConfig config,
                            final BedeworkConnector cnctr,
                            final Subscription sub,
                            final SynchEnd end,
                            final BedeworkSubscriptionInfo info) {
    this.config = config;
    this.cnctr = cnctr;
    this.sub = sub;
    this.end = end;
    this.info = info;

    debug = getLogger().isDebugEnabled();
  }

  /* (non-Javadoc)
   * @see org.bedework.synch.ConnectorInstance#subscribe(org.bedework.synch.wsmessages.SubscribeResponseType)
   */
  @Override
  public SubscribeResponseType subscribe(final SubscribeResponseType val) throws SynchException {
    return val;
  }

  /* (non-Javadoc)
   * @see org.bedework.synch.ConnectorInstance#open()
   */
  @Override
  public BaseResponseType open() throws SynchException {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.bedework.synch.ConnectorInstance#changed()
   */
  @Override
  public boolean changed() throws SynchException {
    /* This implementation needs to at least check the change token for the
     * collection and match it against the stored token.
     */
    return false;
  }

  /* (non-Javadoc)
   * @see org.bedework.synch.ConnectorInstance#getItemsInfo()
   */
  @Override
  public List<ItemInfo> getItemsInfo() throws SynchException {
    List<ItemInfo> items = new ArrayList<ItemInfo>();

    GetSynchInfoType gsi = new GetSynchInfoType();

    gsi.setCalendarHref(info.getCalPath());

    SynchInfoResponseType sir = cnctr.getPort().getSynchInfo(getIdToken(),
                                                             gsi);

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
    CalendarQueryType cq = new CalendarQueryType();

    cq.setHref(info.getCalPath());
    cq.setAllprop(new AllpropType());


    FilterType fltr = new FilterType();
    cq.setFilter(fltr);

    CompFilterType cf = new CompFilterType();
    cf.setName("vcalendar");

    fltr.setCompFilter(cf);

    /* XXX This will not work in general - this query will only allow events to work.
     * We need better expressions.
     */

    CompFilterType cfev = new CompFilterType();
    cf.getCompFilter().add(cfev);
    cfev.setName("vevent");

    /* XXX We need to limit the time range we are synching
    if (start != null) {
      UTCTimeRangeType tr = new UTCTimeRangeType();

      tr.setStart(XcalUtil.getXMlUTCCal(start));
      tr.setEnd(XcalUtil.getXMlUTCCal(end));

      cfev.setTimeRange(tr);
    }*/

    PropFilterType pr = new PropFilterType();
    pr.setName("uid");
    TextMatchType tm = new TextMatchType();
    tm.setValue(uid);

    pr.setTextMatch(tm);

    cfev.getPropFilter().add(pr);

    CalendarQueryResponseType cqr = cnctr.getPort().calendarQuery(getIdToken(), cq);

    FetchItemResponseType fir = new FetchItemResponseType();

    fir.setStatus(cqr.getStatus());

    if (fir.getStatus() != StatusType.OK) {
      fir.setErrorResponse(cqr.getErrorResponse());
      fir.setMessage(cqr.getMessage());
      return fir;
    }

    List<MultistatResponseElementType> mres = cqr.getResponse();
    if (mres.size() == 0) {
      fir.setStatus(StatusType.NOT_FOUND);
      return fir;
    }

    if (mres.size() > 0) {
      fir.setStatus(StatusType.ERROR);
      fir.setMessage("More than one response");
      return fir;
    }

    MultistatResponseElementType mre = mres.get(0);
    fir.setHref(mre.getHref());
    fir.setEtoken(mre.getEtoken());

    /* Expect a single propstat element */

    if (mre.getPropstat().size() != 1) {
      fir.setStatus(StatusType.ERROR);
      fir.setMessage("More than one propstat in response");
      return fir;
    }

    PropstatType pstat = mre.getPropstat().get(0);
    if (pstat.getStatus() != StatusType.OK) {
      fir.setStatus(pstat.getStatus());
      fir.setErrorResponse(pstat.getErrorResponse());
      fir.setMessage(pstat.getMessage());
      return fir;
    }

    if (pstat.getProp().size() != 1) {
      fir.setStatus(StatusType.ERROR);
      fir.setMessage("More than one prop in propstat");
      return fir;
    }

    CalendarDataResponseType cdr = pstat.getProp().get(0).getCalendarData();

    if ((cdr == null) || (cdr.getIcalendar() == null)) {
      fir.setStatus(StatusType.NOT_FOUND);
      return fir;
    }

    fir.setIcalendar(cdr.getIcalendar());

    return fir;
  }

  @Override
  public List<FetchItemResponseType> fetchItems(final List<String> uids) throws SynchException {
    // XXX this should be a search for multiple uids - need to reimplement caldav search

    List<FetchItemResponseType> firs = new ArrayList<FetchItemResponseType>();

    for (String uid: uids) {
      firs.add(fetchItem(uid));
    }

    return firs;
  }

  @Override
  public UpdateItemResponseType updateItem(final UpdateItemType updates) throws SynchException {
    return cnctr.getPort().updateItem(getIdToken(), updates);
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

  SynchIdTokenType getIdToken() {
    return cnctr.getIdToken(info.getPrincipalHref());
  }
}
