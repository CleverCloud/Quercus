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
import com.caucho.util.L10N;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Implements the object java extension functions.
 */
public class ObjectJavaExpr extends Expr {
  private static L10N L = new L10N(ObjectJavaExpr.class);
  
  private static final int J_BOOLEAN = 1;
  private static final int J_BYTE = J_BOOLEAN + 1;
  private static final int J_SHORT = J_BYTE + 1;
  private static final int J_INT = J_SHORT + 1;
  private static final int J_LONG = J_INT + 1;
  private static final int J_FLOAT = J_LONG + 1;
  private static final int J_DOUBLE = J_FLOAT + 1;
  private static final int J_STRING = J_DOUBLE + 1;
  private static final int J_OBJECT = J_STRING + 1;
  
  // Code of the expression defined in Expr
  private Method method;

  private Expr objArg;
  private ArrayList args;
  private int []argTypes;

  private int retType;

  /**
   * Create a StringExpression with three arguments.
   *
   * @param method Java method
   * @param args the arguments
   */
  public ObjectJavaExpr(Method method, Expr objArg, ArrayList args)
  {
    this.method = method;
    this.objArg = objArg;
    this.args = args;

    argTypes = new int[args.size()];
    Class []paramClasses = method.getParameterTypes();
    for (int i = 0; i < paramClasses.length; i++)
      argTypes[i] = classToType(paramClasses[i]);
    
    retType = classToType(method.getReturnType());
  }

  private int classToType(Class cl)
  {
    if (boolean.class.equals(cl) || Boolean.class.equals(cl))
      return J_BOOLEAN;
    else if (byte.class.equals(cl) || Byte.class.equals(cl))
      return J_BYTE;
    else if (short.class.equals(cl) || Short.class.equals(cl))
      return J_SHORT;
    else if (int.class.equals(cl) || Integer.class.equals(cl))
      return J_INT;
    else if (long.class.equals(cl) || Long.class.equals(cl))
      return J_LONG;
    else if (float.class.equals(cl) || Float.class.equals(cl))
      return J_FLOAT;
    else if (double.class.equals(cl) || Double.class.equals(cl))
      return J_DOUBLE;
    else if (String.class.equals(cl))
      return J_STRING;
    else
      return J_OBJECT;
  }

  /**
   * True if it returns a string.
   */
  public boolean isString()
  {
    return retType == J_STRING;
  }

  /**
   * True if this returns a boolean.
   */
  public boolean isBoolean()
  {
    return retType == J_BOOLEAN;
  }

  /**
   * True if this returns a boolean.
   */
  public boolean isNumber()
  {
    return retType >= J_BYTE && retType <= J_DOUBLE;
  }

  /**
   * Evaluates the expression as an string.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return the string representation of the expression.
   */
  public String evalString(Node node, ExprEnvironment env)
    throws XPathException
  {
    Object value = evalObject(node, env);

    return String.valueOf(value);
  }

  /**
   * Evaluate the expression as a boolean, i.e. evaluate it as a string
   * and then convert it to a boolean.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return the boolean representation of the expression.
   */
  public boolean evalBoolean(Node node, ExprEnvironment env)
    throws XPathException
  {
    return toBoolean(evalObject(node, env));
  }

  /**
   * Evaluate the expression as a double, i.e. evaluate it as a string
   * and then convert it to a double.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return the numeric representation of the expression.
   */
  public double evalNumber(Node node, ExprEnvironment env)
    throws XPathException
  {
    return toDouble(evalObject(node, env));
  }

  /**
   * Evaluate the expression as an object, i.e. return the string value.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return the boolean representation of the expression.
   */
  public Object evalObject(Node node, ExprEnvironment env)
    throws XPathException
  {
    Object []argArray = new Object[args.size()];

    Object obj = objArg.evalObject(node, env);

    if (obj == null ||
        ! (method.getDeclaringClass().isAssignableFrom(obj.getClass())))
      throw new XPathException(L.l("Can't call method `{0}' on {1}.",
                                   method.getName(), obj));

    for (int i = 0; i < argArray.length; i++) {
      Expr expr = (Expr) args.get(i);

      switch (argTypes[i]) {
      case J_BOOLEAN:
        argArray[i] = new Boolean(expr.evalBoolean(node, env));
        break;
      case J_BYTE:
        argArray[i] = new Byte((byte) expr.evalNumber(node, env));
        break;
      case J_SHORT:
        argArray[i] = new Short((short) expr.evalNumber(node, env));
        break;
      case J_INT:
        argArray[i] = new Integer((int) expr.evalNumber(node, env));
        break;
      case J_LONG:
        argArray[i] = new Long((long) expr.evalNumber(node, env));
        break;
      case J_FLOAT:
        argArray[i] = new Float((float) expr.evalNumber(node, env));
        break;
      case J_DOUBLE:
        argArray[i] = new Double(expr.evalNumber(node, env));
        break;
      case J_STRING:
        argArray[i] = expr.evalString(node, env);
        break;
      default:
        argArray[i] = expr.evalObject(node, env);
        break;
      }
    }

    try {
      return method.invoke(obj, argArray);
    } catch (Exception e) {
      throw new XPathException(e);
    }
  }

  /**
   * Return the expression as a string.  toString() returns a valid
   * XPath expression.  This lets applications like XSLT use toString()
   * to print the string in the generated Java.
   */
  public String toString()
  {
    CharBuffer cb = CharBuffer.allocate();
    cb.append("java:");
    cb.append(method.getDeclaringClass().getName());
    cb.append(".");
    cb.append(method.getName());

    cb.append("(");
    cb.append(objArg);
    for (int i = 0; i < args.size(); i++) {
      cb.append(",");
      cb.append(args.get(i));
    }
    cb.append(")");

    return cb.close();
  }
}
