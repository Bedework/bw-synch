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

import org.bedework.synch.Notification.NotificationItem;
import org.bedework.synch.Notification.NotificationItem.ActionType;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchEndType;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/** Subscriptions which are waiting for a period before resynching. These are
 * generally the polled kind but other subscriptions may be made to wait before
 * retrying failed operations.
 *
 *   @author Mike Douglass   douglm   rpi.edu
 */
public class SynchTimer {
  private boolean debug;

  protected transient Logger log;

  private SynchEngine syncher;

  /** This is the class that goes into a wait. The run method MUST only take  a
   * short period or it will hang the timer. Usually it will allocate a synchling
   * then return.
   *
   */
  class SynchTask extends TimerTask {
    private Subscription sub;

    SynchTask(final Subscription sub) {
      this.sub = sub;

      synchronized (waiting) {
        waiting.put(sub.getSubscriptionId(), this);
        maxWaitingCt = Math.max(maxWaitingCt, waiting.size());
      }
    }

    @Override
    public void run() {
      synchronized (waiting) {
        waiting.remove(sub.getSubscriptionId());
      }

      if (debug){
        trace("About to send resynch notification for " + sub.getSubscriptionId());
      }

      NotificationItem ni = new NotificationItem(ActionType.FullSynch,
                                                 null, null);
      Notification<NotificationItem> note = new Notification<NotificationItem>(
          sub, SynchEndType.NONE, ni);

      try {
        syncher.handleNotification(note);
      } catch (SynchException se) {
        if (debug) {
          error(se);
        } else {
          error(se.getMessage());
        }
      }
    }
  }

  Timer timer;

  private Map<String, SynchTask> waiting = new HashMap<String, SynchTask>();

  private long maxWaitingCt;

  /** Start the SynchTimer
   *
   * @param syncher
   */
  public SynchTimer(final SynchEngine syncher){
    this.syncher = syncher;

    timer = new Timer("SynchTimer", true);

    debug = getLogger().isDebugEnabled();
  }

  /** Stop our timer thread.
   *
   */
  public void stop() {
    if (timer == null) {
      return;
    }

    timer.cancel();
    timer = null;
  }

  /** Schedule a subscription for the given time
   *
   * @param sub
   * @param when
   * @throws SynchException
   */
  public void schedule(final Subscription sub,
                       final Date when) throws SynchException {
    if (debug){
      trace("reschedule " + sub.getSubscriptionId() + " for " + when);
    }

    SynchTask st = new SynchTask(sub);
    timer.schedule(st, when);
  }

  /** Schedule a subscription after the given delay
   *
   * @param sub
   * @param delay - delay in milliseconds before subscription is processed.
   * @throws SynchException
   */
  public void schedule(final Subscription sub,
                       final long delay) throws SynchException {
    SynchTask st = new SynchTask(sub);
    timer.schedule(st, delay);
  }

  /**
   * @return number waiting
   */
  public long getWaitingCt() {
    return waiting.size();
  }

  /**
   * @return number waiting
   */
  public long getMaxWaitingCt() {
    return maxWaitingCt;
  }

  /** Get the current stats
   *
   * @return List of Stat
   */
  public List<Stat> getStats() {
    List<Stat> stats = new ArrayList<Stat>();

    stats.add(new Stat("waiting", getWaitingCt()));
    stats.add(new Stat("max waiting", getMaxWaitingCt()));

    return stats;
  }

  private Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  private void trace(final String msg) {
    getLogger().debug(msg);
  }

  @SuppressWarnings("unused")
  private void warn(final String msg) {
    getLogger().warn(msg);
  }

  private void error(final Throwable t) {
    getLogger().error(this, t);
  }

  private void error(final String msg) {
    getLogger().error(msg);
  }

  @SuppressWarnings("unused")
  private void info(final String msg) {
    getLogger().info(msg);
  }
}
