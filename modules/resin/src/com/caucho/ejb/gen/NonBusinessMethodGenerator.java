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
 * @author Emil Ong
 */

package com.caucho.ejb.gen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.AspectGeneratorUtil;
import com.caucho.config.gen.MethodHeadGenerator;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents a non-business method that is package accessible.
 * XXX extend NullGenerator?
 */
@Module
public class NonBusinessMethodGenerator<X> implements AspectGenerator<X>
{
  private final AnnotatedMethod<? super X> _method;

  public NonBusinessMethodGenerator(AnnotatedMethod<? super X> method)
  {
    _method = method;
  }

  // will always be @Override because we're only generating this method
  // to hide the implementation method of a no-interface view.
  protected boolean isOverride()
  {
    return true;
  }

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
 
  protected Class<?> []getThrowsExceptions()
  {
    return getJavaMethod().getExceptionTypes();
  }

  /**
   * Top-level generator.
   */
  @Override
  public void generate(JavaWriter out,
                       HashMap<String,Object> prologueMap)
    throws IOException
  {
    int modifiers = getJavaMethod().getModifiers();
    String accessModifier = null;
    
    if (Modifier.isProtected(modifiers))
      accessModifier = "protected";
    else if (Modifier.isPublic(modifiers) || Modifier.isPrivate(modifiers))
      throw new IllegalStateException(getJavaMethod().toString()
                                      + " must be protected or package protected");

    String prefix = "";
    String suffix = "";

    AspectGeneratorUtil.generateHeader(out, 
                                       isOverride(),
                                       accessModifier, 
                                       prefix, 
                                       getJavaMethod(),
                                       suffix, 
                                       getThrowsExceptions());


    out.println("{");
    out.pushDepth();

    out.println("throw new EJBException(\"Illegal non-business method call\");");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateBeanPrologue(JavaWriter out, 
                                   HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates initialization in the constructor
   */
  @Override
  public void generateBeanConstructor(JavaWriter out, 
                                      HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates initialization in the constructor
   */
  @Override
  public void generateInject(JavaWriter out, 
                             HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates initialization in the proxy constructor
   */
  @Override
  public void generateProxyConstructor(JavaWriter out, 
                                       HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates @PostConstruct code
   */
  @Override
  public void generatePostConstruct(JavaWriter out, 
                                    HashMap<String,Object> map)
    throws IOException
  {
  }
  
  @Override
  public void generatePreDestroy(JavaWriter out, 
                                 HashMap<String,Object> map)
    throws IOException
  {
  }
  
  @Override
  public void generateEpilogue(JavaWriter out, 
                               HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates destroy code
   */
  @Override
  public void generateDestroy(JavaWriter out, 
                              HashMap<String,Object> map)
    throws IOException
  {
  }

  //
  // method call interception
  //

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateMethodPrologue(JavaWriter out, 
                                     HashMap<String,Object> map)
    throws IOException
  {
  }
  
  //
  // async dispatch method
  //
  @Override
  public void generateAsync(JavaWriter out)
    throws IOException
  {
  }
  
  /**
   * Generates code before the "try" block
   * <code><pre>
   * retType myMethod(...)
   * {
   *   [pre-try]
   *   try {
   *     ...
   * }
   * </pre></code>
   */
  @Override
  public void generatePreTry(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates code in the "try" block before the call
   * <code><pre>
   * retType myMethod(...)
   * {
   *   ...
   *   try {
   *     [pre-call]
   *     ret = super.myMethod(...)
   *     ...
   * }
   * </pre></code>
   */
  @Override
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
  }
  
  /**
   * Generates code for the invocation itself.
   */
  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates aspect code after the invocation.
   * <code><pre>
   * retType myMethod(...)
   * {
   *   ...
   *   try {
   *     ...
   *     ret = super.myMethod(...)
   *     [post-call]
   *     return ret;
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
  }
  
  /**
   * Generates code for an application (checked) exception.
   */
  @Override
  public void generateApplicationException(JavaWriter out,
                                           Class<?> exn)
    throws IOException
  {
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
  }

  /**
   * Generates the code in the finally block
   * <code><pre>
   * myRet myMethod(...)
   * {
   *   try {
   *     ...
   *   } finally {
   *     [finally]
   *   }
   * </pre></code>
   */
  @Override
  public void generateFinally(JavaWriter out)
    throws IOException
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}
