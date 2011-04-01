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

import java.util.logging.Logger;

/**
 * A node selection pattern.  AbstractPatterns represent compiled XPath node selectors.
 * They can be used to find nodes, select nodes, and test if a node matches
 * a pattern.
 *
 * <p>There are two types of patterns: select patterns and match patterns.
 * <p>Select patterns match a node relative to another node.
 * <code>find</code> and <code>select</code> use select patterns.
 * <p>Match patterns match a node in isolation.  <code>isMatch</code> uses
 * match patterns.
 */
abstract public class AbstractPattern {
  protected static final Logger log
    = Logger.getLogger(AbstractPattern.class.getName());

  // This is the value Axis wants
  public static final String XMLNS = "http://www.w3.org/2000/xmlns/";
  
  protected AbstractPattern _parent;
  protected AbstractPattern _child;

  AbstractPattern(AbstractPattern parent)
  {
    _parent = parent;
    
    if (parent != null && parent._child == null)
      parent._child = this;
  }

  /**
   * Returns the parent pattern.
   */
  public AbstractPattern getParent()
  {
    return _parent;
  }
 
  /**
   * Returns the pattern's default priority as defined by the XSLT draft.
   */
  public double getPriority()
  {
    return 0.5;
  } 

  /**
   * Returns the name of the matching node or '*' if many nodes match.
   *
   * <p>The Xsl package uses this to speed template matching.
   */
  public String getNodeName()
  {
    return "*";
  }

  /**
   * Returns an iterator selecting nodes in document order.
   *
   * @param node the starting node.
   * @param env the variable environment.
   *
   * @return an iterator selecting nodes in document order.
   */
  public NodeIterator select(Node node, ExprEnvironment env)
    throws XPathException
  {
    NodeIterator base = createNodeIterator(node, env, copyPosition());

    if (isStrictlyAscending())
      return base;
    else
      return new MergeIterator(env, base);
  }

  /**
   * Returns an iterator selecting unique nodes.  The nodes are not
   * necessarily in document order.
   *
   * @param node the starting node.
   * @param env the variable environment.
   * @param context the context node.
   *
   * @return an iterator selecting unique nodes.
   */
  public NodeIterator selectUnique(Node node, ExprEnvironment env)
    throws XPathException
  {
    NodeIterator base = createNodeIterator(node, env, copyPosition());

    if (isUnique())
      return base;
    else
      return new UniqueIterator(env, base);
  }

  /**
   * Find any node matching the pattern.
   *
   * @param node the current node
   * @param env the xpath environment
   *
   * @return one of the matching nodes
   */
  public Node findAny(Node node, ExprEnvironment env)
    throws XPathException
  {
    NodeIterator base = createNodeIterator(node, env, copyPosition());

    return base.nextNode();
  }
  
  /**
   * Returns true if the pattern is strictly ascending.
   */
  public boolean isStrictlyAscending()
  {
    if (_parent != null)
      return _parent.isStrictlyAscending();
    else
      return false;
  }
  
  /**
   * Returns true if the pattern's iterator returns unique nodes.
   */
  public boolean isUnique()
  {
    if (_parent != null)
      return _parent.isUnique();
    else
      return false;
  }
  
  /**
   * Returns true if the pattern selects a single node
   */
  boolean isSingleSelect()
  {
    if (_parent != null)
      return _parent.isSingleSelect();
    else
      return false;
  }
  
  /**
   * Returns true if the pattern returns nodes on a single level.
   */
  boolean isSingleLevel()
  {
    return isSingleSelect();
  }

  /**
   * Creates a new node iterator.
   *
   * @param node the starting node
   * @param env the variable environment
   * @param pattern the level pattern
   *
   * @return the node iterator
   */
  public NodeIterator createNodeIterator(Node node, ExprEnvironment env,
                                         AbstractPattern pattern)
    throws XPathException
  {
    if (_parent == null)
      throw new RuntimeException(String.valueOf(this) + " " + getClass());
    else
      return _parent.createNodeIterator(node, env, pattern);
  }

  /**
   * Returns the first node in the selection order.
   *
   * @param node the current node
   * @param variable environment
   *
   * @return the first node
   */
  public Node firstNode(Node node, ExprEnvironment env)
    throws XPathException
  {
    throw new UnsupportedOperationException(String.valueOf(this) + " " + getClass());
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
    return null;
  }

  /**
   * Returns the next node in the selection order.
   *
   * @param node the current node
   * @param last the last node
   *
   * @return the next node
   */
  public Node nextNode(Node node, Node last)
    throws XPathException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * The core match function test if the pattern matches the node.
   *
   * @param node the node to test
   * @param env the variable environment.
   *
   * @return true if the node matches the pattern.
   */
  public abstract boolean match(Node node, ExprEnvironment env)
    throws XPathException;

  /**
   * Return true if the iterator is in document-order.
   */
  public boolean isAscending()
  {
    return true;
  }

  /**
   * Returns the position of the node in its context for a match pattern.
   *
   * @param node the current node
   * @param env the variable environment
   * @param pattern the position pattern
   *
   * @return the node's position.
   */
  public int position(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    return _parent.position(node, env, pattern);
  }

  /**
   * Returns the number of nodes in its context for a match pattern.
   *
   * @param node the current node
   * @param env the variable environment
   * @param pattern the position pattern
   *
   * @return the count of nodes in the match selection
   */
  public int count(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    return _parent.count(node, env, pattern);
  }

  /**
   * Returns the owning axis for the pattern.
   */
  public AbstractPattern copyAxis()
  {
    if (_parent != null)
      return _parent.copyAxis();
    else
      return null;
  }

  /**
   * Returns the position matching pattern.
   */
  public AbstractPattern copyPosition()
  {
    return this;
  }

  /**
   * For string conversion, returns the string prefix corresponding to
   * the parents.
   */
  protected String getPrefix()
  {
    if (_parent == null ||
        _parent instanceof FromAny)
      return "";
    else if (_parent instanceof FromContext) { // check for in Filter?
      FromContext context = (FromContext) _parent;

      String name = "";
      for (int i = 0; i < context.getCount(); i++) {
        name += "../";
      }
      return name;
    }
    else if (_parent instanceof FromRoot)
      return "/";
    else
      return _parent + "/";
  }

  public String toPatternString()
  {
    return toString();
  }
}
