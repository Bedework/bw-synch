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

import org.bedework.synch.BaseSubscriptionInfo;
import org.bedework.synch.BaseSubscriptionInfo.CrudCts;
import org.bedework.synch.db.SerializableProperties;
import org.bedework.synch.db.Subscription;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.ActiveSubscriptionRequestType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;
import org.bedework.synch.wsmessages.UnsubscribeResponseType;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ws_calendar.ns.soap.BaseResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;

/** Abstract connector instance to handle some trivia.
 *
 * @author Mike Douglass
 */
public abstract class AbstractConnectorInstance implements ConnectorInstance {
  private transient Logger log;

  protected boolean debug;

  protected Subscription sub;

  protected SynchEndType end;

  protected BaseSubscriptionInfo baseInfo;

  protected AbstractConnectorInstance(final Subscription sub,
                                      final SynchEndType end,
                                      final BaseSubscriptionInfo baseInfo) {
    this.sub = sub;
    this.end = end;
    this.baseInfo = baseInfo;

    debug = getLogger().isDebugEnabled();
  }

  /* (non-Javadoc)
   * @see org.bedework.synch.ConnectorInstance#open()
   */
  @Override
  public BaseResponseType open() throws SynchException {
    return null;
  }

  /* (non-Javadoc)
   * @see org.bedework.synch.cnctrs.ConnectorInstance#subscribe(org.bedework.synch.wsmessages.SubscribeResponseType)
   */
  @Override
  public boolean subscribe(final SubscribeResponseType sr) throws SynchException {
    return validateSubInfo(sr, getConnector(), getSubInfo());
  }

  @Override
  public boolean unsubscribe(final UnsubscribeRequestType usreq,
                             final UnsubscribeResponseType usresp) throws SynchException {
    return validateActiveSubInfo(usreq, usresp, getConnector(), getSubInfo());
  }

  @Override
  public boolean validateActiveSubInfo(final ActiveSubscriptionRequestType req,
                                       final BaseResponseType resp,
                                       final Connector cnctr,
                                       final BaseSubscriptionInfo info) throws SynchException {
    if (req.getEnd() != end) {
      return true;
    }

    if (!cnctr.getPropertyInfo().validRequestProperties(info,
        new SerializableProperties(req.getConnectorInfo().getProperties()))) {
      resp.setStatus(StatusType.ERROR);
      return false;
    }

    return true;
  }

  /**
   * @param val
   * @throws SynchException
   */
  @Override
  public void setLastCrudCts(final CrudCts val) throws SynchException {
    baseInfo.setLastCrudCts(val);
  }

  /**
   * @return cts
   * @throws SynchException
   */
  @Override
  public CrudCts getLastCrudCts() throws SynchException {
    return baseInfo.getLastCrudCts();
  }

  /**
   * @param val
   * @throws SynchException
   */
  @Override
  public void setTotalCrudCts(final CrudCts val) throws SynchException {
    baseInfo.setTotalCrudCts(val);
  }

  /**
   * @return cts
   * @throws SynchException
   */
  @Override
  public CrudCts getTotalCrudCts() throws SynchException {
    return baseInfo.getTotalCrudCts();
  }

  /* ====================================================================
   *                   Protected methods
   * ==================================================================== */

  /** Ensure subscription info is valid
   *
   * @param sr
   * @param cnctr
   * @param info
   * @return true if all ok
   * @throws SynchException
   */
  protected boolean validateSubInfo(final SubscribeResponseType sr,
                                    final Connector cnctr,
                                    final BaseSubscriptionInfo info) throws SynchException {
    if (!cnctr.getPropertyInfo().validSubscribeInfoProperties(info)) {
      sr.setStatus(StatusType.ERROR);
      return false;
    }

    return true;
  }

  /*

  private String decryptPw(final BwCalendar val) throws CalFacadeException {
    try {
      return getSvc().getEncrypter().decrypt(val.getRemotePw());
    } catch (Throwable t) {
      throw new CalFacadeException(t);
    }
  }
   *
   */

  protected void info(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  protected void warn(final String msg) {
    getLogger().warn(msg);
  }

  /* Get a logger for messages
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }
}
