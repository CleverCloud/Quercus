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

package com.caucho.quercus.env;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

/**
 * Represents the introspected static function information.
 */
public class JavaOverloadMethod extends AbstractJavaMethod {
  private static final L10N L = new L10N(JavaOverloadMethod.class);
  
  private AbstractJavaMethod [][]_methodTable
    = new AbstractJavaMethod[0][];

  private AbstractJavaMethod [][]_restMethodTable
    = new AbstractJavaMethod[0][];
  
  public JavaOverloadMethod(AbstractJavaMethod fun)
  {
    overload(fun);
  }

  @Override
  public int getMaxArgLength()
  {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public int getMinArgLength()
  {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public boolean getHasRestArgs()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns an overloaded java method.
   */
  public AbstractJavaMethod overload(AbstractJavaMethod fun)
  {
    if (fun.getHasRestArgs()) {
      int len = fun.getMinArgLength();
      
      if (_restMethodTable.length <= len) {
        AbstractJavaMethod [][]restMethodTable
          = new AbstractJavaMethod[len + 1][];

        System.arraycopy(_restMethodTable, 0,
                         restMethodTable, 0, _restMethodTable.length);

        _restMethodTable = restMethodTable;
      }

      AbstractJavaMethod []methods = _restMethodTable[len];

      if (methods == null)
        _restMethodTable[len] = new AbstractJavaMethod[] { fun };
      else {
        AbstractJavaMethod []newMethods
          = new AbstractJavaMethod[methods.length + 1];

        System.arraycopy(methods, 0, newMethods, 0, methods.length);

        newMethods[methods.length] = fun;

        _restMethodTable[len] = newMethods;
      }
    }
    else {
      int maxLen = fun.getMaxArgLength();
      
      if (_methodTable.length <= maxLen) {
        AbstractJavaMethod [][]methodTable
          = new AbstractJavaMethod[maxLen + 1][];

        System.arraycopy(_methodTable, 0, methodTable, 0, _methodTable.length);

        _methodTable = methodTable;
      }
      
      for (int len = fun.getMinArgLength(); len <= maxLen; len++) {
        AbstractJavaMethod []methods = _methodTable[len];

        if (methods == null)
          _methodTable[len] = new AbstractJavaMethod[] { fun };
        else {
          AbstractJavaMethod []newMethods
            = new AbstractJavaMethod[methods.length + 1];

          System.arraycopy(methods, 0, newMethods, 0, methods.length);

          newMethods[methods.length] = fun;

          _methodTable[len] = newMethods;
        }
      }
    }
    
    return this;
  }
  
  /**
   * Returns the actual function
   */
  @Override
  public AbstractFunction getActualFunction(Expr []args)
  {
    if (args.length <= _methodTable.length) {
      AbstractJavaMethod []methods = _methodTable[args.length];

      if (methods != null) {
        if (methods.length == 1)
          return methods[0];
        else
          return getBestFitJavaMethod(methods, _restMethodTable, args);
      }
      else {
        if (_restMethodTable.length == 0)
          return this;

        return getBestFitJavaMethod(methods, _restMethodTable, args);
      }
    }
    else {
      if (_restMethodTable.length == 0)
        return this;
      else
        return getBestFitJavaMethod(null, _restMethodTable, args);
    }
  }

  /**
   * Evaluates the function.
   */
  @Override
  public Value callMethod(Env env, QuercusClass qClass, Value qThis,
                          Value []args)
  {
    if (args.length < _methodTable.length) {
      AbstractJavaMethod []methods = _methodTable[args.length];

      if (methods != null) {
        if (methods.length == 1)
          return methods[0].callMethod(env, qClass, qThis, args);
        else {
          AbstractJavaMethod method
            = getBestFitJavaMethod(methods, _restMethodTable, args);

          return method.callMethod(env, qClass, qThis, args);
        }
      }
      else {
        if (_restMethodTable.length == 0) {
          env.warning(L.l(
            "'{0}' overloaded method call with {1} arguments "
            + "does not match any overloaded method",
            getName(),
            args.length));

          return NullValue.NULL;
        }

        AbstractJavaMethod method
          = getBestFitJavaMethod(methods, _restMethodTable, args);

        return method.callMethod(env, qClass, qThis, args);
      }
    }
    else {
      if (_restMethodTable.length == 0) {
        env.warning(L.l(
          "'{0}' overloaded method call with {1} "
          + "arguments has too many arguments", getName(), args.length));

        return NullValue.NULL;
      }
      else {
        AbstractJavaMethod method
          = getBestFitJavaMethod(null, _restMethodTable, args);

        return method.callMethod(env, qClass, qThis, args);
      }
    }
  }
  
  /**
   * Returns the Java function that matches the args passed in.
   */
  private AbstractJavaMethod
    getBestFitJavaMethod(AbstractJavaMethod []methods,
                         AbstractJavaMethod [][]restMethodTable,
                         Value []args)
  {

    AbstractJavaMethod minCostJavaMethod = null;
    int minCost = Integer.MAX_VALUE;

    if (methods != null) {
      for (int i = 0; i < methods.length; i++) {
        AbstractJavaMethod javaMethod = methods[i];

        int cost = javaMethod.getMarshalingCost(args);

        if (cost == 0)
          return javaMethod;

        if (cost <= minCost) {
          minCost = cost;
          minCostJavaMethod = javaMethod;
        }
      }
    }

    for (int i = Math.min(args.length, restMethodTable.length) - 1;
         i >= 0;
         i--) {
      if (restMethodTable[i] == null)
        continue;
      
      for (int j = 0; j < restMethodTable[i].length; j++) {
        AbstractJavaMethod javaMethod = restMethodTable[i][j];
        
        int cost = javaMethod.getMarshalingCost(args);

        if (cost == 0)
          return javaMethod;

        if (cost <= minCost) {
          minCost = cost;
          minCostJavaMethod = javaMethod;
        }
      }
    }

    return minCostJavaMethod;
  }
  
  /**
   * Returns the Java function that matches the args passed in.
   */
  private AbstractJavaMethod
    getBestFitJavaMethod(AbstractJavaMethod []methods,
                         AbstractJavaMethod [][]restMethodTable,
                         Expr []args)
  {
    AbstractJavaMethod minCostJavaMethod = null;
    int minCost = Integer.MAX_VALUE;

    if (methods != null) {
      for (int i = 0; i < methods.length; i++) {
        AbstractJavaMethod javaMethod = methods[i];

        int cost = javaMethod.getMarshalingCost(args);

        if (cost == 0)
          return javaMethod;

        if (cost <= minCost) {
          minCost = cost;
          minCostJavaMethod = javaMethod;
        }
      }
    }

    for (int i = Math.min(args.length, restMethodTable.length) - 1;
     i >= 0;
     i--) {
      if (restMethodTable[i] == null)
        continue;
      
      for (int j = 0; j < restMethodTable[i].length; j++) {
        AbstractJavaMethod javaMethod = restMethodTable[i][j];
        
        int cost = javaMethod.getMarshalingCost(args);

        if (cost == 0)
          return javaMethod;

        if (cost <= minCost) {
          minCost = cost;
          minCostJavaMethod = javaMethod;
        }
      }
    }

    return minCostJavaMethod;
  }
  
  /**
   * Returns the cost of marshaling for this method given the args.
   */
  public int getMarshalingCost(Value []args)
  {
    AbstractJavaMethod []methods = null;
    
    if (args.length < _methodTable.length) {
      methods = _methodTable[args.length];
    }
    
    AbstractJavaMethod bestFitMethod
      = getBestFitJavaMethod(methods, _restMethodTable, args);
    
    return bestFitMethod.getMarshalingCost(args);
  }
  
  /**
   * Returns the cost of marshaling for this method given the args.
   */
  public int getMarshalingCost(Expr []args)
  {
    throw new UnsupportedOperationException();
    /*
    int size = _methods.size();
    int minCost = Integer.MAX_VALUE;
    
    for (int i = 0; i < size; i++) {
      int cost = _methods.get(i).getMarshalingCost(args);
      
      if (cost < minCost)
        minCost = cost;
    }

    return minCost;
    */
  }

  @Override
  public String getName()
  {
    AbstractJavaMethod method;

    for (int i = 0; i < _methodTable.length; i++) {
      if (_methodTable[i] != null)
        return _methodTable[i][0].getName();
    }

    return "unknown";
  }

  public String toString()
  {
    return "JavaOverloadMethod[" + getName() + "]";
  }
}
