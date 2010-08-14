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

package com.caucho.ejb.protocol;

import com.caucho.config.ConfigException;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.server.e_app.EnterpriseApplication;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.naming.NamingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server containing all the EJBs for a given configuration.
 * 
 * <p>
 * Each protocol will extend the container to override Handle creation.
 */
public class EjbProtocolManager {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(EjbProtocolManager.class);
  protected static final Logger log = Logger.getLogger(EjbProtocolManager.class
      .getName());

  private static ThreadLocal<String> _protocolLocal = new ThreadLocal<String>();

  private static Hashtable<String, WeakReference<AbstractEjbBeanManager<?>>> _staticServerMap
    = new Hashtable<String, WeakReference<AbstractEjbBeanManager<?>>>();

  private final EjbManager _ejbContainer;
  private final ClassLoader _loader;

  private String _localJndiPrefix; // = "java:comp/env/cmp";
  private String _remoteJndiPrefix; // = "java:comp/env/ejb";

  private String _jndiPrefix; // java:comp/env/ejb/FooBean/local

  private HashMap<String, AbstractEjbBeanManager<?>> _serverMap 
    = new HashMap<String, AbstractEjbBeanManager<?>>();

  // handles remote stuff
  protected ProtocolContainer _protocolContainer;
  protected HashMap<String, ProtocolContainer> _protocolMap = new HashMap<String, ProtocolContainer>();

  /**
   * Create a server with the given prefix name.
   */
  public EjbProtocolManager(EjbManager ejbContainer) throws ConfigException
  {
    _ejbContainer = ejbContainer;
    _loader = _ejbContainer.getClassLoader();

    EjbManager parent = ejbContainer.getParent();
    if (parent != null) {
      _localJndiPrefix = parent.getProtocolManager().getLocalJndiPrefix();
      _remoteJndiPrefix = parent.getProtocolManager().getRemoteJndiPrefix();
      _jndiPrefix = parent.getProtocolManager().getJndiPrefix();
    }
  }

  public void setJndiPrefix(String name)
  {
    _jndiPrefix = name;
  }

  public String getJndiPrefix()
  {
    return _jndiPrefix;
  }

  public void setLocalJndiPrefix(String name)
  {
    _localJndiPrefix = name;
  }

  public String getLocalJndiPrefix()
  {
    return _localJndiPrefix;
  }

  public void setRemoteJndiPrefix(String name)
  {
    _remoteJndiPrefix = name;
  }

  public String getRemoteJndiPrefix()
  {
    return _remoteJndiPrefix;
  }

  /**
   * Returns the EJB server.
   */
  public EjbManager getEjbContainer()
  {
    return _ejbContainer;
  }

  /**
   * Initialize the protocol manager.
   */
  public void init() throws NamingException
  {
  }

  /**
   * Gets the current protocol.
   */
  public static String getThreadProtocol()
  {
    return _protocolLocal.get();
  }

  /**
   * Gets the current protocol.
   */
  public static String setThreadProtocol(String protocol)
  {
    String oldProtocol = _protocolLocal.get();

    _protocolLocal.set(protocol);

    return oldProtocol;
  }

  public void setProtocolContainer(ProtocolContainer protocol)
  {
    _protocolContainer = protocol;

    synchronized (_protocolMap) {
      _protocolMap.put(protocol.getName(), protocol);
    }

    addProtocolServers(protocol);
  }

  public void addProtocolContainer(ProtocolContainer protocol)
  {
    if (_protocolContainer == null)
      _protocolContainer = protocol;

    addProtocolContainer(protocol.getName(), protocol);
  }

  public void removeProtocolContainer(ProtocolContainer protocol)
  {
    if (_protocolContainer == protocol)
      _protocolContainer = null;

    synchronized (_protocolMap) {
      _protocolMap.remove(protocol.getName());
    }
  }

  public void addProtocolContainer(String name, ProtocolContainer protocol)
  {
    synchronized (_protocolMap) {
      if (_protocolMap.get(name) == null)
        _protocolMap.put(name, protocol);
    }

    addProtocolServers(protocol);
  }

