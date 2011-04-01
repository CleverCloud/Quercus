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
import com.caucho.xml.XmlChar;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Implements the builtin XPath string expressions.
 */
abstract public class AbstractStringExpr extends Expr {
  /**
   * The StringExpr returns a string value.
   */
  public boolean isString()
  {
    return true;
  }

  /**
   * Evaluates the expression as an string.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return the string representation of the expression.
   */
  abstract public String evalString(Node node, ExprEnvironment env)
    throws XPathException;

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
    String string = evalString(node, env);

    return string != null && string.length() > 0;
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
    return stringToNumber(evalString(node, env));
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
    return evalString(node, env);
  }

  /**
   * Normalize the string, converting all whitespace to a space and
   * eliminating consecutive spaces.
   */
  protected static String normalize(String string)
  {
    CharBuffer result = new CharBuffer();

    int i = 0;
    int len = string.length();
    for (; i < len && XmlChar.isWhitespace(string.charAt(i)); i++) {
    }

    boolean lastIsWhitespace = false;
    for (; i < len; i++) {
      if (XmlChar.isWhitespace(string.charAt(i))) {
        lastIsWhitespace = true;
      }
      else if (lastIsWhitespace) {
        result.append(' ');
        result.append(string.charAt(i));
        lastIsWhitespace = false;
      }
      else
        result.append(string.charAt(i));
    }

    return result.toString();
  }
}
