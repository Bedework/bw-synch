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


/** Serializable form of information about the whole subscription.
 *
 * @author douglm
 */
public class SubscriptionInfo extends SerializableProperties<SubscriptionInfo> {
  /* properties saved by connector instance */


  /** Strip out alarms if true */
  public static final String propnameStripAlarms = "strip-alarms";

  /** Strip out scheduling properties if true */
  public static final String propnameStripScheduling = "strip-scheduling";

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** stripAlarms - boolean
   *
   * @param val
   * @throws SynchException
   */
  public void setStripAlarms(final boolean val) throws SynchException {
    setProperty(propnameStripAlarms, String.valueOf(val));
  }

  /** stripAlarms - boolean
   *
   * @return boolean
   * @throws SynchException
   */
  public boolean getStripAlarms() throws SynchException {
    return Boolean.parseBoolean(getProperty(propnameStripAlarms));
  }

  /** stripScheduling - boolean
   *
   * @param val
   * @throws SynchException
   */
  public void setStripScheduling(final boolean val) throws SynchException {
    setProperty(propnameStripScheduling, String.valueOf(val));
  }

  /** stripScheduling - boolean
   *
   * @return boolean
   * @throws SynchException
   */
  public boolean getStripScheduling() throws SynchException {
    return Boolean.parseBoolean(getProperty(propnameStripScheduling));
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
