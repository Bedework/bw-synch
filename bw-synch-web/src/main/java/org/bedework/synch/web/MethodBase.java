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

import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;

import org.w3c.dom.Document;

import java.net.URLDecoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

/** Base class for all webdav servlet methods.
 */
public abstract class MethodBase implements Logged {
  protected boolean dumpContent;

  protected SynchEngine syncher;

  //private String resourceUri;

  // private String content;

  //protected XmlEmit xml;

  /** Called at each request
   *
   * @throws SynchException on fatal error
   */
  public abstract void init() throws SynchException;

  private final SimpleDateFormat httpDateFormatter =
      new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ");

  /**
   * @param req http request
   * @param resp http response
   * @throws SynchException on fatal error
   */
  public abstract void doMethod(HttpServletRequest req,
                                HttpServletResponse resp)
        throws SynchException;

  /** Allow servlet to create method.
   */
  public static class MethodInfo {
    private final Class<? extends MethodBase> methodClass;

    private final boolean requiresAuth;

    /**
     * @param methodClass class
     * @param requiresAuth true if method requires authentication
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
    @SuppressWarnings("unused")
    public boolean getRequiresAuth() {
      return requiresAuth;
    }
  }

  /** Called at each request
   *
   * @param syncher the synch engine
   * @param dumpContent true for debug dums of content
   * @throws SynchException on init failure
   */
  public void init(final SynchEngine syncher,
                   final boolean dumpContent) throws SynchException {
    this.syncher = syncher;
    this.dumpContent = dumpContent;
//    xml = syncher.getXmlEmit();

    // content = null;
    //resourceUri = null;

    init();
  }

  /** Get syncher
   *
   * @return ExchangeSynch
   */
  public SynchEngine getSyncher() {
    return syncher;
  }

  /** Get the decoded and fixed resource URI. This calls getServletPath() to
   * obtain the path information. The description of that method is a little
   * obscure in it's meaning. In a request of this form:<br/><br/>
   * "GET /ucaldav/user/douglm/calendar/1302064354993-g.ics HTTP/1.1[\r][\n]"<br/><br/>
   * getServletPath() will return <br/><br/>
   * /user/douglm/calendar/1302064354993-g.ics<br/><br/>
   * that is the context has been removed. In addition this method will URL
   * decode the path. getRequestUrl() does neither.
   *
   * @param req      Servlet request object
   * @return List    Path elements of fixed up uri
   */
  public List<String> getResourceUri(final HttpServletRequest req) {
    String uri = req.getServletPath();

    if ((uri == null) || (uri.length() == 0)) {
      /* No path specified - set it to root. */
      uri = "/";
    }

    return fixPath(uri);
  }

  /** Return a path, broken into its elements, after "." and ".." are removed.
   * If the parameter path attempts to go above the root we return null.
   *
   * Other than the backslash thing why not use URI?
   *
   * @param path      String path to be fixed
   * @return fixed path broken into elements or null for bad or missing path
   */
  public static List<String> fixPath(final String path) {
    if (path == null) {
      return null;
    }

    String decoded;
    try {
      decoded = URLDecoder.decode(path, "UTF8");
    } catch (final Throwable ignored) {
      return null; // bad path
    }

    if (decoded == null) {
      return (null);
    }

    /* Make any backslashes into forward slashes.
     */
    if (decoded.indexOf('\\') >= 0) {
      decoded = decoded.replace('\\', '/');
    }

    /* Ensure a leading '/'
     */
    if (!decoded.startsWith("/")) {
      decoded = "/" + decoded;
    }

    /* Remove all instances of '//'.
     */
    while (decoded.contains("//")) {
      decoded = decoded.replaceAll("//", "/");
    }

    /* Somewhere we may have /./ or /../
     */

    final StringTokenizer st = new StringTokenizer(decoded, "/");

    final ArrayList<String> al = new ArrayList<>();
    while (st.hasMoreTokens()) {
      final String s = st.nextToken();

      if (s.equals(".")) {
        // ignore
        continue;
      }

      if (s.equals("..")) {
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

    return al;
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

  @SuppressWarnings("unused")
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
   * @exception SynchException Some error occurred.
   */
  @SuppressWarnings("unused")
  protected Document parseContent(final HttpServletRequest req,
                                  final HttpServletResponse resp)
      throws SynchException {
    final int len = req.getContentLength();
    if (len == 0) {
      return null;
    }

    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
    } catch (final Throwable t) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      throw new SynchException(t);
    }
  }

  @SuppressWarnings("unused")
  protected String formatHTTPDate(final Timestamp val) {
    if (val == null) {
      return null;
    }

    synchronized (httpDateFormatter) {
      return httpDateFormatter.format(val) + "GMT";
    }
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}

