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

package com.caucho.ejb.gen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.AspectBeanFactory;
import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Generates the skeleton for a session bean.
 */
@Module
abstract public class SessionGenerator<X> extends BeanGenerator<X> {
  private static final L10N L = new L10N(SessionGenerator.class);
  
  private boolean _hasNoInterfaceView;

  private ArrayList<AnnotatedType<? super X>> _localApi;
  private AnnotatedType<X> _localBean;
  
  private ArrayList<AnnotatedType<? super X>> _remoteApi;
  
  private ArrayList<AnnotatedMethod<? super X>> _annotatedMethods
    = new ArrayList<AnnotatedMethod<? super X>>();

  protected String _contextClassName = "dummy";

  private final NonBusinessAspectBeanFactory<X> _nonBusinessAspectBeanFactory;

  private final ArrayList<AspectGenerator<X>> _businessMethods 
    = new ArrayList<AspectGenerator<X>>();

  public SessionGenerator(String ejbName, 
                          AnnotatedType<X> beanType,
                          ArrayList<AnnotatedType<? super X>> localApi,
                          AnnotatedType<X> localBean,
                          ArrayList<AnnotatedType<? super X>> remoteApi, 
                          String beanTypeName)
  {
    super(toFullClassName(ejbName, beanType.getJavaClass().getName(), beanTypeName),
          beanType);

    _contextClassName = "dummy";

    _localApi = new ArrayList<AnnotatedType<? super X>>(localApi);
    _localBean = localBean;

    _remoteApi = new ArrayList<AnnotatedType<? super X>>(remoteApi);

    _nonBusinessAspectBeanFactory 
      = new NonBusinessAspectBeanFactory<X>(getBeanType());
   }

  public static String toFullClassName(String ejbName, String className,
                                       String beanType)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(className);
    
    sb.append("__");
    
    // XXX: restore this to distinguish similar beans
    /*
    if (!Character.isJavaIdentifierStart(ejbName.charAt(0)))
      sb.append('_');

    for (int i = 0; i < ejbName.length(); i++) {
      char ch = ejbName.charAt(i);

      if (ch == '/')
        sb.append('.');
      else if (Character.isJavaIdentifierPart(ch))
        sb.append(ch);
      else
        sb.append('_');
    }

    sb.append(".");
    sb.append(className);
    sb.append("__");
    */
    
    sb.append(beanType);
    sb.append("Proxy");

