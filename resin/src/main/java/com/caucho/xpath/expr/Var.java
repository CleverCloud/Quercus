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
import com.caucho.xpath.pattern.NodeArrayListIterator;
import com.caucho.xpath.pattern.NodeIterator;
import com.caucho.xpath.pattern.NodeListIterator;
import com.caucho.xpath.pattern.SingleNodeIterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public abstract class Var {
  /**
   * Returns the value as a boolean.
   */
  boolean getBoolean()
    throws XPathException
  {
    return Expr.toBoolean(getObject());
  }
  
  /**
   * Returns the value as a double.
   */
  double getDouble()
    throws XPathException
  {
    Object o = getObject();
    return Expr.toDouble(getObject());
  }
  
  /**
   * Returns the value as a string.
   */
  String getString()
    throws XPathException
  {
    return Expr.toString(getObject());
  }
  
  /**
   * Returns the value as a string.
   */
  void getString(CharBuffer cb)
    throws XPathException
  {
    cb.append(getString());
  }

  /**
   * Returns the value as a node set.
   */
  NodeIterator getNodeSet(ExprEnvironment env)
    throws XPathException
  {
    Object obj = getObject();

    if (obj instanceof Node)
      return new SingleNodeIterator(env, (Node) obj);
    else if (obj instanceof NodeList)
      return new NodeListIterator(env, (NodeList) obj);
    else if (obj instanceof ArrayList)
      return new NodeArrayListIterator(env, (ArrayList) obj);
    else
      return new SingleNodeIterator(env, null);
  }
  
  /**
   * Returns the value as an object.
   */
  abstract Object getObject();
  
  /**
   * Frees the var.
   */
  public void free()
  {
  }
}
