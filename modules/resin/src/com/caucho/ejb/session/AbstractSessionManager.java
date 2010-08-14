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

package com.caucho.ejb.session;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.SessionBeanType;
import javax.inject.Named;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.j2ee.BeanName;
import com.caucho.config.j2ee.BeanNameLiteral;
import com.caucho.config.reflect.AnnotatedMethodImpl;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.config.reflect.BaseType;
import com.caucho.ejb.cfg.EjbLazyGenerator;
import com.caucho.ejb.inject.ProcessSessionBeanImpl;
import com.caucho.ejb.inject.SessionRegistrationBean;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

/**
 * Server container for a session bean.
 */
abstract public class AbstractSessionManager<X> extends AbstractEjbBeanManager<X> {
  private static final L10N L = new L10N(AbstractSessionManager.class);
  private final static Logger log
     = Logger.getLogger(AbstractSessionManager.class.getName());

  private EjbLazyGenerator<X> _lazyGenerator;
  
  private Class<?> _proxyImplClass;
  
  private HashMap<Class<?>, AbstractSessionContext<X,?>> _contextMap
    = new HashMap<Class<?>, AbstractSessionContext<X,?>>();

  private InjectManager _injectManager;
  private Bean<X> _bean;
  
  private String[] _declaredRoles;

  public AbstractSessionManager(EjbManager manager,
                                String moduleName,
                                AnnotatedType<X> rawAnnType,
                                AnnotatedType<X> annotatedType,
                                EjbLazyGenerator<X> lazyGenerator)
  {
    super(manager, moduleName, rawAnnType, annotatedType);
    
    _lazyGenerator = lazyGenerator;
    
    DeclareRoles declareRoles 
      = annotatedType.getJavaClass().getAnnotation(DeclareRoles.class);

    RolesAllowed rolesAllowed 
      = annotatedType.getJavaClass().getAnnotation(RolesAllowed.class); 
    
    if (declareRoles != null && rolesAllowed != null) {
      _declaredRoles = new String[declareRoles.value().length +
                                  rolesAllowed.value().length];

      System.arraycopy(declareRoles.value(), 0, 
                       _declaredRoles, 0, 
                       declareRoles.value().length);

      System.arraycopy(rolesAllowed.value(), 0, 
                       _declaredRoles, declareRoles.value().length, 
                       rolesAllowed.value().length);
    }
    else if (declareRoles != null) {
      _declaredRoles = declareRoles.value();
    }
    else if (rolesAllowed != null) {
      _declaredRoles = rolesAllowed.value();
    }
  }

  @Override
  protected String getType()
  {
    return "session:";
  }

  @Override
  public Bean<X> getDeployBean()
  {
    return _bean;
  }
  
  public Class<?> getProxyImplClass()
  {
    return _proxyImplClass;
  }
  
  @Override
  public InjectManager getInjectManager()
  {
    return _injectManager;
  }
  
  protected EjbLazyGenerator<X> getLazyGenerator()
  {
    return _lazyGenerator;
  }
  
  @Override
  public ArrayList<AnnotatedType<? super X>> getLocalApi()
  {
    return _lazyGenerator.getLocalApi();
  }

  @Override
  public AnnotatedType<X> getLocalBean()
  {
    return _lazyGenerator.getLocalBean();
  }
  
  @SuppressWarnings("unchecked")
  protected <T> AbstractSessionContext<X,T> getSessionContext(Class<T> api)
  {
    return (AbstractSessionContext<X,T>) _contextMap.get(api);
  }

  /**
   * Initialize the server during the config phase.
   */
  @Override
  public void init() throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      super.init();
      
      _injectManager = InjectManager.create();

      for (AnnotatedType<? super X> localApi : _lazyGenerator.getLocalApi()) {
        createContext(localApi.getJavaClass());
      }
      
      AnnotatedType<X> localBean = _lazyGenerator.getLocalBean();
      if (localBean != null)
        createContext(localBean.getJavaClass());
      
      for (AnnotatedType<? super X> remoteApi : _lazyGenerator.getRemoteApi()) {
        createContext(remoteApi.getJavaClass());
      }

      bindContext();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
    
    registerCdiBeans();

