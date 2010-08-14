/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.webapp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.event.*;
import javax.faces.lifecycle.*;
import javax.faces.webapp.*;

import javax.servlet.*;

import com.caucho.config.*;
import com.caucho.jsf.application.*;
import com.caucho.jsf.cfg.*;
import com.caucho.jsf.dev.*;
import com.caucho.vfs.*;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.dispatch.ServletMapping;

public class FacesServletImpl extends GenericServlet
{
  private static final Logger log
    = Logger.getLogger(FacesServletImpl.class.getName());

  private static final String FACES_SCHEMA
    = "com/caucho/jsf/cfg/jsf.rnc";

  private ConfigException _configException;

  private ArrayList<PhaseListener> _phaseListenerList
    = new ArrayList<PhaseListener>();

  public FacesServletImpl()
  {
  }

  public void init(ServletConfig config)
    throws ServletException
  {
    initFactory(FactoryFinder.APPLICATION_FACTORY,
                "com.caucho.jsf.application.ApplicationFactoryImpl");

    initFactory(FactoryFinder.LIFECYCLE_FACTORY,
                "com.caucho.jsf.lifecycle.LifecycleFactoryImpl");

    initFactory(FactoryFinder.RENDER_KIT_FACTORY,
                "com.caucho.jsf.render.RenderKitFactoryImpl");

    initFactory(FactoryFinder.FACES_CONTEXT_FACTORY,
                "com.caucho.jsf.context.FacesContextFactoryImpl");
    
    ApplicationFactory appFactory = (ApplicationFactory)
      FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);

    Application app = appFactory.getApplication();

    if (app == null) {
      app = new ApplicationImpl();
      appFactory.setApplication(app);
    }

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    try {
      Enumeration e = loader.getResources("META-INF/faces-config.xml");
      while (e != null && e.hasMoreElements()) {
        URL url = (URL) e.nextElement();
        initPath(app, Vfs.lookup(url.toString()));
      }
    } catch (IOException e) {
      throw ConfigException.create(e);
    }

    String path = config.getServletContext().getInitParameter(FacesServlet.CONFIG_FILES_ATTR);
    
    String []paths = null;

    if (path != null)
      paths = path.split("\\s*,\\s*");

    for (int i = 0; paths != null && i < paths.length; i++)
      initPath(app, Vfs.lookup("./" + paths[i]));

    initPath(app, Vfs.lookup("WEB-INF/faces-config.xml"));

    if (app.getViewHandler() == null)
      app.setViewHandler(new JspViewHandler());

    JsfPropertyGroup jsfPropertyGroup = WebApp.getLocal().getJsf();

    if (app.getStateManager() == null) {
      SessionStateManager stateManager = new SessionStateManager();

      if (jsfPropertyGroup != null)
        stateManager.setStateSerializationMethod(
          jsfPropertyGroup.getStateSerializationMethod());

      app.setStateManager(stateManager);
    }

    LifecycleFactory lifecycleFactory = (LifecycleFactory)
      FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);

    PhaseListener developerAidListener = null;

    if (jsfPropertyGroup != null && jsfPropertyGroup.isEnableDeveloperAid()) {
      developerAidListener = new JsfDeveloperAid();

      //will use Servlet 3.0
      ServletMapping servletMapping = WebApp.getCurrent()
        .createServletMapping();

      servletMapping.addURLPattern("/caucho.jsf.developer.aid");
      servletMapping.setServletClass(JsfDeveloperAidServlet.class.getName());

      WebApp.getCurrent().addServletMapping(servletMapping);
    }

    Iterator iter = lifecycleFactory.getLifecycleIds();
    while (iter.hasNext()) {
      Lifecycle lifecycle
        = lifecycleFactory.getLifecycle((String) iter.next());

      if (developerAidListener != null)
        lifecycle.addPhaseListener(developerAidListener);
      
      for (PhaseListener listener : _phaseListenerList) {
        lifecycle.addPhaseListener(listener);
      }
    }

  }

  private static void initFactory(String factoryName, String defaultName)
  {
    Object factoryObj = null;
    String factory;
    
    try {
      factoryObj = FactoryFinder.getFactory(factoryName);
    } catch (FacesException e) {
    }


    if (factoryObj == null) {
      FactoryFinder.setFactory(factoryName, defaultName);

      factory = getServiceFactory(factoryName);

      if (factory != null && ! "".equals(factory))
        FactoryFinder.setFactory(factoryName, factory);
    }
  }

  private static String getServiceFactory(String factoryName)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      InputStream is = loader.getResourceAsStream("META-INF/services/" + factoryName);

      try {
        if (is != null) {
          BufferedReader reader = new BufferedReader(new InputStreamReader(is));
          String line = reader.readLine();

          if (line != null) {
            if (line.indexOf('#') >= 0)
              line = line.substring(0, line.indexOf('#'));

            return line.trim();
          }
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        is.close();
      }
    } catch (Exception e) {
    }
    
    return null;
  }
    
  private void initPath(Application app, Path facesPath)
    throws ServletException
  {
    if (facesPath.canRead() && ! facesPath.isDirectory()) {
      try {
        FacesConfig facesConfig = new FacesConfig();

        Config config = new Config();

        config.setEL(false);

        config.configure(facesConfig, facesPath, FACES_SCHEMA);

        if (app instanceof ApplicationImpl) {
          ApplicationImpl appImpl = (ApplicationImpl) app;

          // jsf/4409
          NavigationHandlerImpl navigationHandler
            = appImpl.getDefaultNavigationHandler();

          List<NavigationRule> navigationRules
            = facesConfig.getNavigationRules();

          for (NavigationRule navigationRule : navigationRules) {
            navigationHandler.addRule(navigationRule);
          }

          facesConfig.configure(appImpl);

          facesConfig.configurePhaseListeners(_phaseListenerList);

          for (ManagedBeanConfig bean : facesConfig.getManagedBeans()) {
            appImpl.addManagedBean(bean.getName(), bean);
          }

          ApplicationConfig appConfig = facesConfig.getApplication();

          if (appConfig != null) {
            appConfig.configure(appImpl);

            for (ResourceBundleConfig bundle
                   : appConfig.getResourceBundleList()) {
              appImpl.addResourceBundle(bundle.getVar(), bundle);
            }
          }
        }
      } catch (ConfigException e) {
        _configException = e;

        throw e;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }
  }

  public void service(ServletRequest request,
                      ServletResponse response)
    throws IOException, ServletException
  {
    throw new UnsupportedOperationException();
  }

  public void destroy()
  {
  }
}
