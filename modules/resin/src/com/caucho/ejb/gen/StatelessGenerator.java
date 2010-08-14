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
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.TimedObject;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Interceptor;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.AspectBeanFactory;
import com.caucho.config.gen.LifecycleInterceptor;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InterceptorRuntimeBean;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Generates the skeleton for a session bean.
 */
@Module
public class StatelessGenerator<X> extends SessionGenerator<X> {
  private static final L10N L = new L10N(StatelessGenerator.class);
  
  private String _timeoutMethod;

  private LifecycleInterceptor _postConstructInterceptor;
  private LifecycleInterceptor _preDestroyInterceptor;
  
  private final AspectBeanFactory<X> _aspectBeanFactory;
  private final StatelessScheduledAspectBeanFactory<X> _scheduledBeanFactory;
  
  public StatelessGenerator(String ejbName, 
                            AnnotatedType<X> beanType,
                            ArrayList<AnnotatedType<? super X>> localApi,
                            AnnotatedType<X> localBean,
                            ArrayList<AnnotatedType<? super X>> remoteApi)
  {
    super(ejbName, beanType, localApi, localBean, remoteApi, 
          Stateless.class.getSimpleName());
    
    InjectManager manager = InjectManager.create();
    
    _aspectBeanFactory 
      = new StatelessAspectBeanFactory<X>(manager, getBeanType());
    _scheduledBeanFactory
      = new StatelessScheduledAspectBeanFactory<X>(manager, getBeanType());
  }

  @Override
  protected AspectBeanFactory<X> getAspectBeanFactory()
  {
    return _aspectBeanFactory;
  }

  @Override
  protected AspectBeanFactory<X> getScheduledAspectBeanFactory()
  {
    return _scheduledBeanFactory;
  }
  
  @Override
  public boolean isStateless()
  {
    return true;
  }
  
  @Override
  protected boolean isTimerSupported()
  {
    return true;
  }

  /**
   * Returns the interface itself for the no-interface view
   */
  @Override
  protected AnnotatedType<? super X> introspectLocalDefault() 
  {
    return getBeanType();
  }

  public String getContextClassName()
  {
    return getClassName();
  }

  /**
   * True if the implementation is a proxy, i.e. an interface stub which
   * calls an instance class.
   */
  @Override
  public boolean isProxy()
  {
    return true;
  }

  @Override
  public String getViewClassName()
  {
    return "StatelessLocal";
  }

  @Override
  public String getBeanClassName()
  {
    // XXX: 4.0.7 CDI TCK package-private issues
    return getBeanType().getJavaClass().getName();
    // return getViewClass().getJavaClass().getSimpleName() + "__Bean";
    // return getStatelessBean().getClassName();
  }
  
  //
  // introspection
  //
  

  /**
   * Introspects the APIs methods, producing a business method for each.
   */
  @Override
  public void introspect()
  {
    super.introspect();
    
    introspectLifecycle(getBeanType().getJavaClass());

    _postConstructInterceptor = new LifecycleInterceptor(PostConstruct.class);
    _postConstructInterceptor.introspect(getBeanType());

    _preDestroyInterceptor = new LifecycleInterceptor(PreDestroy.class);
    _preDestroyInterceptor.introspect(getBeanType());

    // XXX: type is incorrect here. Should be moved to stateless generator?
    introspectTimer(getBeanType());
  }

