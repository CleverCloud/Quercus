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
import com.caucho.xpath.Env;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.pattern.AbstractPattern;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Iterator;

public class NumericExpr extends Expr {
  private int _code;
  private Expr _left;
  private Expr _right;
  private double _value;
  private ArrayList<Expr> _args;
  private AbstractPattern _axis;
  private AbstractPattern _pattern;

  public NumericExpr(int code, Expr left, Expr right)
  {
    _code = code;
    _left = left;
    _right = right;
  }

  public NumericExpr(int code, Expr expr)
  {
    _code = code;
    _left = expr;
  }

  public NumericExpr(double value)
  {
    _code = CONST;
    _value = value;
  }

  public NumericExpr(int code, ArrayList<Expr> args)
  {
    _code = code;
    _args = args;

    if (args.size() > 0)
      _left = args.get(0);
    if (args.size() > 1)
      _right = args.get(1);
  }

  public NumericExpr(int code, AbstractPattern axis, AbstractPattern pattern)
  {
    _code = code;
    _axis = axis;
    _pattern = pattern;
  }

  public NumericExpr(int code, AbstractPattern listPattern)
  {
    _code = code;

    if ((code == POSITION || code == LAST) && listPattern != null) {
      _axis = listPattern.copyAxis();
      _pattern = listPattern.copyPosition();
    }
    else
      _pattern = listPattern;
  }

  public AbstractPattern getListContext()
  {
    switch (_code) {
    case POSITION:
    case LAST:
      return _pattern;
      
    default:
      return null;
    }
  }

  public boolean isNumber()
  {
    return true;
  }

  /**
   * Returns true of the expression is constant.
   */
  public boolean isConstant()
  {
    return _code == CONST;
  }

  /**
   * Returns the expression's value.
   */
  public double getValue()
  {
    return _value;
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
  public double evalNumber(Node node, ExprEnvironment env)
    throws XPathException
  {
    switch (_code) {
    case CONST:
      return _value;

    case NEG: 
      return - _left.evalNumber(node, env);

    case ADD: 
      return (_left.evalNumber(node, env) + _right.evalNumber(node, env));

    case SUB: 
      return (_left.evalNumber(node, env) - _right.evalNumber(node, env));

    case MUL: 
      return (_left.evalNumber(node, env) * _right.evalNumber(node, env));

    case DIV: 
      return (_left.evalNumber(node, env) / _right.evalNumber(node, env));

    case QUO: 
      return (int) (_left.evalNumber(node, env) /
                    _right.evalNumber(node, env));

    case MOD:
      return (_left.evalNumber(node, env) % _right.evalNumber(node, env));

    case NUMBER:
      if (_left != null)
        return _left.evalNumber(node, env);
      else
        return toDouble(node);

    case FLOOR:
      return Math.floor(_left.evalNumber(node, env));

    case CEILING:
      return Math.ceil(_left.evalNumber(node, env));

    case ROUND:
      return Math.rint(_left.evalNumber(node, env));

    case SUM:
      return sum(node, env);

    case POSITION:
      return position(node, env);

    case LAST:
      return last(node, env);

    case COUNT:
      return count(node, env);

    case STRING_LENGTH:
      String str = _left.evalString(node, env);
      if (str == null)
        return 0;
      else
        return str.length();

    default:
      throw new RuntimeException("unknown code: " + (char) _code);
    }
  }

  /**
   * Calculates the position of the node.  For select patterns, the
   * position will be given in the env variable.
   */
  private int position(Node node, ExprEnvironment env)
    throws XPathException
  {
    int position = env.getContextPosition();

    if (position > 0)
      return position;

    if (_axis == null || ! (env instanceof Env))
      throw new RuntimeException("position called with no context");
    else if (_pattern == null)
      return _axis.position(node, (Env) env, null);
    else
      return _axis.position(node, (Env) env, _pattern.copyPosition());
  }

  private int last(Node node, ExprEnvironment env)
    throws XPathException
  {
    int size = env.getContextSize();

    if (size > 0)
      return size;

    if (_axis == null || ! (env instanceof Env)) {
      throw new RuntimeException("last called with no context");
    }
    else if (_pattern == null)
      return _axis.position(node, (Env) env, null);
    else
      return _axis.count(node, (Env) env, _pattern.copyPosition());
  }

  /**
   * Counts the nodes in a subpattern.
   */
  private int count(Node node, ExprEnvironment env)
    throws XPathException
  {
    int count = 0;

    Iterator iter = _pattern.selectUnique(node, env);
    while (iter.hasNext()) {
      iter.next();
      count++;
    }

    return count;
  }

  /**
   * Returns the sum of all the node values.
   *
   * @param node the current node
   * @param env the variable environment
   */
  private double sum(Node node, ExprEnvironment env)
    throws XPathException
  {
    double sum = 0;

    Iterator iter = _pattern.selectUnique(node, env);
    while (iter.hasNext()) {
      Node subnode = (Node) iter.next();
      String textValue = XmlUtil.textValue(subnode);

      sum += stringToNumber(textValue);
    }

    return sum;
  }

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
  
  public String toString()
  {
    switch (_code) {
    case CONST: return String.valueOf(_value);
    case NEG: return "-" + _left;
    case ADD: return "(" + _left + " + " + _right + ")";
    case SUB: return "(" + _left + " - " + _right + ")";
    case MUL: return "(" + _left + " * " + _right + ")";
    case DIV: return "(" + _left + " div " + _right + ")";
    case QUO: return "(" + _left + " quo " + _right + ")";
    case MOD: return "(" + _left + " mod " + _right + ")";

    case NUMBER: return "number(" + _left + ")";
    case SUM: return "sum(" + _pattern + ")";
    case FLOOR: return "floor(" + _left + ")";
    case CEILING: return "ceiling(" + _left + ")";
    case ROUND: return "round(" + _left + ")";

    case POSITION: return "position()";
    case COUNT: return "count(" + _pattern + ")";
    case LAST: return "last()";
      
    case STRING_LENGTH:
      return "string-length(" + _left + ")";

    default: return super.toString();
    }
  }
}
