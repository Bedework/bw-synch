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
package org.bedework.synch.shared.filters;

import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.exception.SynchException;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

import java.util.List;

/** Given an iCalendar object applies some sort of transform to the
 * object. This may involve adding, removing or changing properties.
 *
 * <p>Standard filters will carry out global processing. Other filters
 * may be defined which are specific to a connection or item type.</p>
 *
 * <p>Each input filter will be called when items are read to be added.</p>
 *
 * <p>When comparing items the addDifferSkipItems will be called to build a
 * list of properties, components etc to be ignored when comparing. There
 * is no need to transform either entity as the compare result will
 * not take tthe ignored fields into account.</p>
 *
 * @author douglm
 *
 */
public interface Filter {
  /** Initialise the filter.
   *
   * @param sub the subscription
   * @throws SynchException on fatal error
   */
  void init(final Subscription sub) throws SynchException;

  /**
   * @param val the object to process
   * @return null for discarded object, otherwise possibly modified object
   * @throws SynchException on fatal error
   */
  IcalendarType doFilter(final IcalendarType val) throws SynchException;

  /** Called to add items to the skip list for the ical differ.
   *
   * @param skipList list of items to skip - properties parameters etc
   * @throws SynchException on fatal error
   */
  void addDifferSkipItems(final List<Object> skipList) throws SynchException;
}