  /**
   * Introspects the lifecycle methods
   */
  public void introspectLifecycle(Class<?> cl)
  {
    if (cl == null || cl.equals(Object.class))
      return;

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(PostConstruct.class)) {
      }
    }

    introspectLifecycle(cl.getSuperclass());
  }

  /**
   * Introspects the lifecycle methods
   */
  public void introspectTimer(AnnotatedType<X> apiClass)
  {
    Class<X> cl = apiClass.getJavaClass();

    if (cl == null || cl.equals(Object.class))
      return;

    if (TimedObject.class.isAssignableFrom(cl)) {
      _timeoutMethod = "ejbTimeout";
      return;
    }

    for (AnnotatedMethod<? super X> apiMethod : apiClass.getMethods()) {
      Method method = apiMethod.getJavaMember();

      if (method.isAnnotationPresent(Timeout.class)) {
        if ((method.getParameterTypes().length != 0)
            && (method.getParameterTypes().length != 1
                || ! Timer.class.equals(method.getParameterTypes()[0]))) {
          throw new ConfigException(L.l(
              "{0}: timeout method '{1}' does not have a (Timer) parameter", cl
                  .getName(), method.getName()));
        }

        _timeoutMethod = method.getName();

        addBusinessMethod(apiMethod);
      }
    }
  }
  
  //
  // Java generation
  //

  /**
   * Generates the stateful session bean
   */
  @Override
  public void generate(JavaWriter out) throws IOException
  {
    generateTopComment(out);

    out.println();
    out.println("package " + getPackageName() + ";");

    out.println();
    out.println("import com.caucho.config.*;");
    out.println("import com.caucho.ejb.session.*;");
    out.println();
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");

    generateHeader(out);
    out.println("{");
    out.pushDepth();

    generateBody(out);

    // generateView(out);

    generateDependency(out);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the local/remote proxy.
   */
  public void generateHeader(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public class " + getClassName() + "<T>");

    if (hasNoInterfaceView())
      out.println("  extends " + getBeanType().getJavaClass().getName());

    out.print("  implements SessionProxyFactory<T>");
    
    for (AnnotatedType<? super X> api : getLocalApi()) {
      out.print(", " + api.getJavaClass().getName());
    }
    out.println();
  }

  private void generateBody(JavaWriter out) throws IOException
  {
    generateClassStaticFields(out);
    
    out.println("private static final boolean __caucho_isFiner = __caucho_log.isLoggable(java.util.logging.Level.FINER);");
    
    out.println();
    out.println("private final StatelessManager _manager;");
    out.println();
    out.println("private final StatelessPool<" + getBeanClassName() + ",T> _statelessPool;");

    generateConstructor(out);

    generateProxyPool(out);
    
    HashMap<String,Object> map = new HashMap<String,Object>();

    generateBusinessMethods(out, map);
    // generateDestroy(out, map);

    /*
    out.println();
    out.println("public void __caucho_timeout_callback(javax.ejb.Timer timer)");
    out.println("{");
    out.pushDepth();

    generateTimer(out);

    out.popDepth();
    out.println("}");

    generateTimeoutCallback(out);*/

    out.println();
    out.println("public void destroy()");
    out.println("{");
    out.pushDepth();

    generateDestroyViews(out);

    out.popDepth();
    out.println("}");
  }
  
  private void generateConstructor(JavaWriter out)
    throws IOException
  {
    out.println();
    out.print("private static final ");
    out.print("java.util.ArrayList<");
    out.printClass(Interceptor.class);
    out.println("<?>> __caucho_interceptor_beans");
    out.print("  = new java.util.ArrayList<");
    out.printClass(Interceptor.class);
    out.println("<?>>();");
    
    out.println();
    out.print("private static final ");
    out.print("java.util.ArrayList<");
    out.printClass(InterceptorRuntimeBean.class);
    out.println("<?>> __caucho_interceptor_static_beans");
    out.print("  = new java.util.ArrayList<");
    out.printClass(InterceptorRuntimeBean.class);
    out.println("<?>>();");
    
    out.println();
    out.println("public " + getClassName() + "(StatelessManager manager"
                + ", StatelessContext context)");
    out.println("{");
    out.pushDepth();

    out.println("_manager = manager;");
    out.println("_statelessPool = manager.createStatelessPool(context, __caucho_interceptor_beans);");
  
    generateProxyConstructor(out);

    out.popDepth();
    out.println("}");

    out.println();
    out.println("@Override");
    out.println("public T __caucho_createProxy(com.caucho.config.inject.CreationalContextImpl<T> env)");
    out.println("{");
    out.println("  return (T) this;");
    out.println("}");
  }
    
  /**
   * Generates the local/remote proxy.
   */
  public void generateProxy(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("{");
    out.pushDepth();

    out.println();
    out.println(getClassName() + "()");
    out.println("{");
    out.pushDepth();
    
    out.println("_context = context;");
    
    out.popDepth();
    out.println("}");

    out.println("public void __caucho_preDestroy(Object instance)");
    out.println("{");
    out.println("}");

    out.println("public void __caucho_postConstruct(Object instance)");
    out.println("{");
    out.println("}");

    out.println();
    out.println("public " + getViewClassName() + " __caucho_get()");
    out.println("{");
    out.println("  return this;");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  protected void generateTimeoutCallback(JavaWriter out) throws IOException
  {
    String beanClass = getBeanType().getJavaClass().getName();

    out.println();
    out.println("public void __caucho_timeout_callback(java.lang.reflect.Method method, javax.ejb.Timer timer)");
    out.println("  throws IllegalAccessException, java.lang.reflect.InvocationTargetException");
    out.println("{");
    out.pushDepth();

    // View<X> objectView = getView();

    //if (objectView != null) {
      // XXX: 4.0.7 - needs to be moved to view
      /*
      out.println("StatelessPool.Item<" + beanClass +"> item");
      out.println("  = _statelessPool.allocate();");

      out.println("try {");
      out.println("  method.invoke(item.getValue(), timer);");
      out.println("} finally {");
      out.println("  _statelessPool.free(item);");
      out.println("}");
      */
    //}

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_timeout_callback(java.lang.reflect.Method method)");
    out.println("  throws IllegalAccessException, java.lang.reflect.InvocationTargetException");
    out.println("{");
    out.pushDepth();

    //if (objectView != null) {
      // XXX: 4.0.7 - must be moved to view
      /*
      out.println("StatelessPool.Item<" + beanClass +"> item");
      out.println("  = _statelessPool.allocate();");

      out.println("try {");
      out.println("  method.invoke(item.getValue());");
      out.println("} finally {");
      out.println("  _statelessPool.free(item);");
      out.println("}");
      */
    //}

    out.popDepth();
    out.println("}");
  }

  //
  // code generation
  //

  protected void generateExtends(JavaWriter out)
    throws IOException
  {
    if (! isProxy()) {
      out.print("extends ");
      out.printClass(getBeanType().getJavaClass());
    }
  }

  public void generateProxyPool(JavaWriter out) throws IOException
  {
    out.println();
    out.println("public void __caucho_destroy()");
    out.println("{");
    out.println("  _statelessPool.destroy();");
    out.println("}");
  }

  public void generateProxyCall(JavaWriter out, Method implMethod)
      throws IOException
  {
    if (! void.class.equals(implMethod.getReturnType())) {
      out.printClass(implMethod.getReturnType());
      out.println(" result;");
    }

    out.println(getBeanClassName() + " bean = _statelessPool.allocate();");

    if (! void.class.equals(implMethod.getReturnType()))
      out.print("result = ");

    out.print("bean." + implMethod.getName() + "(");

    Class<?>[] types = implMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
        out.print(", ");

      out.print(" a" + i);
    }

    out.println(");");

    out.println("_ejb_free(bean);");

    if (!void.class.equals(implMethod.getReturnType()))
      out.println("return result;");
  }

  protected void generateSuper(JavaWriter out, String serverVar)
      throws IOException
  {
    out.println("super(" + serverVar + ");");
  }

  @Override
  public void generateTimer(JavaWriter out) throws IOException
  {
    if (_timeoutMethod != null) {
      // String localVar = "_local_" + getViewClass().getJavaClass().getSimpleName();
      
      String beanClassName = getBeanType().getJavaClass().getName();
      
      out.println("StatelessPool.Item<" + beanClassName + "> item");
      out.println("  = _statelessPool.allocate();");

      out.println("try {");
      out.println("  item.getValue()." + _timeoutMethod + "(timer);");
      out.println("} finally {");
      out.println("  _statelessPool.free(item);");
      out.println("}");
    }
  }
}
