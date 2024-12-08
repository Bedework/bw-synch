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

import org.bedework.synch.shared.filters.Filter;

import java.util.List;

/** Serializable form of information for a connection to a system via a
 * connector - a connector id and the serialized properties.
 *
 * @author douglm
 */
public interface SubscriptionConnectorInfo<T>
        extends SerializableProperties<T> {
  /**
   * @param val id
   */
  void setConnectorId(String val);

  /**
   * @param sub the subscription
   * @return Ordered list of filters
   */
  List<Filter> getInputFilters(Subscription sub);

  /**
   * @param sub the subscription
   * @return Ordered list of filters
   */
  List<Filter> getOutputFilters(Subscription sub);

  /**
   * @return id
   */
  String getConnectorId();
}
