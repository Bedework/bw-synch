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

import edu.rpi.cmt.db.hibernate.HibException;
import edu.rpi.cmt.db.hibernate.HibSession;
import edu.rpi.cmt.db.hibernate.HibSessionFactory;
import edu.rpi.cmt.db.hibernate.HibSessionImpl;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/** This class manages the Exchange synch database.
 *
 * @author Mike Douglass
 */
public class SynchDb implements Serializable {
  private transient Logger log;

  private final boolean debug;

  /** */
  protected boolean open;

  /** When we were created for debugging */
  protected Timestamp objTimestamp;

  /** Current hibernate session - exists only across one user interaction
   */
  protected HibSession sess;

  /**
   *
   */
  public SynchDb() {
    debug = getLogger().isDebugEnabled();
  }

  /**
   * @throws SynchException
   */
  public void open() throws SynchException {
    if (isOpen()) {
      return;
    }
    openSession();
    open = true;
  }

  /**
   * @return true for open
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * @throws SynchException
   */
  public void close() throws SynchException {
    try {
      endTransaction();
    } catch (SynchException wde) {
      try {
        rollbackTransaction();
      } catch (SynchException wde1) {}
      throw wde;
    } finally {
      try {
        closeSession();
      } catch (SynchException wde1) {}
      open = false;
    }
  }

  /**
   * @return list of subscriptions
   * @throws SynchException
   */
  @SuppressWarnings("unchecked")
  public List<BaseSubscription> getAll() throws SynchException {
    StringBuilder sb = new StringBuilder();

    sb.append("from ");
    sb.append(BaseSubscription.class.getName());

    try {
      sess.createQuery(sb.toString());

      return sess.getList();
    } catch (HibException he) {
      throw new SynchException(he);
    }
  }

  /** The synch engine generates a unique subscription id
   * for each subscription. This is used as a key for each subscription.
   *
   * @param id - unique id
   * @return a matching subscription or null
   * @throws SynchException
   */
  public BaseSubscription get(final String id) throws SynchException {
    try {
      StringBuilder sb = new StringBuilder();

      sb.append("from ");
      sb.append(BaseSubscription.class.getName());
      sb.append(" sub where sub.subscriptionId=:subid");

      sess.createQuery(sb.toString());
      sess.setString("subid", id);

      return (BaseSubscription)sess.getUnique();
    } catch (HibException he) {
      throw new SynchException(he);
    }
  }

  /** Find subscriptions that match the end points.
   *
   * @param calPath - remote calendar
   * @param exCal - Exchange calendar
   * @param exId - Exchange principal
   * @return matching subscriptions
   * @throws SynchException
   */
  @SuppressWarnings("unchecked")
  public List<BaseSubscription> find(final String calPath,
                                         final String exCal,
                                         final String exId) throws SynchException {
    try {
      StringBuilder sb = new StringBuilder();

      sb.append("from ");
      sb.append(BaseSubscription.class.getName());
      sb.append(" sub where sub.calPath=:calPath");
      sb.append(" and where sub.exchangeCalendar=:exCal");
      sb.append(" and where sub.exchangeId=:exId");

      sess.createQuery(sb.toString());
      sess.setString("calPath", calPath);
      sess.setString("exCal", exCal);
      sess.setString("exId", exId);

      return sess.getList();
    } catch (HibException he) {
      throw new SynchException(he);
    }
  }

  /** Update the persisted state of the subscription.
   *
   * @param sub
   * @throws SynchException
   */
  public void update(final BaseSubscription sub) throws SynchException {

  }

  /** Delete the subscription.
   *
   * @param sub
   * @throws SynchException
   */
  public void delete(final BaseSubscription sub) throws SynchException {
    try {
      sess.delete(sub);
    } catch (HibException he) {
      throw new SynchException(he);
    }
  }

  /* ====================================================================
   *                   Session methods
   * ==================================================================== */

  protected void checkOpen() throws SynchException {
    if (!isOpen()) {
      throw new SynchException("Session call when closed");
    }
  }

  protected synchronized void openSession() throws SynchException {
    if (isOpen()) {
      throw new SynchException("Already open");
    }

    open = true;

    if (sess != null) {
      warn("Session is not null. Will close");
      try {
        close();
      } finally {
      }
    }

    if (sess == null) {
      if (debug) {
        trace("New hibernate session for " + objTimestamp);
      }
      sess = new HibSessionImpl();
      try {
        sess.init(HibSessionFactory.getSessionFactory(), getLogger());
      } catch (HibException he) {
        throw new SynchException(he);
      }
      trace("Open session for " + objTimestamp);
    }

    beginTransaction();
  }

  protected synchronized void closeSession() throws SynchException {
    if (!isOpen()) {
      if (debug) {
        trace("Close for " + objTimestamp + " closed session");
      }
      return;
    }

    if (debug) {
      trace("Close for " + objTimestamp);
    }

    try {
      if (sess != null) {
        if (sess.rolledback()) {
          sess = null;
          return;
        }

        if (sess.transactionStarted()) {
          sess.rollback();
        }
//        sess.disconnect();
        sess.close();
        sess = null;
      }
    } catch (Throwable t) {
      try {
        sess.close();
      } catch (Throwable t1) {}
      sess = null; // Discard on error
    } finally {
      open = false;
    }
  }

  protected void beginTransaction() throws SynchException {
    checkOpen();

    if (debug) {
      trace("Begin transaction for " + objTimestamp);
    }
    try {
      sess.beginTransaction();
    } catch (HibException he) {
      throw new SynchException(he);
    }
  }

  protected void endTransaction() throws SynchException {
    checkOpen();

    if (debug) {
      trace("End transaction for " + objTimestamp);
    }

    try {
      if (!sess.rolledback()) {
        sess.commit();
      }
    } catch (HibException he) {
      throw new SynchException(he);
    }
  }

  protected void rollbackTransaction() throws SynchException {
    try {
      checkOpen();
      sess.rollback();
    } catch (HibException he) {
      throw new SynchException(he);
    } finally {
    }
  }

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  /**
   * @param t
   */
  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  /**
   * @param msg
   */
  protected void warn(final String msg) {
    getLogger().warn(msg);
  }

  /**
   * @param msg
   */
  protected void trace(final String msg) {
    getLogger().debug(msg);
  }

  /* ====================================================================
   *                   private methods
   * ==================================================================== */

}
