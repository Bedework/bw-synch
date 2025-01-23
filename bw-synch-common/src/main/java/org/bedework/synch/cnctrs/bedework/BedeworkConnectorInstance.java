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

import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.cnctrs.AbstractConnectorInstance;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.SynchIdTokenType;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.xml.tagdefs.XcalTags;

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
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemType;
import org.oasis_open.docs.ws_calendar.ns.soap.AllpropType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarDataResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarQueryResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarQueryType;
import org.oasis_open.docs.ws_calendar.ns.soap.CompFilterType;
import org.oasis_open.docs.ws_calendar.ns.soap.DeleteItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.DeleteItemType;
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

/** Handles bedework synch interactions.
 *
 * @author Mike Douglass
 */
public class BedeworkConnectorInstance
        extends AbstractConnectorInstance<BedeworkConnector,
        BedeworkSubscriptionInfo,
        BedeworkConnectorConfig> {
  BedeworkConnectorInstance(final BedeworkConnectorConfig config,
                            final BedeworkConnector cnctr,
                            final Subscription sub,
                            final SynchEndType end,
                            final BedeworkSubscriptionInfo info) {
    super(sub, end, info, cnctr, config);
  }

  @Override
  public boolean changed() {
    /* This implementation needs to at least check the change token for the
     * collection and match it against the stored token.
     */
    return false;
  }

  @Override
  public void forceRefresh() {
    info.setChangeToken(null);  // Force refresh next time
  }

  @Override
  public SynchItemsInfo getItemsInfo() {
    /* Build a calendar query to fetch all the items in the referenced
     * collection
     */
    final CalendarQueryType cq = new CalendarQueryType();

    final ObjectFactory of = cnctr.getIcalObjectFactory();

    cq.setHref(info.getUri());

    /* Build a set of required properties which we will specify for all
     * component types we can handle
     */

    cq.setIcalendar(new IcalendarType());
    final VcalendarType vcal = new VcalendarType();
    cq.getIcalendar().getVcalendar().add(vcal);

    final ArrayOfComponents aovcc = new ArrayOfComponents();
    vcal.setComponents(aovcc);

    /* Build the properties we want
     * uid: to identify the component
     * lastMod: we can trust that for bedework
     *
     * We want the same properties for tasks or events.
     *
     * TODO add match on any component
    */

    final ArrayOfProperties aop = new ArrayOfProperties();

    final UidPropType propUid = new UidPropType();
    aop.getBasePropertyOrTzid().add(of.createUid(propUid));

    final LastModifiedPropType propLastMod = new LastModifiedPropType();
    aop.getBasePropertyOrTzid().add(of.createLastModified(propLastMod));

    BaseComponentType comp = new VeventType();
    comp.setProperties(aop);

    aovcc.getBaseComponent().add(of.createVevent((VeventType)comp));

    comp = new VtodoType();
    comp.setProperties(aop);

    aovcc.getBaseComponent().add(of.createVtodo((VtodoType)comp));

    /* Now build a filter which returns all the types we want.
     */
    final FilterType fltr = new FilterType();
    cq.setFilter(fltr);

    final CompFilterType cf = new CompFilterType();
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

    /* Execute the query */

    final CalendarQueryResponseType cqr =
            getPort().calendarQuery(getIdToken(),
                                          cq);

    final SynchItemsInfo sii = new SynchItemsInfo();
    sii.items = new ArrayList<>();
    sii.setStatus(StatusType.OK);

    if (cqr.getStatus() != StatusType.OK) {
      sii.setStatus(cqr.getStatus());
      sii.setErrorResponse(cqr.getErrorResponse());
      sii.setMessage(cqr.getMessage());

      return sii;
    }

    /* Go through each element in the response '
     */
    final List<MultistatResponseElementType> responses = cqr.getResponse();

    for (final MultistatResponseElementType mre: responses) {
      final List<PropstatType> pss = mre.getPropstat();

      for (final PropstatType ps: pss) {
        if (ps.getStatus() != StatusType.OK) {
          continue;
        }

        for (final MultistatusPropElementType prop: ps.getProp()) {
          if (prop.getCalendarData() == null) {
            continue;
          }

          final CalendarDataResponseType cd = prop.getCalendarData();

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
    final VcalendarType vcal = ical.getVcalendar().get(0);

    final List<JAXBElement<? extends BaseComponentType>> comps =
        vcal.getComponents().getBaseComponent();
    final BaseComponentType comp = comps.get(0).getValue();

    final UidPropType uid =
            (UidPropType)XcalUtil.findProperty(comp,
                                               XcalTags.uid);

    final LastModifiedPropType lastmod =
            (LastModifiedPropType)XcalUtil.findProperty(comp,
                                                        XcalTags.lastModified);

    return new ItemInfo(uid.getText(), lastmod.getUtcDateTime().toXMLFormat(), null);
  }

  @Override
  public AddItemResponseType addItem(final IcalendarType val) {
    final AddItemType ai = new AddItemType();

    ai.setHref(info.getUri());
    ai.setIcalendar(val);

    return getPort().addItem(getIdToken(), ai);
  }

  @Override
  public FetchItemResponseType fetchItem(final String uid) {
    final CalendarQueryType cq = new CalendarQueryType();

    final ObjectFactory of = cnctr.getIcalObjectFactory();

    cq.setHref(info.getUri());
    cq.setAllprop(new AllpropType());

    final FilterType fltr = new FilterType();
    cq.setFilter(fltr);

    final CompFilterType cf = new CompFilterType();
    cf.setVcalendar(new VcalendarType());

    fltr.setCompFilter(cf);

    /* XXX This will not work in general - this query will only allow events to work.
     * We need better expressions.
     */

    final CompFilterType cfev = new CompFilterType();
    cf.getCompFilter().add(cfev);
    cfev.setBaseComponent(of.createVevent(new VeventType()));

    /* XXX We need to limit the time range we are synching
    if (start != null) {
      UTCTimeRangeType tr = new UTCTimeRangeType();

      tr.setStart(XcalUtil.getXMlUTCCal(start));
      tr.setEnd(XcalUtil.getXMlUTCCal(end));

      cfev.setTimeRange(tr);
    }*/

    final PropFilterType pr = new PropFilterType();
    pr.setBaseProperty(of.createUid(new UidPropType()));

    final TextMatchType tm = new TextMatchType();
    tm.setValue(uid);

    pr.setTextMatch(tm);

    cfev.getPropFilter().add(pr);

    final CalendarQueryResponseType cqr = getPort().calendarQuery(getIdToken(), cq);

    final FetchItemResponseType fir = new FetchItemResponseType();

    fir.setStatus(cqr.getStatus());

    if (fir.getStatus() != StatusType.OK) {
      fir.setErrorResponse(cqr.getErrorResponse());
      fir.setMessage(cqr.getMessage());
      return fir;
    }

    final List<MultistatResponseElementType> mres = cqr.getResponse();
    if (mres.isEmpty()) {
      fir.setStatus(StatusType.NOT_FOUND);
      return fir;
    }

    if (mres.size() > 1) {
      fir.setStatus(StatusType.ERROR);
      fir.setMessage("More than one response");
      return fir;
    }

    final MultistatResponseElementType mre = mres.get(0);
    fir.setHref(mre.getHref());
    fir.setChangeToken(mre.getChangeToken());

    /* Expect a single propstat element */

    if (mre.getPropstat().size() != 1) {
      fir.setStatus(StatusType.ERROR);
      fir.setMessage("More than one propstat in response");
      return fir;
    }

    final PropstatType pstat = mre.getPropstat().get(0);
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

    final CalendarDataResponseType cdr = pstat.getProp().get(0).getCalendarData();

    if ((cdr == null) || (cdr.getIcalendar() == null)) {
      fir.setStatus(StatusType.NOT_FOUND);
      return fir;
    }

    fir.setIcalendar(cdr.getIcalendar());

    return fir;
  }

  @Override
  public List<FetchItemResponseType> fetchItems(final List<String> uids) {
    // XXX this should be a search for multiple uids - need to reimplement caldav search

    final List<FetchItemResponseType> firs = new ArrayList<>();

    for (final String uid: uids) {
      firs.add(fetchItem(uid));
    }

    return firs;
  }

  @Override
  public UpdateItemResponseType updateItem(final UpdateItemType updates) {
    return getPort().updateItem(getIdToken(), updates);
  }

  @Override
  public DeleteItemResponseType deleteItem(final String uid)
          throws SynchException {
    /* At the moment have to fetch it just to get the href */

    final FetchItemResponseType fresp = fetchItem(uid);
    if (debug()) {
      debug("deleteItem: status=" + fresp.getStatus() +
                    " msg=" + fresp.getMessage());
    }

    if (fresp.getStatus() != StatusType.OK) {
      final DeleteItemResponseType dirt = new DeleteItemResponseType();

      dirt.setStatus(fresp.getStatus());
      return dirt;
    }

    final DeleteItemType dit = new DeleteItemType();
    dit.setHref(fresp.getHref());

    return getPort().deleteItem(getIdToken(), dit);
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  SynchIdTokenType getIdToken() {
    return cnctr.getIdToken(info.getPrincipalHref(),
                            info.getOpaqueData());
  }
}
