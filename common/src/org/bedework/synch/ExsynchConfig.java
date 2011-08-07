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

import java.io.Serializable;

/** This class defines the various properties we need for an Exchange synch
 *
 * @author Mike Douglass
 */
public class ExsynchConfig implements Serializable {
  /* Fro bedework build */
  private String appType;

  private String exchangeWSDLURI;

  /* Exchange web service push callback uri - null for no exchange sync */
  private String exchangeWsPushURI;

  /* Path to keystore - null for use default */
  private String keystore;

  /* WSDL for remote service */
  private String remoteWSDLURI;

  /* Remote system ws url */
  private String remoteWsURL;

  private int remoteKeepAliveInterval;

  /**
   * @param val
   */
  public void setAppType(final String val) {
    appType = val;
  }

  /**
   * @return String
   */
  public String getAppType() {
    return appType;
  }

  /** Exchange web service WSDL uri
   *
   * @param val    String
   */
  public void setExchangeWSDLURI(final String val) {
    exchangeWSDLURI = val;
  }

  /** Exchange web service WSDL uri
   *
   * @return String
   */
  public String getExchangeWSDLURI() {
    return exchangeWSDLURI;
  }

  /** Exchange web service push callback uri - null for no exchange sync
   *
   * @param val    String
   */
  public void setExchangeWsPushURI(final String val) {
    exchangeWsPushURI = val;
  }

  /** Exchange web service push callback uri - null for no exchange sync
   *
   * @return String
   */
  public String getExchangeWsPushURI() {
    return exchangeWsPushURI;
  }

  /** Path to keystore - null for use default
   *
   * @param val    String
   */
  public void setKeystore(final String val) {
    keystore = val;
  }

  /** Path to keystore - null for use default
   *
   * @return String
   */
  public String getKeystore() {
    return keystore;
  }

  /** Remote web service WSDL uri
   *
   * @param val    String
   */
  public void setRemoteWSDLURI(final String val) {
    remoteWSDLURI = val;
  }

  /** Remote web service WSDL uri
   *
   * @return String
   */
  public String getRemoteWSDLURI() {
    return remoteWSDLURI;
  }

  /** Remote web service url
   *
   * @param val    String
   */
  public void setRemoteWsURL(final String val) {
    remoteWsURL = val;
  }

  /** Remote web service url
   *
   * @return String
   */
  public String getRemoteWsURL() {
    return remoteWsURL;
  }

  /** Remote KeepAliveInterval - seconds
   *
   * @param val    int seconds
   */
  public void setRemoteKeepAliveInterval(final int val) {
    remoteKeepAliveInterval = val;
  }

  /** Remote KeepAliveInterval - seconds
   *
   * @return int seconds
   */
  public int getRemoteKeepAliveInterval() {
    return remoteKeepAliveInterval;
  }
}
