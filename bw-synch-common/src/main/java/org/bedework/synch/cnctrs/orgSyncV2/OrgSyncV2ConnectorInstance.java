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

import org.bedework.synch.shared.BaseSubscriptionInfo;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.cnctrs.AbstractConnectorInstance;
import org.bedework.synch.shared.cnctrs.Connector;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.http.Headers;
import org.bedework.util.http.HttpUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.tagdefs.XcalTags;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfParameters;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.CategoriesPropType;
import ietf.params.xml.ns.icalendar_2.CreatedPropType;
import ietf.params.xml.ns.icalendar_2.DateDatetimePropertyType;
import ietf.params.xml.ns.icalendar_2.DescriptionPropType;
import ietf.params.xml.ns.icalendar_2.DtendPropType;
import ietf.params.xml.ns.icalendar_2.DtstampPropType;
import ietf.params.xml.ns.icalendar_2.DtstartPropType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.LastModifiedPropType;
import ietf.params.xml.ns.icalendar_2.LocationPropType;
import ietf.params.xml.ns.icalendar_2.ObjectFactory;
import ietf.params.xml.ns.icalendar_2.ProdidPropType;
import ietf.params.xml.ns.icalendar_2.RdatePropType;
import ietf.params.xml.ns.icalendar_2.SummaryPropType;
import ietf.params.xml.ns.icalendar_2.UidPropType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;
import ietf.params.xml.ns.icalendar_2.VersionPropType;
import ietf.params.xml.ns.icalendar_2.VeventType;
import ietf.params.xml.ns.icalendar_2.XBedeworkLocKeyParamType;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.DeleteItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.FetchItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemType;

import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/** Handles orgsync v2 synch interactions.
 *
 * @author Mike Douglass
 */
public class OrgSyncV2ConnectorInstance extends AbstractConnectorInstance {
  private final OrgSyncV2ConnectorConfig config;

  private final OrgSyncV2Connector cnctr;

  private final OrgSyncV2SubscriptionInfo info;

  private CloseableHttpClient client;

  /* Only non-null if we actually fetched the data */
  private IcalendarType fetchedIcal;
  private String prodid;

  /* Each entry in the map is the set of entities - master + overrides
   * for a single uid along with some extracted data
   */
  private static class MapEntry {
    List<JAXBElement<? extends BaseComponentType>> comps =
        new ArrayList<>();
    String lastMod;
    String uid;
  }

  private Map<String, MapEntry> uidMap;

  private final ObjectFactory of = new ObjectFactory();
  private final ObjectMapper om = new ObjectMapper();

