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

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.el.EL;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

/**
 * Configuration for an instantiation
 */
public class InstantiationConfig {
  private static L10N L = new L10N(InstantiationConfig.class);

  private String _value;
  private Class _type;
  
  private ArrayList<Object> _args = new ArrayList<Object>();

  private int _index;

  /**
   * Returns the type.
   */
  public Class getType()
  {
    return _type;
  }
  
  /**
   * Adds the text value to the signature.
   */
  public void addText(String value)
  {
    _value = value;
  }

  /**
   * Adds an argument.
   */
  public void addArg(Object value)
  {
    _args.add(value);
  }

  /**
   * Initialize the signature.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    if (_value == null)
      throw new ConfigException(L.l("An instantiation requires the class name."));

    parseSignature();
  }

  /**
   * Instantiates the object.
   */
  public Object create()
    throws Exception
  {
    Object []args = new Object[_args.size()];
    
    Class []paramTypes = new Class[args.length];

    for (int i = 0; i < _args.size(); i++) {
      Object arg = _args.get(i);

      args[i] = arg;
      if (arg == null)
        paramTypes[i] = Object.class;
      else
        paramTypes[i] = arg.getClass();
    }
    
    Constructor constructor = getConstructor(_type, paramTypes);

    if (constructor == null)
      throw new ConfigException(L.l("Can't find public constructor for `{0}'.",
                                    _type.getName()));

    return constructor.newInstance(args);
  }

  private Constructor getConstructor(Class cl, Class []types)
  {
    Constructor []constructors = cl.getConstructors();

    for (int i = 0; i < constructors.length; i++) {
      Class []args = constructors[i].getParameterTypes();

      if (args.length == types.length)
        return constructors[i];
    }

    return null;
  }

  /**
   * Parses the constructor.
   */
  private void parseSignature()
    throws Exception
  {
    _index = 0;

    String type = parseType(skipWhitespace(read()));

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    _type = Class.forName(type, false, loader);

    int ch = skipWhitespace(read());

    if (ch < 0)
      return;

    if (ch != '(')
      throw new ConfigException(L.l("expected `(' in constructor `{0}'",
                                      _value));

    CharBuffer cb = CharBuffer.allocate();

    ch = ',';
    while (ch == ',') {
      ch = skipWhitespace(read());

      cb.clear();
      
      if (ch == '\'' || ch == '"') {
        int end = ch;

        for (ch = read(); ch > 0 && ch != end; ch = read()) {
          if (ch == '\\')
            cb.append(read());
          else
            cb.append((char) ch);
        }

        _args.add(cb.toString());
      }
      else if (ch == '$') {
        ch = read();

        if (ch != '{')
          throw new ConfigException(L.l("expected EL-expression at $ in `{0}'",
                                        _value));

        for (ch = read(); ch > 0 && ch != '}'; ch = read()) {
          cb.append((char) ch);
        }

        _args.add(EL.evalObject(cb.toString()));
      }
      else
        throw new ConfigException(L.l("expected string or EL-expression in `{0}'",
                                      _value));
    }
  }

  /**
   * Parses the type.
   */
  private String parseType(int ch)
    throws ConfigException
  {
    CharBuffer cb = CharBuffer.allocate();

    for (; Character.isJavaIdentifierPart((char) ch) || ch == '.'; ch = read())
      cb.append((char) ch);

    if (cb.length() == 0)
      throw new ConfigException(L.l("unexpected empty type in `{0}'",
                                    _value));

    String className = cb.toString();

    unread(ch);

    return className;
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
    if (_index < _value.length())
      return _value.charAt(_index++);
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
}

