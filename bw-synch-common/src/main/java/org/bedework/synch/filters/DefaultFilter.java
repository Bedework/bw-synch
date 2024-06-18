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
package org.bedework.synch.filters;

import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.shared.filters.AbstractFilter;
import org.bedework.synch.wsmessages.CalProcessingType;

import ietf.params.xml.ns.icalendar_2.AttendeePropType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.MethodPropType;
import ietf.params.xml.ns.icalendar_2.OrganizerPropType;
import ietf.params.xml.ns.icalendar_2.ValarmType;

import java.util.HashMap;
import java.util.Map;

/** This filter strips out unwanted properties and components.
 *
 * @author douglm
 *
 */
public class DefaultFilter extends AbstractFilter {
  @Override
  public IcalendarType doFilter(final IcalendarType val) {
    return stripIcal(val);
  }

  @Override
  protected synchronized Map<String, Object> getStripMap()
          throws SynchException {
    if (stripMap != null) {
      return stripMap;
    }

    stripMap = new HashMap<>();

    addSkip(stripMap, new MethodPropType());

    if (sub.getInfo() == null) {
      // No special instructions
      return stripMap;
    }

    /* Any needed for stuff we skip */
    if (sub.getInfo().getAlarmsProcessing() == CalProcessingType.REMOVE) {
      addSkip(stripMap, new ValarmType());
    }

    if (sub.getInfo().getSchedulingProcessing() == CalProcessingType.REMOVE) {
      addSkip(stripMap, new OrganizerPropType());
      addSkip(stripMap, new AttendeePropType());
    }

    return stripMap;
  }
}
