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

package com.caucho.resin;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.cfg.BeansConfig;
import com.caucho.config.inject.InjectManager;
import com.caucho.ejb.manager.EjbEnvironmentListener;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.env.jpa.ListenerPersistenceEnvironment;
import com.caucho.inject.ThreadContext;
import com.caucho.java.WorkDir;
import com.caucho.loader.CompilingLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.ResourceLoader;
import com.caucho.server.webbeans.ResinCdiProducer;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Embeddable Resin context for unit testing of
 * application modules in the correct environment but
 * without the overhead of the Resin server.
 *
 * <code><pre>
 * static void main(String []args)
 * {
 *   ResinBeanContainer beans = new ResinBeanContainer();
 *   
 *   // optional resin configuration file
 *   // beans.addBeansXml("resin-context.xml");
 *
 *   beans.addModule("test.jar");
 *   beans.start();
 *
 *   RequestContext req = beans.beginRequest();
 *   try {
 *     MyMain main = beans.getInstance(MyMain.class);
 *
 *     main.main(args);
 *   } finally {
 *     req.close();
 *   }
 *
 *   beans.close();
 * }
 * </pre></code>
 *
 * <h2>Configuration File</h2>
 *
 * The optional configuration file for the ResinContext allows the same
 * environment and bean configuration as the resin-web.xml, but without the
 * servlet-specific configuration.
 *
 * <pre><code>
 * &lt;beans xmlns="http://caucho.com/ns/resin"
 *              xmlns:resin="urn:java:com.caucho.resin">
 *
 *    &lt;resin:import path="${__DIR__}/my-include.xml"/>
 *
 *    &lt;database name="my-database">
 *      &lt;driver ...>
 *        ...
 *      &lt;/driver>
 *    &lt;/database>
 *
 *    &lt;mypkg:MyBean xmlns:mypkg="urn:java:com.mycom.mypkg">
 *      &lt;my-property>my-data&lt;/my-property>
 *    &lt;/mypkg:MyBean>
 * &lt;/beans>
 * </code></pre>
 */
public class ResinBeanContainer
{
  private static final L10N L = new L10N(ResinBeanContainer.class);
  private static final String SCHEMA = "com/caucho/resin/resin-context.rnc";

  private EnvironmentClassLoader _classLoader;
  private InjectManager _injectManager;

  private ThreadLocal<BeanContainerRequest> _localContext
    = new ThreadLocal<BeanContainerRequest>();

  /**
   * Creates a new ResinContext.
   */
  public ResinBeanContainer()
  {
    _classLoader = EnvironmentClassLoader.create("resin-context");
    _injectManager = InjectManager.create(_classLoader);
    
    // ioc/0b07
    _injectManager.replaceContext(new RequestScope());
    _injectManager.replaceContext(ThreadContext.getContext());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);
      
      // ioc/0p62
      EjbManager.create(_classLoader);
      // XXX: currently this would cause a scanning of the classpath even
      // if there's no ejb-jar.xml
      // EjbManager.setScanAll();

      Environment.init();

      Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
      Environment.addChildLoaderListener(new EjbEnvironmentListener());
      
      Environment.addCloseListener(this);

      _injectManager.addManagedBean(_injectManager.createManagedBean(ResinCdiProducer.class));

      Class<?> resinValidatorClass = ResinCdiProducer.createResinValidatorProducer();
      
      if (_injectManager != null)
        _injectManager.addManagedBean(_injectManager.createManagedBean(resinValidatorClass));

      _classLoader.scanRoot();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Adds a new module (jar or classes directory)
   */
  public void addModule(String modulePath)
  {
    Path path = Vfs.lookup(modulePath);

    if (modulePath.endsWith(".jar")) {
      _classLoader.addJar(path);
    }
    else {
      CompilingLoader loader = new CompilingLoader(_classLoader);
      loader.setPath(path);
      loader.init();
    }
  }
  
