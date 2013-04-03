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


import org.bedework.synch.exception.SynchException;
import org.bedework.synch.exception.SynchTimeout;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/** manage a pool of synchlings.
 *
 * @author Mike Douglass
 *
 */
public class SynchlingPool {
  protected transient Logger log;

  private SynchEngine syncher;

  private ArrayBlockingQueue<Synchling> pool;

  private Map<Long, Synchling> active = new HashMap<Long, Synchling>();

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

  /** Shut down active synchlings
   */
  public void stop() {
    long maxWait = 1000 * 90; // 90 seconds - needs to be longer than longest poll interval
    long startTime = System.currentTimeMillis();
    long delay = 1000 * 5; // 5 sec delay

    while (getActiveCt() > 0) {
      if ((System.currentTimeMillis() - startTime) > maxWait) {
        warn("**************************************************");
        warn("Synch shutdown completed with " +
            getActiveCt() + " active synchlings");
        warn("**************************************************");

        break;
      }

      info("**************************************************");
      info("Synch shutdown - " +
           getActiveCt() + " active synchlings");
      info("**************************************************");

      try {
        wait(delay);
      } catch (InterruptedException ie) {
        maxWait = 0; // Force exit
      }
    }
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
   * @return number active
   */
  public long getActiveCt() {
    return active.size();
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

    return thePool.remainingCapacity();
  }

  /** Put a synchling back in the pool if there's room else discard it
   *
   * @param s
   * @throws SynchException
   */
  public void add(final Synchling s) throws SynchException {
    synchronized (active) {
      active.remove(s.getSynchlingId());
    }
    getPool().offer(s);
  }

  /** Get a synchling from the pool if possible
   *
   * @return a sychling
   * @throws SynchException if none available
   */
  public Synchling get() throws SynchException {
    return get(true);
  }

  /** Get a synchling from the pool if possible. Return null if timed out
   *
   * @return a sychling or null
   * @throws SynchException on error
   */
  public Synchling getNoException() throws SynchException {
    return get(false);
  }

  private Synchling get(final boolean throwOnFailure) throws SynchException {
    Synchling s = null;
    gets++;
    long st = System.currentTimeMillis();

    try {
      s = getPool().poll(getTimeout(), TimeUnit.MILLISECONDS);
    } catch (Throwable t) {
      throw new SynchException(t);
    }

    waitTimes += System.currentTimeMillis() - st;

    if (s == null) {
      getSynchlingFailures++;

      if (throwOnFailure) {
        throw new SynchTimeout("Synchling pool wait");
      }
    } else {
      synchronized (active) {
        active.put(s.getSynchlingId(), s);
      }
    }

    return s;
  }

  private synchronized ArrayBlockingQueue<Synchling> getPool() {
    return pool;
  }

  /** Get the current stats
   *
   * @return List of Stat
   */
  public List<Stat> getStats() {
    List<Stat> stats = new ArrayList<Stat>();

    stats.add(new Stat("synchling get timeout", getTimeout()));
    stats.add(new Stat("synchling active", getActiveCt()));
    stats.add(new Stat("synchling gets", getGets()));
    stats.add(new Stat("synchling waitTimes", getWaitTimes()));
    stats.add(new Stat("synchling get failures", getGetSynchlingFailures()));
    stats.add(new Stat("synchling currentMaxSize", getCurrentMaxSize()));
    stats.add(new Stat("synchling currentAvailable", getCurrentAvailable()));

    return stats;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    sb.append("timeout=");
    sb.append(getTimeout());

    sb.append(", gets=");
    sb.append(getGets());

    sb.append("\n,     waitTimes=");
    sb.append(getWaitTimes());

    sb.append(", getSynchlingFailures=");
    sb.append(getGetSynchlingFailures());

    sb.append("\n,     currentMaxSize=");
    sb.append(getCurrentMaxSize());

    sb.append(", currentAvailable=");
    sb.append(getCurrentAvailable());

    sb.append("}");

    return sb.toString();
  }

  /* ====================================================================
   *                        private methods
   * ==================================================================== */

  private Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  private void info(final String msg) {
    getLogger().info(msg);
  }

  private void warn(final String msg) {
    getLogger().warn(msg);
  }
}
