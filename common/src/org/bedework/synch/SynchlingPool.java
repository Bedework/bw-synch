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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/** manage a pool of synchlings.
 *
 * @author Mike Douglass
 *
 */
public class SynchlingPool {
  private SynchEngine syncher;

  private ArrayBlockingQueue<Synchling> pool;

  private long timeout; // millisecs

  private long waitTimes;

  private long gets;

  private long getSynchlingFailures;

  /** Create a pool with the given size
   *
   * @param syncher
   * @param size
   * @param timeout - millisecs
   * @throws SynchException
   */
  public void start(final SynchEngine syncher,
                    final int size,
                    final long timeout) throws SynchException {
    this.syncher = syncher;
    this.timeout = timeout;
    resize(size);
  }

  /** Resize the pool
   *
   * @param size
   * @throws SynchException
   */
  public void resize(final int size) throws SynchException {
    ArrayBlockingQueue<Synchling> oldPool = getPool();
    pool = new ArrayBlockingQueue<Synchling>(size);
    int oldSize = 0;

    if (oldPool != null) {
      oldSize = oldPool.size();
      pool.drainTo(oldPool, Math.max(size, oldSize));
    }

    while (size > oldSize) {
      pool.add(new Synchling(syncher));
      oldSize++;
    }
  }

  /**
   * @param val timeout in millisecs
   */
  public void setTimeout(final long val) {
    timeout = val;
  }

  /**
   * @return timeout in millisecs
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * @return total waitTimes in millisecs
   */
  public long getWaitTimes() {
    return waitTimes;
  }

  /**
   * @return number of gets
   */
  public long getGets() {
    return gets;
  }

  /**
   * @return number of get failures
   */
  public long getGetSynchlingFailures() {
    return getSynchlingFailures;
  }

  /**
   * @return current size of pool
   */
  public int getCurrentMaxSize() {
    ArrayBlockingQueue<Synchling> thePool = pool;
    if (thePool == null) {
      return 0;
    }

    return thePool.size();
  }

  /** Return approximate number of available synchlings
   *
   * @return current avail
   */
  public int getCurrentAvailable() {
    ArrayBlockingQueue<Synchling> thePool = pool;
    if (thePool == null) {
      return 0;
    }

    return thePool.size() - thePool.remainingCapacity();
  }

  /** Put a synchling back in the pool if there's room else discard it
   *
   * @param s
   * @throws SynchException
   */
  public void add(final Synchling s) throws SynchException {
    getPool().offer(s);
  }

  /** Get a synchling from the pool if possible
   *
   * @return a sychling
   * @throws SynchException if none available
   */
  public Synchling get() throws SynchException {
    Synchling s = null;
    try {
      s = getPool().poll(getTimeout(), TimeUnit.MILLISECONDS);
    } catch (Throwable t) {
      throw new SynchException(t);
    }

    if (s == null) {
      throw new SynchTimeout("Synchling pool wait");
    }

    return s;
  }

  private synchronized ArrayBlockingQueue<Synchling> getPool() {
    return pool;
  }
}
