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

import org.apache.log4j.Logger;
import org.bedework.synch.SynchEngine;
import org.bedework.synch.SynchException;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;

/** Handle SOAP interactions for exchange synch servlet.
 */
public abstract class SOAPHandler {
  protected boolean debug;

  protected transient Logger log;

  private SynchEngine syncher;

  private String contextPath;

  // Are these thread safe?
  private MessageFactory soapMsgFactory;
  private JAXBContext ewsjc;

  /**
   * @param syncher
   * @throws SynchException
   */
  public void init(final SynchEngine syncher) throws SynchException {
    debug = getLogger().isDebugEnabled();
    this.syncher = syncher;
  }

  protected void setContextPath(final String contextPath) {
    this.contextPath = contextPath;
  }

  /** Get syncher
   *
   * @return SynchEngine
   */
  protected SynchEngine getSyncher() {
    return syncher;
  }

  protected MessageFactory getSoapMsgFactory() throws SynchException {
    try {
      if (soapMsgFactory == null) {
        soapMsgFactory = MessageFactory.newInstance();
      }

      return soapMsgFactory;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  protected JAXBContext getEwsJAXBContext() throws SynchException {
    try {
      if (ewsjc == null) {
        ewsjc = JAXBContext.newInstance(contextPath);
//                     "com.microsoft.schemas.exchange.services._2006.messages:" +
  //                   "com.microsoft.schemas.exchange.services._2006.types");
      }

      return ewsjc;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  /**
   * @param req
   * @param resp
   * @param resourceUri
   * @throws SynchException
   */
  public abstract void doRequest(final HttpServletRequest req,
                                 final HttpServletResponse resp,
                                 final String resourceUri) throws SynchException;

  protected Object unmarshalBody(final HttpServletRequest req) throws SynchException {
    try {
      SOAPMessage msg = getSoapMsgFactory().createMessage(null, // headers
                                                          req.getInputStream());

      SOAPBody body = msg.getSOAPBody();

      Unmarshaller u = getEwsJAXBContext().createUnmarshaller();

      Object o = u.unmarshal(body.getFirstChild());

      if (o instanceof JAXBElement) {
        // Some of them get wrapped.
        o = ((JAXBElement)o).getValue();
      }

      return o;
    } catch (SynchException se) {
      throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  protected void marshalBody(final HttpServletResponse resp,
                             final Object body) throws SynchException {
    try {
      Marshaller marshaller = getEwsJAXBContext().createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      Document doc = dbf.newDocumentBuilder().newDocument();

      SOAPMessage msg = getSoapMsgFactory().createMessage();
      msg.getSOAPBody().addDocument(doc);

      marshaller.marshal(body,
                         msg.getSOAPBody());

      resp.setCharacterEncoding("UTF-8");
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/xml; charset=UTF-8");

      msg.writeTo(resp.getOutputStream());
    } catch (SynchException se) {
      throw se;
    } catch(Throwable t) {
      throw new SynchException(t);
    }
  }

  /** ===================================================================
   *                   Logging methods
   *  =================================================================== */

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(final String msg) {
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

  protected void info(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }
}

