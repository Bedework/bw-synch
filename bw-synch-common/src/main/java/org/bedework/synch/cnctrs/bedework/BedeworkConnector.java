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
package org.bedework.synch.cnctrs.bedework;

import org.bedework.synch.shared.Notification;
import org.bedework.synch.shared.PropertiesInfo;
import org.bedework.synch.shared.Subscription;
import org.bedework.synch.shared.SubscriptionInfo;
import org.bedework.synch.shared.SynchDefs.SynchKind;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.cnctrs.AbstractConnector;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.wsmessages.CalProcessingType;
import org.bedework.synch.wsmessages.KeepAliveNotificationType;
import org.bedework.synch.wsmessages.KeepAliveResponseType;
import org.bedework.synch.wsmessages.StartServiceNotificationType;
import org.bedework.synch.wsmessages.StartServiceResponseType;
import org.bedework.synch.wsmessages.SynchEndType;
import org.bedework.synch.wsmessages.SynchIdTokenType;
import org.bedework.synch.wsmessages.SynchRemoteServicePortType;

import org.oasis_open.docs.ws_calendar.ns.soap.GetPropertiesResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.GetPropertiesType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;

/** The synch processor connector for connections to bedework.
 *
 * @author Mike Douglass
 */
public class BedeworkConnector
      extends AbstractConnector<BedeworkConnector,
                                BedeworkConnectorInstance,
                                Notification<?>,
                                BedeworkConnectorConfig,
                                BedeworkSubscriptionInfo> {
  private static final PropertiesInfo bwPropInfo = new PropertiesInfo();

  static {
    bwPropInfo.requiredUri(null);

    bwPropInfo.requiredPrincipal(null);

    bwPropInfo.optionCalProcessing(SubscriptionInfo.propnameAlarmProcessing,
                                   "",
                                   CalProcessingType.REMOVE.toString());

    bwPropInfo.optionCalProcessing(SubscriptionInfo.propnameSchedulingProcessing,
                                   "",
                                   CalProcessingType.REMOVE.toString());
  }

  /* If non-null this is the token we currently have for bedework */
  private String remoteToken;

  private GetPropertiesResponseType sysInfo;

  /**
   */
  public BedeworkConnector() {
    super(bwPropInfo);
  }

  private final static Object threadLockObj =
          new Object();

  /** This process will send keep-alive notifications to the remote system.
   * During startup the first notification is sent so this process starts with
   * a wait
   *
   */
  private class PingThread extends Thread {
    boolean showedTrace;

    BedeworkConnector conn;

    /**
     * @param name - for the thread
     * @param conn = the connector
     */
    public PingThread(final String name,
                      final BedeworkConnector conn) {
      super(name);
      this.conn = conn;
    }

    @Override
    public void run() {
      while (!conn.isStopped()) {
        if (debug()) {
          debug("About to call service - token = " + remoteToken);
        }
        /* First see if we need to reinitialize or ping */

        try {
          if (remoteToken == null) {
            initConnection();
            if (remoteToken != null) {
              running = true;
            }
          } else {
            ping();
          }
        } catch (final Throwable t) {
          if (!showedTrace) {
            error(t);
            showedTrace = true;
          } else {
            if (t.getMessage() == null) {
              error(t);
            } else {
              error(t.getMessage());
            }
          }
        }

        // Wait a bit before trying again

        if (debug()) {
          debug("About to pause - token = " + remoteToken);
        }

        try {
          final long waitTime;

          if (remoteToken == null) {
            waitTime = config.getRetryInterval() * 1000L;
          } else {
            waitTime = config.getKeepAliveInterval() * 1000L;
          }

          synchronized (threadLockObj) {
            threadLockObj.wait(waitTime);
          }
        } catch (final InterruptedException ie) {
          break;
        } catch (final Throwable t) {
          if (t.getMessage() == null) {
            error(t);
          } else {
            error(t.getMessage());
          }
        }
      }
    }
  }

  private PingThread pinger;

  @Override
  public void start(final String connectorId,
                    final BedeworkConnectorConfig conf,
                    final String callbackUri,
                    final SynchEngine syncher) {
    super.start(connectorId, conf, callbackUri, syncher);

    if (pinger == null) {
      pinger = new PingThread(connectorId, this);
      pinger.start();
    }
  }

  @Override
  public SynchKind getKind() {
    return SynchKind.notify;
  }

  @Override
  public BedeworkConnectorInstance makeInstance(final Subscription sub,
                                                final SynchEndType end) {
    final BedeworkSubscriptionInfo info;

    if (end == SynchEndType.A) {
      info = new BedeworkSubscriptionInfo(sub.getEndAConnectorInfo());
    } else {
      info = new BedeworkSubscriptionInfo(sub.getEndBConnectorInfo());
    }

    return new BedeworkConnectorInstance(config,
                                         this, sub, end, info);
  }

  @Override
  public void stop() {
    stopped = true;
    if (pinger != null) {
      pinger.interrupt();
    }

    pinger = null;
  }

  /* ====================================================================
   *                         Package methods
   * ==================================================================== */

  SynchRemoteServicePortType getPort() {
    return getPort(config.getBwWSDLURI());
  }

  SynchIdTokenType getIdToken(final String principal,
                              final String opaqueData) {
    if (remoteToken == null) {
      throw new SynchException(SynchException.connectorNotStarted);
    }

    final SynchIdTokenType idToken = new SynchIdTokenType();

    idToken.setPrincipalHref(principal);
    idToken.setSubscribeUrl(callbackUri);
    idToken.setSynchToken(remoteToken);
    idToken.setOpaqueData(opaqueData);

    return idToken;
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /**
   * @throws SynchException on fatal error
   */
  public void ping() {
    final KeepAliveNotificationType kan = new KeepAliveNotificationType();

    kan.setSubscribeUrl(callbackUri);
    kan.setToken(remoteToken);

    final KeepAliveResponseType kar = getPort().pingService(kan);

    if (kar.getStatus() != StatusType.OK) {
      warn("Received status " + kar.getStatus() + " for ping");
      remoteToken = null; // Force reinit after wait

      running = false;
    }
  }

  private void initConnection() {
    final StartServiceNotificationType ssn =
            new StartServiceNotificationType();

    ssn.setConnectorId(getConnectorId());
    ssn.setSubscribeUrl(callbackUri);

    final StartServiceResponseType ssr = getPort().startService(ssn);

    if (ssr == null) {
      warn("Received null response to start notification");
      return;
    }

    if (ssr.getStatus() != StatusType.OK) {
      warn("Received status " + ssr.getStatus() + " to start notification");
      return;
    }

    remoteToken = ssr.getToken();

    if (sysInfo == null) {
      // Try to get info
      final GetPropertiesType gp = new GetPropertiesType();

      gp.setHref("/");

      sysInfo = getPort().getProperties(getIdToken(null, null),
                                        gp);
    }
  }
}
