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

import org.bedework.base.exc.BedeworkException;
import org.bedework.database.db.DbSession;
import org.bedework.database.db.DbSessionFactoryProvider;
import org.bedework.database.db.DbSessionFactoryProviderImpl;
import org.bedework.synch.conf.SynchConfig;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.util.logging.BwLogger;
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

  /* Factory used to obtain a session
   */
  private static DbSessionFactoryProvider factoryProvider;

  /** Current database session - exists only across one user interaction
   */
  protected DbSession sess;

  /**
   * @param config the configuration
   *
   */
  public SynchDb(final SynchConfig config) {
    this.config = config;
  }

  /**
   * @return true if we had to open it. False if already open
   */
  public boolean open() {
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
   */
  public void close() {
    try {
      endTransaction();
    } catch (final SynchException wde) {
      try {
        rollbackTransaction();
      } catch (final SynchException ignored) {}
      throw wde;
    } finally {
      try {
        closeSession();
      } catch (final SynchException ignored) {}
      open = false;
    }
  }

  /* ====================================================================
   *                   Subscription Object methods
   * ==================================================================== */

  private static final String getAllQuery =
          "select sub from SubscriptionImpl sub";

  /**
   * @return list of subscriptions
   */
  @SuppressWarnings("unchecked")
  public List<Subscription> getAll() {
    try {
      return (List<Subscription>)createQuery(getAllQuery)
              .getList();
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    }
  }

  private static final String getSubQuery =
          "select sub from SubscriptionImpl sub " +
                  "where sub.subscriptionId=:subid";

  /** The synch engine generates a unique subscription id
   * for each subscription. This is used as a key for each subscription.
   *
   * @param id - unique id
   * @return a matching subscription or null
   */
  public Subscription get(final String id) {
    try {
      return (Subscription)createQuery(getSubQuery)
              .setString("subid", id)
              .getUnique();
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    }
  }

  private static final String findSubQuery =
          "select sub from SubscriptionImpl sub " +
                  "where sub.endAConnectorInfo.connectorId=:aconnid " +
                  "and sub.endAConnectorInfo.synchProperties=:aconnprops " +
                  "and sub.endBConnectorInfo.connectorId=:bconnid " +
                  "and sub.endBConnectorInfo.synchProperties=:bconnprops " +
                  "and sub.direction=:dir " +
                  "and sub.master=:mstr";

  /** Find any subscription that matches this one. There can only be one with
   * the same endpoints
   *
   * @param sub subscription
   * @return matching subscriptions
   */
  public Subscription find(final Subscription sub) {
    try {
      final var eAinfo = sub.getEndAConnectorInfo();
      final var eBinfo = sub.getEndBConnectorInfo();

      return (Subscription)createQuery(findSubQuery)
              .setString("aconnid",
                         eAinfo.getConnectorId())
              .setString("aconnprops",
                         eAinfo.getSynchProperties())
              .setString("bconnid",
                         eBinfo.getConnectorId())
              .setString("bconnprops",
                         eBinfo.getSynchProperties())
              .setString("dir",
                         sub.getDirection())
              .setString("mstr",
                         sub.getMaster())
              .getUnique();
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    }
  }

  /** Add the subscription.
   *
   * @param sub subscription
   */
  public void add(final Subscription sub) {
    try {
      sess.add(sub);
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    }
  }

  /** Update the persisted state of the subscription.
   *
   * @param sub subscription
   */
  public Subscription update(final Subscription sub) {
    try {
      return (Subscription)sess.update(sub);
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    }
  }

  /** Delete the subscription.
   *
   * @param sub subscription
   */
  public void delete(final Subscription sub) {
    final boolean opened = open();

    try {
      sess.delete(sub);
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    } finally {
      if (opened) {
        close();
      }
    }
  }

  /* ==============================================================
   *                   Session methods
   * ============================================================== */

  protected void checkOpen() {
    if (!isOpen()) {
      throw new SynchException("Session call when closed");
    }
  }

  protected synchronized void openSession() {
    if (isOpen()) {
      throw new SynchException("Already open");
    }

    try {
      if (factoryProvider == null) {
        factoryProvider =
                new DbSessionFactoryProviderImpl()
                        .init(config.getOrmProperties());
      }

      open = true;

      if (sess != null) {
        warn("Session is not null. Will close");
        close();
      }

      if (sess == null) {
        if (debug()) {
          debug("New orm session for " + objTimestamp);
        }
        sess = factoryProvider.getNewSession();

        debug("Open session for " + objTimestamp);
      }
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    }

    beginTransaction();
  }

  protected synchronized void closeSession() {
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

        sess.close();
        sess = null;
      }
    } catch (final Throwable t) {
      try {
        sess.close();
      } catch (final Throwable ignored) {}
      sess = null; // Discard on error
    } finally {
      open = false;
    }
  }

  protected void beginTransaction() {
    checkOpen();

    if (debug()) {
      debug("Begin transaction for " + objTimestamp);
    }
    try {
      sess.beginTransaction();
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    }
  }

  protected void endTransaction() {
    checkOpen();

    if (debug()) {
      debug("End transaction for " + objTimestamp);
    }

    try {
      if (!sess.rolledback()) {
        sess.commit();
      }
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    }
  }

  protected void rollbackTransaction() {
    try {
      checkOpen();
      sess.rollback();
    } catch (final BedeworkException e) {
      throw new SynchException(e);
    }
  }

  protected DbSession createQuery(final String query) {
    return sess.createQuery(query);
  }

  /* ==============================================================
   *                   Logged methods
   * ============================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
