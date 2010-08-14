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

package com.caucho.server.e_app;

import com.caucho.config.ConfigException;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.inject.Module;
import com.caucho.java.WorkDir;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.*;
import com.caucho.server.deploy.EnvironmentDeployInstance;
import com.caucho.server.webapp.WebAppContainer;
import com.caucho.server.webapp.WebAppController;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An enterprise application (ear)
 */
@Module
public class EnterpriseApplication
  implements EnvironmentBean, EnvironmentDeployInstance
{
  static final L10N L = new L10N(EnterpriseApplication.class);
  static final Logger log
    = Logger.getLogger(EnterpriseApplication.class.getName());

  private static final EnvironmentLocal<EnterpriseApplication> _localEApp
    = new EnvironmentLocal<EnterpriseApplication>();

  /*
    protected static EnvironmentLocal<EJBServerInterface> _localServer
    = new EnvironmentLocal<EJBServerInterface>("caucho.ejb-server");
  */

  private EnvironmentClassLoader _loader;

  private String _name;

  private Path _rootDir;

  private EarDeployController _controller;

  private Path _webappsPath;

  private WebAppContainer _webAppContainer;
  private String _version;

  private String _libraryDirectory;

  private ArrayList<Path> _ejbPaths
    = new ArrayList<Path>();

  private ArrayList<WebModule> _webConfigList
    = new ArrayList<WebModule>();

  private ArrayList<WebAppController> _webApps
    = new ArrayList<WebAppController>();

  private Throwable _configException;

  private final Lifecycle _lifecycle;

  /**
   * Creates the application.
   */
  EnterpriseApplication(WebAppContainer container,
                        EarDeployController controller, String name)
  {
    _webAppContainer = container;

    _controller = controller;
    
    _name = name;

    ClassLoader parentLoader;

    if (container != null)
      parentLoader = container.getClassLoader();
    else
      parentLoader = Thread.currentThread().getContextClassLoader();

    _loader = EnvironmentClassLoader.create(parentLoader, "eapp:" + name);
    
    if (_controller != null) {
      Path entAppRoot = _controller.getRootDirectory(); 

      // XXX: restrict to META-INF?
      ResourceLoader loader = new ResourceLoader(_loader, entAppRoot);
      loader.init();

      _webappsPath = entAppRoot.lookup("webapps");
      WorkDir.setLocalWorkDir(entAppRoot.lookup("META-INF/work"),
                              _loader);
    }

    _lifecycle = new Lifecycle(log, toString(), Level.INFO);

    if (_controller != null && _controller.getArchivePath() != null)
      Environment.addDependency(new Depend(_controller.getArchivePath()), _loader);

    _localEApp.set(this, _loader);
  }
  
  public static EnterpriseApplication create(String name)
  {
    EnterpriseApplication application = _localEApp.getLevel();
    
    if (application == null) {
      application = new EnterpriseApplication(null, null, name);
    }
    
    return application;
  }

  public static EnterpriseApplication getCurrent()
  {
    return _localEApp.get();
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
    _loader.setId("eapp:" + name + "");
  }

  /**
   * Gets the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the library directory.
   */
  public void setLibraryDirectory(String libraryDirectory)
  {
    _libraryDirectory = libraryDirectory;
  }

  /**
   * Gets the library directory.
   */
  public String getLibraryDirectory()
  {
    return _libraryDirectory;
  }

  /**
   * Sets the ejb-server jndi name.
   */
  public void setEjbServerJndiName(String name)
  {
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path rootDir)
  {
    _rootDir = rootDir;
  }

  /**
   * Sets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDir;
  }

  /**
   * Returns the class loader.
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Sets the class loader.
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Sets the path to the .ear file
   */
  public void setEarPath(Path earPath)
  {
  }

  /**
   * Sets the path to the expanded webapps
   */
  public void setWebapps(Path webappsPath)
  {
    _webappsPath = webappsPath;
  }

  /**
   * Sets the prefix URL for web applications.
   */
  public void setPrefix(String prefix)
  {
  }

  /**
   * Sets the id
   */
  public void setId(String id)
  {
  }

  /**
   * Sets the application version.
   */
  public void setVersion(String version)
  {
    _version = version;
  }

  /**
   * Sets the schema location
   */
  public void setSchemaLocation(String schema)
  {
  }

  /**
   * Sets the display name.
   */
  public void setDisplayName(String name)
  {
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the icon.
   */
  public void setIcon(Icon icon)
  {
  }

  /**
   * Adds a module.
   */
  public Module createModule()
  {
    return new Module();
  }

  /**
   * Finds a web module based on the web-uri
   */
  WebModule findWebModule(String webUri)
  {
    for (int i = 0; i < _webConfigList.size(); i++) {
      WebModule web = _webConfigList.get(i);

      if (webUri.equals(web.getWebURI()))
        return web;
    }

    return null;
  }

  /**
   * Finds a web module based on the web-uri
   */
  void addWebModule(WebModule webModule)
  {
    _webConfigList.add(webModule);
  }

  /**
   * Adds a security role.
   */
  public void addSecurityRole(SecurityRole role)
  {
  }

  /**
   * Returns true if it's modified.
   */
  public boolean isModified()
  {
    return _loader.isModified();
  }

  /**
   * Returns true if it's modified.
   */
  public boolean isModifiedNow()
  {
    return _loader.isModifiedNow();
  }

  /**
   * Log the reason for modification.
   */
  public boolean logModified(Logger log)
  {
    return _loader.logModified(log);
  }

  /**
   * Returns true if it's modified.
   */
  public boolean isDeployError()
  {
    return _configException != null;
  }

  /**
   * Returns true if the application is idle.
   */
  public boolean isDeployIdle()
  {
    return false;
  }

  /**
   * Sets the config exception.
   */
  @Override
  public void setConfigException(Throwable e)
  {
    _configException = e;

    for (WebAppController controller : _webApps) {
      controller.setConfigException(e);
    }
  }

  /**
   * Gets the config exception.
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Initialization before configuration
   */
  public void preConfigInit()
  {
  }

  /**
   * Configures the application.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    if (! _lifecycle.toInit())
      return;

    try {
      Vfs.setPwd(_rootDir, _loader);

      _loader.addJarManifestClassPath(_rootDir);

      // server/13bb vs TCK
      if ("1.4".equals(_version)) {
        // XXX: tck ejb30/persistence/basic needs to add the lib/*.jar
        // to find the META-INF/persistence.xml
        fillDefaultLib();
      }
      else {
        fillDefaultModules();
      }

      EjbManager ejbManager = EjbManager.create();

      if (_ejbPaths.size() != 0) {
        for (Path path : _ejbPaths) {
          // ejbManager.addRoot(path);
          _loader.addJar(path);
        }

        _loader.validate();

      // XXX:??
      /*
      */

        // starts with the environment
        // ejbServer.start();
      }
    
      // ioc/0p63
      Path ejbJar = _rootDir.lookup("META-INF/ejb-jar.xml");

      if (ejbJar.canRead()) {
        ejbManager.configureRootPath(_rootDir);
      }

      _loader.start();

      // updates the invocation caches
      if (_webAppContainer != null)
        _webAppContainer.clearCache();
    } catch (Exception e) {
      _configException = ConfigException.create(e);
      
      log.log(Level.WARNING, e.toString(), e);
      
      _loader.setConfigException(_configException);
    }
    
    fillErrors();
  }

  private void fillDefaultModules()
  {
    try {
      fillDefaultLib();

      for (String file : _rootDir.list()) {
        if (file.endsWith(".jar")) {
          Path path = _rootDir.lookup(file);
          Path jar = JarPath.create(path);

          try {
            if (jar.lookup("META-INF/application-client.xml").canRead()) {
              // app-client jar
            }
            else if (jar.lookup("META-INF/ejb-jar.xml").canRead()) {
              addEjbPath(path);

              _loader.addJar(path);
              _loader.addJarManifestClassPath(path);
            }
            else {
              addEjbPath(path);

              _loader.addJar(path);
            }
          }
          catch (java.util.zip.ZipException e) {
            // XXX: jpa tck, error in opening zip file
            // canRead() is throwing an exception when
            // META-INF/application-client.xml does not exist?
            log.log(Level.WARNING, e.toString(), e);
          }
        }
        else if (file.endsWith(".war")) {
          Module module = createModule();
          WebModule web = new WebModule();
          web.setWebURI(file);

          module.addWeb(web);
        }
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private void fillDefaultLib()
    throws Exception
  {
    String libDir = "lib";

    // ejb/0fa0
    if (_libraryDirectory != null)
      libDir = _libraryDirectory;

    if (_rootDir.lookup(libDir).isDirectory()) {
      Path lib = _rootDir.lookup(libDir);

      for (String file : lib.list()) {
        if (file.endsWith(".jar")) {
          _loader.addJar(lib.lookup(file));
        }
      }
    }
  }

  /**
   * Configures the application.
   */
  @Override
  public void start()
  {
    if (! _lifecycle.toStarting())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      getClassLoader().start();

      for (int i = 0; i < _webConfigList.size(); i++) {
        WebModule web = _webConfigList.get(i);

        initWeb(web);
      }

      if (_webAppContainer != null) {
        for (WebAppController webApp : _webApps) {
          // server/13bb
          //_webAppContainer.getWebAppGenerator().updateNoStart(webApp.getContextPath());
          _webAppContainer.getWebAppGenerator().update(webApp.getContextPath());
        }
      }
      
      if (_configException != null) {
        for (WebAppController controller : _webApps) {
          controller.setConfigException(_configException);
        }
      }
    } finally {
      if (_configException != null)
        _lifecycle.toError();
      else
        _lifecycle.toActive();

      thread.setContextClassLoader(oldLoader);
      
      if (_configException != null)
        throw ConfigException.create(_configException);
    }
  }
  
  private void fillErrors()
  {
    if (_configException != null) {
      for (WebAppController controller : _webApps) {
        controller.setConfigException(_configException);
      }
    }

  }

  void initWeb(WebModule web)
  {
    String webUri = web.getWebURI();
    String contextUrl = web.getContextRoot();
    Path path = _rootDir.lookup(webUri);
    Path archivePath = null;

    if (contextUrl == null)
      contextUrl = webUri;

    WebAppController controller = null;

    if (webUri.endsWith(".war")) {
      // server/2a16
      String name = webUri.substring(0, webUri.length() - 4);
      int p = name.lastIndexOf('/');
      if (p > 0)
        name = name.substring(p + 1);

      // XXX:
      if (contextUrl.equals(""))
        contextUrl = "/" + name;

      if (contextUrl.endsWith(".war"))
        contextUrl = contextUrl.substring(0, contextUrl.length() - 4);

      Path expandPath = _webappsPath;

      try {
        expandPath.mkdirs();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      archivePath = path;
      path = expandPath.lookup(name);
    } else {
      // server/2a15
      if (contextUrl.equals("")) {
        String name = webUri;
        int p = name.lastIndexOf('/');
        if (p > 0)
          name = name.substring(p + 1);
        contextUrl = "/" + name;
      }

      // server/2a17
      if (contextUrl.endsWith(".war"))
        contextUrl = contextUrl.substring(0, contextUrl.length() - 4);
    }

    if (! contextUrl.startsWith("/"))
      contextUrl = "/" + contextUrl;

    controller = findWebAppEntry(contextUrl);

    if (controller == null) {
      controller = new WebAppController(contextUrl,
                                        contextUrl,
                                        path,
                                        _webAppContainer);
      
      _webApps.add(controller);
    }

    if (archivePath != null)
      controller.setArchivePath(archivePath);

    controller.setDynamicDeploy(true);

    if (_configException != null)
      controller.setConfigException(_configException);

    for (WebAppConfig config : web.getWebAppList())
      controller.addConfigDefault(config);
  }


  /**
   * Returns any matching web-app.
   */
  public WebAppController findWebAppEntry(String name)
  {
    for (int i = 0; i < _webApps.size(); i++) {
      WebAppController controller = _webApps.get(i);

      if (controller.isNameMatch(name))
        return controller;
    }

    return null;
  }

  /**
   * Returns the webapps for the enterprise-application.
   */
  public ArrayList<WebAppController> getApplications()
  {
    return _webApps;
  }

  /**
   * Stops the e-application.
   */
  public void stop()
  {
    if (! _lifecycle.toStopping())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      //log.info(this + " stopping");

      _loader.stop();

      //log.fine(this + " stopped");
    } finally {
      _lifecycle.toStop();

      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * destroys the e-application.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      log.fine(this + " destroying");

      ArrayList<WebAppController> webApps = _webApps;
      _webApps = null;

      if (webApps != null && _webAppContainer != null) {
        for (WebAppController webApp : webApps) {
          _webAppContainer.getWebAppGenerator().update(webApp.getContextPath());
        }
      }
    } finally {
      thread.setContextClassLoader(oldLoader);

      _loader.destroy();

      log.fine(this + " destroyed");
    }
  }

  //
  // JMX utilities
  //

  public String getClientRefs()
  {
    throw new UnsupportedOperationException(getClass().getName());
    /*
    EjbContainer ejbContainer = EjbContainer.create();

    return ejbContainer.getClientRemoteConfig();
    */
  }

  public String toString()
  {
    return "EnterpriseApplication[" + getName() + "]";
  }

  public class Module {
    /**
     * Sets the module id.
     */
    public void setId(String id)
    {
    }

    /**
     * Creates a new web module.
     */
    public void addWeb(WebModule web)
      throws Exception
    {
      String webUri = web.getWebURI();

      WebModule oldWeb = findWebModule(webUri);

      if (oldWeb != null) {
        String contextUrl = web.getContextRoot();

        if (contextUrl != null && ! "".equals(contextUrl))
          oldWeb.setContextRoot(contextUrl);

        oldWeb.addWebAppList(web.getWebAppList());
      }
      else
        addWebModule(web);
    }

    /**
     * Adds a new ejb module.
     */
    public void addEjb(Path path)
      throws Exception
    {
      addEjbPath(path);

      _loader.addJar(path);
      // ejb/0853
      _loader.addJarManifestClassPath(path);
    }

    /**
     * Adds a new java module.
     */
    public void addJava(Path path)
      throws ConfigException
    {
      if (! path.canRead())
        throw new ConfigException(L.l("<java> module {0} must be a valid path.",
                                      path));
    }

    /**
     * Adds a new connector
     */
    public void addConnector(String path)
    {
    }

    /**
     * Adds a new alt-dd module.
     */
    public void addAltDD(String path)
    {
    }
  }

  private void addEjbPath(Path ejbPath)
  {
    if (_ejbPaths.contains(ejbPath))
      return;

    _ejbPaths.add(ejbPath);
    EjbManager ejbContainer = EjbManager.create();

    //ejbContainer.addRoot(ejbPath);
  }
}
