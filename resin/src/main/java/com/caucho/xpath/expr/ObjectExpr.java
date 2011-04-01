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

import com.caucho.xml.XmlUtil;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.pattern.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;

public class ObjectExpr extends Expr {
  private int _code;
  private String _name;
  private Expr _left;
  private Expr _right;
  private Expr _third;

  public ObjectExpr(int code, ArrayList args)
  {
    _code = code;

    if (args != null && args.size() > 0)
      _left = (Expr) args.get(0);
    if (args != null && args.size() > 1)
      _right = (Expr) args.get(1);
    if (args != null && args.size() > 2)
      _third = (Expr) args.get(2);

    if (_right == null || _third == null)
      throw new NullPointerException();
  }

  public ObjectExpr(int code, String name)
  {
    _code = code;
    _name = name;
  }

  /**
   * Returns true if the expression evaluates to a node-set.
   */
  public boolean isNodeSet()
  {
    return _code == SELF;
  }

  /**
   * Returns true if the expression evaluates to a node-set.
   */
  public boolean isString()
  {
    return _code == ATTRIBUTE;
  }

  /**
   * Evaluates the expression as a boolean.
   *
   * @param node current node
   * @param env the environment
   *
   * @return the boolean value
   */
  public boolean evalBoolean(Node node, ExprEnvironment env)
    throws XPathException
  {
    switch (_code) {
    case IF:
      if (_left.evalBoolean(node, env))
        return _right.evalBoolean(node, env);
      else
        return _third.evalBoolean(node, env);

    case ATTRIBUTE:
      if (node instanceof Element)
        return ! ((Element) node).getAttribute(_name).equals("");
      else
        return false;
      
    case SELF:
      return true;

    default:
      return toBoolean(evalObject(node, env));
    }
  }

  /**
   * Evaluates the expression as number.
   *
   * @param node current node
   * @param env the environment
   */
  public double evalNumber(Node node, ExprEnvironment env)
    throws XPathException
  {
    switch (_code) {
    case IF:
      if (_left.evalBoolean(node, env))
        return _right.evalNumber(node, env);
      else
        return _third.evalNumber(node, env);

    case ATTRIBUTE:
      if (node instanceof Element)
        return toDouble(((Element) node).getAttribute(_name));
      else
        return Double.NaN;
      
    case SELF:
      return toDouble(XmlUtil.textValue(node));
      
    default:
      return toDouble(evalObject(node, env));
    }
  }

  /**
   * Evaluates the expression as string.
   *
   * @param node current node
   * @param env the environment
   *
   * @return the string representation
   */
  public String evalString(Node node, ExprEnvironment env)
    throws XPathException
  {
    switch (_code) {
    case IF:
      if (_left.evalBoolean(node, env))
        return _right.evalString(node, env);
      else
        return _third.evalString(node, env);
      
    case ATTRIBUTE:
      if (node instanceof Element)
        return ((Element) node).getAttribute(_name);
      else
        return "";

    case SELF:
      return XmlUtil.textValue(node);
      
    default:
      return toString(evalObject(node, env));
    }
  }

  /**
   * Evaluates the expression as an object.
   *
   * @param node current node
   * @param env the environment
   *
   * @return the object representation
   */
  public Object evalObject(Node node, ExprEnvironment env)
    throws XPathException
  {
    switch (_code) {
    case IF:
      if (_left.evalBoolean(node, env))
        return _right.evalObject(node, env);
      else
        return _third.evalObject(node, env);

    case SELF:
      return node;
      
    case ATTRIBUTE:
      if (node instanceof Element)
        return ((Element) node).getAttributeNode(_name);
      else
        return null;
      
    default:
      return null;
    }
  }

  /**
   * Evaluates the expression as a node set.
   *
   * @param node current node
   * @param env the variable environment
   */
  public NodeIterator evalNodeSet(Node node, ExprEnvironment env)
    throws XPathException
  {
    switch (_code) {
    case IF:
      if (_left.evalBoolean(node, env))
        return _right.evalNodeSet(node, env);
      else
        return _third.evalNodeSet(node, env);

    case SELF:
      return new SingleNodeIterator(env, node);

    case ATTRIBUTE:
      if (node instanceof Element)
        return new SingleNodeIterator(env, ((Element) node).getAttributeNode(_name));
      else
        return new SingleNodeIterator(env, null);
      
    default:
      return null;
    }
  }
  
  /**
   * Convert from an expression to a pattern.
   */
  protected AbstractPattern toNodeList()
  {
    switch (_code) {
    case SELF:
      return NodeTypePattern.create(new FromSelf(null), NodeTypePattern.ANY);

    case ATTRIBUTE:
      return new NodePattern(new FromAttributes(null),
                             _name, Node.ATTRIBUTE_NODE);

    default:
      return super.toNodeList();
    }
  }

  public String toString()
  {
    switch (_code) {
    case IF:
      return "if(" + _left + "," + _right + "," + _third + ")";

    case SELF:
      return ".";

    case ATTRIBUTE:
      return "@" + _name;

    default:
      return super.toString();
    }
  }
}
