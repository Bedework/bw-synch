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
package org.bedework.synch.cnctrs;

import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import java.util.HashMap;
import java.util.Map;


/** A map for use by Connectors.
 *
 * @author Mike Douglass
 *
 * @param <CI>
 */
public class ConnectorInstanceMap<CI extends ConnectorInstance> {
  static class Key {
    Subscription sub;

    SynchEndType end;

    Key(final Subscription sub,
        final SynchEndType end) {
      this.sub = sub;
      this.end = end;
    }

    @Override
    public int hashCode() {
      return sub.hashCode() * end.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      Key that = (Key)o;

      if (that.end != end) {
        return false;
      }

      return sub.equals(that.sub);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

      sb.append("sub=");
      sb.append(sub);

      sb.append(", end=");
      sb.append(end);

      sb.append("}");
      return sb.toString();
    }
  }

  private Map<Key, CI> theMap = new HashMap<Key, CI>();

  /** Add a connector
   *
     * @param sub
     * @param end
     * @param cinst
   * @throws SynchException
   */
  public synchronized void add(final Subscription sub,
                               final SynchEndType end,
                               final CI cinst) throws SynchException {
    Key key = new Key(sub, end);

    if (theMap.containsKey(key)) {
      throw new SynchException("instance already in map for " + key);
    }

    theMap.put(key, cinst);
  }

  /** Find a connector
   *
   * @param sub
   * @param end
   * @return CI or null
   * @throws SynchException
   */
  public synchronized CI find(final Subscription sub,
                              final SynchEndType end) throws SynchException {
    return theMap.get(new Key(sub, end));
  }


  /** Remove a connector
   *
   * @param sub
   * @param end
   * @throws SynchException
   */
  public synchronized void remove(final Subscription sub,
                                  final SynchEndType end) throws SynchException {
    theMap.remove(new Key(sub, end));
  }
}
