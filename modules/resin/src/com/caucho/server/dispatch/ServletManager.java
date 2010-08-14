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

package com.caucho.server.dispatch;

import com.caucho.util.L10N;
import com.caucho.config.*;
import com.caucho.config.inject.InjectManager;

import javax.annotation.PostConstruct;
import javax.el.ELContext;
import javax.el.ELContextEvent;
import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.context.FacesContextFactory;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletSecurityElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the servlets.
 */
public class ServletManager {
  static final Logger log = Logger.getLogger(ServletManager.class.getName());
  static final L10N L = new L10N(ServletManager.class);

  private HashMap<String,ServletConfigImpl> _servlets
    = new HashMap<String,ServletConfigImpl>();

  private ArrayList<ServletConfigImpl> _servletList
    = new ArrayList<ServletConfigImpl>();

  private ArrayList<ServletConfigImpl> _cronList
  = new ArrayList<ServletConfigImpl>();

  private Map<Class<? extends Servlet>, ServletSecurityElement>
    _servletSecurityElements
    = new HashMap<Class<? extends Servlet>, ServletSecurityElement>();

  private boolean _isLazyValidate;


  public ServletManager()
  {
  }

  /**
   * Sets true if validation is lazy.
   */
  public void setLazyValidate(boolean isLazy)
  {
    _isLazyValidate = isLazy;
  }

  public boolean isFacesServletConfigured()
  {
    for (ServletConfigImpl servletConfig : _servletList) {
      Class<?> servletClass = servletConfig.getServletClass();
      
      if (servletClass == null)
        continue;
      
      String className = servletClass.getName();

      if ("javax.faces.webapp.FacesServlet".equals(className))
        return true;
    }

    return false;
  }

  public void addServlet(ServletConfigImpl config)
    throws ServletException
  {
    addServlet(config, false);
  }

  /**
   * Adds a servlet to the servlet manager.
   */
  public void addServlet(ServletConfigImpl config, boolean merge)
    throws ServletException
  {
    if (config.getServletContext() == null)
      throw new NullPointerException();

    config.setServletManager(this);

    synchronized (_servlets) {
      ServletConfigImpl mergedConfig = null;

      ServletConfigImpl existingConfig = _servlets.get(config.getServletName());

      if (! merge && existingConfig != null) {
        for (int i = _servletList.size() - 1; i >= 0; i--) {
          ServletConfigImpl oldConfig = _servletList.get(i);

          if (config.getServletName().equals(oldConfig.getServletName())) {
            _servletList.remove(i);
            break;
          }
        }

        /* XXX: need something more sophisticated since the
          * resin.conf needs to override the web.xml
          * throw new ServletConfigException(L.l("'{0}' is a duplicate servlet-name.  Servlets must have a unique servlet-name.", config.getServletName()));
          */
      } else if (merge && existingConfig != null) {
        mergedConfig = existingConfig;
        mergedConfig.merge(config);
      }

      try {
        // ioc/0000, server/12e4
        if (mergedConfig == null)
          config.validateClass(false);
      } catch (ConfigException e) {
        throw e;
      } catch (Exception e) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, e.toString(), e);
        else if (e instanceof ConfigException)
          log.config(e.getMessage());
        else
          log.config(e.toString());
      }

      if (mergedConfig == null) {
        _servlets.put(config.getServletName(), config);
        _servletList.add(config);
      }
    }
  }

  public void addSecurityElement(Class<? extends Servlet> servletClass,
                                   ServletSecurityElement securityElement)
  {
    _servletSecurityElements.put(servletClass, securityElement);
  }

  public ServletSecurityElement getSecurityElement(Class<? extends Servlet> servletClass)
  {
    return _servletSecurityElements.get(servletClass);
  }

  /**
   * Returns ServletConfigImpl to the servlet manager.
   */
  public ServletConfigImpl getServlet(String servletName)
  {
    return _servlets.get(servletName);
  }

  public HashMap<String, ServletConfigImpl> getServlets()
  {
    return _servlets;
  }

  /**
   * Initialize servlets that need starting at server start.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    ArrayList<ServletConfigImpl> loadOnStartup;
    loadOnStartup = new ArrayList<ServletConfigImpl>();

    for (int j = 0; j < _servletList.size(); j++) {
      ServletConfigImpl config = _servletList.get(j);

      if (config.getLoadOnStartup() == Integer.MIN_VALUE)
        continue;

      int i = 0;
      for (; i < loadOnStartup.size(); i++) {
        ServletConfigImpl config2 = loadOnStartup.get(i);

        if (config.getLoadOnStartup() < config2.getLoadOnStartup()) {
          loadOnStartup.add(i, config);
          break;
        }
      }

      if (i == loadOnStartup.size())
        loadOnStartup.add(config);

      if (config.getRunAt() != null || config.getCron() != null) {
        _cronList.add(config);
      }
    }

    for (int i = 0; i < loadOnStartup.size(); i++) {
      ServletConfigImpl config = loadOnStartup.get(i);

      try {
        config.createServlet(false);
      } catch (ServletException e) {
        // XXX: should JSP failure also cause a system failure?
        if (config.getJspFile() == null)
          throw e;
        else {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }
  }

  /**
   * Creates the servlet chain for the servlet.
   */
  public FilterChain createServletChain(String servletName,
                                        ServletConfigImpl config,
                                        ServletInvocation invocation)
    throws ServletException
  {
    if (config == null)
      config = _servlets.get(servletName);

    if (config == null) {
      throw new ServletConfigException(L.l("'{0}' is not a known servlet.  Servlets must be defined by <servlet> before being used.", servletName));
    }

    if (invocation != null) { // XXX: namedDispatcher
      if (! config.isAsyncSupported())
        invocation.clearAsyncSupported();

      invocation.setMultipartConfig(config.getMultipartConfig());

      // server/12h2
      if (config.getRoleMap() != null)
        invocation.setSecurityRoleMap(config.getRoleMap());
    }

    return config.createServletChain();
  }

  /**
   * Instantiates a servlet given its configuration.
   *
   * @param servletName the servlet
   *
   * @return the initialized servlet.
   */
  public Servlet createServlet(String servletName)
    throws ServletException
  {
    ServletConfigImpl config = _servlets.get(servletName);

    if (config == null) {
      throw new ServletException(L.l("'{0}' is not a known servlet.  Servlets must be defined by <servlet> before being used.", servletName));
    }

    return (Servlet) config.createServlet(false);
  }

  /**
   * Returns the servlet config.
   */
  ServletConfigImpl getServletConfig(String servletName)
  {
    return _servlets.get(servletName);
  }

  public void destroy()
  {
    ArrayList<ServletConfigImpl> servletList;
    servletList = new ArrayList<ServletConfigImpl>();

    if (_servletList != null) {
      synchronized (_servletList) {
        servletList.addAll(_servletList);
      }
    }

    for (int i = 0; i < servletList.size(); i++) {
      ServletConfigImpl config = servletList.get(i);

      try {
        config.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }
}
