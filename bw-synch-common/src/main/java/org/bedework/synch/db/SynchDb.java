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
package org.bedework.synch.db;

import org.bedework.synch.conf.SynchConfig;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.util.hibernate.HibException;
import org.bedework.util.hibernate.HibSession;
import org.bedework.util.hibernate.HibSessionFactory;
import org.bedework.util.hibernate.HibSessionImpl;
import org.bedework.util.logging.Logged;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/** This class manages the Exchange synch database.
 *
 * @author Mike Douglass
 */
public class SynchDb implements Logged, Serializable {
  private final SynchConfig config;

  /** */
  protected boolean open;

  /** When we were created for debugging */
  protected Timestamp objTimestamp;

  /** Current hibernate session - exists only across one user interaction
   */
  protected HibSession sess;

  /**
   * @param config the configuration
   *
   */
  public SynchDb(final SynchConfig config) {
    this.config = config;
  }

  /**
   * @return true if we had to open it. False if already open
   * @throws SynchException
   */
  public boolean open() throws SynchException {
    if (isOpen()) {
      return false;
    }

    openSession();
    open = true;
    return true;
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
    } catch (final SynchException wde) {
      try {
        rollbackTransaction();
      } catch (final SynchException wde1) {}
      throw wde;
    } finally {
      try {
        closeSession();
      } catch (final SynchException wde1) {}
      open = false;
    }
  }

  /* ====================================================================
   *                   Subscription Object methods
   * ==================================================================== */

  private static final String getAllQuery =
          "from " + SubscriptionImpl.class.getName();

  /**
   * @return list of subscriptions
   * @throws SynchException
   */
  @SuppressWarnings("unchecked")
  public List<Subscription> getAll() throws SynchException {
    try {
      sess.createQuery(getAllQuery);

      return sess.getList();
    } catch (final HibException he) {
      throw new SynchException(he);
    }
  }

  private static final String getSubQuery =
          "from " + SubscriptionImpl.class.getName() +
                  " sub where sub.subscriptionId=:subid";

  /** The synch engine generates a unique subscription id
   * for each subscription. This is used as a key for each subscription.
   *
   * @param id - unique id
   * @return a matching subscription or null
   * @throws SynchException
   */
  public Subscription get(final String id) throws SynchException {
    try {
      sess.createQuery(getSubQuery);
      sess.setString("subid", id);

      return (Subscription)sess.getUnique();
    } catch (final HibException he) {
      throw new SynchException(he);
    }
  }

  private static final String findSubQuery =
          "from " + SubscriptionImpl.class.getName() +
                  " sub where sub.endAConnectorInfo.connectorId=:aconnid" +
                  " and sub.endAConnectorInfo.synchProperties=:aconnprops" +
                  " and sub.endBConnectorInfo.connectorId=:bconnid" +
                  " and sub.endBConnectorInfo.synchProperties=:bconnprops" +
                  " and sub.direction=:dir" +
                  " and sub.master=:mstr";

  /** Find any subscription that matches this one. There can only be one with
   * the same endpoints
   *
   * @param sub subscription
   * @return matching subscriptions
   * @throws SynchException
   */
  public Subscription find(final Subscription sub) throws SynchException {
    try {
      sess.createQuery(findSubQuery);
      sess.setString("aconnid",
                     sub.getEndAConnectorInfo().getConnectorId());
      sess.setString("aconnprops",
                     sub.getEndAConnectorInfo().getSynchProperties());
      sess.setString("bconnid",
                     sub.getEndBConnectorInfo().getConnectorId());
      sess.setString("bconnprops",
                     sub.getEndBConnectorInfo().getSynchProperties());
      sess.setString("dir",
                     sub.getDirection().name());
      sess.setString("mstr",
                     sub.getMaster().name());

      return (Subscription)sess.getUnique();
    } catch (final HibException he) {
      throw new SynchException(he);
    }
  }

  /** Add the subscription.
   *
   * @param sub subscription
   * @throws SynchException
   */
  public void add(final Subscription sub) throws SynchException {
    try {
      sess.save(sub);
    } catch (final HibException he) {
      throw new SynchException(he);
    }
  }

  /** Update the persisted state of the subscription.
   *
   * @param sub subscription
   * @throws SynchException
   */
  public void update(final Subscription sub) throws SynchException {
    try {
      sess.update(sub);
    } catch (final HibException he) {
      throw new SynchException(he);
    }
  }

  /** Delete the subscription.
   *
   * @param sub subscription
   * @throws SynchException
   */
  public void delete(final Subscription sub) throws SynchException {
    final boolean opened = open();

    try {
      sess.delete(sub);
    } catch (final HibException he) {
      throw new SynchException(he);
    } finally {
      if (opened) {
        close();
      }
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
      if (debug()) {
        debug("New hibernate session for " + objTimestamp);
      }
      sess = new HibSessionImpl();
      try {
        sess.init(HibSessionFactory.getSessionFactory(config.getHibernateProperties()));
      } catch (HibException he) {
        throw new SynchException(he);
      }
      debug("Open session for " + objTimestamp);
    }

    beginTransaction();
  }

  protected synchronized void closeSession() throws SynchException {
    if (!isOpen()) {
      if (debug()) {
        debug("Close for " + objTimestamp + " closed session");
      }
      return;
    }

    if (debug()) {
      debug("Close for " + objTimestamp);
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

    if (debug()) {
      debug("Begin transaction for " + objTimestamp);
    }
    try {
      sess.beginTransaction();
    } catch (HibException he) {
      throw new SynchException(he);
    }
  }

  protected void endTransaction() throws SynchException {
    checkOpen();

    if (debug()) {
      debug("End transaction for " + objTimestamp);
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

  /* ====================================================================
   *                   private methods
   * ==================================================================== */

}
