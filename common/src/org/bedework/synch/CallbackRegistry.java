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

import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

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
    private String connectorId;

    private Subscription sub;

    private SynchEndType end;

    /**
     * @param connectorId
     * @param sub
     * @param end
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
      StringBuilder sb = new StringBuilder("CallbackRegistryEntry{");

      sb.append("connectorId=");
      sb.append(getConnectorId());

      sb.append("sub=");
      sb.append(getSub());

      sb.append(", end=");
      sb.append(getEnd());

      sb.append("}");

      return sb.toString();
    }
  }

  private Map<String, CallbackRegistryEntry> theMap =
      new HashMap<String, CallbackRegistryEntry>();

  /** null constructor
   *
   */
  public CallbackRegistry() {
  }

  /**
   * @param connectorId
   * @return entry or null for none.
   */
  public CallbackRegistryEntry get(final String connectorId) {
    return theMap.get(connectorId);
  }

  /** Add an entry to the registry. If it's already there we throw an exception.
   * Each callback must be unique and unchanging.
   *
   * @param connectorId
   * @param val
   * @throws SynchException
   */
  public synchronized void put(final String connectorId,
                               final CallbackRegistryEntry val) throws SynchException {
    CallbackRegistryEntry tblVal = get(connectorId);

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
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    sb.append("theMap = ");
    sb.append(theMap);

    sb.append("}");
    return sb.toString();
  }
}
