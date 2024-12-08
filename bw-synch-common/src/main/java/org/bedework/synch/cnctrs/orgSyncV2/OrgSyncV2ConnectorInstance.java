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
package org.bedework.synch.cnctrs.orgSyncV2;

import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.cnctrs.BaseConnectorInstance;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.misc.Util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfParameters;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.CategoriesPropType;
import ietf.params.xml.ns.icalendar_2.CreatedPropType;
import ietf.params.xml.ns.icalendar_2.DateDatetimePropertyType;
import ietf.params.xml.ns.icalendar_2.DescriptionPropType;
import ietf.params.xml.ns.icalendar_2.DtendPropType;
import ietf.params.xml.ns.icalendar_2.DtstampPropType;
import ietf.params.xml.ns.icalendar_2.DtstartPropType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.LocationPropType;
import ietf.params.xml.ns.icalendar_2.ProdidPropType;
import ietf.params.xml.ns.icalendar_2.RdatePropType;
import ietf.params.xml.ns.icalendar_2.SummaryPropType;
import ietf.params.xml.ns.icalendar_2.UidPropType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;
import ietf.params.xml.ns.icalendar_2.VersionPropType;
import ietf.params.xml.ns.icalendar_2.VeventType;
import ietf.params.xml.ns.icalendar_2.XBedeworkLocKeyParamType;
import org.apache.http.client.utils.URIBuilder;

import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/** Handles orgsync v2 synch interactions.
 *
 * @author Mike Douglass
 */
