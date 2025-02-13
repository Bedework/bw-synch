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

import org.bedework.synch.SynchEngineImpl;
import org.bedework.synch.service.SynchConf;
import org.bedework.synch.shared.SynchEngine;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.synch.web.MethodBase.MethodInfo;
import org.bedework.util.http.service.HttpOut;
import org.bedework.util.jmx.ConfBase;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.servlet.io.CharArrayWrappedResponse;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.WebdavTags;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

import javax.management.ObjectName;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import javax.xml.namespace.QName;

/** WebDAV Servlet.
 * This abstract servlet handles the request/response nonsense and calls
 * abstract routines to interact with an underlying data source.
 *
 * @author Mike Douglass   douglm@rpi.edu
 * @version 1.0
 */
public class SynchServlet extends HttpServlet
        implements Logged, HttpSessionListener, ServletContextListener {
  protected boolean dumpContent;

  /** Table of methods - set at init
   */
  protected HashMap<String, MethodInfo> methods = new HashMap<>();

  /* Try to serialize requests from a single session
   * This is very imperfect.
   */
  static class Waiter {
    boolean active;
    int waiting;
  }

  private static final HashMap<String, Waiter> waiters = new HashMap<>();

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);

    dumpContent = "true".equals(config.getInitParameter("dumpContent"));

    addMethods();
  }

  @Override
  protected void service(final HttpServletRequest req,
                         HttpServletResponse resp)
      throws IOException {
	SynchEngine syncher = null;
    boolean serverError = false;

    try {
      if (debug()) {
        debug("entry: " + req.getMethod());
        dumpRequest(req);
      }

      tryWait(req, true);

      syncher = SynchEngineImpl.getSyncher();

      if (req.getCharacterEncoding() == null) {
        req.setCharacterEncoding("UTF-8");
        if (debug()) {
          debug("No charset specified in request; forced to UTF-8");
        }
      }

      if (debug() && dumpContent) {
        resp = new CharArrayWrappedResponse(resp);
      }

      String methodName = req.getHeader("X-HTTP-Method-Override");

      if (methodName == null) {
        methodName = req.getMethod();
      }

      final MethodBase method = getMethod(syncher, methodName);

      if (method == null) {
        info("No method for '" + methodName + "'");

        // ==========================================================
        //     Set the correct response
        // ==========================================================
      } else {
        method.init(syncher, dumpContent);
        method.doMethod(req, resp);
      }
//    } catch (WebdavForbidden wdf) {
  //    sendError(syncher, wdf, resp);
    } catch (final Throwable t) {
      serverError = handleException(syncher, t, resp, serverError);
    } finally {
      if (syncher != null) {
        try {
//          syncher.close();
        } catch (final Throwable t) {
          serverError = handleException(syncher, t, resp, serverError);
        }
      }

      try {
        tryWait(req, false);
      } catch (final Throwable ignored) {}

      if (debug() && dumpContent &&
          (resp instanceof final CharArrayWrappedResponse wresp)) {
        /* instanceof check because we might get a subsequent exception before
         * we wrap the response
         */

        if (wresp.getUsedOutputStream()) {
          debug("------------------------ response written to output stream -------------------");
        } else {
          final String str = wresp.toString();

          debug("------------------------ Dump of response -------------------");
          debug(str);
          debug("---------------------- End dump of response -----------------");

          final byte[] bs = str.getBytes();
          resp = (HttpServletResponse)wresp.getResponse();
          debug("contentLength=" + bs.length);
          resp.setContentLength(bs.length);
          resp.getOutputStream().write(bs);
        }
      }

      /* WebDAV is stateless - toss away the session */
      try {
        final HttpSession sess = req.getSession(false);
        if (sess != null) {
          sess.invalidate();
        }
      } catch (final Throwable ignored) {}
    }
  }

  /* Return true if it's a server error */
  private boolean handleException(final SynchEngine syncher, final Throwable t,
                                  final HttpServletResponse resp,
                                  boolean serverError) {
    if (serverError) {
      return true;
    }

    try {
      if (t instanceof final SynchException se) {
        final int status = se.getStatusCode();
        if (status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
          error(se);
          serverError = true;
        }
        sendError(syncher, t, resp);
        return serverError;
      }

      error(t);
      sendError(syncher, t, resp);
      return true;
    } catch (final Throwable t1) {
      // Pretty much screwed if we get here
      return true;
    }
  }

  private void sendError(final SynchEngine syncher, final Throwable t,
                         final HttpServletResponse resp) {
    try {
      if (t instanceof final SynchException se) {
        final QName errorTag = se.getErrorTag();

        if (errorTag != null) {
          if (debug()) {
            debug("setStatus(" + se.getStatusCode() + ")");
          }
          resp.setStatus(se.getStatusCode());
          resp.setContentType("text/xml; charset=UTF-8");
          if (!emitError(syncher, errorTag, se.getMessage(),
                         resp.getWriter())) {
            final StringWriter sw = new StringWriter();
            emitError(syncher, errorTag, se.getMessage(), sw);

            try {
              if (debug()) {
                debug("setStatus(" + se.getStatusCode() + ")");
              }
              resp.sendError(se.getStatusCode(), sw.toString());
            } catch (final Throwable ignored) {
            }
          }
        } else {
          if (debug()) {
            debug("setStatus(" + se.getStatusCode() + ")");
          }
          resp.sendError(se.getStatusCode(), se.getMessage());
        }
      } else {
        if (debug()) {
          debug("setStatus(" + HttpServletResponse.SC_INTERNAL_SERVER_ERROR + ")");
        }
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                       t.getMessage());
      }
    } catch (final Throwable ignored) {
      // Pretty much screwed if we get here
    }
  }

  private boolean emitError(final SynchEngine syncher,
                            final QName errorTag,
                            final String extra,
                            final Writer wtr) {
    try {
      final XmlEmit xml = new XmlEmit();
//      syncher.addNamespace(xml);

      xml.startEmit(wtr);
      xml.openTag(WebdavTags.error);

  //    syncher.emitError(errorTag, extra, xml);

      xml.closeTag(WebdavTags.error);
      xml.flush();

      return true;
    } catch (final Throwable t1) {
      // Pretty much screwed if we get here
      return false;
    }
  }

  /** Add methods for this namespace
   *
   */
  protected void addMethods() {
    methods.put("POST", new MethodInfo(PostMethod.class, true));
    /*
    methods.put("ACL", new MethodInfo(AclMethod.class, false));
    methods.put("COPY", new MethodInfo(CopyMethod.class, false));
    methods.put("GET", new MethodInfo(GetMethod.class, false));
    methods.put("HEAD", new MethodInfo(HeadMethod.class, false));
    methods.put("OPTIONS", new MethodInfo(OptionsMethod.class, false));
    methods.put("PROPFIND", new MethodInfo(PropFindMethod.class, false));

    methods.put("DELETE", new MethodInfo(DeleteMethod.class, true));
    methods.put("MKCOL", new MethodInfo(MkcolMethod.class, true));
    methods.put("MOVE", new MethodInfo(MoveMethod.class, true));
    methods.put("POST", new MethodInfo(PostMethod.class, true));
    methods.put("PROPPATCH", new MethodInfo(PropPatchMethod.class, true));
    methods.put("PUT", new MethodInfo(PutMethod.class, true));
    */

    //methods.put("LOCK", new MethodInfo(LockMethod.class, true));
    //methods.put("UNLOCK", new MethodInfo(UnlockMethod.class, true));
  }

  /**
   * @param syncher the engine
   * @param name of method
   * @return method
   */
  public MethodBase getMethod(final SynchEngine syncher,
                              final String name) {
    final MethodInfo mi = methods.get(name.toUpperCase());

//    if ((mi == null) || (getAnonymous() && mi.getRequiresAuth())) {
  //    return null;
    //}

    try {
      final MethodBase mb = mi.getMethodClass().newInstance();

      mb.init(syncher, dumpContent);

      return mb;
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }
      throw new SynchException(t);
    }
  }

  private void tryWait(final HttpServletRequest req, final boolean in) throws Throwable {
    Waiter wtr;
    synchronized (waiters) {
      //String key = req.getRequestedSessionId();
      final String key = req.getRemoteUser();
      if (key == null) {
        return;
      }

      wtr = waiters.get(key);
      if (wtr == null) {
        if (!in) {
          return;
        }

        wtr = new Waiter();
        wtr.active = true;
        waiters.put(key, wtr);
        return;
      }
    }

    synchronized (wtr) {
      if (!in) {
        wtr.active = false;
        wtr.notify();
        return;
      }

      wtr.waiting++;
      while (wtr.active) {
        if (debug()) {
          debug("in: waiters=" + wtr.waiting);
        }

        wtr.wait();
      }
      wtr.waiting--;
      wtr.active = true;
    }
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
   */
  @Override
  public void sessionCreated(final HttpSessionEvent se) {
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
   */
  @Override
  public void sessionDestroyed(final HttpSessionEvent se) {
    final HttpSession session = se.getSession();
    final String sessid = session.getId();
    if (sessid == null) {
      return;
    }

    synchronized (waiters) {
      waiters.remove(sessid);
    }
  }

  /** Debug
   *
   * @param req http request
   */
  public void dumpRequest(final HttpServletRequest req) {
    try {
      final var names = req.getHeaderNames();

      String title = "Request headers";

      debug(title);

      while (names.hasMoreElements()) {
        final String key = names.nextElement();
        final String val = req.getHeader(key);
        debug("  " + key + " = \"" + val + "\"");
      }

      final var parnames = req.getParameterNames();

      title = "Request parameters";

      debug(title + " - global info and uris");
      debug("getRemoteAddr = " + req.getRemoteAddr());
      debug("getRequestURI = " + req.getRequestURI());
      debug("getRemoteUser = " + req.getRemoteUser());
      debug("getRequestedSessionId = " + req.getRequestedSessionId());
      debug("HttpUtils.getRequestURL(req) = " + req.getRequestURL());
      debug("contextPath=" + req.getContextPath());
      debug("query=" + req.getQueryString());
      debug("contentlen=" + req.getContentLength());
      debug("request=" + req);
      debug("parameters:");

      debug(title);

      while (parnames.hasMoreElements()) {
        final String key = parnames.nextElement();
        final String val = req.getParameter(key);
        debug("  " + key + " = \"" + val + "\"");
      }
    } catch (final Throwable ignored) {
    }
  }

  /* -----------------------------------------------------------------------
   *                         JMX support
   */

  class Configurator extends ConfBase {
    SynchConf synchConf;

    Configurator() {
      super("org.bedework.synch:service=Synch",
            (String)null,
            null);
    }

    @Override
    public String loadConfig() {
      return null;
    }

    @Override
    public void start() {
      try {
        getManagementContext().start();

        synchConf = new SynchConf();
        register(new ObjectName(synchConf.getServiceName()), synchConf);
        synchConf.loadConfig();
        synchConf.start();

      /* ------------- Http properties -------------------- */
        final HttpOut ho = new HttpOut("synch",
                                       "httpConfig");
        register(new ObjectName(ho.getServiceName()), ho);
        ho.loadConfig();
      } catch (final Throwable t){
        t.printStackTrace();
      }
    }

    @Override
    public void stop() {
      try {
        synchConf.stop();
        getManagementContext().stop();
      } catch (final Throwable t){
        t.printStackTrace();
      }
    }
  }

  private final Configurator conf = new Configurator();

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    conf.start();
  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    conf.stop();
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
