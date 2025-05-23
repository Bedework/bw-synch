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

import org.bedework.synch.shared.Stat;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.shared.exception.SynchTimeout;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.base.ToString;

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
public class SynchlingPool implements Logged {
  private SynchEngine syncher;

  private ArrayBlockingQueue<Synchling> pool;

  private final Map<Long, Synchling> active = new HashMap<>();

  private long timeout; // millisecs

  private long waitTimes;

  private long gets;

  private long getSynchlingFailures;

  /** Create a pool with the given size
   *
   * @param syncher the engine
   * @param size of pool
   * @param timeout - millisecs
   */
  public void start(final SynchEngine syncher,
                    final int size,
                    final long timeout) {
    this.syncher = syncher;
    this.timeout = timeout;
    resize(size);
  }

  /** Shut down active synchlings
   */
  public void stop() {
    long maxWait = 1000 * 90; // 90 seconds - needs to be longer than longest poll interval
    final long startTime = System.currentTimeMillis();
    final long delay = 1000 * 5; // 5 sec delay

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
      } catch (final InterruptedException ie) {
        maxWait = 0; // Force exit
      }
    }
  }

  /** Resize the pool
   *
   * @param size new pool size
   */
  public void resize(final int size) {
    final ArrayBlockingQueue<Synchling> oldPool = getPool();
    pool = new ArrayBlockingQueue<>(size);
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
    final ArrayBlockingQueue<Synchling> thePool = pool;
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
    final ArrayBlockingQueue<Synchling> thePool = pool;
    if (thePool == null) {
      return 0;
    }

    return thePool.remainingCapacity();
  }

  /** Put a synchling back in the pool if there's room else discard it
   *
   * @param s synchling to return
   */
  public void add(final Synchling s) {
    synchronized (active) {
      active.remove(s.getSynchlingId());
    }
    getPool().offer(s);
  }

  /** Get a synchling from the pool if possible
   *
   * @return a sychling
   */
  public Synchling get() {
    return get(true);
  }

  /** Get a synchling from the pool if possible. Return null if timed out
   *
   * @return a sychling or null
   */
  public Synchling getNoException() {
    return get(false);
  }

  private Synchling get(final boolean throwOnFailure) {
    final Synchling s;
    gets++;
    final long st = System.currentTimeMillis();

    try {
      s = getPool().poll(getTimeout(), TimeUnit.MILLISECONDS);
    } catch (final SynchException se) {
      throw se;
    } catch (final Throwable t) {
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
    final List<Stat> stats = new ArrayList<>();

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
    final ToString ts = new ToString(this);

    return ts.append("timeout", getTimeout())
             .append("gets", getGets())
             .newLine()
             .append("waitTimes", getWaitTimes())
             .append("getSynchlingFailures", getGetSynchlingFailures())
             .newLine()
             .append("currentMaxSize", getCurrentMaxSize())
             .append("currentAvailable", getCurrentAvailable())
             .toString();
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
