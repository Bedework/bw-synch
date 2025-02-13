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

import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/** This filter takes a list of from, to duples. The from is a property
 * name, the to is a new property name.
 *
 * <p>We use this to rename properties, generally as x-properties.</p>
 *
 * @author douglm
 *
 */
public abstract class PropRenameFilter extends AbstractFilter {
  protected static class RenameElement {
    private final QName from;
    private final QName to;
    private final Class<? extends BasePropertyType> toClass;

    public RenameElement(final QName from,
                         final QName to,
                         final Class<? extends BasePropertyType> toClass) {
      this.from = from;
      this.to = to;
      this.toClass = toClass;
    }

    public QName getFrom() {
      return from;
    }

    public QName getTo() {
      return to;
    }

    public Class<? extends BasePropertyType> getToClass() {
      return toClass;
    }
  }

  protected abstract List<RenameElement> getRenameList();

  protected abstract BasePropertyType getNewProperty(RenameElement rl,
                                                     JAXBElement<? extends BasePropertyType> el);

  @Override
  public IcalendarType doFilter(final IcalendarType val) {
    for (final VcalendarType vcal: val.getVcalendar()) {
      doRename(vcal);
    }

    return val;
  }

  private void doRename(final BaseComponentType val) {
    try {
      final ArrayOfComponents comps = val.getComponents();

      if (comps == null) {
        return;
      }

      for (final var jaxbCcomp: comps.getBaseComponent()) {
        final BaseComponentType jcomp = jaxbCcomp.getValue();

        doRenameProps(jcomp);
      }
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  private void doRenameProps(final BaseComponentType val) {
    final ArrayOfProperties props = val.getProperties();

    if (props == null) {
      return;
    }

    final List<JAXBElement<? extends BasePropertyType>> renameProps =
            new ArrayList<>();

    int i = 0;

    final List<JAXBElement<? extends BasePropertyType>> jprops =
            props.getBasePropertyOrTzid();

    buildRenames:
    while (i < jprops.size()) {
      final JAXBElement<? extends BasePropertyType> jprop =
              jprops.get(i);

      for (final RenameElement rl: getRenameList()) {
        if (jprop.getName().equals(rl.getFrom())) {
          renameProps.add(jprop);
          jprops.remove(i);
          continue buildRenames;
        }
      }

      i++;
    }

    doRename:
    for (final JAXBElement<? extends BasePropertyType> el: renameProps) {
      for (final RenameElement rl: getRenameList()) {
        if (el.getName().equals(rl.getFrom())) {
          //noinspection unchecked
          jprops.add(new JAXBElement<>(rl.getTo(),
                                       (Class<BasePropertyType>)rl.getToClass(),
                                       el.getScope(),
                                       getNewProperty(rl, el)));
          continue doRename;
        }
      }
    }

    doRename(val);
  }
}
