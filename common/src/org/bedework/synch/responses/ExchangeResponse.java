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
package org.bedework.exchgsynch.responses;

import org.bedework.exchgsynch.intf.SynchException;

import org.apache.log4j.Logger;

import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType.MessageXml;
import com.microsoft.schemas.exchange.services._2006.types.ResponseClassType;

/** Base Response from Exchange.
 *
 */
public class ExchangeResponse {
  private Logger logger;

  protected boolean debug;

  private String messageText;

  private String responseCode;

  private Integer descriptiveLinkKey;

  private MessageXml messageXml;

  private boolean valid;
  private boolean warning;
  private boolean error;

  ExchangeResponse(final ResponseMessageType resp) throws SynchException {
    debug = getLogger().isDebugEnabled();

    messageText = resp.getMessageText();

    responseCode = resp.getResponseCode();

    descriptiveLinkKey = resp.getDescriptiveLinkKey();

    messageXml = resp.getMessageXml();

    ResponseClassType rcl = resp.getResponseClass();
    if (rcl.equals(ResponseClassType.ERROR)) {
      error = true;
      return;
    }

    if (rcl.equals(ResponseClassType.WARNING)) {
      warning = true;
      return;
    }

    valid = true;
  }

  /**
   * @return - message text
   */
  public String getMessageText() {
    return messageText;
  }

  /**
   * @return - responseCode
   */
  public String getResponseCode() {
    return responseCode;
  }

  /**
   * @return Integer or null
   */
  public Integer getDescriptiveLinkKey() {
      return descriptiveLinkKey;
  }

  /**
   * @return - message xml
   */
  public MessageXml getMessageXml() {
    return messageXml;
  }

  /**
   * @return - was the response valid?
   */
  public boolean getValid() {
    return valid;
  }

  /**
   * @return - was the response an error?
   */
  public boolean getError() {
    return error;
  }

  /**
   * @return - was the response a warning?
   */
  public boolean getWarning() {
    return warning;
  }

  /**
   * @param sb
   */
  public void toStringSegment(final StringBuilder sb) {
    if (getError()) {
      sb.append("error");
    } else if (getWarning()) {
      sb.append("warning");
    } else {
      sb.append("success");
    }

    sb.append(", responseCode=");
    sb.append(getResponseCode());

    if (getMessageText() != null) {
      sb.append(",\n    message=");
      sb.append(getMessageText());
    }

    if (getDescriptiveLinkKey() != null) {
      sb.append(", descriptiveLinkKey=");
      sb.append(getDescriptiveLinkKey());
    }
  }

  protected Logger getLogger() {
    if (logger == null) {
      logger = Logger.getLogger(this.getClass());
    }

    return logger;
  }

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }
}