  OrgSyncV2ConnectorInstance(final OrgSyncV2ConnectorConfig config,
                             final OrgSyncV2Connector cnctr,
                             final Subscription sub,
                             final SynchEndType end,
                             final OrgSyncV2SubscriptionInfo info) {
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

  @Override
  public boolean changed() throws SynchException {
    /* This implementation needs to at least check the change token for the
     * collection and match it against the stored token.
     */

    if (info.getChangeToken() == null) {
      fetchedIcal = null; // Force refetch
      return true;
    }

    /*
       OrgSync doesn't support HEAD - we get a 500 error back. We'll
       just do a GET and throw the result away. We'll be back soon
       for the content.
     */
    try (CloseableHttpResponse hresp =
                 HttpUtil.doGet(getClient(),
                                getUri(),
                                null,
                                "application/json")) {
      final int rc = HttpUtil.getStatus(hresp);

      if (rc != HttpServletResponse.SC_OK) {
        info.setLastRefreshStatus(String.valueOf(rc));
        if (debug) {
          debug("Unsuccessful response from server was " + rc);
        }
        info.setChangeToken(null);  // Force refresh next time
        fetchedIcal = null; // Force refetch
        return true;
      }

      final String etag = HttpUtil.getFirstHeaderValue(hresp, "Etag");
      if (etag == null) {
        if (debug) {
          debug("Received null etag");
        }

        return false;
      }

      if (debug) {
        debug("Received etag:" + etag +
              ", ours=" + info.getChangeToken());
      }

      if (info.getChangeToken().equals(etag)) {
        return false;
      }

      fetchedIcal = null; // Force refetch
      return true;
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  @Override
  public SynchItemsInfo getItemsInfo() throws SynchException {
    final SynchItemsInfo sii = new SynchItemsInfo();
    sii.items = new ArrayList<>();
    sii.setStatus(StatusType.OK);

    if (!getIcal()) {
      sii.setStatus(StatusType.ERROR);
      return sii;
    }

    if (sub.changed()) {
      cnctr.getSyncher().updateSubscription(sub);
    }

    if (uidMap == null) {
      // Possibly the wrong check. We get this if we're unable to fetch the data
      return sii;
    }

    for (final MapEntry me: uidMap.values()) {
      sii.items.add(new ItemInfo(me.uid, me.lastMod,
                                 null));  // lastSynch
    }

    return sii;
  }

  @Override
  public AddItemResponseType addItem(final IcalendarType val) throws SynchException {
    if (config.getReadOnly()) {
      throw new SynchException("Immutable");
    }

    throw new SynchException("Unimplemented");
  }

  @Override
  public FetchItemResponseType fetchItem(final String uid) throws SynchException {
    final FetchItemResponseType fir = new FetchItemResponseType();

    if (!getIcal()) {
      fir.setStatus(StatusType.ERROR);
      return fir;
    }

    if (sub.changed()) {
      cnctr.getSyncher().updateSubscription(sub);
    }

    final MapEntry me = uidMap.get(uid);

    if (me == null) {
      fir.setStatus(StatusType.NOT_FOUND);
      return fir;
    }

    fir.setHref(info.getUri() + "#" + uid);
    fir.setChangeToken(info.getChangeToken());

    final IcalendarType ical = new IcalendarType();
    final VcalendarType vcal = new VcalendarType();

    ical.getVcalendar().add(vcal);

    vcal.setProperties(new ArrayOfProperties());
    final List<JAXBElement<? extends BasePropertyType>> pl =
            vcal.getProperties().getBasePropertyOrTzid();

    final ProdidPropType prod = new ProdidPropType();
    prod.setText(prodid);
    pl.add(of.createProdid(prod));

    final VersionPropType vers = new VersionPropType();
    vers.setText("2.0");
    pl.add(of.createVersion(vers));

    final ArrayOfComponents aoc = new ArrayOfComponents();
    vcal.setComponents(aoc);

    aoc.getBaseComponent().addAll(me.comps);
    fir.setIcalendar(ical);

    return fir;
  }

  @Override
  public List<FetchItemResponseType> fetchItems(final List<String> uids) throws SynchException {
    // XXX this should be a search for multiple uids - need to reimplement caldav search

    final List<FetchItemResponseType> firs = new ArrayList<>();

    for (final String uid: uids) {
      firs.add(fetchItem(uid));
    }

    return firs;
  }

  @Override
  public UpdateItemResponseType updateItem(final UpdateItemType updates) throws SynchException {
    if (config.getReadOnly()) {
      throw new SynchException("Immutable");
    }

    throw new SynchException("Unimplemented");
  }

  @Override
  public DeleteItemResponseType deleteItem(final String uid)
          throws SynchException {
    return null;
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private CloseableHttpClient getClient() throws SynchException {
    if (client != null) {
      return client;
    }

    final CloseableHttpClient cl = HttpClients.createDefault();

    final HttpClientContext context = HttpClientContext.create();
    if (info.getPrincipalHref() != null) {
      final CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(
              new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
              new UsernamePasswordCredentials(info.getPrincipalHref(),
                                              cnctr.getSyncher().decrypt(info.getPassword())));
      context.setCredentialsProvider(credsProvider);
    }

    client = cl;

    return cl;
  }

  private URI getUri() throws SynchException {
    try {
      //Get yesterdays date
      final LocalDate yesterday = LocalDate.now().minus(1, ChronoUnit.DAYS);
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

  /* Fetch the iCalendar for the subscription. If it fails set the status and
   * return false. Unchanged data will return true with no status change.
   *
   */
  private boolean getIcal() throws SynchException {
    try {
      if (fetchedIcal != null) {
        return true;
      }

      final Headers hdrs;

      if ((uidMap != null) && (info.getChangeToken() != null) &&
              (fetchedIcal != null)) {
        hdrs = new Headers();
        hdrs.add("If-None-Match", info.getChangeToken());
      } else {
        hdrs = null;
      }

      try (CloseableHttpResponse hresp = HttpUtil.doGet(getClient(),
                                                 getUri(),
                                                 hdrs,
                                                 "application/json")) {
        final int rc = HttpUtil.getStatus(hresp);

        info.setLastRefreshStatus(String.valueOf(rc));

        if (rc == HttpServletResponse.SC_NOT_MODIFIED) {
          // Data unchanged.
          if (debug) {
            debug("data unchanged");
          }
          return true;
        }

        if (rc != HttpServletResponse.SC_OK) {
          if (debug) {
            debug("Unsuccessful response from server was " + rc);
          }
          info.setLastRefreshStatus(String.valueOf(rc));
          info.setChangeToken(null);  // Force refresh next time
          return false;
        }

        final InputStream is = hresp.getEntity().getContent();

        final List<OrgSyncV2Event> osEvents =
                om.readValue(is, new TypeReference<List<OrgSyncV2Event>>(){});

        /* Convert each entity to XML */

        fetchedIcal = toXcal(osEvents,
                             info.getOrgSyncPublicOnly());

        uidMap = new HashMap<>();

        prodid = null;

        for (final VcalendarType vcal: fetchedIcal.getVcalendar()) {
          /* Extract the prodid from the converted calendar - we use it when we
           * generate a new icalendar for each entity.
           */
          if ((prodid == null) &&
                  (vcal.getProperties() != null)) {
            for (final JAXBElement<? extends BasePropertyType> pel :
                    vcal.getProperties().getBasePropertyOrTzid()) {
              if (pel.getValue() instanceof ProdidPropType) {
                prodid = ((ProdidPropType)pel.getValue()).getText();
                break;
              }
            }
          }

          for (final JAXBElement<? extends BaseComponentType> comp :
                  vcal.getComponents().getBaseComponent()) {
            final UidPropType uidProp = (UidPropType)XcalUtil
                    .findProperty(
                            comp.getValue(),
                            XcalTags.uid);

            if (uidProp == null) {
              // Should flag as an error
              continue;
            }

            final String uid = uidProp.getText();

            MapEntry me = uidMap.get(uid);

            if (me == null) {
              me = new MapEntry();
              me.uid = uid;
              uidMap.put(uidProp.getText(), me);
            }

            final LastModifiedPropType lm =
                    (LastModifiedPropType)XcalUtil
                            .findProperty(comp.getValue(),
                                          XcalTags.lastModified);

            String lastmod = null;
            if (lm != null) {
              lastmod = lm.getUtcDateTime().toXMLFormat();
            }

            if (Util.cmpObjval(me.lastMod, lastmod) < 0) {
              me.lastMod = lastmod;
            }

            me.comps.add(comp);
          }
        }

        /* Looks like we translated ok. Save any etag.
         */

        final String etag = HttpUtil.getFirstHeaderValue(hresp, "Etag");
        if (etag != null) {
          info.setChangeToken(etag);
        }
      } // try

      return true;
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

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
