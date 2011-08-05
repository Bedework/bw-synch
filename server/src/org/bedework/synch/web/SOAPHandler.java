/* **********************************************************************
    Copyright 2010 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/

package org.bedework.exchgsynch.web;

import org.bedework.exchgsynch.ExchangeSynch;
import org.bedework.exchgsynch.intf.SynchException;

import org.apache.log4j.Logger;
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

  private ExchangeSynch syncher;

  private String contextPath;

  // Are these thread safe?
  private MessageFactory soapMsgFactory;
  private JAXBContext ewsjc;

  /**
   * @param syncher
   * @throws SynchException
   */
  public void init(final ExchangeSynch syncher) throws SynchException {
    debug = getLogger().isDebugEnabled();
    this.syncher = syncher;
  }

  protected void setContextPath(final String contextPath) {
    this.contextPath = contextPath;
  }

  /** Get syncher
   *
   * @return ExchangeSynch
   */
  protected ExchangeSynch getSyncher() {
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

