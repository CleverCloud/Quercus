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

import com.caucho.xml.NodeListImpl;
import com.caucho.xml.XmlUtil;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.pattern.*;

import org.w3c.dom.Node;

public class NodeSetExpr extends Expr {
  private AbstractPattern _pattern;

  NodeSetExpr(AbstractPattern pattern)
  {
    _pattern = pattern;
  }

  /**
   * Creates an expr, handling some special cases.
   */
  public static Expr create(AbstractPattern pattern)
  {
    if (pattern instanceof NodeTypePattern
        && pattern.getParent() instanceof FromSelf
        && pattern.toString().equals("."))
      return new ObjectExpr(SELF, ".");
    else if (pattern instanceof FromContext
             && ((FromContext) pattern).getCount() == 0
             && pattern.getParent() == null)
      return new ObjectExpr(SELF, ".");
    else if (pattern instanceof NodePattern
             && pattern.getParent() instanceof FromAttributes
             && pattern.getParent().getParent() instanceof FromContext
             && ((FromContext) pattern.getParent().getParent()).getCount() == 0)
      return new ObjectExpr(ATTRIBUTE, ((NodePattern) pattern).getNodeName());
    else
      return new NodeSetExpr(pattern);
  }

  /**
   * Returns the underlying pattern.
   */
  public AbstractPattern getPattern()
  {
    return _pattern;
  }

  /**
   * NodeSetExprs prefer to be node sets.
   */
  public boolean isNodeSet()
  {
    return true;
  }

  /**
   * Returns the value of the expression as a number.
   *
   * @param node the current node
   * @param env the variable environment.
   */
  public double evalNumber(Node node, ExprEnvironment env)
    throws XPathException
  {
    Node value = _pattern.findAny(node, env);

    if (value == null)
      return Double.NaN;

    String string = XmlUtil.textValue(value);

    return stringToNumber(string);
  }

  /**
   * Returns true if there are any patterns matching the pattern.
   *
   * @param node the current node
   * @param env the variable environment.
   */
  public boolean evalBoolean(Node node, ExprEnvironment env)
    throws XPathException
  {
    return _pattern.findAny(node, env) != null;
  }

  /**
   * Returns the value of the node set expression as a string.
   * The value is the text value of the first node.
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return the combined text value of the node.
   */
  public String evalString(Node node, ExprEnvironment env)
    throws XPathException
  {
    Node value = _pattern.findAny(node, env);
    
    if (value == null)
      return "";
    else
      return XmlUtil.textValue(value);
  }

  /**
   * Evaluate a node-set object, returning an ArrayList of the node set.
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return an array list of the nodes
   */
  public Object evalObject(Node node, ExprEnvironment env)
    throws XPathException
  {
    NodeListImpl list = new NodeListImpl();

    NodeIterator iter = _pattern.select(node, env);

    Node value = null;
    while ((value = iter.nextNode()) != null)
      list.add(value);

    return list;
  }

  /**
   * Evaluate a node-set object, returning an iterator of the node set.
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return an iterator of the nodes
   */
  public NodeIterator evalNodeSet(Node node, ExprEnvironment env)
    throws XPathException
  {
    return _pattern.select(node, env);
  }
  
  /**
   * Convert from an expression to a pattern.
   */
  protected AbstractPattern toNodeList()
  {
    return _pattern;
  }

  public boolean equals(Object b)
  {
    if (! (b instanceof NodeSetExpr))
      return false;

    return _pattern.equals(((NodeSetExpr) b)._pattern);
  }

  public String toString()
  {
    return _pattern.toString();
  }
}
