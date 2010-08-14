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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.es.parser;

import com.caucho.es.ESBase;
import com.caucho.es.ESException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * JavaNewExpr is an intermediate form representing a new expression
 * when the type is known to be a java class.
 */
class JavaNewExpr extends CallExpr {
  private Class javaClass;
  
  private Constructor constructor;
  
  JavaNewExpr(Block block, Class javaClass)
    throws ESException
  {
    super(block, null, null, true);
    
    this.javaClass = javaClass;
  }

  /**
   * adds a new call parameter
   */
  void addCallParam(Expr param)
  {
    param.setUsed();
    args.add(param);
  }

  int getType()
  {
    calculateType();
    
    return type;
  }

  Expr getTypeExpr()
  {
    calculateType();

    return typeExpr;
  }

  private void calculateType()
  {
    if (isCalculated)
      return;
    
    isCalculated = true;

    if (javaClass == null)
      return;

    method = JavaMethod.bestMethod(javaClass, "create", true, args);
    if (method == null ||
        ! method.getReturnType().equals(javaClass))
      method = null;

    if (method != null)
      term = new JavaClassExpr(block, javaClass);

    Constructor []constructors = javaClass.getConstructors();
    Constructor bestConstructor = null;
    int bestCost = Integer.MAX_VALUE;
    
    for (int i = 0; i < constructors.length; i++) {
      Constructor constructor = constructors[i];
      
      if (! Modifier.isPublic(constructor.getModifiers()))
        continue;

      Class []param = constructor.getParameterTypes();

      int cost = JavaMethod.methodCost(param, args);

      if (cost < bestCost) {
        bestCost = cost;
        bestConstructor = constructor;
      }
    }

    this.constructor = bestConstructor;
    
    type = TYPE_JAVA;
    typeExpr = new JavaTypeExpr(block, javaClass);
  }

  void printJavaImpl()
    throws IOException
  {
    if (method != null) {
      super.printJavaImpl();
      return;
    }
    
    if (constructor == null)
      throw new IOException("can't create `" + javaClass.getName() + "'");
    
    cl.print("new ");

    cl.print(javaClass.getName());
    cl.print("(");
    
    Class []params = null;
    if (constructor != null)
      params = constructor.getParameterTypes();
    
    for (int i = 0; i < params.length; i++) {
      Expr expr;

      if (i < args.size())
        expr = (Expr) args.get(i);
      else
        expr = block.newLiteral(ESBase.esUndefined);
      
      if (i != 0)
        cl.print(", ");

      if (params != null && ! params[i].isPrimitive()) {
        cl.print("(");
        printJavaClass(params[i]);
        cl.print(")");
      }

      expr.printJava();
    }
    cl.print(")");
  }
}
