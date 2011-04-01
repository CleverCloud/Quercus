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

package com.caucho.xpath;

import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xpath.pattern.NodeIterator;

import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.logging.Logger;

/**
 * A node selection pattern.  Patterns represent compiled XPath node selectors.
 * They can be used to find nodes, select nodes, and test if a node matches
 * a pattern.
 *
 * <p>There are two types of patterns: select patterns and match patterns.
 * <p>Select patterns match a node relative to another node.
 * <code>find</code> and <code>select</code> use select patterns.
 * <p>Match patterns match a node in isolation.  <code>isMatch</code> uses
 * match patterns.
 */
public class Pattern {
  protected final static Logger log
    = Logger.getLogger(Pattern.class.getName());
  
  private AbstractPattern pattern;

  Pattern(AbstractPattern pattern)
  {
    this.pattern = pattern;
  }

  /**
   * Returns the first node matching the pattern.  The pattern should
   * be a select pattern.
   *
   * @param node node represented by '.' and start of match.
   *
   * @return first matching node
   */
  public Node find(Node node)
    throws XPathException
  {
    if (node == null)
      throw new NullPointerException();

    Env env = XPath.createEnv();
    // XXX: doesn't make sense for a match to have a context?
    //env.setCurrentNode(node);
    //env.setContextNode(node);
    
    Iterator iter = pattern.select(node, env);

    Node value = null;
    if (iter.hasNext())
      value = (Node) iter.next();

    XPath.freeEnv(env);
    
    return value;
  }

  /**
   * Returns the first node matching the pattern.  The pattern should
   * be a select pattern.
   *
   * @param node node represented by '.' and start of match.
   * @param env variable environment.
   *
   * @return first matching node
   */
  public Node find(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      throw new NullPointerException();

    if (env instanceof Env) {
      Env globalEnv = (Env) env;
      
      // XXX: doesn't make sense for a match to have a context?
      //globalEnv.setCurrentNode(node);
      //globalEnv.setContextNode(node);
    }
    
    Iterator iter = pattern.select(node, env);

    Node value = null;
    if (iter.hasNext())
      value = (Node) iter.next();

    return value;
  }

  /**
   * Selects all nodes matching the pattern.  The pattern should be a
   * select pattern.
   *
   * @param node node represented by '.' and start of match.
   *
   * @return iterator of matching nodes
   */
  public NodeIterator select(Node node)
    throws XPathException
  {
    if (node == null)
      throw new NullPointerException();

    Env env = XPath.createEnv();

    env.setCurrentNode(node);
    
    NodeIterator iter = pattern.select(node, env);

    XPath.freeEnv(env);

    return iter;
  }

  /**
   * Selects all nodes matching the pattern.  The pattern should be a
   * select pattern.
   *
   * @param context node represented by '.' and start of match.
   * @param env variable environment.
   *
   * @return iterator of matching nodes
   */
  public NodeIterator select(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      throw new NullPointerException();

    if (env instanceof Env) {
      Env globalEnv = (Env) env;
      globalEnv.setCurrentNode(node);
      globalEnv.setContextNode(node);
    }
    
    return pattern.select(node, env);
  }
  
  /**
   * Test if the node matches the pattern.  The pattern should be a
   * match pattern.
   *
   * @param node node to test
   *
   * @return true if the pattern matches.
   */
  public boolean isMatch(Node node)
    throws XPathException
  {
    Env env = XPath.createEnv();

    // XXX: doesn't make sense for a match to have a context?
    //env.setCurrentNode(node);
    //env.setContextNode(node);
    
    boolean value = pattern.match(node, env);

    XPath.freeEnv(env);

    return value;
  }

  /**
   * Test if the node matches the pattern.  The pattern should be a
   * match pattern.
   *
   * @param node node to test
   * @param env variable environment.
   *
   * @return true if the pattern matches.
   */
  public boolean isMatch(Node node, ExprEnvironment env)
    throws XPathException
  {
    return pattern.match(node, env);
  }

  /**
   * Returns the underlying pattern implementation.
   */
  public AbstractPattern getPattern()
  {
    return pattern;
  }

  public String toString()
  {
    return pattern.toString();
  }
}
