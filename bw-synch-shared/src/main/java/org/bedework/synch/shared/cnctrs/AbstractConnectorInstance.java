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
package org.bedework.synch.shared.cnctrs;

import org.bedework.synch.shared.BaseSubscriptionInfo;
import org.bedework.synch.shared.BaseSubscriptionInfo.CrudCts;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.conf.ConnectorConfigI;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.ActiveSubscriptionRequestType;
import org.bedework.synch.wsmessages.SubscribeResponseType;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.UnsubscribeRequestType;
import org.bedework.synch.wsmessages.UnsubscribeResponseType;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.oasis_open.docs.ws_calendar.ns.soap.BaseResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;

/** Abstract connector instance to handle some trivia.
 *
 * @author Mike Douglass
 */
public abstract class AbstractConnectorInstance<CnctrT extends AbstractConnector,
        InfoT extends BaseSubscriptionInfo,
        ConfigT extends ConnectorConfigI>
        implements Logged, ConnectorInstance<InfoT> {
  protected Subscription sub;

  protected SynchEndType end;

  protected InfoT info;

  protected final CnctrT cnctr;

  protected final ConfigT config;

  private CloseableHttpClient client;

  protected AbstractConnectorInstance(final Subscription sub,
                                      final SynchEndType end,
                                      final InfoT info,
                                      final CnctrT cnctr,
                                      final ConfigT config) {
    this.sub = sub;
    this.end = end;
    this.info = info;
    this.cnctr = cnctr;
    this.config = config;
  }

  public Connector getConnector() {
    return cnctr;
  }

  @Override
  public InfoT getSubInfo() {
    return info;
  }

  @Override
  public BaseResponseType open() throws SynchException {
    return null;
  }

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
    resp.setStatus(StatusType.OK);
    if (req.getEnd() != end) {
      return true;
    }

    if (!cnctr.getPropertyInfo().
            validRequestProperties(info,
                                   req.getConnectorInfo().getProperties())) {
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
    info.setLastCrudCts(val);
  }

  /**
   * @return cts
   * @throws SynchException
   */
  @Override
  public CrudCts getLastCrudCts() throws SynchException {
    return info.getLastCrudCts();
  }

  /**
   * @param val
   * @throws SynchException
   */
  @Override
  public void setTotalCrudCts(final CrudCts val) throws SynchException {
    info.setTotalCrudCts(val);
  }

  /**
   * @return cts
   * @throws SynchException
   */
  @Override
  public CrudCts getTotalCrudCts() throws SynchException {
    return info.getTotalCrudCts();
  }

  /* ============================================================
   *                   Protected methods
   * ============================================================ */

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

  protected CloseableHttpClient getClient() throws SynchException {
    if (client != null) {
      return client;
    }

    final CloseableHttpClient cl =
            HttpClients.custom()
                       .setUserAgent("Bedework Calendar System").build();

    final HttpClientContext context = HttpClientContext.create();
    if (info.getPrincipalHref() != null) {
      final CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(
              new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
              new UsernamePasswordCredentials(info.getPrincipalHref(),
                                              cnctr.getSyncher().decrypt(info.getPassword())));
      context.setCredentialsProvider(credsProvider);
    }

    client = cl;

    return cl;
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
