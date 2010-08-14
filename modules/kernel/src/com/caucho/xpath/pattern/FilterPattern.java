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
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * matches a node if it matches a filter expression.  Filter implements
 * the a[b] pattern.
 */
public class FilterPattern extends AbstractPattern {
  private Expr _expr;
  
  private AbstractPattern _position;

  public FilterPattern(AbstractPattern parent, Expr expr)
  {
    super(parent);
    
    _expr = expr;

    if (parent == null)
      throw new RuntimeException();
  }

  public String getNodeName()
  {
    return _parent.getNodeName();
  }

  /**
   * Returns the filter's expression.
   */
  public Expr getExpr()
  {
    return _expr;
  }

  /**
   * Matches if the filter expression matches.  When the filter expression
   * returns a number, match if the position() equals the expression.
   *
   * @param node the current node to test
   * @param env the variable environment
   *
   * @return true if the pattern matches.
   */
  /* Because match works from the bottom up, it must sometimes evaluate
   * the expression several times.  Take the match pattern
   * 'a/preceding-sibling::b[2]'.  Given the XML:
   *   <foo>
   *     <b id=1/>
   *     <b id=2/>
   *     <a id=3/>
   *     <b id=4/>
   *     <a id=5/>
   *   </foo>
   * The node b[@id=1] has two valid positions: 2 and 3, corresponding to
   * 'axis-contexts' a[@id=3] and a[@id=5].
   *
   * The position index iterates through the 'axis-contexts.'  The axis,
   * e.g. preceding-sibling, signals the filter that another axis-context
   * is available through env.setMorePositions().
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (! _parent.match(node, env))
      return false;
    
    // Select patterns use the first shortcut.
    int envPosition = env.getContextPosition();

    if (envPosition > 0) {
      if (_expr.isBoolean()) {
        return _expr.evalBoolean(node, env);
      }
      else if (_expr.isNumber()) {
        double test = _expr.evalNumber(node, env);

        return (envPosition == (int) test);
      }
      else {
        Object value = _expr.evalObject(node, env);

        if (value instanceof Number)
          return (envPosition == ((Number) value).intValue());

        return Expr.toBoolean(value);
      }
    }

    // Match patterns need to use a more complicated test.
    if (! (env instanceof Env))
      throw new RuntimeException(String.valueOf(env));
    
    Env globalEnv = (Env) env;
    boolean oldMorePositions = globalEnv.setMorePositions(true);
    int oldIndex = globalEnv.setPositionIndex(0);
    try {
      for (int i = 0; globalEnv.hasMorePositions(); i++) {
        globalEnv.setPositionIndex(i);
        globalEnv.setMorePositions(false);

        if (_expr.isNumber()) {
          double test = _expr.evalNumber(node, env);
          double position = _parent.position(node, globalEnv,
                                             _parent.copyPosition());

          if (position == test)
            return true;
        }
        else if (_expr.isBoolean()) {
          if (_expr.evalBoolean(node, env))
            return true;
        }
        else {
          Object value = _expr.evalObject(node, env);

          if (value instanceof Number) {
            double test = ((Number) value).doubleValue();
            double position = _parent.position(node, globalEnv,
                                               _parent.copyPosition());

            if (position == test)
              return true;
          }
          else if (Expr.toBoolean(value))
            return true;
        }
      }

      return false;
    } finally {
      globalEnv.setPositionIndex(oldIndex);
      globalEnv.setMorePositions(oldMorePositions);
    }
  }

  /**
   * Creates a new node iterator.
   *
   * @param node the starting node
   * @param env the variable environment
   * @param match the axis match pattern
   *
   * @return the node iterator
   */
  public NodeIterator createNodeIterator(Node node, ExprEnvironment env,
                                         AbstractPattern match)
    throws XPathException
  {
    NodeIterator parentIter;
    parentIter = _parent.createNodeIterator(node, env, _parent.copyPosition());

    return new FilterIterator(parentIter, _expr, env, node);
  }

  public AbstractPattern copyPosition()
  {
    return null;
  }

  public String toString()
  {
    return (_parent.toString() + "[" + _expr + "]");
  }
}
