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

import org.bedework.synch.shared.Subscription;
import org.bedework.util.xml.tagdefs.XcalTags;

import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.LocationPropType;
import ietf.params.xml.ns.icalendar_2.TextPropertyType;
import ietf.params.xml.ns.icalendar_2.XBwContactPropType;
import ietf.params.xml.ns.icalendar_2.XBwLocationPropType;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

/** This filter strips out unwanted properties and components.
 *
 * @author douglm
 *
 */
public class XlocXContactFilter extends PropRenameFilter {
  private static final List<RenameElement> renameList =
          new ArrayList<>();

  @Override
  public synchronized void init(final Subscription sub) {
    super.init(sub);

    if (!renameList.isEmpty()) {
      return;
    }

    renameList.add(new RenameElement(XcalTags.location,
                                     XcalTags.xBedeworkLocation,
                                     XBwLocationPropType.class));
    renameList.add(new RenameElement(XcalTags.contact,
                                     XcalTags.xBedeworkContact,
                                     XBwContactPropType.class));
  }

  @Override
  protected List<RenameElement> getRenameList() {
    return renameList;
  }

  @Override
  protected BasePropertyType getNewProperty(final RenameElement rl,
                                            final JAXBElement<? extends BasePropertyType> el) {
    final TextPropertyType tp = (TextPropertyType)el.getValue();
    final TextPropertyType x;
    if (el.getValue() instanceof LocationPropType) {
      x = icalOf.createXBwLocationPropType();
    } else {
      x = icalOf.createXBwContactPropType();
    }

    x.setText(tp.getText());
    x.setParameters(tp.getParameters());

    return x;
  }
}
