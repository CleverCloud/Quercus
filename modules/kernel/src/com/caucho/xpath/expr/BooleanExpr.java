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

import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.pattern.NodeIterator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Iterator;

public class BooleanExpr extends Expr {
  private int _code;
  private Expr _left;
  private Expr _right;
  private boolean _value;
  private ArrayList _args;

  public BooleanExpr(int code, Expr left, Expr right)
  {
    _code = code;
    _left = left;
    _right = right;

    if (code == Expr.EQ) {
      if (_left.isNodeSet() || _right.isNodeSet())
        _code = Expr.EQ;
      else if (_left.isBoolean() || _right.isBoolean())
        _code = Expr.BOOLEAN_EQ;
      else if (left.isNumber() || right.isNumber())
        _code = Expr.NUMBER_EQ;
      else if (left.isString() && right.isString())
        _code = Expr.STRING_EQ;
      else
        _code = Expr.EQ;
    }
    else if (code == Expr.NEQ) {
      if (left.isNodeSet() || right.isNodeSet())
        _code = Expr.NEQ;
      else if (left.isBoolean() || right.isBoolean())
        _code = Expr.BOOLEAN_NEQ;
      else if (left.isNumber() || right.isNumber())
        _code = Expr.NUMBER_NEQ;
      else if (left.isString() && right.isString())
        _code = Expr.STRING_NEQ;
      else
        _code = Expr.NEQ;
    }
    else if (code == Expr.LT) {
      if (left.isNodeSet() || right.isNodeSet())
        _code = Expr.LT;
      else if (left.isNumber() || right.isNumber())
        _code = Expr.NUMBER_LT;
      else
        _code = Expr.LT;
    }
    else if (code == Expr.LE) {
      if (left.isNodeSet() || right.isNodeSet())
        _code = Expr.LE;
      else if (left.isNumber() || right.isNumber())
        _code = Expr.NUMBER_LE;
      else
        _code = Expr.LE;
    }
    else if (code == Expr.GT) {
      if (left.isNodeSet() || right.isNodeSet())
        _code = Expr.GT;
      else if (left.isNumber() || right.isNumber())
        _code = Expr.NUMBER_GT;
      else
        _code = Expr.GT;
    }
    else if (code == Expr.GE) {
      if (left.isNodeSet() || right.isNodeSet())
        _code = Expr.GE;
      else if (left.isNumber() || right.isNumber())
        _code = Expr.NUMBER_GE;
      else
        _code = Expr.GE;
    }
  }

  public BooleanExpr(int code, Expr expr)
  {
    _code = code;
    _left = expr;
  }

  public BooleanExpr(boolean value)
  {
    _code = CONST;
    _value = value;
  }

  public BooleanExpr(int code, ArrayList args)
  {
    _code = code;
    _args = args;

    if (args.size() > 0)
      _left = (Expr) args.get(0);
    if (args.size() > 1)
      _right = (Expr) args.get(1);
  }

  public boolean isBoolean() { return true; }

