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

import com.caucho.util.CharBuffer;
import com.caucho.xml.XmlChar;
import com.caucho.xml.XmlUtil;
import com.caucho.xpath.expr.ObjectVar;
import com.caucho.xpath.expr.Var;
import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xpath.pattern.FromExpr;
import com.caucho.xpath.pattern.NodeArrayListIterator;
import com.caucho.xpath.pattern.NodeIterator;
import com.caucho.xpath.pattern.NodeListIterator;
import com.caucho.xpath.pattern.SingleNodeIterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Compute values from nodes.  Because the expressions themselves are
 * untyped, the class provides methods for creating the type of the
 * desired result.
 */
abstract public class Expr {
  protected static final int CONST = 0;

  protected static final int NODE_SET = CONST + 1;
  protected static final int ID = NODE_SET + 1;

  protected static final int OR = ID + 1;
  protected static final int AND = OR + 1;

  protected static final int EQ = AND + 1;
  protected static final int NEQ = EQ + 1;
  protected static final int LT = NEQ + 1;
  protected static final int LE = LT + 1;
  protected static final int GT = LE + 1;
  protected static final int GE = GT + 1;
  
  protected static final int BOOLEAN_EQ = GE + 1;
  protected static final int BOOLEAN_NEQ = BOOLEAN_EQ + 1;
  
  protected static final int NUMBER_EQ = BOOLEAN_NEQ + 1;
  protected static final int NUMBER_NEQ = NUMBER_EQ + 1;
  protected static final int NUMBER_LT = NUMBER_NEQ + 1;
  protected static final int NUMBER_LE = NUMBER_LT + 1;
  protected static final int NUMBER_GT = NUMBER_LE + 1;
  protected static final int NUMBER_GE = NUMBER_GT + 1;
  
  protected static final int STRING_EQ = NUMBER_GE + 1;
  protected static final int STRING_NEQ = STRING_EQ + 1;

  protected static final int NEG = STRING_NEQ + 1;
  protected static final int ADD = NEG + 1;
  protected static final int SUB = ADD + 1;
  protected static final int MUL = SUB + 1;
  protected static final int DIV = MUL + 1;
  protected static final int QUO = DIV + 1;
  protected static final int MOD = QUO + 1;

  protected static final int TRUE = MOD + 1;
  protected static final int FALSE = TRUE + 1;
  protected static final int NOT = FALSE + 1;
  protected static final int BOOLEAN = NOT + 1;
  protected static final int LANG = BOOLEAN + 1;

  protected static final int NUMBER = LANG + 1;
  protected static final int SUM = NUMBER + 1;
  protected static final int FLOOR = SUM + 1;
  protected static final int CEILING = FLOOR + 1;
  protected static final int ROUND = CEILING + 1;
  public static final int POSITION = ROUND + 1;
  protected static final int COUNT = POSITION + 1;
  protected static final int LAST = COUNT + 1;

  protected static final int STRING = LAST + 1;
  protected static final int CONCAT = STRING + 1;
  protected static final int STARTS_WITH = CONCAT + 1;
  protected static final int CONTAINS = STARTS_WITH + 1;
  protected static final int SUBSTRING = CONTAINS + 1;
  protected static final int SUBSTRING_BEFORE = SUBSTRING + 1;
  protected static final int SUBSTRING_AFTER = SUBSTRING_BEFORE + 1;
  protected static final int STRING_LENGTH = SUBSTRING_AFTER + 1;
  protected static final int NORMALIZE = STRING_LENGTH + 1;
  protected static final int TRANSLATE = NORMALIZE + 1;
  protected static final int FORMAT_NUMBER = TRANSLATE + 1;

  protected static final int LOCAL_PART = FORMAT_NUMBER + 1;
  protected static final int NAMESPACE = LOCAL_PART + 1;
  protected static final int QNAME = NAMESPACE + 1;
  protected static final int GENERATE_ID = QNAME + 1;

  protected static final int FUNCTION_AVAILABLE = GENERATE_ID + 1;
  protected static final int SYSTEM_PROPERTY = FUNCTION_AVAILABLE + 1;

