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

package com.caucho.xpath.pattern;

import com.caucho.xpath.Env;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * The self axis matches the current node.
 */
public class FromSelf extends Axis {
  public FromSelf(AbstractPattern parent)
  {
    super(parent);
  }
  /**
   * The self axis always matches.
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return true
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    return _parent == null || _parent.match(node, env);
  }

  /**
   * Only one node in the self axis.
   */
  public int position(Node node, Env env, AbstractPattern pattern)
  {
    return 1;
  }

  /**
   * Only one node in the self axis.
   */
  public int count(Node node, Env env, AbstractPattern pattern)
  {
    return 1;
  }

  /**
   * Returns the first node in the selection order.
   *
   * @param node the current node
   *
   * @return the first node
   */
  public Node firstNode(Node node, ExprEnvironment env)
  {
    return node;
  }

  /**
   * Returns the next node in the selection order.
   *
   * @param node the current node
   * @param lastNode the last node
   *
   * @return the next node
   */
  public Node nextNode(Node node, Node lastNode)
  {
    return null;
  }

  /**
   * Returns true if the two patterns are equal.
   */
  public boolean equals(Object b)
  {
    if (! (b instanceof FromSelf))
      return false;

    FromSelf bPattern = (FromSelf) b;
    
    return (_parent == bPattern._parent ||
            (_parent != null && _parent.equals(bPattern._parent)));
  }

  public String toString()
  {
    return getPrefix() + "self::";
  }
}
