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
import org.bedework.synch.Subscription;
import org.bedework.synch.SynchDefs.SynchEnd;
import org.bedework.synch.SynchPropertyInfo;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SubscribeResponseType;

import org.apache.log4j.Logger;
import org.oasis_open.docs.ns.wscal.calws_soap.BaseResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;

import java.util.List;

/** Abstract connector instance to handle some trivia.
 *
 * @author Mike Douglass
 */
public abstract class AbstractConnectorInstance implements ConnectorInstance {
  private transient Logger log;

  protected boolean debug;

  protected Subscription sub;

  protected SynchEnd end;

  protected BaseSubscriptionInfo baseInfo;

  protected AbstractConnectorInstance(final Subscription sub,
                                      final SynchEnd end,
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

  protected boolean validateSubInfo(final SubscribeResponseType sr,
                                    final Connector cnctr,
                                    final BaseSubscriptionInfo info) throws SynchException {
    @SuppressWarnings("unchecked")
    List<SynchPropertyInfo> propInfo = cnctr.getPropertyInfo();

    for (SynchPropertyInfo spi: propInfo) {
      if (spi.isRequired() &&
          (info.getProperty(spi.getName()) == null)) {
        sr.setStatus(StatusType.ERROR);
        return false;
      }
    }

    return true;
  }

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
