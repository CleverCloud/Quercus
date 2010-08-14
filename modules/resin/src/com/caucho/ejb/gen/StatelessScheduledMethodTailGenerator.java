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
 */

package com.caucho.ejb.gen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.java.JavaWriter;

public class StatelessScheduledMethodTailGenerator<X> 
  extends StatelessMethodTailGenerator<X>
{
  private final String _methodName;
  
  public StatelessScheduledMethodTailGenerator(StatelessScheduledMethodTailFactory<X> factory,
                                               AnnotatedMethod<? super X> method)
  {
    super(factory, method);
    
    _methodName = 
      "__caucho_schedule_method_" + _method.getJavaMember().getName();
  }

  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String, Object> map)
    throws IOException
  {
    if (map.containsKey(_methodName))
      return;
    
    out.println("private java.lang.reflect.Method " + _methodName + ";");
    
    map.put(_methodName, true);
  }
  
  @Override
  public void generateProxyConstructor(JavaWriter out,
                                       HashMap<String, Object> map)
    throws IOException
  {
    Method javaMethod = _method.getJavaMember();

    out.println("try {");
    out.pushDepth();
    
    out.println("java.lang.reflect.Method []methods = " 
                + getBeanType().getJavaClass().getName() + ".class"
                + ".getDeclaredMethods();");    

    out.println();
    
    out.println("for (java.lang.reflect.Method method : methods) {");
    out.pushDepth();
    out.println("if (method.getName().equals(\"" 
                + javaMethod.getName() + "\")) {");
    out.pushDepth();
    out.println("Class<?> []parameterTypes = method.getParameterTypes();");
    out.println();
    
    Class<?> []parameterTypes = javaMethod.getParameterTypes();
    
    out.println("if (parameterTypes.length != " + parameterTypes.length + ")");
    out.pushDepth();
    out.println("continue;");
    out.popDepth();
    
    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> cl = parameterTypes[i];
      
      out.println("if (! parameterTypes[" + i + "].equals(" 
                  + cl.getName() + ".class))");
      out.pushDepth();
      out.println("continue;");
      out.popDepth();
    }
    
    out.println();
    
    out.println(_methodName  + " = method;");
    out.println(_methodName  + ".setAccessible(true);");
    out.println();
    out.println("break;");
      
    out.popDepth();
    out.println("}"); // if method name == method name
    
    out.popDepth();
    out.println("}"); // method loop
    
    out.popDepth();
    out.println("}"); // try
    out.println("catch (Exception e) {");
    out.pushDepth();
    out.println("throw new RuntimeException(\"Cannot find method \\\"" 
                + javaMethod.getName() + "\\\"\", e);");
    out.popDepth();
    out.println("}");
    
    out.println();
    out.println("if (" + _methodName + " == null)");
    out.pushDepth();
    out.println("throw new RuntimeException(\"Cannot find method \\\"" 
                + javaMethod.getName() + "\\\"\");");
    out.popDepth();
  }

  @Override
  public void generateCall(JavaWriter out) throws IOException
  {
    Method javaMethod = _method.getJavaMember();
    String instance = _factory.getAspectBeanFactory().getBeanInstance();
    
    out.println();
    
    out.println("try {");
    out.pushDepth();

    out.print(_methodName + ".invoke(" + instance);

    Class<?>[] types = javaMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      out.print(", ");

      out.print(" a" + i);
    }

    out.println(");");
    
    out.popDepth();
    out.println("}");
    out.println("catch (IllegalAccessException e) {");
    out.pushDepth();
    out.println("throw new RuntimeException(e);");
    out.popDepth();
    out.println("}");
    out.println("catch (java.lang.reflect.InvocationTargetException e) {");
    out.pushDepth();
    out.println("throw new RuntimeException(e);");
    out.popDepth();
    out.println("}");

  }
}
