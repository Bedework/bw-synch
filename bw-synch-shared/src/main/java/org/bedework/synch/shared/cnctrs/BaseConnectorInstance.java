/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.synch.shared.cnctrs;

import org.bedework.synch.shared.BaseSubscriptionInfo;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.conf.ConnectorConfigI;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.http.Headers;
import org.bedework.util.http.HttpUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.tagdefs.XcalTags;

import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.LastModifiedPropType;
import ietf.params.xml.ns.icalendar_2.ObjectFactory;
import ietf.params.xml.ns.icalendar_2.ProdidPropType;
import ietf.params.xml.ns.icalendar_2.TextPropertyType;
import ietf.params.xml.ns.icalendar_2.UidPropType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;
import ietf.params.xml.ns.icalendar_2.VersionPropType;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.DeleteItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.FetchItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemType;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * User: mike Date: 3/12/18 Time: 21:32
 * Basic implementation with some commonly used methods.
 */
public abstract class BaseConnectorInstance<CnctrT extends AbstractConnector,
        InfoT extends BaseSubscriptionInfo,
        ConfigT extends ConnectorConfigI>
        extends AbstractConnectorInstance<CnctrT, InfoT, ConfigT> {
  /* Only non-null if we actually fetched the data */
  protected IcalendarType fetchedIcal;

  protected String prodid;

  protected final ObjectFactory of = new ObjectFactory();

  /* Each entry in the map is the set of entities - master + overrides
   * for a single uid along with some extracted data
   */
  public static class MapEntry {
    public List<JAXBElement<? extends BaseComponentType>> comps =
            new ArrayList<>();
    public String lastMod;
    public String uid;
  }

  protected Map<String, MapEntry> uidMap;

  protected BaseConnectorInstance(final Subscription sub,
                                  final SynchEndType end,
                                  final InfoT info,
                                  final CnctrT cnctr,
                                  final ConfigT config) {
    super(sub, end, info, cnctr, config);
  }

  public abstract URI getUri() throws SynchException;

  public abstract IcalendarType makeXcal(final InputStream is) throws SynchException;

  /* Fetch the iCalendar for the subscription. If it fails set the status and
   * return false. Unchanged data will return true with no status change.
   */
  public abstract boolean getIcal() throws SynchException;

    @Override
  public AddItemResponseType addItem(final IcalendarType val) throws SynchException {
    if (config.getReadOnly()) {
      throw new SynchException("Immutable");
    }

    throw new SynchException("Unimplemented");
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

  protected boolean changed(final boolean headSupported,
                            final String contentType) throws SynchException {
    /* This implementation needs to at least check the change token for the
     * collection and match it against the stored token.
     */

    if (info.getChangeToken() == null) {
      fetchedIcal = null; // Force refetch
      return true;
    }

    try (final CloseableHttpResponse hresp =
                 getChangedResponse(headSupported,
                                    contentType)) {
      final int rc = HttpUtil.getStatus(hresp);

      if (rc != HttpServletResponse.SC_OK) {
        info.setLastRefreshStatus(String.valueOf(rc));
        if (debug()) {
          debug("Unsuccessful response from server was " + rc);
        }
        info.setChangeToken(null);  // Force refresh next time
        fetchedIcal = null; // Force refetch
        return true;
      }

      final String ctoken = getHttpChangeToken(hresp);
      if (ctoken == null) {
        if (debug()) {
          debug("Received null change token");
        }

        return false;
      }

      if (debug()) {
        debug("Received change token:" + ctoken +
                      ", ours=" + info.getChangeToken());
      }

      if (info.getChangeToken().equals(ctoken)) {
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

  private CloseableHttpResponse getChangedResponse(
          final boolean headSupported,
          final String contentType) throws SynchException {
    try {
      if (headSupported) {
        return HttpUtil.doHead(getClient(),
                               getUri(),
                               null,
                               contentType);
      }

      return HttpUtil.doGet(getClient(),
                            getUri(),
                            null,
                            contentType);
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  public <T extends TextPropertyType> String getText(final Class<T> cl,
                                                      final JAXBElement<? extends BaseComponentType> comp,
                                                      final QName tag) {
    final T pt = (T)XcalUtil.findProperty(comp.getValue(), tag);

    if (pt == null) {
      return null;
    }

    return pt.getText();
  }

  /* Fetch the iCalendar for the subscription. If it fails set the status and
   * return false. Unchanged data will return true with no status change.
   *
   */
  protected boolean getIcal(final String contentType) throws SynchException {
    try {
      if (fetchedIcal != null) {
        return true;
      }

      try (final CloseableHttpResponse hresp =
                   HttpUtil.doGet(getClient(),
                                  getUri(),
                                  this::getHeaders,
                                  "text/calendar")) {
        final int rc = HttpUtil.getStatus(hresp);

        info.setLastRefreshStatus(String.valueOf(rc));

        if (rc == HttpServletResponse.SC_NOT_MODIFIED) {
          // Data unchanged.
          if (debug()) {
            debug("data unchanged");
          }
          return true;
        }

        if (rc != HttpServletResponse.SC_OK) {
          if (debug()) {
            debug("Unsuccessful response from server was " + rc);
          }
          info.setLastRefreshStatus(String.valueOf(rc));
          info.setChangeToken(null);  // Force refresh next time
          return false;
        }

        fetchedIcal = makeXcal(hresp.getEntity().getContent());

        uidMap = new HashMap<>();

        prodid = null;

        for (final VcalendarType vcal: fetchedIcal.getVcalendar()) {
          /* Extract the prodid from the converted calendar - we use it when we
         * generate a new icalendar for each entity.
         */
          if ((prodid == null) &&
                  (vcal.getProperties() != null)) {
            for (final JAXBElement<? extends BasePropertyType> pel:
                    vcal.getProperties().getBasePropertyOrTzid()) {
              if (pel.getValue() instanceof ProdidPropType) {
                prodid = ((ProdidPropType)pel.getValue()).getText();
                break;
              }
            }
          }

          for (final JAXBElement<? extends BaseComponentType> comp:
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

        /* Looks like we translated ok. Save any etag.
        */

        final String ctoken = getHttpChangeToken(hresp);
        if (ctoken != null) {
          info.setChangeToken(ctoken);
        }
      } // try

      return true;
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  /* Might be an etag - might be last-modified

   */
  public final String getHttpChangeToken(final CloseableHttpResponse hresp) {
    final String etag = HttpUtil.getFirstHeaderValue(hresp, "Etag");
    if (etag != null) {
      return etag;
    }

    return HttpUtil.getFirstHeaderValue(hresp, "Last-modified");
  }

  private Headers getHeaders() {
    final Headers hdrs = new Headers();
    final String changeToken;

    try {
      changeToken = info.getChangeToken();
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }

    if ((uidMap != null) && (changeToken != null) &&
            (fetchedIcal != null)) {
      hdrs.add("If-None-Match", changeToken);
    }

    hdrs.add("User-Agent", "Bedework Calendar System");

    return hdrs;
  }
}
