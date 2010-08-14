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

package com.caucho.resin.deploy;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.webapp.*;
import com.caucho.config.types.*;

/**
 * HTTP-based deployment service for local updates to the embedded
 * server, e.g. from an IDE.
 *
 * <pre><code>
 * &lt;web-app xmlns="http://caucho.com/ns/resin">
 *
 *   &lt;servlet-mapping url-pattern="/deploy"
 *               servlet-class="com.caucho.resin.deploy.LocalDeployServlet">
 *     &lt;init>
 *       &lt;enable>true&lt;/enable>
 *     &lt;/init>
 *   &lt;/servlet-mapping>
 *
 * &lt;/web-app>
 * </code</pre>
 */
public class LocalDeployServlet extends GenericServlet
{
  private static final Logger log
    = Logger.getLogger(LocalDeployServlet.class.getName());
  private static final L10N L = new L10N(LocalDeployServlet.class);
  
  private boolean _isEnable;
  private String _role = "manager";

  private HashMap<String,WebAppContainer> _webAppMap
    = new HashMap<String,WebAppContainer>();

  /**
   * Enable is required to enable the servlet.
   */
  public void setEnable(boolean isEnable)
  {
    _isEnable = isEnable;
  }

  /**
   * Role is a login requirement.  The default is 'manager', so applications
   * which don't want a login requirement will need to set this null.
   */
  public void setRole(String role)
  {
    if ("*".equals(role) || "".equals(role) || "any".equals(role))
      _role = null;
    else
      _role = role;
  }

  /**
   * Returns the owning webapp
   */
  public WebApp getWebApp()
  {
    return (WebApp) getServletContext();
  }

  /**
   * Returns the webapp container
   */
  public WebAppContainer getWebAppContainer()
  {
    return getWebApp().getParent();
  }
  
  /**
   * Handle the deployment request
   */
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if (! _isEnable) {
      log.warning(L.l("LocalDeployServlet[] non-enabled access from IP='{0}'",
                      req.getRemoteAddr()));
      
      res.sendError(HttpServletResponse.SC_FORBIDDEN);

      return;
    }

    String addr = req.getRemoteAddr();
    
    if (! addr.startsWith("127.")) {
      log.warning(L.l("LocalDeployServlet[] non-local access from IP='{0}'",
                      req.getRemoteAddr()));
      
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      
      return;
    }

    if (_role != null && ! req.isUserInRole(_role)) {
      log.warning(L.l("LocalDeployServlet[] user not in role '{0}' from IP='{1}'",
                      _role,
                      req.getRemoteAddr()));
      
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      
      return;
    }

    String action = req.getParameter("action");
    
    if ("add-web-app".equals(action)) {
      addWebApp(req, res);
    }
    else {
      log.warning(L.l("LocalDeployServlet[] unknown action '{0}' from IP='{1}'",
                      action, req.getRemoteAddr()));
      
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      
      return;
    }
  }

  /**
   * Adds a new web-app
   */
  private void addWebApp(HttpServletRequest req,
                         HttpServletResponse res)
    throws IOException, ServletException
  {
    String contextPath = req.getParameter("context-path");
    String root = req.getParameter("root");
    String war = req.getParameter("war");

    if ("".equals(root))
      root = null;

    if ("".equals(war))
      war = null;

    if (contextPath == null || "".equals(contextPath)) {
      log.warning(L.l("LocalDeployServlet[] add-web-app requires context-path from IP='{0}'",
                      req.getRemoteAddr()));
      
      res.sendError(HttpServletResponse.SC_FORBIDDEN);

      return;
    }

    if (root == null && war == null) {
      log.warning(L.l("LocalDeployServlet[] add-web-app requires root or war from IP='{0}'",
                      req.getRemoteAddr()));
      
      res.sendError(HttpServletResponse.SC_FORBIDDEN);

      return;
    }

    WebAppContainer container = getWebAppContainer();

    Path containerRoot = container.getRootDirectory();
    Path rootDirectory;

    if (root == null) {
      rootDirectory = containerRoot.lookup("work").lookup("./" + contextPath);
      rootDirectory.mkdirs();
    }
    else
      rootDirectory = containerRoot.lookup(root);

    if (war != null) {
      Path warPath = containerRoot.lookup(war);

      if (! warPath.exists()) {
        log.warning(L.l("LocalDeployServlet[] add-web-app war='{0}' cannot be read, from IP='{0}'",
                      warPath.getURL(), req.getRemoteAddr()));
      }
    }

    WebAppController controller = container.findController(contextPath);

    PrintWriter out = res.getWriter();
    
    if (controller != null) {
      out.println("web-app " + contextPath + " exists");
      // XXX: force update?
      return;
    }

    WebAppConfig webApp = new WebAppConfig();
    webApp.setContextPath(contextPath);
    webApp.setRootDirectory(new RawString(rootDirectory.getURL()));

    if (war != null)
      webApp.setArchivePath(new RawString(war));

    try {
      container.addWebApp(webApp);
    } catch (Exception e) {
      throw new ServletException(e);
    }
    
    out.println("OK");
  }
}
