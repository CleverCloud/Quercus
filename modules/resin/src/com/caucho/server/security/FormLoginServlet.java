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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.security;

import com.caucho.security.FormLogin;
import com.caucho.security.Login;
import com.caucho.security.LoginList;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.RequestDispatcherImpl;
import com.caucho.util.L10N;

import javax.servlet.GenericServlet;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FormLoginServlet extends GenericServlet {
  private final Logger log
    = Logger.getLogger(FormLoginServlet.class.getName());
  private static final L10N L = new L10N(FormLoginServlet.class);
  
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    WebApp app = (WebApp) getServletContext();
    FormLogin login = getFormLogin(app.getLogin());

    String username = request.getParameter("j_username");
    String password = request.getParameter("j_password");

    Principal user = login.login(req, res, true);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " login " + user);

    if (res.isCommitted())
      return;

    if (user == null) {
      // A failure internally redirects to the error page (not redirect)
      String errorPage = login.getFormErrorPage();
      RequestDispatcherImpl disp;
      disp = (RequestDispatcherImpl) app.getRequestDispatcher(errorPage);

      // req.setAttribute("caucho.login", "login");
      if (res instanceof CauchoResponse) {
        ((CauchoResponse) res).killCache();
        ((CauchoResponse) res).setNoCache(true);
      }
      else {
        res.setDateHeader("Expires", 0);
        res.setHeader("Cache-Control", "no-cache");
      }
      
      disp.error(req, res);
      return;
    }
    
    HttpSession session = req.getSession();
    
    String uri = (String) session.getValue(FormLogin.LOGIN_SAVED_PATH);
    String query = (String) session.getValue(FormLogin.LOGIN_SAVED_QUERY);

    session.removeAttribute(FormLogin.LOGIN_SAVED_PATH);
    session.removeAttribute(FormLogin.LOGIN_SAVED_QUERY);

    if (log.isLoggable(Level.FINE)) {
      log.fine("old path:" + uri + " query:" + query + " j_uri:" +
               req.getParameter("j_uri"));
    }

    boolean formURIPriority = login.getFormURIPriority();

    // The saved uri has priority.
    if ((uri == null || formURIPriority) && req.getParameter("j_uri") != null)
      uri = req.getParameter("j_uri");
    else if (uri != null && query != null)
      uri = uri + "?" + query;

    if (uri == null) {
      log.warning(L.l("FormLogin: session has timed out for session '{0}'",
                      req.getSession().getId()));
      
      RequestDispatcher disp = request.getRequestDispatcher("/");
      if (disp != null) {
        disp.forward(request, response);
        return;
      }
      else {
        throw new ServletException(L.l("Session has timed out for form authentication, no forwarding URI is available.  Either the login form must specify j_uri or the session must have a saved URI."));
      }
    }

    if (uri.indexOf('\n') >= 0 || uri.indexOf('\r') >= 0)
      throw new ServletException(L.l("Forwarding URI '{0}' is invalid.",
                                     uri));

    String uriPwd = req.getRequestURI();
    int p = uriPwd.indexOf("/j_security_check");
    if (p >= 0)
      uriPwd = uriPwd.substring(0, p + 1);
    
    if (uri.length() == 0) {
    }
    else if (uri.charAt(0) == '/')
      uri = req.getContextPath() + uri;
    else if (uri.indexOf(':') >= 0 &&
             (uri.indexOf(':') < uri.indexOf('/') ||
              uri.indexOf('/') < 0)) {
    }
    else {
      uri = uriPwd + uri;
    }

    // The spec says that a successful login uses a redirect.  Resin
    // adds a configuration option to allow an internal forward
    // if the URL is in the same directory.
    
    // Logins to POST pages need to use an internal forward.
    // Most GETs will want a redirect.
    boolean useInternalForward = login.getInternalForward();
    
    if (useInternalForward
        && uri.startsWith(uriPwd)
        && uri.indexOf('/', uriPwd.length() + 1) < 0) {
      WebApp newApp = (WebApp) app.getContext(uri);
      String suffix = uri.substring(newApp.getContextPath().length());
      
      // force authorization of the page because the normal forward()
      // bypasses authorization
      RequestDispatcher disp = newApp.getLoginDispatcher(suffix);
      if (disp != null) {
        disp.forward(req, res);
        return;
      }
    }
    
    res.sendRedirect(res.encodeRedirectURL(uri));
  }

  private FormLogin getFormLogin(Login login)
    throws ServletException
  {
    if (login == null)
      throw new ServletException(L.l("j_security_check requires a login"));

    if (login instanceof FormLogin)
      return (FormLogin) login;
    else if (login instanceof LoginList) {
      for (Login subLogin : ((LoginList) login).getLoginList()) {
        if (subLogin instanceof FormLogin)
          return (FormLogin) subLogin;
      }
    }

    throw new ServletException(L.l("FormLoginServlet requires a form login auth-type configuration at '{0}' in '{1}'",
                                   login != null
                                   ? login.getAuthType()
                                   : null,
                                   getServletContext()));
  }
}