  protected static final int IF = SYSTEM_PROPERTY + 1;
  
  protected static final int SELF = IF + 1;
  protected static final int SELF_NAME = SELF + 1;
  protected static final int ATTRIBUTE = SELF_NAME + 1;
  protected static final int ELEMENT = ATTRIBUTE + 1;
  
  protected static final int BASE_URI = ELEMENT + 1;
  protected static final int LAST_FUN = BASE_URI + 1;

  private AbstractPattern listContext;
  
  protected Expr() {}

  public void setListContext(AbstractPattern listContext)
  {
    this.listContext = listContext;
  }

  public AbstractPattern getListContext()
  {
    return listContext;
  }
  
  /**
   * true if the expression prefers to return a number.
   */
  public boolean isNumber()
  {
    return false;
  }
  
  /**
   * Evaluates the expression as a double using the node as a context.
   *
   * @param node the node to evaluate and use as a context
   *
   * @return the numeric value.
   */
  public double evalNumber(Node node)
    throws XPathException
  {
    Env env = XPath.createEnv();
    env.setCurrentNode(node);
    env.setContextNode(node);

    double result = evalNumber(node, env);

    XPath.freeEnv(env);

    return result;
  }

  /**
   * Evaluates the expression as a number.
   *
   * @param node the current node.
   * @param env variable environment.
   *
   * @return the numeric value.
   */
  public abstract double evalNumber(Node node, ExprEnvironment env)
    throws XPathException;

  /**
   * true if the expression prefers to return a boolean.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns the boolean value of the node.
   *
   * @param node the node to evaluate and use as a context
   *
   * @return the boolean value
   */
  public boolean evalBoolean(Node node)
    throws XPathException
  {
    Env env = XPath.createEnv();
    env.setCurrentNode(node);
    env.setContextNode(node);

    boolean result = evalBoolean(node, env);

    XPath.freeEnv(env);

    return result;
  }

  /**
   * Returns the boolean value of the node.
   *
   * @param node the node to evaluate and use as a context
   * @param env variable environment.
   *
   * @return the boolean value.
   */
  public abstract boolean evalBoolean(Node node, ExprEnvironment env)
    throws XPathException;

  /**
   * Returns the expression evaluated as a string.
   *
   * @param node the node to evaluate and use as a context
   *
   * @return the string value of the expression.
   */
  public String evalString(Node node)
    throws XPathException
  {
    Env env = XPath.createEnv();
    env.setCurrentNode(node);
    env.setContextNode(node);

    String result = evalString(node, env);

    XPath.freeEnv(env);

    return result;
  }

  /**
   * true if the expression prefers to return a string.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns the string value of the node.
   *
   * @param node the node to evaluate and use as a context
   * @param env variable environment.
   */
  public abstract String evalString(Node node, ExprEnvironment env)
    throws XPathException;
  
  /**
   * Fills a char buffer with the evaluated string results.
   *
   * @param cb the buffer containing the results.
   * @param node the node to evaluate and use as a context
   */
  public void evalString(CharBuffer cb, Node node)
    throws XPathException
  {
    Env env = XPath.createEnv();
    env.setCurrentNode(node);
    env.setContextNode(node);

    evalString(cb, node, env);

    XPath.freeEnv(env);
  }
  
  /**
   * Fills a char buffer with the evaluated string results.
   *
   * @param cb the buffer containing the results.
   * @param node the node to evaluate and use as a context
   * @param env the variable environment
   */
  public void evalString(CharBuffer cb, Node node, ExprEnvironment env)
    throws XPathException
  {
    cb.append(evalString(node, env));
  }

  /**
   * true if the expression prefers to return a node set.
   */
  public boolean isNodeSet()
  {
    return false;
  }

  /**
   * Returns an iterator of matching nodes
   *
   * @param node the node to evaluate and use as a context
   *
   * @return the value as a node iterator.
   */
  public NodeIterator evalNodeSet(Node node)
    throws XPathException
  {
    Env env = XPath.createEnv();
    env.setCurrentNode(node);
    env.setContextNode(node);

    NodeIterator result = evalNodeSet(node, env);

    XPath.freeEnv(env);

    return result;
  }

