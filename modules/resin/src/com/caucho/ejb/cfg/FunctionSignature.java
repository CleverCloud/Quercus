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

package com.caucho.ejb.cfg;

import com.caucho.config.ConfigException;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * A builtin SQL function expression
 */
public class FunctionSignature {
  static L10N L = new L10N(FunctionSignature.class);
  
  private String _signature;
  // function name
  private String _name;
  // arguments
  private Class []_parameterTypes;
  // return type
  private Class _returnType;

  private int _index;

  private String _sql;

  /**
   * Creates a function definition.
   *
   * @param signature the function signature in java syntax
   */
  public FunctionSignature(String signature)
    throws ConfigException
  {
    _signature = signature;

    parseSignature();
  }

  /**
   * Returns the function name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the function signature.
   */
  public String getSignature()
  {
    return _signature;
  }

  /**
   * Returns the function arguments.
   */
  public Class []getParameterTypes()
  {
    return _parameterTypes;
  }

  /**
   * Returns the return type;
   */
  public Class getReturnType()
  {
    return _returnType;
  }

  /**
   * Sets the SQL.
   */
  public void setSQL(String sql)
  {
    _sql = sql;
  }

  /**
   * Gets the SQL.
   */
  public String getSQL()
  {
    return _sql;
  }

  /**
   * Parses the function signature.
   */
  private void parseSignature()
    throws ConfigException
  {
    _index = 0;

    _returnType = parseType(skipWhitespace(read()));

    CharBuffer cb = CharBuffer.allocate();
    int ch = skipWhitespace(read());

    for (; Character.isJavaIdentifierPart((char) ch); ch = read())
      cb.append((char) ch);

    if (cb.length() == 0)
      throw new ConfigException(L.l("unexpected empty function name in '{0}'",
                                        _signature));

    _name = cb.toString();

    ch = skipWhitespace(ch);

    if (ch != '(')
      throw new ConfigException(L.l("function syntax is 'ret-type name(arg1, ..., argn)' in '{0}'",
                                        _signature));

    ArrayList<Class> argList = new ArrayList<Class>();
    
    ch = read();
    while (Character.isJavaIdentifierStart((char) (ch = skipWhitespace(ch)))) {
      Class type = parseType(ch);

      argList.add(type);

      ch = skipWhitespace(read());

      if (ch == ',')
        ch = read();
    }

    _parameterTypes = argList.toArray(new Class[argList.size()]);

    if (ch != ')')
      throw new ConfigException(L.l("function syntax is 'ret-type name(arg1, ..., argn)' in '{0}'",
                                        _signature));

    ch = skipWhitespace(read());

    if (ch != -1)
      throw new ConfigException(L.l("function syntax is 'ret-type name(arg1, ..., argn)' in '{0}'",
                                        _signature));
  }

  /**
   * Parses the type.
   */
  private Class parseType(int ch)
    throws ConfigException
  {
    CharBuffer cb = CharBuffer.allocate();

    for (; Character.isJavaIdentifierPart((char) ch); ch = read())
      cb.append((char) ch);

    if (cb.length() == 0)
      throw new ConfigException(L.l("unexpected empty type in '{0}'",
                                        _signature));

    String className = cb.toString();

    unread(ch);

    return findClass(className);
  }

  /**
   * Converts the type to a classname.
   */
  private Class findClass(String className)
    throws ConfigException
  {
    if ("int".equals(className))
      return int.class;
    else if ("boolean".equals(className))
      return boolean.class;
    else if ("double".equals(className))
      return double.class;
    else if ("String".equals(className))
      return String.class;
    else if ("Date".equals(className))
      return java.util.Date.class;
    else if ("any".equals(className))
      return Object.class;

    throw new ConfigException(L.l("unknown type '{0}' in '{1}'",
                                      className, _signature));
  }

  /**
   * Skips whitespace to get to the next valid value.
   */
  private int skipWhitespace(int ch)
  {
    for (; Character.isWhitespace((char) ch); ch = read()) {
    }

    return ch;
  }

  /**
   * Reads the next character.
   */
  private int read()
  {
    if (_index < _signature.length())
      return _signature.charAt(_index++);
    else
      return -1;
  }

  /**
   * Unreads the last character.
   */
  private void unread(int ch)
  {
    if (ch >= 0)
      _index--;
  }

  /**
   * Returns a hash-code.
   */
  public int hashCode()
  {
    return _name.hashCode();
  }

  /**
   * True if the function signatures are equal.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof FunctionSignature))
      return false;

    FunctionSignature sig = (FunctionSignature) o;

    if (! _name.equalsIgnoreCase(sig._name))
      return false;

    if (_parameterTypes.length != sig._parameterTypes.length)
      return false;

    for (int i = 0; i < _parameterTypes.length; i++)
      if (! _parameterTypes[i].equals(sig._parameterTypes[i]))
        return false;

    return true;
  }

  /**
   * Returns a string value.
   */
  public String toString()
  {
    return "Function[" + _signature + "]";
  }
}
