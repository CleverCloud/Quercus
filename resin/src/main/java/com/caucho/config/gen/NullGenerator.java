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
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents a filter for invoking a method
 */
@Module
public class NullGenerator<X> implements AspectGenerator<X> {
  public static NullGenerator<?> NULL = new NullGenerator<Object>();
  
  NullGenerator()
  {
  }
  
  @Override
  public AnnotatedMethod<? super X> getMethod()
  {
    throw new NullPointerException(getClass().getName());
  }
  
  //
  // top-level method generation

  /*
   * Generator for the entire method.
   */
  @Override
  public void generate(JavaWriter out, HashMap<String, Object> prologueMap)
      throws IOException
  {
  }
  
  //
  // bean instance interception
  //

  /**
   * Generates the bean instance class prologue
   */
  @Override
  public void generateBeanPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates bean instance interception
   */
  @Override
  public void generateBeanConstructor(JavaWriter out,
                                      HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates bean instance interception
   */
  @Override
  public void generateProxyConstructor(JavaWriter out,
                                       HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates bean injection
   */
  @Override
  public void generateInject(JavaWriter out,
                             HashMap<String,Object> map)
    throws IOException
  {
  }

  /**
   * Generates bean post construct interception
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

  @Override
  public void generateDestroy(JavaWriter out,
                              HashMap<String,Object> map)
    throws IOException
  {
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
  }

  @Override
  public void generateAsync(JavaWriter out)
    throws IOException
  {
  }
  
  /**
   * Generates code before the try block
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
  }

  /**
   * Generates the method interception code
   */
  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
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
   * Generates finally code for the method
   */
  @Override
  public void generateFinally(JavaWriter out)
    throws IOException
  {
  }
}