  /**
   * Evaluates the expression as a boolean.
   *
   * @param node current node
   * @param env the environment
   *
   * @return the boolean representation
   */
  public boolean evalBoolean(Node node, ExprEnvironment env)
    throws XPathException
  {
    switch (_code) {
    case CONST:
      return _value;

    case BOOLEAN_EQ: 
      return (_left.evalBoolean(node, env) == _right.evalBoolean(node, env));

    case NUMBER_EQ:
      return (_left.evalNumber(node, env) == _right.evalNumber(node, env));

    case STRING_EQ: 
      String lstr = _left.evalString(node, env);
      String rstr = _right.evalString(node, env);

      return lstr.equals(rstr);

    case EQ: 
      Object lobj = _left.evalObject(node, env);
      Object robj = _right.evalObject(node, env);

      if (lobj == robj)
        return true;

      return cmp(P_EQ, lobj, robj);

    case BOOLEAN_NEQ: 
      return (_left.evalBoolean(node, env) != _right.evalBoolean(node, env));

    case NUMBER_NEQ: 
      return (_left.evalNumber(node, env) != _right.evalNumber(node, env));

    case STRING_NEQ: 
      lstr = _left.evalString(node, env);
      rstr = _right.evalString(node, env);
      return ! lstr.equals(rstr);

    case NEQ: 
      lobj = _left.evalObject(node, env);
      robj = _right.evalObject(node, env);

      if (lobj == robj)
        return false;

      return cmp(P_NEQ, lobj, robj);

    case LT: 
      return cmp(P_LT,
                 _left.evalObject(node, env),
                 _right.evalObject(node, env));

    case LE: 
      return cmp(P_LE,
                 _left.evalObject(node, env),
                 _right.evalObject(node, env));

    case GT:
      return cmp(P_GT,
                 _left.evalObject(node, env),
                 _right.evalObject(node, env));

    case GE:
      return cmp(P_GE,
                 _left.evalObject(node, env),
                 _right.evalObject(node, env));
      
    case NUMBER_LT: 
      return (_left.evalNumber(node, env) < _right.evalNumber(node, env));

    case NUMBER_LE: 
      return (_left.evalNumber(node, env) <= _right.evalNumber(node, env));

    case NUMBER_GT: 
      return (_left.evalNumber(node, env) > _right.evalNumber(node, env));

    case NUMBER_GE: 
      return (_left.evalNumber(node, env) >= _right.evalNumber(node, env));

    case OR:
      return (_left.evalBoolean(node, env) || _right.evalBoolean(node, env));

    case AND:
      return (_left.evalBoolean(node, env) && _right.evalBoolean(node, env));

    case TRUE:
      return true;

    case FALSE:
      return false;

    case NOT:
      return ! _left.evalBoolean(node, env);

    case BOOLEAN:
      return _left.evalBoolean(node, env);

    case STARTS_WITH:
      lstr = _left.evalString(node, env);
      rstr = _right.evalString(node, env);
      return lstr.startsWith(rstr);

    case CONTAINS:
      lstr = _left.evalString(node, env);
      rstr = _right.evalString(node, env);
      return lstr.indexOf(rstr) >= 0;

    case LANG:
      lstr = _left.evalString(node, env);
      for (; node != null; node = node.getParentNode()) {
        if (! (node instanceof Element))
          continue;
        String lang = ((Element) node).getAttribute("xml:lang");
        if (lang != null && lang.equals(lstr))
          return true;
      }
      return false;

    case FUNCTION_AVAILABLE:
      return false;

    default:
      throw new RuntimeException("unknown code: " + _code);
    }
  }

  private boolean cmp(Predicate test, Object lobj, Object robj)
    throws XPathException
  {
    if (lobj instanceof Node) {
    }
    else if (lobj instanceof NodeList) {
      NodeList list = (NodeList) lobj;

      int length = list.getLength();

      for (int i = 0; i < length; i++) {
        if (cmp(test, list.item(i), robj))
          return true;
      }
      
      return false;
    }
    else if (lobj instanceof ArrayList) {
      ArrayList list = (ArrayList) lobj;

      for (int i = 0; i < list.size(); i++) {
        if (cmp(test, list.get(i), robj))
          return true;
      }
      
      return false;
    }
    else if (lobj instanceof Iterator) {
      Iterator iter = (Iterator) lobj;
      
      while (iter.hasNext()) {
        if (cmp(test, iter.next(), robj))
          return true;
      }
      
      return false;
    }

    if (robj instanceof Node) {
    }
    else if (robj instanceof NodeList) {
      NodeList list = (NodeList) robj;

      int length = list.getLength();
      for (int i = 0; i < length; i++) {
        if (cmp(test, lobj, list.item(i)))
          return true;
      }
      
      return false;
    }
    else if (robj instanceof ArrayList) {
      ArrayList list = (ArrayList) robj;

      for (int i = 0; i < list.size(); i++) {
        if (cmp(test, lobj, list.get(i)))
          return true;
      }
      
      return false;
    }
    else if (robj instanceof NodeIterator) {
      Iterator iter = null;

      iter = (Iterator) ((NodeIterator) robj).clone();

      while (iter.hasNext()) {
        if (cmp(test, lobj, iter.next()))
          return true;
      }
      return false;
    }

    return test.test(lobj, robj);
  }

