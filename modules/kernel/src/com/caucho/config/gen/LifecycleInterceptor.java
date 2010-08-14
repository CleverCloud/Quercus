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

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.security.*;
import javax.ejb.*;
import javax.interceptor.*;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;

/**
 * Represents the interception
 */
public class LifecycleInterceptor {
  private static final L10N L = new L10N(LifecycleInterceptor.class);

  private Class<? extends Annotation> _annType;
  
  private ArrayList<Class<?>> _defaultInterceptors = new ArrayList<Class<?>>();
  private ArrayList<Class<?>> _classInterceptors = new ArrayList<Class<?>>();

  private boolean _isExcludeDefaultInterceptors;
  private boolean _isExcludeClassInterceptors;

  private String _uniqueName;
  
  private ArrayList<Class<?>> _interceptors = new ArrayList<Class<?>>();

  // map from the interceptor class to the local variable for the interceptor
  private HashMap<Class<?>,String> _interceptorVarMap
    = new HashMap<Class<?>,String>();
  
  // interceptors we're responsible for initializing
  private ArrayList<Class<?>> _ownInterceptors = new ArrayList<Class<?>>();

  public LifecycleInterceptor(Class<? extends Annotation> annType)
  {
    _annType = annType;
  }
  
  /**
   * Returns true if the business method has any active XA annotation.
   */
  public boolean isEnhanced()
  {
    return _interceptors.size() > 0;
  }

  public ArrayList<Class<?>> getInterceptors()
  {
    return _interceptors;
  }

  /**
   * Introspects the @Interceptors annotation on the method
   * and the class.
   */
  public void introspect(AnnotatedType<?> implClass)
  {
    Interceptors iAnn;
    
    iAnn = implClass.getAnnotation(Interceptors.class);

    if (iAnn != null) {
      for (Class<?> iClass : iAnn.value()) {
        introspectClass(iClass);
      }
    }

    _interceptors.addAll(_classInterceptors);
  }

  private void introspectClass(Class<?> iClass)
  {
    if (findInterceptorMethod(iClass) != null) {
      _classInterceptors.add(iClass);
    }
  }

  private Method findInterceptorMethod(Class<?> cl)
  {
    for (Method method : cl.getMethods()) {
      if (method.isAnnotationPresent(_annType)
          && method.getParameterTypes().length == 1
          && method.getParameterTypes()[0].equals(InvocationContext.class)) {
        return method;
      }
    }

    return null;
  }

  public void generatePrologue(JavaWriter out, HashMap map)
    throws IOException
  {
    if (_interceptors.size() == 0) {
      return;
    }
    
    _uniqueName = "_v" + out.generateId();
    
    out.println();
    out.println("private static java.lang.reflect.Method " + _uniqueName + "_method;");
    out.println("private static java.lang.reflect.Method " + _uniqueName + "_implMethod;");

    boolean isAroundInvokePrologue = false;
    
    out.println("private static java.lang.reflect.Method []" + _uniqueName + "_methodChain;");
    out.println("private transient Object []" + _uniqueName + "_objectChain;");

    Class cl = null;//_implMethod.getDeclaringClass();
    
    out.println();
    out.println("static {");
    out.pushDepth();
    
    out.println("try {");
    out.pushDepth();
    
    generateMethodChain(out);
    
    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    out.popDepth();
    out.println("}");

    for (Class iClass : _interceptors) {
      String var = (String) map.get("interceptor-" + iClass.getName());
      if (var == null) {
        var = "__caucho_i" + out.generateId();

        out.println();
        out.print("private static ");
        out.printClass(Bean.class);
        out.println(" " + var + "_f;");

        out.print("private transient ");
        out.printClass(iClass);
        out.println(" " + var + ";");

        map.put("interceptor-" + iClass.getName(), var);

        _ownInterceptors.add(iClass);
      }

      _interceptorVarMap.put(iClass, var);
    }
  }

