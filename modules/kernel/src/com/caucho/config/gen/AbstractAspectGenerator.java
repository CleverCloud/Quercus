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
package com.caucho.config.gen;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

import javax.ejb.ApplicationException;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents a filter for invoking a method
 */
@Module
abstract public class AbstractAspectGenerator<X> implements AspectGenerator<X> {
  private AspectFactory<X> _factory;
  private AnnotatedMethod<? super X> _method;
  private AspectGenerator<X> _next;

  protected AbstractAspectGenerator(AspectFactory<X> factory,
                                    AnnotatedMethod<? super X> method,
                                    AspectGenerator<X> next)
  {
    _factory = factory;
    _method = method;
    _next = next;
    
    if (next == null)
      throw new NullPointerException();
  }

  protected AspectFactory<X> getFactory()
  {
    return _factory;
  }

  protected AspectBeanFactory<X> getBeanFactory()
  {
    return _factory.getAspectBeanFactory();
  }
  
  /**
   * Returns the owning bean type.
   */
  protected AnnotatedType<X> getBeanType()
  {
    return _factory.getBeanType();
  }

  protected Class<X> getJavaClass()
  {
    return getBeanType().getJavaClass();
  }
   
  @Override
  public AnnotatedMethod<? super X> getMethod()
  {
    return _method;
  }
  
  /**
   * Returns the JavaMethod for this aspect.
   */
  protected Method getJavaMethod()
  {
    return _method.getJavaMember();
  }
  
  /**
   * Top-level generator.
   */
  public void generate(JavaWriter out,
                       HashMap<String,Object> prologueMap)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Generates the body of the method.
   *
   * <code><pre>
   * MyType myMethod(...)
   * {
   *   [pre-try]
   *   try {
   *     [pre-call]
   *     [call]   // retValue = super.myMethod(...)
   *     [post-call]
   *     return retValue;
   *   } catch (RuntimeException e) {
   *     [system-exception]
   *     throw e;
   *   } catch (Exception e) {
   *     [application-exception]
   *     throw e;
   *   } finally {
   *     [finally]
   *   }
   * </pre></code>
   */
  protected void generateContent(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    generatePreTry(out);

    out.println();
    out.println("try {");
    out.pushDepth();

    Method method = getJavaMethod();
    
    if (! void.class.equals(method.getReturnType())) {
      out.printClass(method.getReturnType());
      out.println(" result;");
    }

    generatePreCall(out);

    generateCall(out);

    generatePostCall(out);

    if (! void.class.equals(method.getReturnType()))
      out.println("return result;");

    out.popDepth();

    generateExceptions(out);

    out.println("} finally {");
    out.pushDepth();

    generateFinally(out);

    out.popDepth();
    out.println("}");
  }
  

  private void generateExceptions(JavaWriter out)
    throws IOException
  {
    HashSet<Class<?>> exceptionSet
      = new HashSet<Class<?>>();

    for (Class<?> exn : getThrowsExceptions()) {
      exceptionSet.add(exn);
    }

    exceptionSet.add(RuntimeException.class);

    Class<?> exn;
    while ((exn = selectException(exceptionSet)) != null) {
      boolean isSystemException
        = (RuntimeException.class.isAssignableFrom(exn)
            && ! exn.isAnnotationPresent(ApplicationException.class));

      out.println("} catch (" + exn.getName() + " e) {");
      out.pushDepth();

      if (isSystemException)
        generateSystemException(out, exn);
      else
        generateApplicationException(out, exn);

      out.println();
      out.println("throw e;");

      out.popDepth();
    }
  }
  
  private Class<?> selectException(HashSet<Class<?>> exnSet)
  {
    for (Class<?> exn : exnSet) {
      if (isMostSpecific(exn, exnSet)) {
        exnSet.remove(exn);

        return exn;
      }
    }

    return null;
  }
  
  private boolean isMostSpecific(Class<?> exn, HashSet<Class<?>> exnSet)
  {
    for (Class<?> testExn : exnSet) {
      if (exn == testExn)
        continue;

      if (exn.isAssignableFrom(testExn))
        return false;
    }

    return true;
  }

  protected Class<?> []getThrowsExceptions()
  {
    return getJavaMethod().getExceptionTypes();
  }

  //
  // bean instance generation
  //

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateBeanPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    _next.generateBeanPrologue(out, map);
  }

  /**
   * Generates initialization in the constructor
   */
  @Override
  public void generateBeanConstructor(JavaWriter out, 
                                      HashMap<String,Object> map)
    throws IOException
  {
    _next.generateBeanConstructor(out, map);
  }

  /**
   * Generates initialization in the proxy constructor
   */
  @Override
  public void generateProxyConstructor(JavaWriter out, 
                                       HashMap<String,Object> map)
    throws IOException
  {
    _next.generateProxyConstructor(out, map);
  }

