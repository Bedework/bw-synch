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

import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.util.misc.ToString;

import java.util.HashMap;
import java.util.Map;

/** Registry of active callbacks. When we receive a call back into the web
 * interface, we strip off the context element and the remainder is a key into
 * this registry.
 *
 * <p>The saved or retrieved CallbackRegistryEntry has a connector id, a
 * subscription and a flag indicating which end of the subscription called back.
 * For example the full path might be:
 *
 * <p>/synchcb/exchg/1234567890/
 *
 * <p>and we remove "/synchcb/" to get "exchg/1234567890/".
 *
 * @author Mike Douglass
 */
@SuppressWarnings("rawtypes")
public class CallbackRegistry {
  /** An entry in the registry
   *
   * @author douglm
   */
  public static class CallbackRegistryEntry {
    private final String connectorId;

    private final Subscription sub;

    private final SynchEndType end;

    /**
     * @param connectorId id of connector
     * @param sub subscription
     * @param end which end
     */
    public CallbackRegistryEntry(final String connectorId,
                                 final Subscription sub,
                                 final SynchEndType end) {
      this.connectorId = connectorId;
      this.sub = sub;
      this.end = end;
    }

    /**
     * @return Connector id
     */
    public String getConnectorId() {
      return connectorId;
    }

    /**
     * @return Subscription
     */
    public Subscription getSub() {
      return sub;
    }

    /**
     * @return end designator
     */
    public SynchEndType getEnd() {
      return end;
    }

    @Override
    public String toString() {
      return new ToString(this)
              .append("connectorId", getConnectorId())
              .append("sub", getSub())
              .append("end", getEnd())
              .toString();
    }
  }

  private final Map<String, CallbackRegistryEntry> theMap =
          new HashMap<>();

  /** null constructor
   *
   */
  public CallbackRegistry() {
  }

  /**
   * @param connectorId id of connector
   * @return entry or null for none.
   */
  public CallbackRegistryEntry get(final String connectorId) {
    return theMap.get(connectorId);
  }

  /** Add an entry to the registry. If it's already there we throw an exception.
   * Each callback must be unique and unchanging.
   *
   * @param connectorId id
   * @param val entry to add
   */
  public synchronized void put(final String connectorId,
                               final CallbackRegistryEntry val) {
    final CallbackRegistryEntry tblVal = get(connectorId);

    if (tblVal != null) {
      throw new SynchException("Entry already in registry." +
                               " Tried to add" + val +
                               " found " + tblVal);
    }

    put(connectorId, val);
  }

  /* ====================================================================
   *                        Object methods
   * ==================================================================== */

  @Override
  public String toString() {
    final StringBuilder sb =
            new StringBuilder(getClass().getSimpleName()).append("{");

    sb.append("theMap = ");
    sb.append(theMap);

    sb.append("}");
    return sb.toString();
  }
}
