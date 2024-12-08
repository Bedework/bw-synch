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

import org.bedework.synch.wsmessages.ArrayOfSynchProperties;

/** Serializable form of information for a connection to a system via a
 * connector - a connector id and the serialized proeprties.
 *
 * @param <T>
 */
public interface SerializableProperties<T> extends Comparable<T> {
  ArrayOfSynchProperties getAllSynchProperties();

  /**
   * @param val serialized properties
   */
  void setSynchProperties(String val);

  /**
   * @return serialized properties
   */
  String getSynchProperties();

  /** Set the changed flag
   *
   * @param val the changed flag
   */
  void setChanged(boolean val);

  /**
   * @return changed flag.
   */
  boolean getChanged();

  /**
   * reset the changed flag.
   */
  void resetChanged();

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /** Load the properties from the serialized form.
   *
   */
  void loadProperties();

  /** Set a property in the internal properties - loading them from the
   * external value first if necessary.
   *
   * @param name of property
   * @param val new value
   */
  void setProperty(String name,
                   String val);

  /** Get a property from the internal properties - loading them from the
   * external value first if necessary.
   *
   * @param name of property
   * @return val
   */
  String getProperty(String name);
}
