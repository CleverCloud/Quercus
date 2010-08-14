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

package com.caucho.config.extension;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;

import com.caucho.config.ConfigException;
import com.caucho.config.event.AbstractObserverMethod;
import com.caucho.config.event.EventManager;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.ProducesFieldBean;
import com.caucho.config.inject.ProducesMethodBean;
import com.caucho.config.program.BeanArg;
import com.caucho.config.reflect.BaseType;
import com.caucho.inject.LazyExtension;
import com.caucho.inject.Module;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

/**
 * Manages custom extensions for the inject manager.
 */
@Module
public class ExtensionManager
{
  private static final L10N L = new L10N(ExtensionManager.class);
  private static final Logger log
    = Logger.getLogger(ExtensionManager.class.getName());
  
  private final InjectManager _cdiManager;

  private HashSet<URL> _extensionSet = new HashSet<URL>();
  
  private HashMap<Class<?>,ExtensionItem> _extensionMap
    = new HashMap<Class<?>,ExtensionItem>();
  
  private boolean _isCustomExtension;

  public ExtensionManager(InjectManager cdiManager)
  {
    _cdiManager = cdiManager;
  }
  
  boolean isCustomExtension()
  {
    return _isCustomExtension;
  }

  public void updateExtensions()
  {
    try {
      ClassLoader loader = _cdiManager.getClassLoader();

      if (loader == null)
        return;

      Enumeration<URL> e = loader.getResources("META-INF/services/" + Extension.class.getName());

      while (e.hasMoreElements()) {
        URL url = (URL) e.nextElement();
        
        if (_extensionSet.contains(url))
          continue;

        _extensionSet.add(url);
        
        InputStream is = null;
        try {
          is = url.openStream();
          ReadStream in = Vfs.openRead(is);

          String line;

          while ((line = in.readLine()) != null) {
            int p = line.indexOf('#');
            if (p >= 0)
              line = line.substring(0, p);
            line = line.trim();

            if (line.length() > 0) {
              loadExtension(line);
            }
          }

          in.close();
        } catch (IOException e1) {
          log.log(Level.WARNING, e1.toString(), e1);
        } finally {
          IoUtil.close(is);
        }
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public void createExtension(String className)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName(className, false, loader);
      Constructor<?> ctor = cl.getConstructor(new Class[] { InjectManager.class });

      Extension extension = (Extension) ctor.newInstance(_cdiManager);

      addExtension(extension);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  void loadExtension(String className)
  {
//    _isCustomExtension = true;
    
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName(className, false, loader);

      if (! Extension.class.isAssignableFrom(cl))
        throw new InjectionException(L.l("'{0}' is not a valid extension because it does not implement {1}",
                                         cl, Extension.class.getName()));
      
      Extension extension = null;
      
      for (Constructor<?> ctor : cl.getDeclaredConstructors()) {
        if (ctor.getParameterTypes().length == 0) {
          ctor.setAccessible(true);
          
          extension = (Extension) ctor.newInstance();
        }
      }

      if (extension == null)
        extension = (Extension) cl.newInstance();

      addExtension(extension);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public void addExtension(Extension ext)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " add extension " + ext);
    
    ExtensionItem item = introspect(ext.getClass());

    for (ExtensionMethod method : item.getExtensionMethods()) {
      Method javaMethod = method.getMethod();
      Class<?> rawType = method.getBaseType().getRawClass();
      
      ExtensionObserver observer;
      observer = new ExtensionObserver(ext,
                                       method.getMethod(),
                                       method.getArgs());

      _cdiManager.getEventManager().addExtensionObserver(observer,
                                                            method.getBaseType(),
                                                            method.getQualifiers());
      
      if ((ProcessAnnotatedType.class.isAssignableFrom(rawType))
          && ! javaMethod.isAnnotationPresent(LazyExtension.class)) {
        _cdiManager.setIsCustomExtension(true);
      }

      if ((ProcessBean.class.isAssignableFrom(rawType))
          && ! javaMethod.isAnnotationPresent(LazyExtension.class)) {
        _cdiManager.setIsCustomExtension(true);
      }

      if ((ProcessInjectionTarget.class.isAssignableFrom(rawType))
          && ! javaMethod.isAnnotationPresent(LazyExtension.class)) {
        _cdiManager.setIsCustomExtension(true);
      }

      if ((ProcessProducer.class.isAssignableFrom(rawType))
          && ! javaMethod.isAnnotationPresent(LazyExtension.class)) {
        _cdiManager.setIsCustomExtension(true);
      }
    }
  }

  private ExtensionItem introspect(Class<?> cl)
  {
    ExtensionItem item = _extensionMap.get(cl);

    if (item == null) {
      item = new ExtensionItem(cl);
      _extensionMap.put(cl, item);
    }

    return item;
  }

  public <T> Bean<T> processBean(Bean<T> bean, ProcessBean<T> processBean)
  {
    InjectManager cdi = _cdiManager;
    
    BaseType baseType = cdi.createTargetBaseType(processBean.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(bean.getBeanClass()));
    
    getEventManager().fireExtensionEvent(processBean, baseType);

    if (processBean instanceof ProcessBeanImpl<?>
        && ((ProcessBeanImpl<?>) processBean).isVeto())
      return null;
    else
      return processBean.getBean();
  }

  @Module
  public <T> Bean<T> processBean(Bean<T> bean, Annotated ann)
  {
    InjectManager cdi = _cdiManager;
    
    ProcessBeanImpl<T> event = new ProcessBeanImpl<T>(_cdiManager, bean, ann);
    
    BaseType baseType = cdi.createTargetBaseType(event.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(bean.getBeanClass()));
    
    getEventManager().fireExtensionEvent(event, baseType);

    if (event.isVeto())
      return null;
    else
      return event.getBean();
  }

  @Module
  public <T> Bean<T> processManagedBean(ManagedBeanImpl<T> bean, Annotated ann)
  {
    InjectManager cdi = _cdiManager;
    
    ProcessManagedBeanImpl<T> event
      = new ProcessManagedBeanImpl<T>(_cdiManager, bean, ann);
    
    BaseType baseType = cdi.createTargetBaseType(event.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(bean.getBeanClass()));
    
    getEventManager().fireExtensionEvent(event, baseType);

    if (event.isVeto())
      return null;
    else
      return event.getBean();
  }

  @Module
  public <T,X> Bean<T> processProducerMethod(ProducesMethodBean<X,T> bean)
  {
    InjectManager cdi = _cdiManager;
    
    ProcessProducerMethodImpl<X,T> event
      = new ProcessProducerMethodImpl<X,T>(_cdiManager, bean);
    
    AnnotatedMethod<? super X> method = bean.getProducesMethod();
    Bean<?> producerBean = bean.getProducerBean();
    
    BaseType baseType = cdi.createTargetBaseType(event.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(producerBean.getBeanClass()),
                             cdi.createTargetBaseType(method.getBaseType()));
                             
    
    getEventManager().fireExtensionEvent(event, baseType);

    if (event.isVeto())
      return null;
    else
      return event.getBean();
  }

  @Module
  public <T,X> Bean<X> processProducerField(ProducesFieldBean<T,X> bean)
  {
    InjectManager cdi = _cdiManager;
    
    ProcessProducerFieldImpl<T,X> event
      = new ProcessProducerFieldImpl<T,X>(_cdiManager, bean);
    
    AnnotatedField<? super T> field = bean.getField();
    
    BaseType baseType = cdi.createTargetBaseType(event.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(bean.getProducerBean().getBeanClass()),
                             cdi.createTargetBaseType(field.getBaseType()));
                             
    
    getEventManager().fireExtensionEvent(event, baseType);

    if (event.isVeto())
      return null;
    else
      return event.getBean();
  }

  /**
   * Processes the discovered InjectionTarget
   */
  public <T> InjectionTarget<T> 
  processInjectionTarget(InjectionTarget<T> target,
                         AnnotatedType<T> annotatedType)
  {
    InjectManager cdi = _cdiManager;
    
    ProcessInjectionTargetImpl<T> processTarget
      = new ProcessInjectionTargetImpl<T>(_cdiManager, target, annotatedType);
    
    BaseType eventType = cdi.createTargetBaseType(ProcessInjectionTargetImpl.class);
    eventType = eventType.fill(cdi.createTargetBaseType(annotatedType.getBaseType()));

    getEventManager().fireExtensionEvent(processTarget, eventType);

    return (InjectionTarget<T>) processTarget.getInjectionTarget();
  }

  /**
   * Processes the discovered method producer
   */
  public <X,T> Producer<T>
  processProducer(AnnotatedMethod<X> producesMethod,
                  Producer<T> producer)
  {
    InjectManager cdi = _cdiManager;
    
    ProcessProducerImpl<X,T> event
      = new ProcessProducerImpl<X,T>(producesMethod, producer);
    
    AnnotatedType<?> declaringType = producesMethod.getDeclaringType();
    
    Type declaringClass;
    
    if (declaringType != null)
      declaringClass = declaringType.getBaseType();
    else
      declaringClass = producesMethod.getJavaMember().getDeclaringClass(); 
    
    BaseType eventType = cdi.createTargetBaseType(ProcessProducerImpl.class);
    eventType = eventType.fill(cdi.createTargetBaseType(declaringClass),
                               cdi.createTargetBaseType(producesMethod.getBaseType()));

    getEventManager().fireExtensionEvent(event, eventType);
    
    return event.getProducer();
  }

  /**
   * Processes the discovered method producer
   */
  public <X,T> Producer<T>
  processProducer(AnnotatedField<X> producesField,
                  Producer<T> producer)
  {
    InjectManager cdi = _cdiManager;
    
    ProcessProducerImpl<X,T> event
      = new ProcessProducerImpl<X,T>(producesField, producer);
    
    AnnotatedType<X> declaringType = producesField.getDeclaringType();
    
    BaseType eventType = cdi.createTargetBaseType(ProcessProducerImpl.class);
    eventType = eventType.fill(cdi.createTargetBaseType(declaringType.getBaseType()),
                               cdi.createTargetBaseType(producesField.getBaseType()));

    getEventManager().fireExtensionEvent(event, eventType);
    
    return event.getProducer();
  }
  
  /**
   * Processes the observer.
   */
  public <T,X> void processObserver(ObserverMethod<T> observer,
                                    AnnotatedMethod<X> method)
  {
    ProcessObserverImpl<T,X> event
      = new ProcessObserverImpl<T,X>(_cdiManager, observer, method);
  
    AnnotatedMethod<X> annotatedMethod = event.getAnnotatedMethod();
    AnnotatedType<X> declaringType = annotatedMethod.getDeclaringType();
    ObserverMethod<T> observerMethod = event.getObserverMethod();
    Type observedType = observerMethod.getObservedType();
    
    BaseType eventType = _cdiManager.createTargetBaseType(ProcessObserverImpl.class);
    eventType = eventType.fill(_cdiManager.createTargetBaseType(observedType),
                               _cdiManager.createTargetBaseType(declaringType.getBaseType()));
    
    getEventManager().fireExtensionEvent(event, eventType);
  }

  public void fireBeforeBeanDiscovery()
  {
    getEventManager().fireExtensionEvent(new BeforeBeanDiscoveryImpl(_cdiManager));
  }

  public void fireAfterBeanDiscovery()
  {
    getEventManager().fireExtensionEvent(new AfterBeanDiscoveryImpl(_cdiManager));
  }

  public void fireAfterDeploymentValidation()
  {
    AfterDeploymentValidationImpl event
      = new AfterDeploymentValidationImpl(_cdiManager);
  
    getEventManager().fireExtensionEvent(event);
  
    /*
    if (event.getDeploymentProblem() != null)
      throw ConfigException.create(event.getDeploymentProblem());
      */
  }
  
  /**
   * Creates a discovered annotated type.
   */
  public <T> AnnotatedType<T> processAnnotatedType(AnnotatedType<T> type)
  {
    InjectManager cdi = _cdiManager;
    
    ProcessAnnotatedTypeImpl<T> processType
      = new ProcessAnnotatedTypeImpl<T>(type);

    BaseType baseType = cdi.createTargetBaseType(ProcessAnnotatedTypeImpl.class);
    baseType = baseType.fill(cdi.createTargetBaseType(type.getBaseType()));
    
    getEventManager().fireExtensionEvent(processType, baseType);

    if (processType.isVeto()) {
      return null;
    }
    
    type = processType.getAnnotatedType();

    return type;
  }
  
  private EventManager getEventManager()
  {
    return _cdiManager.getEventManager();
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cdiManager + "]";
  }

  class ExtensionItem {
    private ArrayList<ExtensionMethod> _observers
      = new ArrayList<ExtensionMethod>();

    ExtensionItem(Class<?> cl)
    {
      for (Method method : cl.getDeclaredMethods()) {
        ExtensionMethod extMethod = bindObserver(cl, method);

        if (extMethod != null)
          _observers.add(extMethod);
      }
    }

    private ArrayList<ExtensionMethod> getExtensionMethods()
    {
      return _observers;
    }

    private ExtensionMethod bindObserver(Class<?> cl, Method method)
    {
      Type []param = method.getGenericParameterTypes();

      if (param.length < 1)
        return null;

      Annotation [][]paramAnn = method.getParameterAnnotations();

      if (! hasObserver(paramAnn))
        return null;

      InjectManager inject = _cdiManager;

      BeanArg<?> []args = new BeanArg[param.length];

      for (int i = 1; i < param.length; i++) {
        Annotation []bindings = inject.getQualifiers(paramAnn[i]);

        if (bindings.length == 0)
          bindings = new Annotation[] { DefaultLiteral.DEFAULT };
        
        InjectionPoint ip = null;

        args[i] = new BeanArg(inject, param[i], bindings, ip);
      }

      BaseType baseType = inject.createTargetBaseType(param[0]);

      return new ExtensionMethod(method, baseType,
                                 inject.getQualifiers(paramAnn[0]),
                                 args);
    }

    private boolean hasObserver(Annotation [][]paramAnn)
    {
      for (int i = 0; i < paramAnn.length; i++) {
        for (int j = 0; j < paramAnn[i].length; j++) {
          if (paramAnn[i][j].annotationType().equals(Observes.class))
            return true;
        }
      }

      return false;
    }
  }

  static class ExtensionMethod {
    private final Method _method;
    private final BaseType _type;
    private final Annotation []_qualifiers;
    private final BeanArg<?> []_args;

    ExtensionMethod(Method method,
                    BaseType type,
                    Annotation []qualifiers,
                    BeanArg<?> []args)
    {
      _method = method;
      method.setAccessible(true);
      
      _type = type;
      _qualifiers = qualifiers;
      _args = args;
    }

    public Method getMethod()
    {
      return _method;
    }

    public BeanArg<?> []getArgs()
    {
      return _args;
    }

    public BaseType getBaseType()
    {
      return _type;
    }

    public Annotation []getQualifiers()
    {
      return _qualifiers;
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _method + "]";
    }
  }

  static class ExtensionObserver extends AbstractObserverMethod<Object> {
    private Extension _extension;
    private Method _method;
    private BeanArg<?> []_args;

    ExtensionObserver(Extension extension,
                      Method method,
                      BeanArg<?> []args)
    {
      _extension = extension;
      _method = method;
      _args = args;
    }

    public void notify(Object event)
    {
      try {
        Object []args = new Object[_args.length];
        args[0] = event;

        for (int i = 1; i < args.length; i++) {
          args[i] = _args[i].eval(null);
        }

        _method.invoke(_extension, args);
      } catch (RuntimeException e) {
        throw e;
      } catch (InvocationTargetException e) {
        String loc = (_extension + "." + _method.getName() + ": ");
        
        Throwable cause = e.getCause();

        if (cause instanceof ConfigException)
          throw (ConfigException) cause;
        
        throw new InjectionException(loc + cause.getMessage(), cause);
      } catch (Exception e) {
        String loc = (_extension + "." + _method.getName() + ": ");

        throw new InjectionException(loc + e.getMessage(), e);
      }
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _extension + "," + _method.getName() + "]";
    }
  }
}

