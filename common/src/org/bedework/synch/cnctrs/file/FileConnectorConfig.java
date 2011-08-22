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
package org.bedework.synch.cnctrs.file;

/** File synch connector config
 *
 * @author douglm
 */
public class FileConnectorConfig {
  private boolean readOnly;

  private int minPoll;

  /** Read only file?
   *
   * @param val    int seconds
   */
  public void setReadOnly(final boolean val) {
    readOnly = val;
  }

  /** Read only file?
   *
   * @return int seconds
   */
  public boolean getReadOnly() {
    return readOnly;
  }

  /** Min poll - seconds
   *
   * @param val    int seconds
   */
  public void setMinPoll(final int val) {
    minPoll = val;
  }

  /** KeepAliveInterval - seconds
   *
   * @return int seconds
   */
  public int getMinPoll() {
    return minPoll;
  }
}