  public void generateConstructor(JavaWriter out, HashMap map)
    throws IOException
  {
    for (Class iClass : _ownInterceptors) {
      String var = _interceptorVarMap.get(iClass);

      out.println("if (" + var + "_f == null)");
      out.println("  " + var + "_f = com.caucho.webbeans.manager.WebBeansContainer.create().createTransient(" + iClass.getName() + ".class);");

      out.print(var + " = (");
      out.printClass(iClass);
      out.println(")" + var + "_f.get();");
    }
  }

  public void generateCall(JavaWriter out)
    throws IOException
  {
    if (_interceptors.size() == 0) {
      return;
    }

    out.println("try {");
    out.pushDepth();

    generateObjectChain(out);
    
    out.print("new com.caucho.ejb3.gen.LifecycleInvocationContext(");
    out.print("this, ");
    out.print(_uniqueName + "_methodChain, ");
    out.println(_uniqueName + "_objectChain).proceed();");
    
    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");

    boolean isException = false;
    Class []exnList = new Class[0];//
    for (Class cl : exnList) {
      if (RuntimeException.class.isAssignableFrom(cl))
        continue;

      if (! isMostGeneralException(exnList, cl))
        continue;
      
      if (cl.isAssignableFrom(Exception.class))
        isException = true;
      
      out.println("} catch (" + cl.getName() + " e) {");
      out.println("  throw e;");
    }

    if (! isException) {
      out.println("} catch (Exception e) {");
      out.println("  throw new RuntimeException(e);");
    }
    
    out.println("}");
  }

  private boolean isMostGeneralException(Class []exnList, Class cl)
  {
    for (Class exn : exnList) {
      if (exn != cl && exn.isAssignableFrom(cl))
        return false;
    }

    return true;
  }

  protected void generateMethodChain(JavaWriter out)
    throws IOException
  {
    out.println(_uniqueName + "_methodChain = new java.lang.reflect.Method[] {");
    out.pushDepth();

    for (Class iClass : _interceptors) {
      Method method = findInterceptorMethod(iClass);

      if (method == null)
        throw new IllegalStateException(L.l("{0}: Can't find {1}",
                                            iClass.getName(), 
                                            _annType.getSimpleName()));
      
      generateGetMethod(out, method);
      out.println(", ");
    }

    out.popDepth();
    out.println("};");
  }

  protected void generateObjectChain(JavaWriter out)
    throws IOException
  {
    out.print(_uniqueName + "_objectChain = new Object[] {");

    for (Class iClass : _interceptors) {
      out.print(_interceptorVarMap.get(iClass) + ", ");
    }
    
    out.println("};");
  }
  
  protected void generateGetMethod(JavaWriter out, Method method)
    throws IOException
  {
    generateGetMethod(out,
                      method.getDeclaringClass().getName(),
                      method.getName(),
                      method.getParameterTypes());
  }
  
  protected void generateGetMethod(JavaWriter out,
                                   String className,
                                   String methodName,
                                   Class []paramTypes)
    throws IOException
  {
    out.print("com.caucho.ejb.util.EjbUtil.getMethod(");
    out.print(className + ".class");
    out.print(", \"" + methodName + "\", new Class[] { ");
    
    for (Class type : paramTypes) {
      out.printClass(type);
      out.print(".class, ");
    }
    out.print("})");
  }

  protected void printCastClass(JavaWriter out, Class type)
    throws IOException
  {
    if (! type.isPrimitive())
      out.printClass(type);
    else if (boolean.class.equals(type))
      out.print("Boolean");
    else if (char.class.equals(type))
      out.print("Character");
    else if (byte.class.equals(type))
      out.print("Byte");
    else if (short.class.equals(type))
      out.print("Short");
    else if (int.class.equals(type))
      out.print("Integer");
    else if (long.class.equals(type))
      out.print("Long");
    else if (float.class.equals(type))
      out.print("Float");
    else if (double.class.equals(type))
      out.print("Double");
    else
      throw new IllegalStateException(type.getName());
  }
}