  /**
   * Generates extra inject code
   */
  @Override
  public void generateInject(JavaWriter out, 
                             HashMap<String,Object> map)
    throws IOException
  {
    _next.generateInject(out, map);
  }

  /**
   * Generates @PostConstruct code
   */
  @Override
  public void generatePostConstruct(JavaWriter out, 
                                    HashMap<String,Object> map)
    throws IOException
  {
    _next.generatePostConstruct(out, map);
  }

  /**
   * Generates @PreDestroy code
   */
  @Override
  public void generatePreDestroy(JavaWriter out, 
                                 HashMap<String,Object> map)
    throws IOException
  {
    _next.generatePreDestroy(out, map);
  }

  /**
   * Generates epilogue
   */
  @Override
  public void generateEpilogue(JavaWriter out, 
                               HashMap<String,Object> map)
    throws IOException
  {
    _next.generateEpilogue(out, map);
  }

  /**
   * Generates destroy code
   */
  @Override
  public void generateDestroy(JavaWriter out, 
                              HashMap<String,Object> map)
    throws IOException
  {
    _next.generateDestroy(out, map);
  }
  
  //
  // business method interception
  //

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    _next.generateMethodPrologue(out, map);
  }
  
  /**
   * Generates pre-async dispatch code.
   */
  @Override
  public void generateAsync(JavaWriter out)
    throws IOException
  {
    _next.generateAsync(out);
  }  
  
  /**
   * Generates code before the try block
   */
  @Override
  public void generatePreTry(JavaWriter out)
    throws IOException
  {
    _next.generatePreTry(out);
  }  
  
  /**
   * Generates code before the call, in the try block.
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     [pre-call]
   *     value = bean.myMethod(...);
   *     ...
   * }
   * </pre></code>
   */
  @Override
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
    _next.generatePreCall(out);
  }

  /**
   * Generates the method interception code
   */
  @Override
  public void generateCall(JavaWriter out) 
    throws IOException
  {
    _next.generateCall(out);
  }
  
  /**
   * Generates code after the call, before the return.
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     ...
   *     value = bean.myMethod(...);
   *     [post-call]
   *     return value;
   *   } finally {
   *     ...
   *   }
   * }
   * </pre></code>
   */
  @Override
  public void generatePostCall(JavaWriter out)
    throws IOException
  {
    _next.generatePostCall(out);
  }
  
  /**
   * Generates application (checked) exception code for
   * the method.
   */
  @Override
  public void generateApplicationException(JavaWriter out,
                                           Class<?> exn)
    throws IOException
  {
    _next.generateApplicationException(out, exn);
  }
  
  /**
   * Generates system (runtime) exception code for
   * the method.
   */
  @Override
  public void generateSystemException(JavaWriter out,
                                      Class<?> exn)
    throws IOException
  {
    _next.generateSystemException(out, exn);
  }
  
  /**
   * Generates finally code for the method
   */
  @Override
  public void generateFinally(JavaWriter out)
    throws IOException
  {
    _next.generateFinally(out);
  }

  protected <Z extends Annotation> Z getAnnotation(Class<Z> annotationType,
                                                   AnnotatedMethod<?> apiMethod, 
                                                   AnnotatedMethod<?> implMethod)
  {
    Z annotation;

    annotation = apiMethod.getAnnotation(annotationType);

    if ((annotation == null) && (implMethod != null)) {
      annotation = implMethod.getAnnotation(annotationType);
    }

    return annotation;
  }
  
  protected <Z extends Annotation> Z getAnnotation(Class<Z> annotationType,
                                                   AnnotatedType<?> apiClass,
                                                   AnnotatedType<?> implClass)
  {
    Z annotation = null;

    if (apiClass != null)
      annotation = apiClass.getAnnotation(annotationType);
  
    if ((annotation == null) && (implClass != null)) {
      annotation = implClass.getAnnotation(annotationType);
    }

    return annotation;    
  }
  
  protected <Z extends Annotation> Z getAnnotation(Class<Z> annotationType,
                                                   AnnotatedMethod<?> apiMethod, 
                                                   AnnotatedType<?> apiClass,
                                                   AnnotatedMethod<?> implementationMethod, 
                                                   AnnotatedType<?> implementationClass) 
  {
    Z annotation = null;

    if (apiMethod != null) {
      annotation = apiMethod.getAnnotation(annotationType);
    }

    if (annotation == null && apiClass != null) {
      annotation = apiClass.getAnnotation(annotationType);
    }

    if ((annotation == null) && (implementationMethod != null)) {
      annotation = implementationMethod.getAnnotation(annotationType);
    }

    if ((annotation == null) && (implementationClass != null)) {
      annotation = implementationClass.getAnnotation(annotationType);
    }

    return annotation;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}
