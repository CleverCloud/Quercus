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
 * 
 * <code><pre>
 * [method-prologue]
 * MyType foo(args)
 * {
 *   [pre-try]
 *   try {
 *     [pre-call]
 *     value = [call]
 *     [post-call]
 *     return value;
 *   } catch (ApplicationException e) {
 *     [application-exception]
 *     throw e;
 *   } catch (RuntimeException e) {
 *     [system-exception]
 *     throw e;
 *   } finally {
 *     [finally]
 *   }
 * }
 * </pre></code>
 */
@Module
public interface AspectGenerator<X> {  
  /**
   * Returns the underlying method.
   */
  public AnnotatedMethod<? super X> getMethod();
  
  /**
   * Top-level generator.
   */
  public void generate(JavaWriter out,
                       HashMap<String,Object> prologueMap)
    throws IOException;

  /**
   * Generates the static class prologue
   */
  public void generateBeanPrologue(JavaWriter out, 
                                   HashMap<String,Object> map)
    throws IOException;

  /**
   * Generates initialization in the constructor
   */
  public void generateBeanConstructor(JavaWriter out, 
                                      HashMap<String,Object> map)
    throws IOException;

  /**
   * Generates initialization in the proxy constructor
   */
  public void generateProxyConstructor(JavaWriter out, 
                                       HashMap<String,Object> map)
    throws IOException;

  /**
   * Generates inject code after the constructor
   */
  public void generateInject(JavaWriter out, 
                             HashMap<String,Object> map)
    throws IOException;

  /**
   * Generates @PostConstruct code
   */
  public void generatePostConstruct(JavaWriter out, 
                                    HashMap<String,Object> map)
    throws IOException;

  /**
   * Generates @PreDestroy code
   */
  public void generatePreDestroy(JavaWriter out, 
                                 HashMap<String,Object> map)
    throws IOException;

  /**
   * Generates epilogue
   */
  public void generateEpilogue(JavaWriter out, 
                               HashMap<String,Object> map)
    throws IOException;

  /**
   * Generates any destroy lifecycle code.
   */
  public void generateDestroy(JavaWriter out, HashMap<String, Object> map)
    throws IOException;
  
  //
  // method call interception
  //

  /**
   * Generates the static class prologue
   */
  public void generateMethodPrologue(JavaWriter out, 
                                     HashMap<String,Object> map)
    throws IOException;
  
  //
  // async dispatch method
  //
  public void generateAsync(JavaWriter out)
    throws IOException;
  
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
  public void generatePreTry(JavaWriter out)
    throws IOException;
  
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
  public void generatePreCall(JavaWriter out)
    throws IOException;

  /**
   * Generates the method interception code
   */
  public void generateCall(JavaWriter out) 
    throws IOException;
  
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
  public void generatePostCall(JavaWriter out)
    throws IOException;
  
  /**
   * Generates application (checked) exception code for
   * the method.
   */
  public void generateApplicationException(JavaWriter out,
                                           Class<?> exn)
    throws IOException;
  
  /**
   * Generates system (runtime) exception code for
   * the method.
   */
  public void generateSystemException(JavaWriter out,
                                      Class<?> exn)
    throws IOException;
  
  /**
   * Generates finally code for the method
   */
  public void generateFinally(JavaWriter out)
    throws IOException;
}