  /**
   * Returns an iterator of matching nodes
   *
   * @param node the node to evaluate and use as a context
   * @param env variable environment.
   *
   * @return the value as a node iterator.
   */
  public NodeIterator evalNodeSet(Node node, ExprEnvironment env)
    throws XPathException
  {
    Object obj = evalObject(node, env);

    if (obj instanceof Node)
      return new SingleNodeIterator(env, (Node) obj);

    else if (obj instanceof NodeList)
      return new NodeListIterator(env, (NodeList) obj);

    else if (obj instanceof NodeIterator)
      return (NodeIterator) obj;

    else if (obj instanceof ArrayList)
      return new NodeArrayListIterator(env, (ArrayList) obj);

    else {
      return new SingleNodeIterator(env, null);
    }
  }
  
  /**
   * Returns the object value of the node.
   *
   * @param node the node to evaluate and use as a context
   */
  public Object evalObject(Node node)
    throws XPathException
  {
    Env env = XPath.createEnv();
    env.setCurrentNode(node);
    env.setContextNode(node);

    Object result = evalObject(node, env);

    XPath.freeEnv(env);

    return result;
  }

  /**
   * Returns the object value of the node.
   *
   * @param node the node to evaluate and use as a context
   * @param env variable environment.
   */
  public abstract Object evalObject(Node node, ExprEnvironment env)
    throws XPathException;

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
    Object obj = evalObject(node, env);