    log.fine(this + " initialized");
  }
  
  @Override
  public void bind()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(getClassLoader());
      
      boolean isAutoCompile = true;

      if (_proxyImplClass == null) {
        BeanGenerator<X> beanGen = createBeanGenerator();

        String fullClassName = beanGen.getFullClassName();
        
        JavaClassGenerator javaGen = getLazyGenerator().getJavaClassGenerator();
      
        if (javaGen.preload(fullClassName) != null) {
        }
        else if (isAutoCompile) {
          beanGen.introspect();
          
          javaGen.generate(beanGen);
        }
      
        javaGen.compilePendingJava();
      
        _proxyImplClass = generateProxyClass(fullClassName, javaGen);
      }
     
      for (AbstractSessionContext<X,?> cxt : _contextMap.values()) {
        cxt.bind();
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  /**
   * Creates the bean generator for the session bean.
   */
  protected BeanGenerator<X> createBeanGenerator()
  {
    throw new UnsupportedOperationException();
  }
  
  private <T> void createContext(Class<T> api)
  {
    if (_contextMap.get(api) != null)
      throw new IllegalStateException(String.valueOf(api));
    
    AbstractSessionContext<X,T> context = createSessionContext(api);
    
    InjectManager injectManager = context.getInjectManager();
    
    BeanBuilder<SessionContext> factory
      = injectManager.createBeanFactory(SessionContext.class);
  
    context.setDeclaredRoles(_declaredRoles);

    // XXX: separate additions?
    if (injectManager.getBeans(SessionContext.class).size() == 0)
      injectManager.addBean(factory.singleton(context));
   
    _contextMap.put(context.getApi(), context);
    
    try {
      String beanName = getAnnotatedType().getJavaClass().getName();
      
      Jndi.bindDeep("java:comp/EJBContext", context);
      Jndi.bindDeep("java:comp/" + beanName + "/ejbContext", context);
      Jndi.bindDeep("java:comp/" + beanName + "/sessionContext", context);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    /*
    if (_sessionContext == null) {
    }
    */
  }

  private Class<?> generateProxyClass(String skeletonName,
                                      JavaClassGenerator javaGen)
    throws ClassNotFoundException
  {
    Class<?> proxyImplClass;
    
    Class<?> ejbClass = getAnnotatedType().getJavaClass();
  
    if (Modifier.isPublic(ejbClass.getModifiers())) {
      proxyImplClass = javaGen.loadClass(skeletonName);
    }
    else {
      // ejb/1103
      proxyImplClass = javaGen.loadClassParentLoader(skeletonName, ejbClass);
    }
    
    try {
      Method method = proxyImplClass.getMethod("__caucho_getException");
      
      RuntimeException exn = (RuntimeException) method.invoke(null);

      if (exn != null)
        throw exn;
    } catch (RuntimeException exn) {
      throw exn;
    } catch (Exception exn) {
      throw new RuntimeException(exn);
    }
    // contextImplClass.getDeclaredConstructors();
    
    return proxyImplClass;
  }
  
  @Override
  public <T> T getLocalProxy(Class<T> api)
  {
    return getSessionContext(api).createProxy(null);
  }
  
  protected <T> AbstractSessionContext<X,T>
  createSessionContext(Class<T> api)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  protected <T> SessionProxyFactory<T>
  createProxyFactory(AbstractSessionContext<X,T> context)
  {
    try {
      if (_proxyImplClass == null)
        bind();
      
      Class<?> []param = new Class[] { getClass(), getContextClass() };
    
      Constructor<?> ctor = _proxyImplClass.getConstructor(param);
      
      return (SessionProxyFactory<T>) ctor.newInstance(this, context);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      
      if (cause instanceof RuntimeException)
        throw (RuntimeException) cause;
      else
        throw new IllegalStateException(cause);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
  
  protected Class<?> getContextClass()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  private void registerCdiBeans()
  {
    ArrayList<AnnotatedType<? super X>> localApiList = getLocalApi();
    ArrayList<Class<?>> remoteApiList = getRemoteApiList();
 
    AnnotatedType<X> rawAnnType = getRawAnnotatedType();
    AnnotatedType<X> annType = getAnnotatedType();
    
    AnnotatedType<X> extAnnType = createExternalAnnotatedType(annType, localApiList);

    InjectManager moduleBeanManager = InjectManager.create();

    Named named = (Named) annType.getAnnotation(Named.class);

    if (named != null) {
    }

    ManagedBeanImpl<X> mBean 
      = new ManagedBeanImpl<X>(getInjectManager(), getAnnotatedType(), true);
    mBean.introspect();
    
    InjectionTarget<X> target = mBean.getInjectionTarget();
    target = moduleBeanManager.processInjectionTarget(target, getRawAnnotatedType());
    mBean.setInjectionTarget(target);
    
    Class<?> baseApi = annType.getJavaClass();
      
    Set<Type> apiList = new LinkedHashSet<Type>();

    AnnotatedType<X> baseType = getLocalBean();
    
    if (baseType != null) {
      BaseType sourceApi = moduleBeanManager.createSourceBaseType(baseType.getBaseType());
        
      apiList.addAll(sourceApi.getTypeClosure(moduleBeanManager));
    }
      
    if (localApiList != null) {
      for (AnnotatedType<? super X> api : localApiList) {
        baseApi = api.getJavaClass();
          
        BaseType sourceApi = moduleBeanManager.createSourceBaseType(api.getJavaClass());
          
        apiList.addAll(sourceApi.getTypeClosure(moduleBeanManager));
      }
    }
      
    apiList.add(Object.class);
    
    if (remoteApiList != null) {
      for (Class<?> api : remoteApiList) {
        baseApi = api;
      }
    }
    
    if (baseApi == null)
      throw new NullPointerException();

    _bean = (Bean<X>) createBean(mBean, baseApi, apiList, extAnnType);
      
    // CDI TCK requires the rawAnnType, not the processed one
    ProcessSessionBeanImpl process
      = new ProcessSessionBeanImpl(moduleBeanManager,
                                   _bean,
                                   rawAnnType,
                                   getEJBName(),
                                   getSessionBeanType());

    moduleBeanManager.addBean(_bean, process);
    
    if (! moduleBeanManager.isSpecialized(annType.getJavaClass())) {
      moduleBeanManager.addProduces(_bean, extAnnType);
    }

    for (AnnotatedType<?> localApi : getLocalApi()) {
      registerLocalSession(moduleBeanManager, localApi.getJavaClass());
    }
    
    if (getLocalBean() != null) {
      registerLocalSession(moduleBeanManager, getLocalBean().getJavaClass());

    }
  }
  
  private AnnotatedType<X> 
  createExternalAnnotatedType(AnnotatedType<X> baseType,
                              ArrayList<AnnotatedType<? super X>> apiList)
  {
    ExtAnnotatedType<X> extAnnType = new ExtAnnotatedType<X>(baseType);
    
    for (AnnotatedField<? super X> field : baseType.getFields()) {
      if (field.isStatic())
        extAnnType.addField(field);
    }
    
    for (AnnotatedMethod<? super X> method : baseType.getMethods()) {
      AnnotatedMethod<? super X> extMethod = mergeMethod(method, apiList);
      
      if (extMethod != null)
        extAnnType.addMethod(extMethod);
      else if (method.isAnnotationPresent(Produces.class)) {
        // TCK: conflict
        // ioc/07fa
        throw new ConfigException(L.l("{0}.{1} is an invalid @Produces EJB method because the method is not in a @Local interface.",
                                      method.getDeclaringType().getJavaClass().getName(),
                                      method.getJavaMember().getName()));
      }
      else if (isDisposes(method)) {
        throw new ConfigException(L.l("{0}.{1} is an invalid @Disposes EJB method because the method is not in a @Local interface.",
                                      method.getDeclaringType().getJavaClass().getName(),
                                      method.getJavaMember().getName()));
      }
    }
    
    return extAnnType;
  }
  
  private boolean isDisposes(AnnotatedMethod<? super X> method)
  {
    for (AnnotatedParameter<? super X> param : method.getParameters()) {
      if (param.isAnnotationPresent(Disposes.class))
        return true;
    }
    
    return false;
  }
  
  private AnnotatedMethod<? super X>
  mergeMethod(AnnotatedMethod<? super X> method,
              ArrayList<AnnotatedType<? super X>> apiList)
  {
    for (AnnotatedType<? super X> api : apiList) {
      AnnotatedMethod<? super X> apiMethod
        = AnnotatedTypeUtil.findMethod(api, method);
      
      if (apiMethod != null) {
        AnnotatedMethodImpl<? super X> extMethod
          = new AnnotatedMethodImpl(apiMethod.getDeclaringType(),
                                    method,
                                    apiMethod.getJavaMember());

        return extMethod;
      }
    }
    
    return null;
  }
  
  private <T> void registerLocalSession(InjectManager beanManager, 
                                        Class<T> localApi)
  {
    AbstractSessionContext<X,T> context = getSessionContext(localApi);
      
    BeanName beanName = new BeanNameLiteral(getEJBName());

    SessionRegistrationBean<X,T> regBean
      = new SessionRegistrationBean<X,T>(beanManager, context, _bean, beanName);
      
    beanManager.addBean(regBean);
  }

  protected Bean<X> getBean()
  {
    return _bean;
  }

  abstract protected <T> Bean<T>
  createBean(ManagedBeanImpl<X> mBean,
             Class<T> api,
             Set<Type> apiList,
             AnnotatedType<X> extAnnType);
  
  protected SessionBeanType getSessionBeanType()
  {
    return SessionBeanType.STATELESS;
  }

  @Override
  public void destroy()
  {
    for (AbstractSessionContext<X,?> context : _contextMap.values()) {
      try {
        context.destroy();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
