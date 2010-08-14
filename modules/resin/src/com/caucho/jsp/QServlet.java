/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp;

import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.RequestAdapter;
import com.caucho.server.http.ResponseAdapter;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base servlet for both JSP and XTP.  It's primarily responsible for
 * returning the proper error messages when things go wrong.
 *
 * <p>The manager create the compiled JSP and XTP pages.  The manager
 * returns a Page object which is actually executed.
 *
 * @see JspManager
 * @see XtpManager
 * @see Page
 */
abstract public class QServlet implements Servlet {
  static final String COPYRIGHT =
    "Copyright(c) 1998-2010 Caucho Technology.  All rights reserved.";

  private static final Logger log
    = Logger.getLogger(QServlet.class.getName());
  private static final L10N L = new L10N(QServlet.class);

  protected WebApp _webApp;
  private PageManager _manager;

  private ServletConfig _config;

  /**
   * Initialize the servlet.  If necessary, convert the ServletContext
   * to a CauchoWebApp.  Also, read the configuration Registry
   * it it hasn't been read yet.
   */
  public void init(ServletConfig config) throws ServletException
  {
    ServletContext cxt = config.getServletContext();
    _webApp = (WebApp) cxt;

    _config = config;

    log.finer(config.getServletName() + " init");
  }

  /**
   * JspServlet and XtpServlet will set the PageManager with this method.
   */
  protected void setManager(PageManager manager)
  {
    _manager = manager;
  }

  protected PageManager getManager()
  {
    return _manager;
  }

  /**
   * Override the Servlet method to return the generated application.
   */
  public ServletContext getServletContext()
  {
    return _webApp;
  }

  /**
   * Returns the config.
   */
  public ServletConfig getServletConfig()
  {
    return _config;
  }

  /**
   * Returns the init parameter
   */
  public String getInitParameter(String name)
  {
    return _config.getInitParameter(name);
  }