    return new ObjectVar(obj);
  }

  /**
   * Adds a variable with the expression's value.
   */
  public void addVar(Env newEnv, String name, Node node, Env env)
    throws XPathException
  {
    Var var = evalVar(node, env);

    newEnv.addVar(name, var);
  }

  /**
   * Sets a variable with the expression's value.
   */
  public void setVar(String name, Node node, Env env)
    throws XPathException
  {
    env.setVar(name, evalVar(node, env));
  }

  /**
   * Adds a param with the expression's value.
   */
  public void addParam(Env newEnv, String name,
                       Node node, Env env)
    throws XPathException
  {
    Var var = env.getVar(name);
      
    if (var == null)
      newEnv.addVar(name, evalVar(node, env));
    else
      newEnv.addVar(name, var);
  }

  /**
   * Convert a Java object to a boolean using the XPath rules.
   */
  public static boolean toBoolean(Object value)
    throws XPathException
  {
    if (value instanceof Node)
      value = XmlUtil.textValue((Node) value);
    else if (value instanceof NodeList) {
      NodeList list = (NodeList) value;

      return list.item(0) != null;
    }
    else if (value instanceof ArrayList) {
      ArrayList list = (ArrayList) value;
      return list.size() > 0;
    }
    else if (value instanceof Iterator) {
      return ((Iterator) value).hasNext();
    }

    if (value == null)
      return false;

    else if (value instanceof Double) {
      Double d = (Double) value;

      return d.doubleValue() != 0;
    }
    else if (value instanceof Boolean) {
      Boolean b = (Boolean) value;
      return b.booleanValue();
    }
    else if (value instanceof String) {
      String string = (String) value;
      return string != null && string.length() > 0;
    }
    else
      return true;
  }

  /**
   * Convert a Java object to a double using the XPath rules.
   */
  public static double toDouble(Object value)
    throws XPathException
  {
    if (value instanceof Node) {
      String string = XmlUtil.textValue((Node) value);

      if (string == null)
        return 0;
      else
        return stringToNumber(string);
    }
    else if (value instanceof NodeList) {
      NodeList list = (NodeList) value;

      value = list.item(0);
    }
    else if (value instanceof ArrayList) {
      ArrayList list = (ArrayList) value;
      if (list.size() > 0)
        value = list.get(0);
      else
        value = null;
    }
    else if (value instanceof NodeIterator) {
      value = ((NodeIterator) value).nextNode();
    }

    if (value instanceof Node)
      value = XmlUtil.textValue((Node) value);

    if (value == null)
      return 0;

    if (value instanceof Number) {
      Number d = (Number) value;

      return d.doubleValue();
    }
    else if (value instanceof Boolean) {
      Boolean b = (Boolean) value;
      return b.booleanValue() ? 1 : 0;
    }
    else if (value instanceof String) {
      return stringToNumber((String) value);
    }
    else
      return 0;
  }

  /**
   * Convert a Java object to a string using the XPath rules.
   */
  public static String toString(Object value)
    throws XPathException
  {
    if (value instanceof Node) {
      String s = XmlUtil.textValue((Node) value);

      if (s == null)
        return "";
      else
        return s;
    }
    else if (value instanceof NodeList) {
      NodeList list = (NodeList) value;

      value = list.item(0);
    }
    else if (value instanceof ArrayList) {
      ArrayList list = (ArrayList) value;
      if (list.size() > 0)
        value = list.get(0);
      else
        value = null;
    }
    else if (value instanceof Iterator) {
      value = ((Iterator) value).next();
    }

    if (value instanceof Node)
      value = XmlUtil.textValue((Node) value);
    else if (value instanceof Double) {
      double d = ((Double) value).doubleValue();

      if ((int) d == d)
        return String.valueOf((int) d);
      else
        return String.valueOf(d);
    }

    if (value == null)
      return "";
    else
      return value.toString();
  }

  /**
   * Convert a Java object to a node using the XPath rules.
   */
  public static Node toNode(Object value)
    throws XPathException
  {
    if (value instanceof Node)
      return (Node) value;
    else if (value instanceof NodeList) {
      NodeList list = (NodeList) value;
      value = list.item(0);
    }
    else if (value instanceof ArrayList) {
      ArrayList list = (ArrayList) value;
      if (list.size() > 0)
        value = list.get(0);
      else
        value = null;
    }
    else if (value instanceof NodeIterator) {
      value = ((NodeIterator) value).nextNode();
    }

    if (value instanceof Node)
      return (Node) value;
    else
      return null;
  }

  /**
   * Convert a string to a double following XPath.
   *
   * @param string string to be treated as a double.
   * @return the double value.
   */
  static protected double stringToNumber(String string)
    throws XPathException
  {
    int i = 0;
    int length = string.length();
    boolean isNumber = false;
    for (; i < length && XmlChar.isWhitespace(string.charAt(i)); i++) {
    }

    if (i >= length)
      return 0;

    int ch = string.charAt(i);;
    int sign = 1;
    if (ch == '-') {
      sign = -1;
      for (i++; i < length && XmlChar.isWhitespace(string.charAt(i)); i++) {
      }
    }
      
    double value = 0;
    double exp = 1;
    for (; i < length && (ch = string.charAt(i)) >= '0' && ch <= '9'; i++) {
      value = 10 * value + ch - '0';
      isNumber = true;
    }

    if (ch == '.') {
      for (i++;
           i < length && (ch = string.charAt(i)) >= '0' && ch <= '9';
           i++) {
        value = 10 * value + ch - '0';
        isNumber = true;
        exp = 10 * exp;
      }
    }

    double pexp = 1.0;
    if (ch == 'e' || ch == 'E') {
      int eSign = 1;
      i++;

      if (i >= length)
        return Double.NaN;

      if (string.charAt(i) == '-') {
        eSign = -1;
        i++;
      }
      else if (string.charAt(i) == '+') {
        i++;
      }

      int v = 0;
      for (; i < length && (ch = string.charAt(i)) >= '0' && ch <= '9'; i++) {
        v = v * 10 + ch - '0';
      }

      pexp = Math.pow(10, eSign * v);
    }

    for (; i < length && XmlChar.isWhitespace(string.charAt(i)); i++) {
    }

    if (i < length || ! isNumber)
      return Double.NaN;
    else
      return sign * value * pexp / exp;
  }

  /**
   * Convert from an expression to a pattern.
   */
  protected AbstractPattern toNodeList()
  {
    return new FromExpr(null, this);
  }
}
