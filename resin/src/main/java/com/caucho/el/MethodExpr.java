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

package com.caucho.el;

import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Represents a named method call on an object.
 */
public class MethodExpr extends Expr {
  private Expr _expr;
  private String _methodName;
  
  private Expr []_args;

  /**
   * Creates a new method expression.
   *
   * @param expr the expression generating the object on which the method 
   *        is to be called
   * @param methodName the name of the method to call
   * @param args the arguments for the method
   */
  public MethodExpr(Expr expr, String methodName, Expr []args)
  {
    _expr = expr;
    _methodName = methodName;
    _args = args;
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    Object aObj = _expr.getValue(env);

    if (aObj == null)
      return null;

    Object []objs = new Object[_args.length];

    try {
      Method method = findMethod(aObj.getClass());

      if (method != null) {
        Class []params = method.getParameterTypes();

        for (int j = 0; j < params.length; j++) {
          objs[j] = evalArg(params[j], _args[j], env);
        }

        // XXX: probably should look for the interface instead.
        try {
          method.setAccessible(true);
        } catch (Throwable e) {
        }
        
        return method.invoke(aObj, objs);
      }
      
      return null;
    } catch (Exception e) {
      return invocationError(e);
    }
  }
  
  private Method findMethod(Class type)
  {
    if (type == null)
      return null;
    
    if (Modifier.isPublic(type.getModifiers())) {
      Method []methods = type.getDeclaredMethods();
      
      for (int i = 0; i < methods.length; i++) {
        Method method = methods[i];
        
        if (! Modifier.isPublic(method.getModifiers()))
          continue;
        
        Class []params = method.getParameterTypes();
        
        if (method.getName().equals(_methodName) &&
            params.length == _args.length)
          return method;
      }
    }
    
    Class []interfaces = type.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      Method method = findMethod(interfaces[i]);
      if (method != null)
        return method;
    }
    
    return findMethod(type.getSuperclass());
  }
  
  static Object evalArg(Class cl, Expr expr, ELContext env)
    throws ELException
  {
    Marshall marshall = Marshall.create(cl);

    return marshall.marshall(expr, env);
  }
  
  /**
   * Prints the code to create an LongLiteral.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.MethodExpr(");
    _expr.printCreate(os);
    os.print(", \"");
    os.print(_methodName);
    os.print("\", new com.caucho.el.Expr[] {");
    
    for (int i = 0; i < _args.length; i++) {
      if (i != 0)
        os.print(", ");
      _args[i].printCreate(os);
    }
    os.println("})");
  }
  
  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof MethodExpr))
      return false;
    
    MethodExpr expr = (MethodExpr) o;
    
    if (! _expr.equals(expr._expr))
      return false;
    
    if (! _methodName.equals(expr._methodName))
      return false;
    
    if (_args.length != expr._args.length)
      return false;
    
    for (int i = 0; i < _args.length; i++) {
      if (! _args[i].equals(expr._args[i]))
        return false;
    }
    
    return true;
  }
  
  /**
   * Returns the printed version.
   */
  public String toString()
  {
    return "MethodExpr[" + _expr + "," + _methodName + "]";
  }
}
