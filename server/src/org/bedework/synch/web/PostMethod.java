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
package org.bedework.synch.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bedework.synch.SynchException;

/** Handle POST for exchange synch servlet.
 */
public class PostMethod extends MethodBase {
  @Override
  public void init() throws SynchException {
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws SynchException {
    try {
      String resourceUri = getResourceUri(req);

      SOAPHandler hdlr;

      if (resourceUri.startsWith(getSyncher().getRemoteCallbackPathPrefix())) {
        // From remote system
        hdlr = new SynchwsSOAPHandler();
      } else {
        hdlr = new EwsSOAPHandler();
      }

      hdlr.init(syncher);

      hdlr.doRequest(req, resp, resourceUri);
    } catch (SynchException se) {
      throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }
}