public class OrgSyncV2ConnectorInstance
        extends BaseConnectorInstance<OrgSyncV2Connector,
                                      OrgSyncV2SubscriptionInfo,
                                      OrgSyncV2ConnectorConfig> {
  private final ObjectMapper om = new ObjectMapper();

  OrgSyncV2ConnectorInstance(final OrgSyncV2ConnectorConfig config,
                             final OrgSyncV2Connector cnctr,
                             final Subscription sub,
                             final SynchEndType end,
                             final OrgSyncV2SubscriptionInfo info) {
    super(sub, end, info, cnctr, config);
  }

  @Override
  public boolean changed() {
    /*
       OrgSync doesn't support HEAD - we get a 500 error back. We'll
       just do a GET and throw the result away. We'll be back soon
       for the content.
     */
    return changed(false, "application/json");
  }

  /* ====================================================================
   *                   BaseConnectorInstance methods
   * ==================================================================== */

  @Override
  public URI getUri() {
    try {
      //Get yesterdays date
      final LocalDate yesterday = LocalDate.now().minusDays(1);
      final String yesterdayStr =
              yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE);

      final URI infoUri = new URI(info.getUri());
      return new URIBuilder()
              .setScheme(infoUri.getScheme())
              .setHost(infoUri.getHost())
              .setPort(infoUri.getPort())
              .setPath(infoUri.getPath())
              .setParameter("key", cnctr.getSyncher()
                                        .decrypt(info.getPassword()))
              .setParameter("start_date", yesterdayStr)
              .build();
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  @Override
  public IcalendarType makeXcal(final InputStream is) {
    try {
      final List<OrgSyncV2Event> osEvents =
              om.readValue(is, new TypeReference<>() {
              });

        /* Convert each entity to XML */

      return toXcal(osEvents,
                    info.getOrgSyncPublicOnly());
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  @Override
  public boolean getIcal() {
    return getIcal("application/json");
  }

  /* ==============================================================
   *                   Private methods
   * ============================================================== */

  private IcalendarType toXcal(final List<OrgSyncV2Event> osEvents,
                               final boolean onlyPublic) {
    final IcalendarType ical = new IcalendarType();
    final VcalendarType vcal = new VcalendarType();

    ical.getVcalendar().add(vcal);

    vcal.setProperties(new ArrayOfProperties());
    final List<JAXBElement<? extends BasePropertyType>> vcalProps =
            vcal.getProperties().getBasePropertyOrTzid();

    final VersionPropType vers = new VersionPropType();
    vers.setText("2.0");
    vcalProps.add(of.createVersion(vers));

    final ProdidPropType prod = new ProdidPropType();
    prod.setText("//Bedework.org//BedeWork V3.11.1//EN");
    vcalProps.add(of.createProdid(prod));

    final ArrayOfComponents aoc = new ArrayOfComponents();
    vcal.setComponents(aoc);

    for (final OrgSyncV2Event osev: osEvents) {
      if (onlyPublic && !osev.getIsPublic()) {
        continue;
      }

      final VeventType ev = new VeventType();

      aoc.getBaseComponent().add(of.createVevent(ev));

      ev.setProperties(new ArrayOfProperties());
      final List<JAXBElement<? extends BasePropertyType>> evProps =
              ev.getProperties().getBasePropertyOrTzid();

      final UidPropType uid = new UidPropType();
      uid.setText(config.getUidPrefix() + "-" + osev.getId());
      evProps.add(of.createUid(uid));

      final DtstampPropType dtstamp = new DtstampPropType();
      try {
        //Get todays date
        final ZonedDateTime today = ZonedDateTime.now(ZoneOffset.UTC);
        final String todayStr = today.format(DateTimeFormatter.ISO_INSTANT);

        dtstamp.setUtcDateTime(XcalUtil.getXMlUTCCal(todayStr));
        evProps.add(of.createDtstamp(dtstamp));

        final CreatedPropType created = new CreatedPropType();
        created.setUtcDateTime(XcalUtil.getXMlUTCCal(todayStr));
        evProps.add(of.createCreated(created));

        final SummaryPropType sum = new SummaryPropType();
        sum.setText(osev.getName());
        evProps.add(of.createSummary(sum));

        final DescriptionPropType desc = new DescriptionPropType();
        desc.setText(osev.getDescription());
        evProps.add(of.createDescription(desc));

        final LocationPropType l = new LocationPropType();
        l .setText(osev.getLocation());
        evProps.add(of.createLocation(l));

        if (info.getLocationKey() != null) {
          final XBedeworkLocKeyParamType par =
                  of.createXBedeworkLocKeyParamType();

          par.setText(info.getLocationKey());
          l.setParameters(new ArrayOfParameters());
          l.getParameters().getBaseParameter().
                  add(of.createXBedeworkLocKey(par));
        }
      } catch (final Throwable t) {
        error(t);
        continue;
      }

      if (osev.getCategory() != null) {
        final CategoriesPropType cat = new CategoriesPropType();
        cat.getText().add(osev.getCategory().getName());
        evProps.add(of.createCategories(cat));
      }

      /* The first (only) element of occurrences is the start/end of the
         event or master.

         If there are more occurrences these become rdates and the event is
         recurring.
       */

      if (Util.isEmpty(osev.getOccurrences())) {
        // warn?
        continue;
      }

      boolean first = true;
      for (final OrgSyncV2Occurrence occ: osev.getOccurrences()) {
        if (first) {
          final DtstartPropType dtstart =
                  (DtstartPropType)makeDt(new DtstartPropType(),
                                          occ.getStartsAt());
          evProps.add(of.createDtstart(dtstart));

          final DtendPropType dtend =
                  (DtendPropType)makeDt(new DtendPropType(),
                                        occ.getEndsAt());
          evProps.add(of.createDtend(dtend));

          first = false;
          continue;
        }

        // Add an rdate
        // TODO - add duration if different from the master
        final RdatePropType rdate =
                (RdatePropType)makeDt(new RdatePropType(),
                                      occ.getStartsAt());
        evProps.add(of.createRdate(rdate));
      }
    }

    return ical;
  }

  private DateDatetimePropertyType makeDt(final DateDatetimePropertyType dt,
                                          final String val) {
    try {
      final DatatypeFactory dtf = DatatypeFactory.newInstance();

      final XMLGregorianCalendar xgc =
              dtf.newXMLGregorianCalendar(val);
      if (val.length() == 10) {
        dt.setDate(xgc);
        return dt;
      }


      dt.setDateTime(xgc);
      return dt;
    } catch (final DatatypeConfigurationException dce) {
      error(dce);
      return null;
    }
  }
}
