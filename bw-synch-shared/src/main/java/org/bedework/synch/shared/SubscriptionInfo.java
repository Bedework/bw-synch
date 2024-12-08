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
package org.bedework.synch.shared;

import org.bedework.synch.wsmessages.CalProcessingType;

/** Serializable form of information about the whole subscription.
 *
 * @author douglm
 */
public interface SubscriptionInfo<T>
        extends SerializableProperties<T> {
  /* properties saved by connector instance */

  /** Strip out alarms if true */
  String propnameAlarmProcessing = "alarm-processing";

  /** Strip out scheduling properties if true */
  String propnameSchedulingProcessing = "scheduling-processing";

  /** Turn locations and contacts into x-properties.
   * The receiving end may reinstate them as real curated values
   */
  String propnameXlocXcontacts = "xlocxcontacts";

  /** Turn categories into x-properties.
   * The receiving end may reinstate them as real curated values
   */
  String propnameXcategories = "xcategories";

  /** If true suppress deletion of events missing in feed.
   */
  String propnameDeleteSuppressed = "delete-suppressed";

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** AlarmsProcessing - CalProcessingType
   *
   * @param val - CalProcessingType
   */
  void setAlarmsProcessing(CalProcessingType val);

  /** AlarmsProcessing - CalProcessingType
   *
   * @return boolean
   */
  CalProcessingType getAlarmsProcessing();

  /** SchedulingProcessing - CalProcessingType
   *
   * @param val - CalProcessingType
   */
  void setSchedulingProcessing(CalProcessingType val);

  /** SchedulingProcessing - CalProcessingType
   *
   * @return CalProcessingType
   */
  CalProcessingType getSchedulingProcessing();

  /** Processing of locations and contacts - boolean
   *
   * @param val true to enable processing of locations and contacts
   */
  void setXlocXcontact(boolean val);

  /** Processing of locations and contacts - boolean
   *
   * @return boolean
   */
  boolean getXlocXcontact();

  /** Processing of categories - boolean
   *
   * @param val true to enable processing of categories
   */
  void setXlocXcategories(boolean val);

  /** Processing of categories - boolean
   *
   * @return boolean
   */
  boolean getXlocXcategories();

  /** Processing of deletions - boolean
   *
   * @param val true to suppress deletion of missing events
   */
  void setDeletionsSuppressed(boolean val);

  /** Processing of deletions - boolean
   *
   * @return boolean true to suppress deletion of missing events
   */
  boolean getDeletionsSuppressed();
}
