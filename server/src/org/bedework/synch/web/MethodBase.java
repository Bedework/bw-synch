/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import java.net.URLDecoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

/** Base class for all webdav servlet methods.
 */
public abstract class MethodBase {
  protected boolean debug;

  protected boolean dumpContent;

  protected transient Logger log;

  protected ExchangeSynch syncher;

  //private String resourceUri;

  // private String content;

  //protected XmlEmit xml;

  /** Called at each request
   *
   * @throws SynchException
   */
  public abstract void init() throws SynchException;

  private SimpleDateFormat httpDateFormatter =
      new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ");

  /**
   * @param req
   * @param resp
   * @throws SynchException
   */
  public abstract void doMethod(HttpServletRequest req,
                                HttpServletResponse resp)
        throws SynchException;

  /** Allow servlet to create method.
   */
  public static class MethodInfo {
    private Class<? extends MethodBase> methodClass;

    private boolean requiresAuth;

    /**
     * @param methodClass
     * @param requiresAuth
     */
    public MethodInfo(final Class<? extends MethodBase> methodClass,
                      final boolean requiresAuth) {
      this.methodClass = methodClass;
      this.requiresAuth = requiresAuth;
    }

    /**
     * @return Class for this method
     */
    public Class<? extends MethodBase> getMethodClass() {
      return methodClass;
    }

    /** Called when servicing a request to determine if this method requires
     * authentication. Allows the servlet to reject attempts to change state
     * while unauthenticated.
     *
     * @return boolean true if authentication required.
     */
    public boolean getRequiresAuth() {
      return requiresAuth;
    }
  }

  /** Called at each request
   *
   * @param syncher
   * @param dumpContent
   * @throws SynchException
   */
  public void init(final ExchangeSynch syncher,
                   final boolean dumpContent) throws SynchException {
    this.syncher = syncher;
    this.dumpContent = dumpContent;

    debug = getLogger().isDebugEnabled();
//    xml = syncher.getXmlEmit();

    // content = null;
    //resourceUri = null;

    init();
  }

  /** Get syncher
   *
   * @return ExchangeSynch
   */
  public ExchangeSynch getSyncher() {
    return syncher;
  }

  /** Get the decoded and fixed resource URI
   *
   * @param req      Servlet request object
   * @return String  fixed up uri
   * @throws SynchException
   */
  public String getResourceUri(final HttpServletRequest req)
      throws SynchException {
    String uri = req.getServletPath();

    if ((uri == null) || (uri.length() == 0)) {
      /* No path specified - set it to root. */
      uri = "/";
    }

    return fixPath(uri);
  }

  /** Return a path, beginning with a "/", after "." and ".." are removed.
   * If the parameter path attempts to go above the root we return null.
   *
   * Other than the backslash thing why not use URI?
   *
   * @param path      String path to be fixed
   * @return String   fixed path
   * @throws SynchException
   */
  public static String fixPath(final String path) throws SynchException {
    if (path == null) {
      return null;
    }

    String decoded;
    try {
      decoded = URLDecoder.decode(path, "UTF8");
    } catch (Throwable t) {
      throw new SynchException("bad path: " + path);
    }

    if (decoded == null) {
      return (null);
    }

    /** Make any backslashes into forward slashes.
     */
    if (decoded.indexOf('\\') >= 0) {
      decoded = decoded.replace('\\', '/');
    }

    /** Ensure a leading '/'
     */
    if (!decoded.startsWith("/")) {
      decoded = "/" + decoded;
    }

    /** Remove all instances of '//'.
     */
    while (decoded.indexOf("//") >= 0) {
      decoded = decoded.replaceAll("//", "/");
    }

    if (decoded.indexOf("/.") < 0) {
      return decoded;
    }

    /** Somewhere we may have /./ or /../
     */

    StringTokenizer st = new StringTokenizer(decoded, "/");

    ArrayList<String> al = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      String s = st.nextToken();

      if (s.equals(".")) {
        // ignore
      } else if (s.equals("..")) {
        // Back up 1
        if (al.size() == 0) {
          // back too far
          return null;
        }

        al.remove(al.size() - 1);
      } else {
        al.add(s);
      }
    }

    /** Reconstruct */
    StringBuilder sb = new StringBuilder();
    for (String s: al) {
      sb.append('/');
      sb.append(s);
    }

    return sb.toString();
  }

  /*
  protected void addStatus(final int status,
                           final String message) throws SynchException {
    try {
      if (message == null) {
//        message = WebdavStatusCode.getMessage(status);
      }

      property(WebdavTags.status, "HTTP/1.1 " + status + " " + message);
    } catch (SynchException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }
  */

  protected void addHeaders(final HttpServletResponse resp) throws SynchException {
    // This probably needs changes
/*
    StringBuilder methods = new StringBuilder();
    for (String name: getSyncher().getMethodNames()) {
      if (methods.length() > 0) {
        methods.append(", ");
      }

      methods.append(name);
    }

    resp.addHeader("Allow", methods.toString());
    */
    resp.addHeader("Allow", "POST, GET");
  }

  /** Parse the request body, and return the DOM representation.
   *
   * @param req        Servlet request object
   * @param resp       Servlet response object for bad status
   * @return Document  Parsed body or null for no body
   * @exception WebdavException Some error occurred.
   */
  protected Document parseContent(final HttpServletRequest req,
                                  final HttpServletResponse resp)
      throws SynchException{
    int len = req.getContentLength();
    if (len == 0) {
      return null;
    }

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      //DocumentBuilder builder = factory.newDocumentBuilder();
/*
      Reader rdr = getNsIntf().getReader(req);

      if (rdr == null) {
        // No content?
        return null;
      }

      return builder.parse(new InputSource(rdr));*/
      return null;
//    } catch (SAXException e) {
  //    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    //  throw new SynchException(HttpServletResponse.SC_BAD_REQUEST);
    } catch (Throwable t) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      throw new SynchException(t);
    }
  }

  protected String formatHTTPDate(final Timestamp val) {
    if (val == null) {
      return null;
    }

    synchronized (httpDateFormatter) {
      return httpDateFormatter.format(val) + "GMT";
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

  protected void logIt(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }
}

