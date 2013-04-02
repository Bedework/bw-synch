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

import org.bedework.synch.cnctrs.ConnectorConfigWrapper;
import org.bedework.synch.conf.ConnectorConfig;

import edu.rpi.sss.util.ToString;

import javax.xml.namespace.QName;

/** File synch connector config
 *
 * @author douglm
 */
public class FileConnectorConfig
  extends ConnectorConfigWrapper<FileConnectorConfig> {
  /** Min polling interval - seconds */
  private static final QName propMinPoll = new QName(ns, "minPoll");

  /**
   * @param conf
   */
  public FileConnectorConfig(final ConnectorConfig conf) {
    super(conf);
  }

  /** Min poll - seconds
   *
   * @param val    int seconds
   */
  public void setMinPoll(final int val) {
    unwrap().setIntegerProperty(propMinPoll, val);
  }

  /** Min poll - seconds
   *
   * @return int seconds
   */
  public int getMinPoll() {
    return unwrap().getIntegerPropertyValue(propMinPoll);
  }

  @Override
  public void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);

    ts.append("propMinPoll", getMinPoll());
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
