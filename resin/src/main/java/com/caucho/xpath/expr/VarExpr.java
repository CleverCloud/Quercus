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

import com.caucho.util.CharBuffer;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.pattern.NodeIterator;
import com.caucho.xpath.pattern.SingleNodeIterator;

import org.w3c.dom.Node;

public class VarExpr extends Expr {
  private String name;

  public VarExpr(String name)
  {
    this.name = name.intern();
  }

  /**
   * Returns the value of the variable as a boolean.
   *
   * @param node the current node
   * @param env the XPath envivonment
   *
   * @return the boolean value
   */
  public boolean evalBoolean(Node node, ExprEnvironment env)
    throws XPathException
  {
    Var var = (Var) env.getVar(name);

    return var == null ? false : var.getBoolean();
  }

  /**
   * Returns the value of the variable as a double.
   *
   * @param node the current node
   * @param env the XPath envivonment
   *
   * @return the double value
   */
  public double evalNumber(Node node, ExprEnvironment env)
    throws XPathException
  {
    Var var = env.getVar(name);

    return var == null ? Double.NaN : var.getDouble();
  }

  /**
   * Returns the value of the variable as a string
   *
   * @param cb the buffer to append the value
   * @param node the current node
   * @param env the XPath envivonment
   *
   * @return the string value
   */
  public void evalString(CharBuffer cb, Node node, ExprEnvironment env)
    throws XPathException
  {
    Var var = env.getVar(name);

    if (var != null)
      var.getString(cb);
  }

  /**
   * Returns the value of the variable as a string
   *
   * @param env the XPath envivonment
   * @param node the current node
   *
   * @return the string value
   */
  public String evalString(Node node, ExprEnvironment env)
    throws XPathException
  {
    Var var = env.getVar(name);

    return var == null ? "" : var.getString();
  }

  /**
   * Returns the value of the variable as an object
   *
   * @param env the XPath envivonment
   * @param node the current node
   *
   * @return the value
   */
  public Object evalObject(Node node, ExprEnvironment env)
    throws XPathException
  {
    Var var = env.getVar(name);

    return var == null ? null : var.getObject();
  }

  /**
   * Returns the value of the variable as an variable
   *
   * @param node the current node
   * @param env the XPath envivonment
   *
   * @return the value
   */
  public Var evalVar(Node node, ExprEnvironment env)
    throws XPathException
  {
    Var var = env.getVar(name);

    return var;
  }

  /**
   * Returns the value of the variable as a node set.
   *
   * @param node the current node
   * @param env the variable envivonment
   *
   * @return the value
   */
  public NodeIterator evalNodeSet(Node node, ExprEnvironment env)
    throws XPathException
  {
    Var var = env.getVar(name);

    if (var == null)
      return new SingleNodeIterator(env, null);
    else
      return var.getNodeSet(env);
  }

  public String toString()
  {
    return "$" + name;
  }
}
