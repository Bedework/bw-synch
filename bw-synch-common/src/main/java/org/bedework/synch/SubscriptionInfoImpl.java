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
package org.bedework.synch;

import org.bedework.synch.db.SerializablePropertiesImpl;
import org.bedework.synch.shared.SubscriptionInfo;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.CalProcessingType;
import org.bedework.util.misc.ToString;

/** Serializable form of information about the whole subscription.
 *
 * @author douglm
 */
public class SubscriptionInfoImpl
        extends SerializablePropertiesImpl<SubscriptionInfoImpl>
        implements SubscriptionInfo<SubscriptionInfoImpl> {
  /* properties saved by connector instance */

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** AlarmsProcessing - CalProcessingType
   *
   * @param val
   * @throws SynchException
   */
  public void setAlarmsProcessing(final CalProcessingType val) {
    setProperty(propnameAlarmProcessing, String.valueOf(val));
  }

  /** AlarmsProcessing - CalProcessingType
   *
   * @return boolean
   * @throws SynchException
   */
  public CalProcessingType getAlarmsProcessing() {
    return CalProcessingType.fromValue(getProperty(propnameAlarmProcessing));
  }

  /** SchedulingProcessing - CalProcessingType
   *
   * @param val
   * @throws SynchException
   */
  public void setSchedulingProcessing(final CalProcessingType val) {
    setProperty(propnameSchedulingProcessing, String.valueOf(val));
  }

  /** SchedulingProcessing - CalProcessingType
   *
   * @return CalProcessingType
   * @throws SynchException
   */
  public CalProcessingType getSchedulingProcessing() {
    return CalProcessingType.fromValue(getProperty(propnameSchedulingProcessing));
  }

  /** Processing of locations and contacts - boolean
   *
   * @param val true to enable processing of locations and contacts
   * @throws SynchException
   */
  public void setXlocXcontact(final boolean val) {
    setProperty(propnameXlocXcontacts, String.valueOf(val));
  }

  public boolean getXlocXcontact() {
    return Boolean.valueOf(getProperty(propnameXlocXcontacts));
  }

  public void setXlocXcategories(final boolean val) {
    setProperty(propnameXcategories, String.valueOf(val));
  }

  public boolean getXlocXcategories() {
    return Boolean.valueOf(getProperty(propnameXcategories));
  }

  /** Processing of deletions - boolean
   *
   * @param val true to suppress deletion of missing events
   * @throws SynchException
   */
  public void setDeletionsSuppressed(final boolean val) {
    setProperty(propnameDeleteSuppressed, String.valueOf(val));
  }

  /** Processing of deletions - boolean
   *
   * @return boolean true to suppress deletion of missing events
   * @throws SynchException
   */
  public boolean getDeletionsSuppressed() {
    return Boolean.valueOf(getProperty(propnameDeleteSuppressed));
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  @Override
  public int compareTo(final SubscriptionInfoImpl that) {
    if (this == that) {
      return 0;
    }

    try {
      return doCompare(that);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public String toString() {
    try {
      ToString ts = new ToString(this);

      super.toStringSegment(ts);

      return ts.toString();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

}
