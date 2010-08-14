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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Implements the builtin XPath string expressions.
 */
public class StringExpr extends Expr {
  // Code of the expression defined in Expr
  private int _code;
  // First argument
  private Expr _left;
  // Second argument
  private Expr _right;
  // Third
  private Expr _third;
  private String _value;
  // Arguments for more than 3.
  private ArrayList _args;

  /**
   * Create a StringExpression with three arguments.
   *
   * @param code the Expr code of the expression.
   * @param left the first argument.
   * @param right the second argument.
   * @param third the third argument.
   */
  public StringExpr(int code, Expr left, Expr right, Expr third)
  {
    _code = code;
    _left = left;
    _right = right;
    _third = third;
  }

  public StringExpr(int code, Expr left, Expr right)
  {
    _code = code;
    _left = left;
    _right = right;
  }

  public StringExpr(int code, Expr expr)
  {
    _code = code;
    _left = expr;
  }

  public StringExpr(String value)
  {
    _code = CONST;
    _value = value;
  }

  /**
   * Creates a string expression from a list of arguments.
   *
   * @param code Expr code for the function.
   * @param args array list of the arguments.
   */
  public StringExpr(int code, ArrayList args)
  {
    _code = code;
    _args = args;

    if (args.size() > 0)
      _left = (Expr) args.get(0);
    if (args.size() > 1)
      _right = (Expr) args.get(1);
    if (args.size() > 2)
      _third = (Expr) args.get(2);
  }

  /**
   * The StringExpr returns a string value.
   */
  public boolean isString()
  {
    return true;
  }

  public String getValue()
  {
    return _value;
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
    switch (_code) {
    case CONST:
      return _value;

    case STRING:
      return _left.evalString(node, env);

    case CONCAT: 
      CharBuffer cb = CharBuffer.allocate();
      for (int i = 0; i < _args.size(); i++)
        ((Expr) _args.get(i)).evalString(cb, node, env);
      return cb.close();

    case SUBSTRING_BEFORE:
      String lstr = _left.evalString(node, env);
      String rstr = _right.evalString(node, env);
      int index = lstr.indexOf(rstr);
      return index > 0 ? lstr.substring(0, index) : "";

    case SUBSTRING_AFTER:
      lstr = _left.evalString(node, env);
      rstr = _right.evalString(node, env);
      index = lstr.indexOf(rstr);
      return index >= 0 ? lstr.substring(index + rstr.length()) : "";

    case NORMALIZE:
      lstr = _left.evalString(node, env);
      return normalize(lstr);

    case TRANSLATE:
      lstr = _left.evalString(node, env);
      rstr = _right.evalString(node, env);
      String tstr = _third.evalString(node, env);
      return translate(lstr, rstr, tstr);

    case FORMAT_NUMBER:
      return _left.evalString(node, env);

    case LOCAL_PART:
      Object lobj = _left.evalObject(node, env);
      Node nodeValue = toNode(lobj);
      if (nodeValue != null)
        return nodeValue.getLocalName();
      else
        return "";

    case NAMESPACE:
      lobj = _left.evalObject(node, env);
      nodeValue = toNode(lobj);
      if (nodeValue != null) {
        String uri = nodeValue.getNamespaceURI();
        return uri != null ? uri : "";
      }
      else
        return "";

    case QNAME:
      lobj = _left.evalObject(node, env);
      nodeValue = toNode(lobj);
      if (nodeValue != null)
        return nodeValue.getNodeName();
      else
        return "";

    case GENERATE_ID:
      Iterator iter = _left.evalNodeSet(node, env);
      return "G" + String.valueOf(System.identityHashCode(iter.next()));

    case SYSTEM_PROPERTY:
      lstr = _left.evalString(node, env);
      if (lstr == null)
        return "";
      else if (lstr.equals("xsl:version"))
        return "1.0";
      else
        return "";

    case SUBSTRING:
      lstr = _left.evalString(node, env);
      if (lstr == null)
        lstr = "";
      double start = _right.evalNumber(node, env) - 1;
      
      double end = lstr.length();
      if (_third != null)
        end = Math.round(start) + _third.evalNumber(node, env);

      if (Double.isNaN(end) || Double.isNaN(start)) {
        start = 0;
        end = 0;
      }

      if (start < 0)
        start = 0;
      else if (lstr.length() < start)
        start = lstr.length();
      
      if (end < 0)
        end = 0;
      else if (end < start)
        end = start;
      else if (lstr.length() < end)
        end = lstr.length();

      return lstr.substring((int) (start + 0.5), (int) (end + 0.5));

    default:
      throw new RuntimeException("unknown code: " + _code);
    }
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
  private String normalize(String string)
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

  /**
   * Translate the string, converting characters.  translate("foo", "f", "b")
   * returns "boo".
   *
   * @param string the string to translate.
   * @param from characters to convert from.
   * @param to the replacement characters.
   */
  private String translate(String string, String from, String to)
  {
    CharBuffer result = new CharBuffer();

  loop:
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);

      for (int j = 0; j < from.length(); j++) {
        if (ch == from.charAt(j)) {
          if (to.length() > j)
            result.append(to.charAt(j));
          continue loop;
        }
      }

      result.append(ch);
    }

    return result.toString();
  }

  /**
   * Return the expression as a string.  toString() returns a valid
   * XPath expression.  This lets applications like XSLT use toString()
   * to print the string in the generated Java.
   */
  public String toString()
  {
    switch (_code) {
    case CONST:
      CharBuffer cb = CharBuffer.allocate();
      cb.append("'");
      for (int i = 0; i < _value.length(); i++) {
        char ch = _value.charAt(i);
        switch (ch) {
        case '\n':
          cb.append("\\n");
          break;
        case '\r':
          cb.append("\\r");
          break;
        case '\\':
          cb.append("\\\\");
          break;
        case '\'':
          cb.append("\\'\\'");
          break;
        case '"':
          cb.append("\\\"");
          break;
        default:
          cb.append(ch);
        }
      }
      cb.append("'");
      return cb.toString();
      
    case STRING: return "string(" + _left + ")";

    case CONCAT: 
      String result = "concat(";
      for (int i = 0; i < _args.size(); i++) {
        if (i > 0)
          result = result + ", ";
        result = result + _args.get(i);
      }
      return result + ")";

    case SUBSTRING_BEFORE:
      return "substring-before(" + _left + ", " + _right + ")"; 

    case SUBSTRING_AFTER:
      return "substring-after(" + _left + ", " + _right + ")"; 

    case NORMALIZE:
      return "normalize-space(" + _left + ")";

    case TRANSLATE:
      return "translate(" + _left + ", " + _right + ", " + _third + ")";

    case FORMAT_NUMBER:
      return "format-number(" + _left + ")";

    case LOCAL_PART:
      return "local-part(" + _left + ")";

    case NAMESPACE:
      return "namespace-uri(" + _left + ")";

    case QNAME:
      return "name(" + _left + ")";

    case GENERATE_ID:
      return "generate-id(" + _left + ")";

    case SYSTEM_PROPERTY:
      return "system-property(" + _left + ")";

    case SUBSTRING:
      return "substring(" + _left + "," + _right + 
        (_third == null ? "" : ("," + _third)) + ")";

    case BASE_URI:
      return "fn:base-uri(" + _left + ")";

    default:
      return super.toString();
    }
  }
}