  /**
   * Evaluates the expression as a number.
   *
   * @param node current node
   * @param env the environment
   *
   * @return the numeric representation
   */
  public double evalNumber(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (evalBoolean(node, env))
      return 1.0;
    else
      return 0.0;
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param node current node
   * @param env the environment
   *
   * @return the string representation
   */
  public String evalString(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (evalBoolean(node, env))
      return "true";
    else
      return "false";
  }

  /**
   * Evaluates the expression as a object.
   *
   * @param node current node
   * @param env the environment
   *
   * @return the object representation
   */
  public Object evalObject(Node node, ExprEnvironment env)
    throws XPathException
  {
    return new Boolean(evalBoolean(node, env));
  }

  public String toString()
  {
    switch (_code) {
    case CONST:
      return String.valueOf(_value);
      
    case BOOLEAN_EQ:
    case NUMBER_EQ:
    case STRING_EQ:
    case EQ:
      return "(" + _left.toString() + " = " + _right.toString() + ")";
      
    case BOOLEAN_NEQ:
    case NUMBER_NEQ:
    case STRING_NEQ:
    case NEQ:
      return "(" + _left.toString() + " != " + _right.toString() + ")";
      
    case LT:
    case NUMBER_LT:
      return "(" + _left.toString() + " < " + _right.toString() + ")";
      
    case LE:
    case NUMBER_LE:
      return "(" + _left.toString() + " <= " + _right.toString() + ")";
      
    case GT:
    case NUMBER_GT:
      return "(" + _left.toString() + " > " + _right.toString() + ")";
      
    case GE:
    case NUMBER_GE:
      return "(" + _left.toString() + " >= " + _right.toString() + ")";

    case OR:
      return "(" + _left.toString() + " or " + _right.toString() + ")";
      
    case AND:
      return "(" + _left.toString() + " and " + _right.toString() + ")";

    case TRUE:
      return "true()";
      
    case FALSE:
      return "false()";

    case NOT:
      return "not(" + _left.toString() + ")";
      
    case BOOLEAN:
      return "boolean(" + _left.toString() + ")";

    case STARTS_WITH:
      return "starts-with(" + _left + ", " + _right + ")"; 

    case CONTAINS:
      return "contains(" + _left + ", " + _right + ")"; 

    case LANG:
      return "lang(" + _left + ")";

    case FUNCTION_AVAILABLE:
      return "function-available(" + _left + ")";

    default: return super.toString();
    }
  }

  abstract static class Predicate {
    abstract public boolean test(Object l, Object r)
      throws XPathException;
  }

  final static Predicate P_EQ = new Predicate() {
      public boolean test(Object lobj, Object robj)
        throws XPathException
      {
        if (lobj instanceof Boolean || robj instanceof Boolean)
          return toBoolean(lobj) == toBoolean(robj);
        else if (lobj instanceof Double || robj instanceof Double)
          return toDouble(lobj) == toDouble(robj);
        else
          return BooleanExpr.toString(lobj).equals(BooleanExpr.toString(robj));
      }
    };

  final static Predicate P_NEQ = new Predicate() {
      public boolean test(Object lobj, Object robj)
        throws XPathException
      {
        if (lobj instanceof Boolean || robj instanceof Boolean)
          return toBoolean(lobj) != toBoolean(robj);
        else if (lobj instanceof Double || robj instanceof Double)
          return toDouble(lobj) != toDouble(robj);
        else
          return ! BooleanExpr.toString(lobj).equals(BooleanExpr.toString(robj));
      }
    };

  final static Predicate P_LT = new Predicate() {
      public boolean test(Object lobj, Object robj)
        throws XPathException
      {
        return toDouble(lobj) < toDouble(robj);
      }
    };

  final static Predicate P_LE = new Predicate() {
      public boolean test(Object lobj, Object robj)
        throws XPathException
      {
        return toDouble(lobj) <= toDouble(robj);
      }
    };

  final static Predicate P_GT = new Predicate() {
      public boolean test(Object lobj, Object robj)
        throws XPathException
      {
        return toDouble(lobj) > toDouble(robj);
      }
    };

  final static Predicate P_GE = new Predicate() {
      public boolean test(Object lobj, Object robj)
        throws XPathException
      {
        return toDouble(lobj) >= toDouble(robj);
      }
    };
}
