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

import org.bedework.http.client.dav.DavClient;
import org.bedework.synch.Subscription;
import org.bedework.synch.SynchDefs.SynchEnd;
import org.bedework.synch.cnctrs.ConnectorInstance;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SubscribeResponseType;

import edu.rpi.cmt.calendar.IcalToXcal;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.httpclient.Header;
import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.BaseResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/** Calls from exchange synch processor to the service.
 *
 * @author Mike Douglass
 */
public class FileConnectorInstance implements ConnectorInstance {
  private transient Logger log;

  private final boolean debug;

  private FileConnectorConfig config;

  private final FileConnector cnctr;

  private FileSubscriptionInfo info;

  private final Subscription sub;

  private SynchEnd end;

  /* Only non-null if we actually fetched the data */
  private IcalendarType fetchedIcal;

  FileConnectorInstance(final FileConnectorConfig config,
                            final FileConnector cnctr,
                            final Subscription sub,
                            final SynchEnd end,
                            final FileSubscriptionInfo info) {
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

    return items;
  }

  @Override
  public AddItemResponseType addItem(final IcalendarType val) throws SynchException {
    if (config.getReadOnly()) {
      throw new SynchException("Immutable");
    }

    throw new SynchException("Unimplemented");
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
    return null;
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

  /* Fetch the iCalendar for the subscription. If it fails set the status and
   * return null. Unchanged data will return null with no status change.
   */
  private void getIcal() throws SynchException {
    DavClient cl = null;

    try {
      cl = new DavClient(info.getUri(),
                         15 * 1000);   // 15 seconds timeout

      if (info.getPrincipalHref() != null) {
        cl.setCredentials(info.getPrincipalHref(),
                          cnctr.getSyncher().decrypt(info.getPassword()));
      }

      Header[] hdrs = null;

      if (info.getEtag() != null) {
        hdrs = new Header[] {
          new Header("If-None-Match", info.getEtag())
        };
      }

      int rc = cl.sendRequest("GET", info.getUri(), hdrs);
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
        info.setEtag(null);  // Force refresh next time
        return;
      }

      CalendarBuilder builder = new CalendarBuilder();

      InputStream is = cl.getResponse().getContentStream();

      Calendar ical = builder.build(is);

      /* Convert each entity to XML */

      fetchedIcal = IcalToXcal.fromIcal(ical, null);

      /* Looks like we translated ok. Save any etag and delete everything in the
       * calendar.
       */

      Header etag = cl.getResponse().getResponseHeader("Etag");
      if (etag != null) {
        info.setEtag(etag.getValue());
      }

//      fetchedIcal = ic;
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    } finally {
      try {
        cl.release();
      } catch (Throwable t) {
      }
    }
  }
}
