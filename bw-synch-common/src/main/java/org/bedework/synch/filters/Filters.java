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
import org.bedework.synch.shared.filters.Filter;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.List;

/** Methods to help in filtering.
 *
 * @author douglm
 *
 */
public class Filters {
  /** Apply a list of filters.
   *
   * @param val object to filter
   * @param filters the list of filters to apply
   * @return null for discarded object, otherwise possibly modified object
   * @throws SynchException
   */
  public static IcalendarType doFilters(final IcalendarType val,
                                        final List<Filter> filters) {
    if (filters == null) {
      return val;
    }

    IcalendarType theEntity = val;

    for (final Filter fltr: filters) {
      if (theEntity == null) {
        return null;
      }

      theEntity = fltr.doFilter(theEntity);
    }

    return theEntity;
  }

  public static void addDifferSkipItems(final List<Object> skipList,
                                        final List<Filter> filters) {
    if (filters == null) {
      return;
    }

    for (final Filter fltr: filters) {
      fltr.addDifferSkipItems(skipList);
    }
  }
}
