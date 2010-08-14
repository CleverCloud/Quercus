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

package com.caucho.config.xml;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;

import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.ServiceStartup;
import com.caucho.config.bytecode.ScopeProxy;
import com.caucho.config.cfg.BeansConfig;
import com.caucho.config.extension.ProcessBeanImpl;
import com.caucho.config.inject.HandleAware;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.ScheduleBean;
import com.caucho.config.inject.SingletonHandle;
import com.caucho.inject.LazyExtension;
import com.caucho.inject.Module;
import com.caucho.vfs.Path;

/**
 * Standard XML behavior for META-INF/beans.xml
 */
@Module
public class XmlStandardPlugin implements Extension
{
  private static final String SCHEMA = "com/caucho/config/cfg/resin-beans.rnc";

  private InjectManager _cdiManager;
  private HashSet<String> _configuredBeans = new HashSet<String>();

  private ArrayList<Path> _paths = new ArrayList<Path>();
  private ArrayList<Path> _pendingPaths = new ArrayList<Path>();

  private ArrayList<BeansConfig> _pendingBeans = new ArrayList<BeansConfig>();

  private ArrayList<Bean<?>> _pendingService = new ArrayList<Bean<?>>();

  private Throwable _configException;

  public XmlStandardPlugin(InjectManager manager)
  {
    _cdiManager = manager;

    Thread.currentThread().getContextClassLoader();
  }

  public void addRoot(Path root)
  {
    if (! _paths.contains(root)) {
      _pendingPaths.add(root);
    }
  }

  public void beforeDiscovery(@Observes BeforeBeanDiscovery event)
  {
    ArrayList<Path> paths = new ArrayList<Path>(_pendingPaths);
    _pendingPaths.clear();

    try {
      for (Path root : paths) {
        configureRoot(root);
      }
      
      for (int i = 0; i < _pendingBeans.size(); i++) {
        BeansConfig config = _pendingBeans.get(i);

        ArrayList<Class<?>> deployList = config.getDeployList();

        if (deployList != null && deployList.size() > 0) {
          _cdiManager.setDeploymentTypes(deployList);
        }
      }
    } catch (Exception e) {
      if (_configException == null)
        _configException = e;

      throw ConfigException.create(e);
    }
  }
  
  private void configureRoot(Path root)
    throws IOException
  {
    configurePath(root.lookup("META-INF/beans.xml"));
    configurePath(root.lookup("META-INF/resin-beans.xml"));

    if (root.getFullPath().endsWith("WEB-INF/classes/")) {
      configurePath(root.lookup("../beans.xml"));
      configurePath(root.lookup("../resin-beans.xml"));
    }
    else if (! root.lookup("META-INF/beans.xml").canRead()
             && ! root.lookup("META-INF/resin-beans.xml").canRead()) {
      // ejb/11h0
      configurePath(root.lookup("beans.xml"));
      configurePath(root.lookup("resin-beans.xml"));
      
    }
  }

  private void configurePath(Path beansPath)
    throws IOException
  {
    if (beansPath.canRead() && beansPath.getLength() > 0) {
      // ioc/0041 - tck allows empty beans.xml
      
      BeansConfig beans = new BeansConfig(_cdiManager, beansPath);

      beansPath.setUserPath(beansPath.getURL());

      new Config().configure(beans, beansPath, SCHEMA);

      _pendingBeans.add(beans);
    }
  }

  public void addConfiguredBean(String className)
  {
    _configuredBeans.add(className);
  }

  @LazyExtension
  public void processType(@Observes ProcessAnnotatedType<?> event)
  {
    AnnotatedType<?> type = event.getAnnotatedType();

    if (type == null)
      return;
    
    if (type.isAnnotationPresent(XmlCookie.class))
      return;

    if (_configuredBeans.contains(type.getJavaClass().getName())) {
      event.veto();
      return;
    }

    // XXX: managed by ResinStandardPlugin
    /*
    if (type.isAnnotationPresent(Stateful.class)
        || type.isAnnotationPresent(Stateless.class)
        || type.isAnnotationPresent(MessageDriven.class)) {
      event.veto();
    }
    */
  }
  
  @LazyExtension
  public void processTarget(@Observes ProcessInjectionTarget<?> event)
  {
    AnnotatedType<?> type = event.getAnnotatedType();
    
    XmlCookie cookie = type.getAnnotation(XmlCookie.class);
    
    if (cookie != null) {
      InjectionTarget target = _cdiManager.getXmlInjectionTarget(cookie.value());
      
      event.setInjectionTarget(target);
      
    }
  }

  public void processType(@Observes AfterBeanDiscovery event)
  {
    if (_configException != null)
      event.addDefinitionError(_configException);
  }

  @LazyExtension
  public void processBean(@Observes ProcessBean<?> event)
  {
    ProcessBeanImpl<?> eventImpl = (ProcessBeanImpl<?>) event;

    if (eventImpl.getManager() != _cdiManager)
      return;

    Annotated annotated = event.getAnnotated();
    Bean<?> bean = event.getBean();
    
    if (isStartup(annotated)) {
      _pendingService.add(bean);
    }
  }

  public void processAfterValidation(@Observes AfterDeploymentValidation event)
  {
    ArrayList<Bean<?>> startupBeans = new ArrayList<Bean<?>>(_pendingService);
    _pendingService.clear();
    
    for (Bean<?> bean : startupBeans) {
      CreationalContext<?> env = _cdiManager.createCreationalContext(bean);

      Object value = _cdiManager.getReference(bean, bean.getBeanClass(), env);
      
      if (value instanceof ScopeProxy)
        ((ScopeProxy) value).__caucho_getDelegate();
      
      if (bean instanceof ScheduleBean) {
        ((ScheduleBean) bean).scheduleTimers(value);
      }
      
      if (value instanceof HandleAware && bean instanceof PassivationCapable) {
        String id = ((PassivationCapable) bean).getId();

        ((HandleAware) value).setSerializationHandle(new SingletonHandle(id));
      }
    }
  }

  private boolean isStartup(Annotated annotated)
  {
    if (annotated == null)
      return false;

    for (Annotation ann : annotated.getAnnotations()) {
      Class<?> annType = ann.annotationType();

      // @Stateless must be on the bean itself
      if (annType.equals(Stateless.class))
        return true;

      if (annType.equals(Startup.class))
        return true;

      if (annType.equals(ServiceStartup.class))
        return true;

      // @Startup & @ServiceStartup can be stereotyped
      if (annType.isAnnotationPresent(Stereotype.class)) {
        if (annType.isAnnotationPresent(ServiceStartup.class))
          return true;

        if (annType.isAnnotationPresent(Startup.class))
          return true;
      }
    }

    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
