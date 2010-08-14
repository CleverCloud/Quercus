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

package com.caucho.java.gen;

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.reflect.*;

/**
 * Basic method generation.
 */
public class BaseMethod extends ClassComponent {
  private static final L10N L = new L10N(BaseMethod.class);

  private Method _method;
  private String _methodName;

  private boolean _isStatic;
  private String _visibility = "public";

  private Class _returnType;
  private Class []_parameterTypes;
  
  private Class []_exceptionTypes;

  private CallChain _call;

  /**
   * Creates the base method
   */
  public BaseMethod(String methodName, CallChain call)
  {
    _methodName = methodName;

    setCall(call);
  }

  /**
   * Creates the base method
   */
  public BaseMethod(Method method, CallChain call)
  {
    _method = method;
    _exceptionTypes = method.getExceptionTypes();

    setCall(call);
  }

  /**
   * Creates the base method
   */
  public BaseMethod(Method method)
  {
    _method = method;
    _exceptionTypes = method.getExceptionTypes();
  }

  /**
   * Creates the base method
   */
  public BaseMethod(Method apiMethod, Method implMethod)
  {
    this(apiMethod, new MethodCallChain(implMethod));
    
    _exceptionTypes = implMethod.getExceptionTypes();
  }

  /**
   * Returns the call.
   */
  public CallChain getCall()
  {
    return _call;
  }

  /**
   * Sets the call.
   */
  public void setCall(CallChain call)
  {
    if (call == null)
      throw new NullPointerException();
    
    _call = call;
  }

  /**
   * Returns the method.
   */
  public Method getMethod()
  {
    return _method;
  }
  
  /**
   * Returns the method name.
   */
  public String getMethodName()
  {
    if (_methodName != null)
      return _methodName;
    else
      return _method.getName();
  }

  /**
   * Returns the parameter types.
   */
  public Class []getParameterTypes()
  {
    // ejb/0f7a

    if (_parameterTypes != null)
      return _parameterTypes;
    else if (_method != null)
      return _method.getParameterTypes();
    else
      return _call.getParameterTypes();
  }

  /**
   * Gets the return type.
   */
  public Class getReturnType()
  {
    if (_method != null)
      return _method.getReturnType();
    else
      return _call.getReturnType();
  }

  /**
   * Returns the exception types.
   */
  public Class []getExceptionTypes()
  {
    return _exceptionTypes;
  }
  
  /**
   * Generates the code for the class.
   *
   * @param out the writer to the output stream.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    String []args = generateMethodHeader(out);

    Class []exceptionTypes = getExceptionTypes();
    if (exceptionTypes != null && exceptionTypes.length > 0) {
      out.print("  throws ");

      for (int i = 0; i < exceptionTypes.length; i++) {
        if (i != 0)
          out.print(", ");

        out.printClass(exceptionTypes[i]);
      }
      out.println();
    }

    // XXX: exception
    out.println("{");
    out.pushDepth();

    generateCall(out, args);
    
    out.popDepth();
    out.println("}");
  }
  
  /**
   * Generates the method header
   *
   * @param out the writer to the output stream.
   *
   * @return the method arguments
   */
  public String []generateMethodHeader(JavaWriter out)
    throws IOException
  {
    if (_visibility != null && ! _visibility.equals(""))
      out.print(_visibility + " ");

    if (_isStatic)
      out.print("static ");

    out.printClass(getReturnType());
    out.print(" " + getMethodName() + "(");

    Class []parameterTypes = getParameterTypes();
    String []args = new String[parameterTypes.length];
    
    for (int i = 0; i < args.length; i++) {
      if (i != 0)
        out.print(", ");

      // ejb/0f7a
      if (i + 1 == args.length && getMethod().isVarArgs()) {
        out.printClass(parameterTypes[i].getComponentType());
        out.print("...");
      }
      else
        out.printClass(parameterTypes[i]);
      
      args[i] = "a" + i;
      out.print(" " + args[i]);

    }
    out.println(")");

    return args;
  }
  
  /**
   * Generates the code for the call.
   *
   * @param out the writer to the output stream.
   * @param args the arguments
   */
  protected void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    _call.generateCall(out, null, null, args);
  }
}
