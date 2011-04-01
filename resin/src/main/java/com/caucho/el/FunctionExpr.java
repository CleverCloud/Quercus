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

/**
 * Represents a method call.  The expr will evaluate to a method.
 */
public class FunctionExpr extends Expr {
  private Expr _expr;
  
  private Expr []_args;

  /**
   * Creates a new method expression.
   *
   * @param expr the expression generating the method to be called
   * @param args the arguments for the method
   */
  public FunctionExpr(Expr expr, Expr []args)
  {
    _expr = expr;
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
    if (_expr instanceof StaticMethodExpr)
      return ((StaticMethodExpr) _expr).evalMethod(_args, env);
    
    Object aObj = _expr.getValue(env);

    Method method = null;
    
    if (aObj instanceof Method)
      method = (Method) aObj;
    
    else if (aObj instanceof Method[]) {
      Method []methods = (Method []) aObj;

      if (methods.length < _args.length)
        return null;
      
      method = methods[_args.length];
    }

    if (method == null) {
      // jsp/18i7
      throw new ELParseException(L.l("'{0}' is an unknown function.", _expr));
    }

    Class []params = method.getParameterTypes();
    
    if (params.length != _args.length) {
      // jsp/18i8
      throw new ELParseException(L.l("arguments '{0}' do not match expected length {1}.", _expr, params.length));
    }

    try {
      Object []objs = new Object[_args.length];
      
      for (int i = 0; i < params.length; i++)
        objs[i] = MethodExpr.evalArg(params[i], _args[i], env);

      return method.invoke(null, objs);
    } catch (ELException e) {
      throw e;
    } catch (Exception e) {
      throw new ELException(e);
    }
  }

  /**
   * Prints the code to create an LongLiteral.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.FunctionExpr(");
    _expr.printCreate(os);
    os.print(", new com.caucho.el.Expr[] {");

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
    if (! (o instanceof FunctionExpr))
      return false;

    FunctionExpr expr = (FunctionExpr) o;

    if (! _expr.equals(expr._expr))
      return false;

    if (_args.length != expr._args.length)
      return false;

    for (int i = 0; i < _args.length; i++) {
      if (! _args[i].equals(expr._args[i]))
        return false;
    }

    return true;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(_expr);
    sb.append('(');
    for (int i = 0; i < _args.length; i++) {
      if (i != 0)
        sb.append(", ");

      sb.append(_args[i]);
    }
    sb.append(')');

    return sb.toString();
  }
}