    return sb.toString();
  }

  public boolean isStateless() 
  {
    return false;
  }

  public boolean hasNoInterfaceView()
  {
    // return _hasNoInterfaceView;
    return getLocalBean() != null;
  }
  
  /**
   * Returns the local API list.
   */
  public ArrayList<AnnotatedType<? super X>> getLocalApi()
  {
    return _localApi;
  }
  
  public AnnotatedType<X> getLocalBean()
  {
    return _localBean;
  }

  /**
   * Returns the remote API list.
   */
  public ArrayList<AnnotatedType<? super X>> getRemoteApi()
  {
    return _remoteApi;
  }
  
  /**
   * Returns the merged annotated methods
   */
  protected ArrayList<AnnotatedMethod<? super X>> getAnnotatedMethods()
  {
    return _annotatedMethods;
  }

  /**
   * Returns the introspected methods
   */
  @Override
  public ArrayList<AspectGenerator<X>> getMethods()
  {
    return _businessMethods;
  }

  /**
   * Introspects the bean.
   */
  @Override
  public void introspect()
  {
    super.introspect();
    
    if (getBeanType().isAnnotationPresent(LocalBean.class) 
        && ! getBeanType().getJavaClass().isInterface())
      _hasNoInterfaceView = true;
        
    if (_localApi.size() == 0 && _remoteApi.size() == 0)
      _hasNoInterfaceView = true;

    if (_hasNoInterfaceView) {
      AnnotatedType<? super X> localDefault = introspectLocalDefault();
      
      if (localDefault.getJavaClass().isInterface())
        _localApi.add(localDefault); 
      else
        // we still want to introspect the methods, but don't add it as
        // a local api because it will be treated as an interface later
        introspectType(localDefault);
    }
    
    for (AnnotatedType<? super X> type : _localApi)
      introspectType(type);
    
    for (AnnotatedType<? super X> type : _remoteApi)
      introspectType(type);
    
    introspectImpl();
    
    // this comes after the other introspection classes because all it
    // does is catch private timer methods and generate aspect wrappers
    // that wouldn't normally be generated if the method didn't have
    // timer behavior associated.
    if (isTimerSupported())
      introspectTimerMethods();
  }
  
  private void introspectType(AnnotatedType<? super X> type)
  {
    for (AnnotatedMethod<? super X> method : type.getMethods())
      introspectMethod(method);
  }
  
  private void introspectMethod(AnnotatedMethod<? super X> method)
  {
    AnnotatedMethod<? super X> oldMethod 
      = findMethod(_annotatedMethods, method);
    
    if (oldMethod != null) {
      // XXX: merge annotations
      return;
    }
    
    AnnotatedMethod<? super X> baseMethod
      = findMethod(getBeanType().getMethods(), method);
    
    if (baseMethod == null)
      throw new IllegalStateException(L.l("{0} does not have a matching base method in {1}",
                                          method, getBeanType()));
    
    // XXX: merge annotations
    
    _annotatedMethods.add(baseMethod);
  }

  
  /**
   * Introspects the APIs methods, producing a business method for
   * each.
   */
  private void introspectImpl()
  {
    for (AnnotatedMethod<? super X> method : getAnnotatedMethods()) {
      introspectMethodImpl(method);
    }
  }

  private void introspectMethodImpl(AnnotatedMethod<? super X> apiMethod)
  {
    Method javaMethod = apiMethod.getJavaMember();

    if (isBusinessMethod(javaMethod)) {
      addBusinessMethod(apiMethod);
    }
    else {
      if (javaMethod.getName().startsWith("ejb")) {
        throw new ConfigException(L.l("{0}: '{1}' must not start with 'ejb'.  The EJB spec reserves all methods starting with ejb.",
                                      javaMethod.getDeclaringClass(),
                                      javaMethod.getName()));
      }
    
      int modifiers = javaMethod.getModifiers();

      if (! Modifier.isPublic(modifiers) && ! Modifier.isPrivate(modifiers))
        addNonBusinessMethod(apiMethod);
    }
  }
  
  private void introspectTimerMethods()
  {
    for (AnnotatedMethod<? super X> method : getAnnotatedMethods()) {
      introspectTimerMethod(method);
    }    
  }
  
  private void introspectTimerMethod(AnnotatedMethod<? super X> apiMethod)
  {
    Method javaMethod = apiMethod.getJavaMember();
      
    int modifiers = javaMethod.getModifiers();

    if (! isBusinessMethod(javaMethod) 
        && ! Modifier.isPublic(modifiers)
        && (javaMethod.isAnnotationPresent(Schedule.class)
            || javaMethod.isAnnotationPresent(Schedules.class)))
      addScheduledMethod(apiMethod);
  }
  
  protected void addBusinessMethod(AnnotatedMethod<? super X> method)
  {
    AspectGenerator<X> bizMethod = getAspectBeanFactory().create(method);
      
    if (bizMethod != null)
      _businessMethods.add(bizMethod);
  }

  protected void addNonBusinessMethod(AnnotatedMethod<? super X> method)
  {
    AspectGenerator<X> nonBizMethod 
      = _nonBusinessAspectBeanFactory.create(method);
      
    // XXX seems weird to add this to the _businessMethods, but the generation
    // is correct.
    if (nonBizMethod != null)
      _businessMethods.add(nonBizMethod);
  } 

  protected void addScheduledMethod(AnnotatedMethod<? super X> method)
  {
    AspectGenerator<X> bizMethod = 
      getScheduledAspectBeanFactory().create(method);
      
    if (bizMethod != null)
      _businessMethods.add(bizMethod);
  }
  
  private AnnotatedMethod<? super X> 
  findMethod(Collection<AnnotatedMethod<? super X>> methodList,
             AnnotatedMethod<? super X> method)
  {
    for (AnnotatedMethod<? super X> oldMethod : methodList) {
      if (AnnotatedTypeUtil.isMatch(oldMethod, method)) {
        return oldMethod;
      }
    }
    
    return null;
  }

  protected AnnotatedType<? super X> introspectLocalDefault()
  {
    return getBeanType();
  }
  
  protected void generateContentImpl(JavaWriter out,
                                     HashMap<String,Object> map)
    throws IOException
  {
    generateBeanPrologue(out, map);

    generateBusinessMethods(out, map);
    
    generateEpilogue(out, map);
    generateInject(out, map);
    generateDelegate(out, map);
    generatePostConstruct(out, map);
    generateDestroy(out, map);
  }

  protected AspectBeanFactory<X> getScheduledAspectBeanFactory()
  {
    throw new UnsupportedOperationException();
  }
  
  abstract protected boolean isTimerSupported();
  abstract protected AspectBeanFactory<X> getAspectBeanFactory();
  
  // abstract protected void generateBody(JavaWriter out) throws IOException;
  
  public static boolean isBusinessMethod(Method method)
  {
    if (method.getDeclaringClass().equals(Object.class))
      return false;
    if (method.getDeclaringClass().getName().startsWith("javax.ejb."))
      return false;
    if (method.getName().startsWith("ejb")) {
    }
    
    int modifiers = method.getModifiers();

    if (! Modifier.isPublic(modifiers))
      return false;
    
    if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers))
      return false;
    
    return true;
  }
}
