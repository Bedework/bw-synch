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
package org.bedework.synch.cnctrs.exchange;

import org.bedework.synch.Connector;
import org.bedework.synch.SynchException;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Properties;

import javax.xml.namespace.QName;

import com.microsoft.schemas.exchange.services._2006.messages.ExchangeServicePortType;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeWebService;

/** Calls from exchange synch processor to the service.
 *
 * @author Mike Douglass
 */
public class ExchangeConnector
      implements Connector<ExchangeSubscription, ExchangeConnectorInstance> {
  @Override
  public void start(final Properties props,
                    final String callbackUri) throws SynchException {

  }

  @Override
  public ExchangeConnectorInstance getConnectorInstance(final ExchangeSubscription sub) throws SynchException {
    return null;
  }

  @Override
  public void stop() throws SynchException {

  }

  ExchangeServicePortType getPort(final ExchangeSubscription sub) throws SynchException {
    try {
      return getExchangeServicePort(sub.getExchangeId(),
                                    sub.getExchangePw().toCharArray()); // XXX need to en/decrypt
    } catch (SynchException se) {
      throw se;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }

  private ExchangeServicePortType getExchangeServicePort(final String user,
                                                         final char[] pw) throws SynchException {
    try {
      URL wsdlURL = new URL(config.getExchangeWSDLURI());

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(
                user,
                pw);
        }
    });

      ExchangeWebService ews =
        new ExchangeWebService(wsdlURL,
                               new QName("http://schemas.microsoft.com/exchange/services/2006/messages",
                                         "ExchangeWebService"));
      ExchangeServicePortType port = ews.getExchangeWebPort();

//      Map<String, Object> context = ((BindingProvider)port).getRequestContext();

  //    context.put(BindingProvider.USERNAME_PROPERTY, user);
    //  context.put(BindingProvider.PASSWORD_PROPERTY, new String(pw));

      /*
        $client->__setSoapHeaders(
        new SOAPHeader('http://schemas.microsoft.com/exchange/services/2006/types',
        'RequestServerVersion',
        array("Version"=>"Exchange2007_SP1"))
        );

        $client is the SoapClient Instance.

      */


      return port;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }
}
