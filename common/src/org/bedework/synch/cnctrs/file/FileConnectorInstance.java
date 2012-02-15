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
package org.bedework.synch.cnctrs.file;

import org.bedework.http.client.DavioException;
import org.bedework.http.client.dav.DavClient;
import org.bedework.synch.BaseSubscriptionInfo;
import org.bedework.synch.cnctrs.AbstractConnectorInstance;
import org.bedework.synch.cnctrs.Connector;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import edu.rpi.cmt.calendar.IcalToXcal;
import edu.rpi.cmt.calendar.XcalUtil;
import edu.rpi.sss.util.Util;
import edu.rpi.sss.util.xml.tagdefs.XcalTags;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.FetchItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.LastModifiedPropType;
import ietf.params.xml.ns.icalendar_2.ObjectFactory;
import ietf.params.xml.ns.icalendar_2.ProdidPropType;
import ietf.params.xml.ns.icalendar_2.UidPropType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;
import ietf.params.xml.ns.icalendar_2.VersionPropType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;

/** Handles file synch interactions.
 *
 * @author Mike Douglass
 */
public class FileConnectorInstance extends AbstractConnectorInstance {
  private FileConnectorConfig config;

  private final FileConnector cnctr;

  private FileSubscriptionInfo info;

  private DavClient client;

  /* Only non-null if we actually fetched the data */
  private IcalendarType fetchedIcal;
  private String prodid;

  /* Each entry in the map is the set of entities - master + overrides
   * for a single uid along with some extracted data
   */
  private static class MapEntry {
    List<JAXBElement<? extends BaseComponentType>> comps =
        new ArrayList<JAXBElement<? extends BaseComponentType>>();
    String lastMod;
    String uid;
  }

  private Map<String, MapEntry> uidMap;

  private ObjectFactory of = new ObjectFactory();

