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

import org.bedework.synch.shared.cnctrs.Connector;
import org.bedework.synch.shared.cnctrs.Connector.NotificationBatch;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.util.misc.Util;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handle POST for exchange synch servlet.
 */
public class PostMethod extends MethodBase {
  @Override
  public void init() {
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) {
    try {
      final List<String> resourceUri = getResourceUri(req);

      if (Util.isEmpty(resourceUri)) {
        throw new SynchException("Bad resource url - no connector specified");
      }

      /* Find a connector to handle the incoming request.
       */
      final Connector conn = syncher.getConnector(resourceUri.get(0));

      if (conn == null) {
        throw new SynchException("Bad resource url - unknown connector specified");
      }

      resourceUri.remove(0);
      final NotificationBatch notes = conn.handleCallback(req, resp, resourceUri);

      if (notes != null) {
        syncher.handleNotifications(notes);
        conn.respondCallback(resp, notes);
      }
    } catch (final SynchException se) {
      throw se;
    } catch(final Throwable t) {
      throw new SynchException(t);
    }
  }
}