  public ProtocolContainer getProtocol(String name)
  {
    synchronized (_protocolMap) {
      return _protocolMap.get(name);
    }
  }

  private void addProtocolServers(ProtocolContainer protocol)
  {
    for (AbstractEjbBeanManager<?> server : _serverMap.values()) {
      protocol.addServer(server);
    }
  }

  /**
   * Returns the named server if it's in the same JVM.
   */
  public static AbstractEjbBeanManager<?> getJVMServer(String serverId)
  {
    WeakReference<AbstractEjbBeanManager<?>> serverRef
      = _staticServerMap.get(serverId);

    return serverRef != null ? serverRef.get() : null;
  }

  /**
   * Adds a server.
   */
  public void addServer(AbstractEjbBeanManager<?> server)
  {
    _serverMap.put(server.getProtocolId(), server);

    for (ProtocolContainer protocol : _protocolMap.values()) {
      protocol.addServer(server);
    }

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(_loader);

      // ejb/0g11
      // remote without a local interface should not get bound
      // with the local prefix

      bindDefaultJndi(_jndiPrefix, server);
      
      bindPortableJndiApis(server);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      Thread.currentThread().setContextClassLoader(loader);
    }
  }

  @SuppressWarnings("unchecked")
  private void bindDefaultJndi(String prefix, AbstractEjbBeanManager server)
  {
    try {
      EnterpriseApplication eApp = EnterpriseApplication.getCurrent();

      if (prefix == null)
        prefix = "";
      else if (!prefix.endsWith("/"))
        prefix = prefix + "/";

      if (eApp != null && eApp.getName() != null)
        prefix = prefix + eApp.getName() + "/";

      prefix = prefix + server.getEJBName();

      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      ArrayList<AnnotatedType<?>> apiList = server.getLocalApi();
      if (apiList != null && apiList.size() > 0) {
        String jndiName = prefix + "/local";

        Class<?> localApi = apiList.get(0).getJavaClass();
        
        Jndi.bindDeep(jndiName, new ServerLocalProxy(server, localApi));

        log.finest(server + " local binding to '" + jndiName + "' " + loader);
      }

      Object localHome = null;

      if (localHome != null) {
        String jndiName = prefix + "/local-home";

        Jndi.bindDeep(jndiName, localHome);

        log.finest(server + " local-home binding to '" + jndiName + "' "
                   + loader);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private void bindPortableJndi(String appName,
                                String moduleName, 
                                String suffix, 
                                ServerLocalProxy proxy)
  {
    try {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      
      try {
        thread.setContextClassLoader(_loader);

        String jndiName = null;
        
        // global _may_ mean across applications, but is not required
        // (EJB 3.1 spec. sec. 3.2.2).
        if (appName != null)
          jndiName = "java:global/" + appName + '/' + moduleName + '/' + suffix;
        else
          jndiName = "java:global/" + moduleName + '/' + suffix;

        // XXX: for embed client must be true global  
        thread.setContextClassLoader(_ejbContainer.getGlobalClassLoader());

        Jndi.bindDeep(jndiName, proxy);
        log.finer(proxy + " global binding to '" + jndiName + "' "
                  + _loader);
        
        thread.setContextClassLoader(_loader);
        
        // application means across modules within an application
        jndiName = "java:app/" + moduleName + '/' + suffix;
        Jndi.bindDeep(jndiName, proxy);
        log.finest(proxy + " application binding to '" + jndiName + "' "
                   + _loader);

        // XXX module binding - this is problematic because this will
        // expose a module-level binding to the application context
        // used by the EJBContainer
        
        // module means local to a single module within an application
        jndiName = "java:module/" + suffix;
        Jndi.bindDeep(jndiName, proxy);
        
        log.finest(proxy + " module binding to '" + jndiName + "' "
                   + _loader);
      }
      finally {
        Thread.currentThread().setContextClassLoader(oldLoader);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private void bindPortableJndiApis(AbstractEjbBeanManager manager)
  {
    String jndiName = null;
    String appName = null;

    EnterpriseApplication app = EnterpriseApplication.getCurrent();
    if (app != null)
      appName = app.getName();
    
    String moduleName = manager.getModuleName();

    if (moduleName != null) {
      ServerLocalProxy proxy = null;
      
      if (manager.getLocalBean() != null) {
        String suffix = manager.getEJBName();
        Class<?> api = manager.getEjbClass();

        proxy = new ServerLocalProxy(manager, api); 

        bindPortableJndi(appName, moduleName, suffix, proxy);

        suffix = suffix + '!' + manager.getEjbClass().getName();
        bindPortableJndi(appName, moduleName, suffix, proxy);
      }

      ArrayList<AnnotatedType<?>> apiList = manager.getLocalApi();

      if (apiList.size() == 1) {
        String suffix = manager.getEJBName();
        Class<?> api = apiList.get(0).getJavaClass();

        if (proxy == null)
          proxy = new ServerLocalProxy(manager, api); 

        if (manager.getLocalBean() == null)
          bindPortableJndi(appName, moduleName, suffix, proxy);

        suffix = suffix + '!' + api.getName();
        bindPortableJndi(appName, moduleName, suffix, proxy);
      }
      else {
        for (AnnotatedType<?> api : apiList) {
          String suffix = manager.getEJBName() + '!' + api.getJavaClass().getName();

          if (proxy == null)
            proxy = new ServerLocalProxy(manager, api.getJavaClass()); 

          bindPortableJndi(appName, moduleName, suffix, proxy);
        }
      }
    }
  }

  /**
   * Adds a server.
   */
  public void removeServer(AbstractEjbBeanManager<?> server)
    throws NamingException
  {
    for (ProtocolContainer protocol : _protocolMap.values()) {
      protocol.removeServer(server);
    }
  }

  /**
   * Returns the server specified by the serverId.
   */
  public AbstractEjbBeanManager<?> getServerByEJBName(String ejbName)
  {
    if (!ejbName.startsWith("/"))
      ejbName = "/" + ejbName;

    return _serverMap.get(ejbName);
  }

  /**
   * Returns the server specified by the serverId.
   */
  public AbstractEjbBeanManager<?> getServerByServerId(String protocolId)
  {
    for (AbstractEjbBeanManager<?> server : _serverMap.values()) {
      if (protocolId.equals(server.getProtocolId()))
        return server;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  public Iterator getLocalNames()
  {
    return _serverMap.keySet().iterator();
  }

  /**
   * Returns a list of child EJB names.
   * 
   * @param ejbName
   *          the name which might be a prefix.
   */
  public ArrayList<String> getLocalChildren(String ejbName)
  {
    if (!ejbName.startsWith("/"))
      ejbName = "/" + ejbName;

    if (!ejbName.endsWith("/"))
      ejbName = ejbName + "/";

    ArrayList<String> children = new ArrayList<String>();

    Iterator<String> iter = _serverMap.keySet().iterator();
    while (iter.hasNext()) {
      String name = iter.next();

      AbstractEjbBeanManager<?> server = _serverMap.get(name);

      if (server.getLocalProxy(null) == null)
        continue;

      if (name.startsWith(ejbName)) {
        int prefixLength = ejbName.length();
        int p = name.indexOf('/', prefixLength);

        if (p > 0)
          name = name.substring(prefixLength, p);
        else
          name = name.substring(prefixLength);

        if (!children.contains(name))
          children.add(name);
      }
    }

    return children;
  }

  /**
   * Returns a list of child EJB names.
   * 
   * @param ejbName
   *          the name which might be a prefix.
   */
  public ArrayList<String> getRemoteChildren(String ejbName)
  {
    if (!ejbName.startsWith("/"))
      ejbName = "/" + ejbName;

    ArrayList<String> children = new ArrayList<String>();

    Iterator<String> iter = _serverMap.keySet().iterator();
    while (iter.hasNext()) {
      String name = iter.next();

      AbstractEjbBeanManager<?> server = _serverMap.get(name);
    }

    if (children.size() == 0)
      return null;
    else
      return children;
  }

  /**
   * Destroys the manager.
   */
  public void destroy()
  {
  }
}