  FileConnectorInstance(final FileConnectorConfig config,
                        final FileConnector cnctr,
                        final Subscription sub,
                        final SynchEndType end,
                        final FileSubscriptionInfo info) {
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

    if (info.getChangeToken() == null) {
      fetchedIcal = null; // Force refetch
      return true;
    }

    DavClient cl = getClient();

    try {
      int rc = cl.sendRequest("HEAD", info.getUri(), null);

      if (rc != HttpServletResponse.SC_OK) {
        info.setLastRefreshStatus(String.valueOf(rc));
        if (debug) {
          trace("Unsuccessful response from server was " + rc);
        }
        info.setChangeToken(null);  // Force refresh next time
        fetchedIcal = null; // Force refetch
        return true;
      }

      String etag = cl.getResponse().getResponseHeaderValue("Etag");
      if (etag == null) {
        if (debug) {
          trace("Received null etag");
        }

        return false;
      }

      if (debug) {
        trace("Received etag:" + etag +
              ", ours=" + info.getChangeToken());
      }

      if (info.getChangeToken().equals(etag)) {
        return false;
      }

      fetchedIcal = null; // Force refetch
      return true;
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    } finally {
      try {
        client.release();
      } catch (Throwable t) {
      }
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.synch.ConnectorInstance#getItemsInfo()
   */
  @Override
  public SynchItemsInfo getItemsInfo() throws SynchException {
    SynchItemsInfo sii = new SynchItemsInfo();
    sii.items = new ArrayList<ItemInfo>();
    sii.setStatus(StatusType.OK);

    getIcal();

    if (sub.changed()) {
      cnctr.getSyncher().updateSubscription(sub);
    }

    for (MapEntry me: uidMap.values()) {
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

  /* (non-Javadoc)
   * @see org.bedework.synch.cnctrs.ConnectorInstance#fetchItem(java.lang.String)
   */
  @Override
  public FetchItemResponseType fetchItem(final String uid) throws SynchException {
    getIcal();

    if (sub.changed()) {
      cnctr.getSyncher().updateSubscription(sub);
    }

    MapEntry me = uidMap.get(uid);

    FetchItemResponseType fir = new FetchItemResponseType();

    if (me == null) {
      fir.setStatus(StatusType.NOT_FOUND);
      return fir;
    }

    fir.setHref(info.getUri() + "#" + uid);
    fir.setChangeToken(info.getChangeToken());

    IcalendarType ical = new IcalendarType();
    VcalendarType vcal = new VcalendarType();

    ical.getVcalendar().add(vcal);

    vcal.setProperties(new ArrayOfProperties());
    List<JAXBElement<? extends BasePropertyType>> pl = vcal.getProperties().getBasePropertyOrTzid();

    ProdidPropType prod = new ProdidPropType();
    prod.setText(prodid);
    pl.add(of.createProdid(prod));

    VersionPropType vers = new VersionPropType();
    vers.setText("2.0");
    pl.add(of.createVersion(vers));

    ArrayOfComponents aoc = new ArrayOfComponents();
    vcal.setComponents(aoc);

    aoc.getBaseComponent().addAll(me.comps);
    fir.setIcalendar(ical);

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
    if (config.getReadOnly()) {
      throw new SynchException("Immutable");
    }

    throw new SynchException("Unimplemented");
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private DavClient getClient() throws SynchException {
    if (client != null) {
      return client;
    }

    DavClient cl = null;

    try {
      cl = new DavClient(15 * 1000);

      if (info.getPrincipalHref() != null) {
        cl.setCredentials(info.getPrincipalHref(),
                          cnctr.getSyncher().decrypt(info.getPassword()));
      }

      client = cl;

      return cl;
    } catch (DavioException de) {
      throw new SynchException(de);
    }
  }

  /* Fetch the iCalendar for the subscription. If it fails set the status and
   * return null. Unchanged data will return null with no status change.
   */
  private void getIcal() throws SynchException {
    try {
      if (fetchedIcal != null) {
        return;
      }

      getClient();

      Header[] hdrs = null;

      if ((uidMap != null) && (info.getChangeToken() != null) &&
          (fetchedIcal != null)) {
        hdrs = new Header[] {
          new BasicHeader("If-None-Match", info.getChangeToken())
        };
      }

      int rc = client.sendRequest("GET", info.getUri(), hdrs);
      info.setLastRefreshStatus(String.valueOf(rc));

      if (rc == HttpServletResponse.SC_NOT_MODIFIED) {
        // Data unchanged.
        if (debug) {
          trace("data unchanged");
        }
        return;
      }

      if (rc != HttpServletResponse.SC_OK) {
        info.setLastRefreshStatus(String.valueOf(rc));
        if (debug) {
          trace("Unsuccessful response from server was " + rc);
        }
        info.setChangeToken(null);  // Force refresh next time
        return;
      }

      CalendarBuilder builder = new CalendarBuilder();

      InputStream is = client.getResponse().getContentStream();

      Calendar ical = builder.build(is);

      /* Convert each entity to XML */

      fetchedIcal = IcalToXcal.fromIcal(ical, null);

      uidMap = new HashMap<String, MapEntry>();

      prodid = null;

      for (VcalendarType vcal: fetchedIcal.getVcalendar()) {
        /* Extract the prodid from the converted calendar - we use it when we
         * generate a new icalendar for each entity.
         */
        if ((prodid == null) &&
            (vcal.getProperties() != null)) {
          for (JAXBElement<? extends BasePropertyType> pel:
            vcal.getProperties().getBasePropertyOrTzid()) {
            if (pel.getValue() instanceof ProdidPropType) {
              prodid = ((ProdidPropType)pel.getValue()).getText();
              break;
            }
          }
        }

        for (JAXBElement<? extends BaseComponentType> comp:
             vcal.getComponents().getBaseComponent()) {
          UidPropType uidProp = (UidPropType)XcalUtil.findProperty(comp.getValue(),
                                                                   XcalTags.uid);

          if (uidProp == null) {
            // Should flag as an error
            continue;
          }

          String uid = uidProp.getText();

          MapEntry me = uidMap.get(uid);

          if (me == null) {
            me = new MapEntry();
            me.uid = uid;
            uidMap.put(uidProp.getText(), me);
          }

          LastModifiedPropType lm = (LastModifiedPropType)XcalUtil.findProperty(comp.getValue(),
                                                                                XcalTags.lastModified);

          String lastmod= null;
          if (lm != null) {
            lastmod = lm.getUtcDateTime().toXMLFormat();
          }

          if (Util.cmpObjval(me.lastMod, lastmod) < 0) {
            me.lastMod = lastmod;
          }

          me.comps.add(comp);
        }
      }

      /* Looks like we translated ok. Save any etag and delete everything in the
       * calendar.
       */

      String etag = client.getResponse().getResponseHeaderValue("Etag");
      if (etag != null) {
        info.setChangeToken(etag);
      }
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    } finally {
      try {
        client.release();
      } catch (Throwable t) {
      }
    }
  }
}