  /**
   * The service method gets the JSP/XTP page and executes it.  The
   * request and response objects are converted to Caucho objects so
   * other servlet runners will produce the same results as the Caucho
   * servlet runner.
   */
  public void service(ServletRequest req, ServletResponse res)
    throws ServletException, IOException
  {
    CauchoRequest request;
    CauchoResponse response;
    ResponseAdapter resAdapt = null;

    if (req instanceof CauchoRequest)
      request = (CauchoRequest) req;
    else
      request = RequestAdapter.create((HttpServletRequest) req, _webApp);

    if (res instanceof CauchoResponse)
      response = (CauchoResponse) res;
    else {
      resAdapt = ResponseAdapter.create((HttpServletResponse) res);
      response = resAdapt;
    }

    Page page = null;

    try {
      page = getPage(request, response);

      if (page == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      page.service(request, response);
    }
    catch (JspParseException e) {
      if (e.getErrorPage() != null)
        forwardErrorPage(request, response, e, e.getErrorPage());
      else
        throw new ServletException(e);
    }
    catch (ClientDisconnectException e) {
      throw e;
    }
    catch (Throwable e) {
      if (page != null && page.getErrorPage() != null &&
          forwardErrorPage(request, response, e, page.getErrorPage())) {
      }
      else if (e instanceof IOException) {
        log.log(Level.FINE, e.toString(), e);
        throw (IOException) e;
      }
      else if (e instanceof ServletException) {
        log.log(Level.FINE, e.toString(), e);
        throw (ServletException) e;
      }
      else {
        log.log(Level.FINE, e.toString(), e);
        throw new ServletException(e);
      }
    }

    if (resAdapt != null) {
      resAdapt.close();
      ResponseAdapter.free(resAdapt);
    }
  }

  /**
   * Creates and returns a new page.
   *
   * @param request the servlet request
   * @param response the servlet response
   *
   * @return the compiled page
   */
  public Page getPage(HttpServletRequest request,
                      HttpServletResponse response)
    throws Exception
  {
    try {
      Page page = getSubPage(request, response);

      return page;
    }
    catch (JspParseException e) {
      if (e.getErrorPage() != null) {
        if (e.getCause() != null && ! (e instanceof JspLineParseException))
          forwardErrorPage(request, response, e.getCause(), e.getErrorPage());
        else
          forwardErrorPage(request, response, e, e.getErrorPage());

        return null;
      }
      else
        throw e;
    }
  }

  /**
   * Given a request and response, returns the compiled Page.  For example,
   * JspManager will return the JspPage and XtpManager will
   * return the XtpPage.
   *
   * @param req servlet request for generating the page.
   * @param res servlet response to any needed error messages.
   */
  private Page getSubPage(HttpServletRequest req, HttpServletResponse res)
    throws Exception
  {
    CauchoRequest cauchoRequest = null;

    initGetPage();
      
    /*
    if (! _webApp.isActive())
      throw new UnavailableException("JSP compilation unavailable during restart", 10);
    */

    if (req instanceof CauchoRequest)
      cauchoRequest = (CauchoRequest) req;

    String servletPath;

    if (cauchoRequest != null)
      servletPath = cauchoRequest.getPageServletPath();
    else
      servletPath = RequestAdapter.getPageServletPath(req);

    if (servletPath == null)
      servletPath = "/";

    String uri;
    String pageURI;

    if (cauchoRequest != null)
      uri = cauchoRequest.getPageURI();
    else
      uri = RequestAdapter.getPageURI(req);

    Path appDir = _webApp.getAppDir();

    String realPath;
    Path subcontext;

    Page page;
    ServletConfig config = null;

    String jspPath = (String) req.getAttribute("caucho.jsp.jsp-file");
    if (jspPath != null) {
      req.removeAttribute("caucho.jsp.jsp-file");

      subcontext = getPagePath(jspPath);

      return _manager.getPage(uri, jspPath, subcontext, config);
    }

    String pathInfo;

    if (cauchoRequest != null)
      pathInfo = cauchoRequest.getPagePathInfo();
    else
      pathInfo = RequestAdapter.getPagePathInfo(req);

    subcontext = getPagePath(servletPath);
    if (subcontext != null)
      return _manager.getPage(servletPath, subcontext);

    if (pathInfo == null) {
      realPath = _webApp.getRealPath(servletPath);
      subcontext = appDir.lookupNative(realPath);

      return _manager.getPage(servletPath, subcontext);
    }

    subcontext = getPagePath(servletPath + pathInfo);
    if (subcontext != null)
      return _manager.getPage(servletPath + pathInfo, subcontext);

    // If servlet path exists, can't use pathInfo to lookup servlet,
    // because /jsp/WEB-INF would be a security hole
    if (servletPath != null && ! servletPath.equals("")) {
      // server/0035
      throw new FileNotFoundException(L.l("{0} was not found on this server.",
                                          uri));
      // return null;
    }

    subcontext = getPagePath(pathInfo);
    if (subcontext != null)
      return _manager.getPage(pathInfo, subcontext);

    subcontext = getPagePath(uri);
    if (subcontext == null)
      throw new FileNotFoundException(L.l("{0} was not found on this server.",
                                          uri));
    
    return _manager.getPage(uri, subcontext);
  }

  private void initGetPage()
  {
    // marks the listener array as used
    _webApp.getJspApplicationContext().getELListenerArray();
  }

  public Page getPage(String uri, String pageURI, ServletConfig config)
    throws Exception
  {
    Path path = getPagePath(pageURI);

    if (path == null)
      return null;

    return _manager.getPage(uri, pageURI, path, config);
  }

  /**
   * Returns the path to be used as the servlet name.
   */
  private Path getPagePath(String pathName)
  {
    Path appDir = _webApp.getAppDir();
    String realPath = _webApp.getRealPath(pathName);
    Path path = appDir.lookupNative(realPath);

    if (path.isFile() && path.canRead())
      return path;

    java.net.URL url;
    ClassLoader loader = _webApp.getClassLoader();
    if (loader != null) {
      url = _webApp.getClassLoader().getResource(pathName);

      String name = url != null ? url.toString() : null;

      path = null;
      if (url != null && (name.endsWith(".jar") || name.endsWith(".zip")))
        path = JarPath.create(Vfs.lookup(url.toString())).lookup(pathName);
      else if (url != null)
        path = Vfs.lookup(url.toString());

      if (path != null && path.isFile() && path.canRead())
        return path;
    }

    url = ClassLoader.getSystemResource(pathName);
    String name = url != null ? url.toString() : null;

      path = null;
    if (url != null && (name.endsWith(".jar") || name.endsWith(".zip")))
      path = JarPath.create(Vfs.lookup(url.toString())).lookup(pathName);
    else if (url != null)
      path = Vfs.lookup(url.toString());

    if (path != null && path.isFile() && path.canRead())
      return path;
    else
      return null;
  }

  /**
   * Remove the page from any cache.
   */
  public void killPage(HttpServletRequest request,
                       HttpServletResponse response,
                       Page page)
  {
    _manager.killPage(request, response, page);
  }

  /**
   * Forwards an error to the proper error page.
   *
   * @param req the servlet request
   * @param res the servlet response
   * @param e the exception
   * @param errorPage the error page
   */
  private boolean forwardErrorPage(HttpServletRequest req,
                                   HttpServletResponse res,
                                   Throwable e, String errorPage)
    throws ServletException, IOException
  {
    req.setAttribute("javax.servlet.jsp.jspException", e);
    req.setAttribute("javax.servlet.error.exception_type", e);
    req.setAttribute("javax.servlet.error.request_uri",
                     req.getRequestURI());

    if (res instanceof CauchoResponse) {
      CauchoResponse cauchoResponse = (CauchoResponse) res;
      cauchoResponse.killCache();
      cauchoResponse.setNoCache(true);
    }

    RequestDispatcher rd = req.getRequestDispatcher(errorPage);

    if (rd == null)
      return false;

    rd.forward(req, res);

    return true;
  }

  public void destroy()
  {
    _manager.destroy();

    log.finer(_config.getServletName() + " destroy");
  }
}
