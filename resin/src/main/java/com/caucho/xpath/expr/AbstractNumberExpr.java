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

package com.caucho.xpath.expr;

import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

abstract public class AbstractNumberExpr extends Expr {
  public boolean isNumber()
  {
    return true;
  }

  /**
   * Evaluates to a variable.
   *
   * @param node the node to evaluate and use as a context.
   * @param env the variable environment.
   *
   * @return a variable containing the value.
   */
  public Var evalVar(Node node, ExprEnvironment env)
    throws XPathException
  {
    double value = evalNumber(node, env);

    return NumberVar.create(value);
  }

  /**
   * Evaluates the expression as a number.
   *
   * @param node the node to evaluate and use as a context.
   * @param env the variable environment.
   *
   * @return the numeric value
   */
  abstract public double evalNumber(Node node, ExprEnvironment env)
    throws XPathException;

  /**
   * Evaluates the expression as a boolean.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return the boolean representation of the number.
   */
  public boolean evalBoolean(Node node, ExprEnvironment env)
    throws XPathException
  {
    double value = evalNumber(node, env);

    return value != 0.0 && ! Double.isNaN(value);
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return the string representation of the number.
   */
  public String evalString(Node node, ExprEnvironment env)
    throws XPathException
  {
    double value = evalNumber(node, env);

    if ((int) value == value)
      return String.valueOf((int) value);
    else
      return String.valueOf(value);
  }

  /**
   * Evaluates the expression as an object.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return the Double representation of the number.
   */
  public Object evalObject(Node node, ExprEnvironment env)
    throws XPathException
  {
    return new Double(evalNumber(node, env));
  }
}