  /**
   * Adds a package as module root.
   * 
   * @param packageName the name of the package to be treated as a virtual
   * module root.
   */
  public void addPackageModule(String modulePath, String packageName)
  {
    Path root = Vfs.lookup(modulePath);
    
    try {
      URL url = new URL(root.getURL());
    
      _classLoader.addScanPackage(url, packageName);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  /**
   * Adds a package in the classpath as module root.
   * 
   * @param packageName the name of the package to be treated as a virtual
   * module root.
   */
  public void addPackageModule(String packageName)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Enumeration<URL> e = loader.getResources(packageName.replace('.', '/'));

      URL bestUrl = null;

      while (e.hasMoreElements()) {
        URL url = e.nextElement();

        if (bestUrl == null) {
          bestUrl = url;
          continue;
        }

        URL urlA = bestUrl;

        Path pathA = Vfs.lookup(urlA);
        Path pathB = Vfs.lookup(url);

        for (String name : pathA.list()) {
          if (name.endsWith(".class")) {
            bestUrl = urlA;
            break;
          }
        }

        for (String name : pathB.list()) {
          if (name.endsWith(".class")) {
            bestUrl = url;
            break;
          }
        }
      }

      if (bestUrl == null)
        throw new NullPointerException(packageName);

      Path path = Vfs.lookup(bestUrl);

      String moduleName = path.getNativePath();

      if (moduleName.endsWith(packageName.replace('.', '/'))) {
        int prefixLength = moduleName.length() - packageName.length();
        moduleName = moduleName.substring(0, prefixLength);
      }

      addResourceRoot(path);
      addPackageModule(moduleName, packageName);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Adds a Resin beans configuration file, allowing creation of
   * databases, or bean configuration.
   *
   * @param pathName URL/path to the configuration file
   */
  public void addBeansXml(String pathName)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      Path path = Vfs.lookup(pathName);

      ContextConfig context = new ContextConfig(_injectManager, path);

      Config config = new Config();
      config.configure(context, path, SCHEMA);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  public void addResourceRoot(Path path)
  {
    ResourceLoader loader = new ResourceLoader(_classLoader, path);
    loader.init();
  }

  /**
   * Sets the work directory for Resin to use when generating temporary
   * files.
   */
  public void setWorkDirectory(String path)
  {
    WorkDir.setLocalWorkDir(Vfs.lookup(path), _classLoader);
  }

  /**
   * Initializes the context.
   */
  public void start()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      _classLoader.start();
      
      InjectManager cdiManager = InjectManager.create();
      
      cdiManager.update();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns a new instance of the given type with optional bindings. If
   * the type is a managed bean, it will be injected before returning.
   *
   * @param className the className of the bean to instantiate
   * @param qualifier optional @Qualifier annotations to select the bean
   */
  public Object getInstance(String className, Annotation ...qualifiers)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      Class<?> cl = Class.forName(className, false, _classLoader);

      return getInstance(cl, qualifiers);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns a new instance of the given type with optional qualifiers.
   */
  @SuppressWarnings("unchecked")
  public <T> T getInstance(Class<T> type, Annotation ...qualifiers)
  {
    if (type == null)
      throw new NullPointerException();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      Set<Bean<?>> beans = _injectManager.getBeans(type, qualifiers);

      if (beans.size() > 0) {
        Bean<?> bean = _injectManager.resolve(beans);

        return (T) _injectManager.getReference(bean);
      }

      return type.newInstance();
    } catch (InstantiationException e) {
      // XXX: proper exception
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      // XXX: proper exception
      throw new RuntimeException(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns an instance of the bean with the given name.
   * If the type is a managed bean, it will be injected before returning.
   *
   * @param name the @Named of the bean to instantiate
   */
  public Object getBeanByName(String name)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      Set<Bean<?>> beans = _injectManager.getBeans(name);

      if (beans.size() > 0) {
        Bean<?> bean = _injectManager.resolve(beans);

        return _injectManager.getReference(bean);
      }

      return null;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Enters the Resin context and begins a new request on the thread. The
   * the returned context must be passed to the completeRequest. To ensure
   * the request is properly closed, use the following pattern:
   *
   * <code><pre>
   * ResinContext resinContext = ...;
   *
   * RequestContext cxt = resinContext.beginRequest();
   *
   * try {
   *    // ... actions inside the Resin request context
   * } finally {
   *   resinContext.completeRequest(cxt);
   * }
   * </pre></code>
   *
   * @return the RequestContext which must be passed to
   *    <code>completeContext</code>
   */
  public BeanContainerRequest beginRequest()
  {
    Thread thread = Thread.currentThread();

    ClassLoader oldLoader = thread.getContextClassLoader();

    BeanContainerRequest oldContext = _localContext.get();

    BeanContainerRequest context = new BeanContainerRequest(this, oldLoader, oldContext);

    thread.setContextClassLoader(_classLoader);

    _localContext.set(context);

    return context;
  }

  /**
   * Completes the thread's request and exits the Resin context.
   */
  void completeRequest(BeanContainerRequest context)
  {
    Thread thread = Thread.currentThread();

    thread.setContextClassLoader(context.getOldClassLoader());
    _localContext.set(context.getOldContext());
  }

  /**
   * Shuts the context down.
   */
  public void close()
  {
    EnvironmentClassLoader loader = _classLoader;
    _classLoader = null;

    if (loader != null) {
      loader.destroy();
    }
  }

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  public String toString()
  {
    return getClass().getName() + "[]";
  }

  private class ContextConfig extends BeansConfig implements EnvironmentBean {
    ContextConfig(InjectManager manager, Path root)
    {
      super(manager, root);
    }

    public ClassLoader getClassLoader()
    {
      return _classLoader;
    }

    public SystemContext createSystem()
    {
      return new SystemContext();
    }
  }

  private class SystemContext implements EnvironmentBean {
    public ClassLoader getClassLoader()
    {
      return ClassLoader.getSystemClassLoader();
    }
  }

  private class RequestScope implements Context {
    @Override
    public <T> T get(Contextual<T> bean)
    {
      BeanContainerRequest cxt = _localContext.get();

      if (cxt == null)
        throw new IllegalStateException(L.l("No RequestScope is active"));

      return cxt.get(bean);
    }

    @Override
    public <T> T get(Contextual<T> bean, CreationalContext<T> creationalContext)
    {
      BeanContainerRequest cxt = _localContext.get();

      if (cxt == null)
        throw new IllegalStateException(L.l("No RequestScope is active"));

      return cxt.get(bean, creationalContext, _injectManager);
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
      return RequestScoped.class;
    }

    @Override
    public boolean isActive()
    {
      return _localContext.get() != null;
    }
  }
}
