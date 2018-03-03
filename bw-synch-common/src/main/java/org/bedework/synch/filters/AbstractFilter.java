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

import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.misc.Logged;
import org.bedework.util.misc.Util;

import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

/** Provide a base for filters with a number of useful methods,.
 *
 * @author douglm
 *
 */
public abstract class AbstractFilter extends Logged implements Filter {
  protected Subscription sub;

  protected Map<String, Object> stripMap;

  protected AbstractFilter() {
    debug = getLogger().isDebugEnabled();
  }

  protected final static ietf.params.xml.ns.icalendar_2.ObjectFactory icalOf =
          new ietf.params.xml.ns.icalendar_2.ObjectFactory();

  /** Initialise the filter.
   *
   * @param sub the subscription
   * @throws SynchException
   */
  public void init(final Subscription sub) throws SynchException {
    this.sub = sub;
  }

  @Override
  public void addDifferSkipItems(final List<Object> skipList) throws SynchException {
    Collection<Object> stripMapValues = getStripMap().values();

    if (Util.isEmpty(stripMapValues)) {
      return;
    }

    skipList.addAll(stripMapValues);
  }

  protected synchronized Map<String, Object> getStripMap()
          throws SynchException {
    if (stripMap != null) {
      return stripMap;
    }

    stripMap = new HashMap<>();

    return stripMap;
  }

  /* Remove all the properties or components we are not sending to the "to" end.
   */
  protected IcalendarType stripIcal(final IcalendarType val) throws SynchException {
    try {
      final Map<String, Object> stripMap = getStripMap();

      if ((stripMap == null) || stripMap.isEmpty()) {
        return val;
      }

      IcalendarType res = new IcalendarType();

      for (VcalendarType vcal: val.getVcalendar()) {
        VcalendarType v = (VcalendarType)XcalUtil.cloneComponent(vcal);
        res.getVcalendar().add(v);

        v.setProperties(stripProps(stripMap, vcal));
        v.setComponents(stripComps(stripMap, vcal));
      }

      return res;
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  @SuppressWarnings("unchecked")
  private ArrayOfComponents stripComps(final Map<String, Object> stripMap,
                                       final BaseComponentType val) throws SynchException {
    try {
      ArrayOfComponents comps = val.getComponents();

      if (comps == null) {
        return null;
      }

      ArrayOfComponents c = new ArrayOfComponents();

      boolean stripped = false;

      for (JAXBElement jaxbCcomp: comps.getBaseComponent()) {
        BaseComponentType jcomp = (BaseComponentType)jaxbCcomp.getValue();

        /* Skip if we don't want this component */
        if (stripMap.get(jcomp.getClass().getCanonicalName()) != null) {
          stripped = true;
          continue;
        }

        /* Possibly remove some properties */
        ArrayOfProperties p = stripProps(stripMap, jcomp);

        if (jcomp.getProperties() != p) {
          // Some properties were removed
          stripped = true;

          jcomp = XcalUtil.cloneComponent(jcomp);
          jcomp.setProperties(p);

          jaxbCcomp.setValue(jcomp);
        }

        /* Possibly remove some sub-components */
        ArrayOfComponents strippedComps = stripComps(stripMap, jcomp);

        if (jcomp.getComponents() != strippedComps) {
          // Some components were removed
          stripped = true;

          BaseComponentType comp = XcalUtil.cloneComponent(jcomp);
          comp.setProperties(jcomp.getProperties());
          comp.setComponents(strippedComps);

          jaxbCcomp.setValue(comp);
        }

        c.getBaseComponent().add(jaxbCcomp);
      }

      if (stripped) {
        return c;
      }

      return comps;
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  private ArrayOfProperties stripProps(final Map<String, Object> stripMap,
                                       final BaseComponentType val) throws SynchException {
    ArrayOfProperties props = val.getProperties();

    if (props == null) {
      return null;
    }

    ArrayOfProperties p = new ArrayOfProperties();

    boolean stripped = false;

    for (JAXBElement<? extends BasePropertyType> jprop:
            props.getBasePropertyOrTzid()) {
      if (stripMap.get(jprop.getValue().getClass().getCanonicalName()) != null) {
        stripped = true;
        continue;
      }

      //XXX Should do params

      p.getBasePropertyOrTzid().add(jprop);
    }

    if (stripped) {
      return p;
    }

    return props;
  }

  protected void addSkip(final Map<String, Object> skipMap,
                         final Object o) {
    skipMap.put(o.getClass().getCanonicalName(), o);
  }
}
