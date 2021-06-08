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

import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.cnctrs.BaseConnectorInstance;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.util.calendar.IcalToXcal;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.util.CompatibilityHints;
import org.apache.http.client.utils.URIBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Handles file synch interactions.
 *
 * @author Mike Douglass
 */
public class FileConnectorInstance
        extends BaseConnectorInstance<FileConnector,
                                      FileSubscriptionInfo,
                                      FileConnectorConfig> {
  FileConnectorInstance(final FileConnectorConfig config,
                        final FileConnector cnctr,
                        final Subscription sub,
                        final SynchEndType end,
                        final FileSubscriptionInfo info) {
    super(sub, end, info, cnctr, config);
  }

  @Override
  public boolean changed() throws SynchException {
    return changed(true, "calendar/text");
  }

  /* ====================================================================
   *                   BaseConnectorInstance methods
   * ==================================================================== */

  @Override
  public URI getUri() throws SynchException {
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
              .build();
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  public IcalendarType makeXcal(final InputStream is) throws SynchException {
    try {
      final CalendarBuilder builder = new CalendarBuilder();
      CompatibilityHints.setHintEnabled(
              CompatibilityHints.KEY_RELAXED_UNFOLDING,
              true);
        /* Allow unrecognized properties - we'll probably ignore them */
      CompatibilityHints
              .setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING,
                              true);

      final UnfoldingReader ufrdr =
              new UnfoldingReader(new InputStreamReader(is), true);
      final Calendar ical = builder.build(ufrdr);

        /* Convert each entity to XML */

      return IcalToXcal.fromIcal(ical, null, true);
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  @Override
  public boolean getIcal() throws SynchException {
    return getIcal("text/calendar");
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */
}
