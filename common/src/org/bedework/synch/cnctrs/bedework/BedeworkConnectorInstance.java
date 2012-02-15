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

import org.bedework.synch.BaseSubscriptionInfo;
import org.bedework.synch.cnctrs.AbstractConnectorInstance;
import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.SynchIdTokenType;

import edu.rpi.cmt.calendar.XcalUtil;
import edu.rpi.sss.util.xml.tagdefs.XcalTags;

import org.oasis_open.docs.ws_calendar.ns.soap.AddItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemType;
import org.oasis_open.docs.ws_calendar.ns.soap.AllpropType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarDataResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarQueryResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarQueryType;
import org.oasis_open.docs.ws_calendar.ns.soap.CompFilterType;
import org.oasis_open.docs.ws_calendar.ns.soap.FetchItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.FilterType;
import org.oasis_open.docs.ws_calendar.ns.soap.MultistatResponseElementType;
import org.oasis_open.docs.ws_calendar.ns.soap.MultistatusPropElementType;
import org.oasis_open.docs.ws_calendar.ns.soap.PropFilterType;
import org.oasis_open.docs.ws_calendar.ns.soap.PropstatType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;
import org.oasis_open.docs.ws_calendar.ns.soap.TextMatchType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.LastModifiedPropType;
import ietf.params.xml.ns.icalendar_2.ObjectFactory;
import ietf.params.xml.ns.icalendar_2.UidPropType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;
import ietf.params.xml.ns.icalendar_2.VeventType;
import ietf.params.xml.ns.icalendar_2.VtodoType;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

/** Handles bedework synch interactions.
 *
 * @author Mike Douglass
 */
public class BedeworkConnectorInstance extends AbstractConnectorInstance {
  @SuppressWarnings("unused")
  private BedeworkConnectorConfig config;

  private final BedeworkConnector cnctr;

  private BedeworkSubscriptionInfo info;

  BedeworkConnectorInstance(final BedeworkConnectorConfig config,
                            final BedeworkConnector cnctr,
                            final Subscription sub,
                            final SynchEndType end,
                            final BedeworkSubscriptionInfo info) {
    super(sub, end, info);
    this.config = config;
    this.cnctr = cnctr;
    this.info = info;
  }

  @Override
  public Connector getConnector() {
    return cnctr;
  }

