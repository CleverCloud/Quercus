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

import com.caucho.xml.XmlUtil;
import com.caucho.xpath.Env;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Matches any descendant.
 */
public class FromDescendants extends Axis {
  private boolean _self;

  public FromDescendants(AbstractPattern parent, boolean self)
  {
    super(parent);
    
    _self = self;

    if (parent == null)
      throw new RuntimeException();
  }

  /**
   * Matches the current node if it can find a parent node matching the
   * parent pattern.
   *
   * @param node the node to test
   * @param env the variable environment
   *
   * @return true if it matches.
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      return false;

    if (! _self)
      node = node.getParentNode();
    
    for (; node != null; node = node.getParentNode()) {
      if (_parent.match(node, env))
        return true;
    }

    return false;
  }

  /**
   * Counts matching nodes between the axis-context and the node
   *
   * @param node the starting node
   * @param env the xpath environment
   * @param pattern the axis match pattern
   *
   * @return the index of the position
   */
  public int position(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int index = env.getPositionIndex();

    int pos = 0;

    Node parentNode = node;
    Node ptr = node;

    for (; index >= 0; index--) {
      for (; parentNode != null; parentNode = parentNode.getParentNode()) {
        if (_parent.match(parentNode, env))
          break;
      }
      
      for (; ptr != null; ptr = XmlUtil.getPrevious(ptr)) {
        if (ptr == parentNode && ! _self)
          break;
        
        if (pattern.match(ptr, env))
          pos++;

        if (ptr == parentNode)
          break;
      }
      
      if (index > 0 && parentNode != null) {
        parentNode = parentNode.getParentNode();
        if (_self)
          ptr = XmlUtil.getPrevious(ptr);
      }
      else
        break;
    }

    if (parentNode != null)
      parentNode = parentNode.getParentNode();
    
    for (; parentNode != null; parentNode = parentNode.getParentNode()) {
      if (_parent.match(parentNode, env)) {
        env.setMorePositions(true);
        break;
      }
    }

    return pos;
  }

  /**
   * Counts the descendant nodes matching the pattern.
   *
   * @param node the starting node
   * @param env the xpath environment
   * @param pattern the axis match pattern
   *
   * @return the count of nodes
   */
  public int count(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int index = env.getPositionIndex();
    
    Node axis;
    if (_self)
      axis = getAxisContext(node, env);
    else
      axis = getAxisContext(node.getParentNode(), env);

    for (; index > 0; index--)
      axis = getAxisContext(axis.getParentNode(), env);

    if (getAxisContext(axis.getParentNode(), env) != null)
      env.setMorePositions(true);

    int count = 0;
    for (Node ptr = axis;
         ptr != null;
         ptr = XmlUtil.getNext(ptr)) {
      if (pattern.match(ptr, env))
        count++;
    }

    return count;
  }

  /**
   * The axis context is any ancestor matching the parent pattern.
   */
  private Node getAxisContext(Node node, ExprEnvironment env)
    throws XPathException
  {
    for (; node != null; node = node.getParentNode()) {
      if (_parent.match(node, env))
        return node;
    }

    return node;
  }

  /**
   * Returns true if the pattern is strictly ascending.
   */
  public boolean isStrictlyAscending()
  {
    if (_parent == null)
      return true;
    else
      return _parent.isSingleLevel();
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
    if (_self)
      return node;
    else
      return node.getFirstChild();
  }

  /**
   * Returns the next node in the selection order.
   *
   * @param node the current node
   * @param ndoe the last node
   *
   * @return the next node
   */
  public Node nextNode(Node node, Node lastNode)
  {
    Node next = XmlUtil.getNext(node);
    
    return next == lastNode ? null : next;
  }

  /**
   * Returns the last node in the selection order.
   *
   * @param node the current node
   *
   * @return the last node
   */
  public Node lastNode(Node node)
  {
    Node last = node;

    for (;
         last != null && last.getNextSibling() == null;
         last = last.getParentNode()) {
    }

    return last != null ? last.getNextSibling() : null;
  }

  public String toString()
  {
    if (_self)
      return getPrefix() + "descendant-or-self::";
    else
      return getPrefix() + "descendant::";
  }
}
