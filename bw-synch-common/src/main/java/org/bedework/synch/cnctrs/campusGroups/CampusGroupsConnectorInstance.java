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
package org.bedework.synch.cnctrs.campusGroups;

import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.cnctrs.BaseConnectorInstance;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.util.calendar.IcalToXcal;
import org.bedework.util.misc.Util;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.util.CompatibilityHints;
import org.apache.http.client.utils.URIBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

/** Handles file synch interactions.
 *
 * @author Mike Douglass
 */
public class CampusGroupsConnectorInstance
        extends BaseConnectorInstance<CampusGroupsConnector,
        CampusGroupsSubscriptionInfo,
        CampusGroupsConnectorConfig> {
  CampusGroupsConnectorInstance(final CampusGroupsConnectorConfig config,
                                final CampusGroupsConnector cnctr,
                                final Subscription sub,
                                final SynchEndType end,
                                final CampusGroupsSubscriptionInfo info) {
    super(sub, end, info, cnctr, config);
  }

  @Override
  public boolean changed() {
    return changed(true, "calendar/text");
  }

  /* ====================================================================
   *                   BaseConnectorInstance methods
   * ==================================================================== */

  @Override
  public URI getUri() {
    try {
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

  public IcalendarType makeXcal(final InputStream is) {
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

      /* Categories come in looking like this:
            CATEGORIES;X-CG-CATEGORY=club_acronym:AACC
            CATEGORIES;X-CG-CATEGORY=event_type:Social
            CATEGORIES;X-CG-CATEGORY=event_tags:Movie
          convert them to:
            CATEGORIES:club_acronym/AACC
            CATEGORIES:event_type/Social
            CATEGORIES:event_tags/Movie
       */

      for (final CalendarComponent comp: ical.getComponents()) {
        if (!comp.getName().equals(Component.VEVENT)) {
          continue;
        }

        /* Fix the date time values that should be UTC
         */
        ensureUTC(comp.getProperty(Property.CREATED));
        ensureUTC(comp.getProperty(Property.DTSTAMP));
        ensureUTC(comp.getProperty(Property.LAST_MODIFIED));

        final PropertyList<Property> cats =
                comp.getProperties(Property.CATEGORIES);
        if (Util.isEmpty(cats)) {
          continue;
        }

        for (final Property prop: cats) {
          final Parameter par = prop.getParameter("X-CG-CATEGORY");
          if ((par != null) && (par.getValue() != null)) {
            prop.setValue(par.getValue() + "/" + prop.getValue());
            prop.getParameters().remove(par);
          }
        }
      }

      /* Convert each entity to XML */

      return IcalToXcal.fromIcal(ical, null, true);
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  @Override
  public boolean getIcal() {
    return getIcal("text/calendar");
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private void ensureUTC(final Property p) {
    if (p == null) {
      return;
    }

    final String val= p.getValue();
    if (!val.endsWith("Z")) {
      try {
        p.setValue(val + "Z");
      } catch (final Throwable t) {
        throw new RuntimeException(t);
      }
    }
  }
}