  @Override
  public BaseSubscriptionInfo getSubInfo() {
    return info;
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
  public SynchItemsInfo getItemsInfo() throws SynchException {
    CalendarQueryType cq = new CalendarQueryType();

    ObjectFactory of = cnctr.getIcalObjectFactory();

    cq.setHref(info.getUri());

    /* Build a set of required properties which we will specify for all
     * component types we can handle
     */

    cq.setIcalendar(new IcalendarType());
    VcalendarType vcal = new VcalendarType();
    cq.getIcalendar().getVcalendar().add(vcal);

    ArrayOfComponents aovcc = new ArrayOfComponents();
    vcal.setComponents(aovcc);

    /* Build the properties we want */

    ArrayOfProperties aop = new ArrayOfProperties();

    UidPropType propUid = new UidPropType();
    aop.getBasePropertyOrTzid().add(of.createUid(propUid));

    LastModifiedPropType propLastMod = new LastModifiedPropType();
    aop.getBasePropertyOrTzid().add(of.createLastModified(propLastMod));

    BaseComponentType comp = new VeventType();
    comp.setProperties(aop);

    aovcc.getBaseComponent().add(of.createVevent((VeventType)comp));

    comp = new VtodoType();
    comp.setProperties(aop);

    aovcc.getBaseComponent().add(of.createVtodo((VtodoType)comp));

    /* Now build a filter which returns all the types we want.
     */
    FilterType fltr = new FilterType();
    cq.setFilter(fltr);

    CompFilterType cf = new CompFilterType();
    //vc.setComponents(new ArrayOfVcalendarContainedComponents());
    cf.setVcalendar(new VcalendarType());

    //cf.setVcalendar(new VcalendarType());
    //cf.setName(XcalTags.vcalendar.getLocalPart());
    cf.setTest("anyof");

    fltr.setCompFilter(cf);

    CompFilterType cfent = new CompFilterType();
    cf.getCompFilter().add(cfent);
    cfent.setBaseComponent(of.createVevent(new VeventType()));
    //cfent.setName(XcalTags.vevent.getLocalPart());

    cfent = new CompFilterType();
    cf.getCompFilter().add(cfent);
    cfent.setBaseComponent(of.createVtodo(new VtodoType()));

    CalendarQueryResponseType cqr = cnctr.getPort().calendarQuery(getIdToken(),
                                                                  cq);

    SynchItemsInfo sii = new SynchItemsInfo();
    sii.items = new ArrayList<ItemInfo>();
    sii.setStatus(StatusType.OK);

    if (cqr.getStatus() != StatusType.OK) {
      sii.setStatus(cqr.getStatus());
      sii.setErrorResponse(cqr.getErrorResponse());
      sii.setMessage(cqr.getMessage());

      return sii;
    }

    List<MultistatResponseElementType> responses = cqr.getResponse();

    for (MultistatResponseElementType mre: responses) {
      List<PropstatType> pss = mre.getPropstat();

      for (PropstatType ps: pss) {
        if (ps.getStatus() != StatusType.OK) {
          continue;
        }

        for (MultistatusPropElementType prop: ps.getProp()) {
          if (prop.getCalendarData() == null) {
            continue;
          }

          CalendarDataResponseType cd = prop.getCalendarData();

          if (cd.getIcalendar() == null) {
            continue;
          }

          sii.items.add(getItem(cd.getIcalendar()));
        }
      }
    }

    return sii;
  }

  private ItemInfo getItem(final IcalendarType ical) {
    VcalendarType vcal = ical.getVcalendar().get(0);

    List<JAXBElement<? extends BaseComponentType>> comps =
        vcal.getComponents().getBaseComponent();
    BaseComponentType comp = comps.get(0).getValue();

    UidPropType uid = (UidPropType)XcalUtil.findProperty(comp,
                                             XcalTags.uid);

    LastModifiedPropType lastmod = (LastModifiedPropType)XcalUtil.findProperty(comp,
                                                         XcalTags.lastModified);

    return new ItemInfo(uid.getText(), lastmod.getUtcDateTime().toXMLFormat(), null);
  }

  @Override
  public AddItemResponseType addItem(final IcalendarType val) throws SynchException {
    AddItemType ai = new AddItemType();

    ai.setHref(info.getUri());
    ai.setIcalendar(val);

    return cnctr.getPort().addItem(getIdToken(), ai);
  }

  /* (non-Javadoc)
   * @see org.bedework.synch.cnctrs.ConnectorInstance#fetchItem(java.lang.String)
   */
  @Override
  public FetchItemResponseType fetchItem(final String uid) throws SynchException {
    CalendarQueryType cq = new CalendarQueryType();

    ObjectFactory of = cnctr.getIcalObjectFactory();

    cq.setHref(info.getUri());
    cq.setAllprop(new AllpropType());

    FilterType fltr = new FilterType();
    cq.setFilter(fltr);

    CompFilterType cf = new CompFilterType();
    cf.setVcalendar(new VcalendarType());

    fltr.setCompFilter(cf);

    /* XXX This will not work in general - this query will only allow events to work.
     * We need better expressions.
     */

    CompFilterType cfev = new CompFilterType();
    cf.getCompFilter().add(cfev);
    cfev.setBaseComponent(of.createVevent(new VeventType()));

    /* XXX We need to limit the time range we are synching
    if (start != null) {
      UTCTimeRangeType tr = new UTCTimeRangeType();

      tr.setStart(XcalUtil.getXMlUTCCal(start));
      tr.setEnd(XcalUtil.getXMlUTCCal(end));

      cfev.setTimeRange(tr);
    }*/

    PropFilterType pr = new PropFilterType();
    pr.setBaseProperty(of.createUid(new UidPropType()));

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

    if (mres.size() > 1) {
      fir.setStatus(StatusType.ERROR);
      fir.setMessage("More than one response");
      return fir;
    }

    MultistatResponseElementType mre = mres.get(0);
    fir.setHref(mre.getHref());
    fir.setChangeToken(mre.getChangeToken());

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
   *                   Private methods
   * ==================================================================== */

  SynchIdTokenType getIdToken() throws SynchException {
    return cnctr.getIdToken(info.getPrincipalHref());
  }
}
