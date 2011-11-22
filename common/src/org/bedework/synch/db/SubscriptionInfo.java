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
package org.bedework.synch.db;

import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.CalProcessingType;


/** Serializable form of information about the whole subscription.
 *
 * @author douglm
 */
public class SubscriptionInfo extends SerializableProperties<SubscriptionInfo> {
  /* properties saved by connector instance */


  /** Strip out alarms if true */
  public static final String propnameAlarmProcessing = "alarm-processing";

  /** Strip out scheduling properties if true */
  public static final String propnameSchedulingProcessing = "scheduling-processing";

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** AlarmsProcessing - CalProcessingType
   *
   * @param val
   * @throws SynchException
   */
  public void setAlarmsProcessing(final CalProcessingType val) throws SynchException {
    setProperty(propnameAlarmProcessing, String.valueOf(val));
  }

  /** AlarmsProcessing - CalProcessingType
   *
   * @return boolean
   * @throws SynchException
   */
  public CalProcessingType getAlarmsProcessing() throws SynchException {
    return CalProcessingType.fromValue(getProperty(propnameAlarmProcessing));
  }

  /** SchedulingProcessing - CalProcessingType
   *
   * @param val
   * @throws SynchException
   */
  public void setSchedulingProcessing(final CalProcessingType val) throws SynchException {
    setProperty(propnameSchedulingProcessing, String.valueOf(val));
  }

  /** SchedulingProcessing - CalProcessingType
   *
   * @return CalProcessingType
   * @throws SynchException
   */
  public CalProcessingType getSchedulingProcessing() throws SynchException {
    return CalProcessingType.fromValue(getProperty(propnameSchedulingProcessing));
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  @Override
  public int compareTo(final SubscriptionInfo that) {
    if (this == that) {
      return 0;
    }

    try {
      return super.compareTo(that);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public String toString() {
    try {
      StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

      super.toStringSegment(sb, "  ");

      sb.append("}");
      return sb.toString();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

}
