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
import javax.servlet.jsp.el.ELException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Constructor for a function signature.
 */
public class ResinType {
  private final static L10N L = new L10N(ResinType.class);

  private String _signature;
  private String _className;
  
  private ArrayList<String> _args = new ArrayList<String>();

  private int _index;

  /**
   * Returns the signature.
   */
  public String getSignature()
  {
    return _signature;
  }

  /**
   * Returns the class name.
   */
  public String getClassName()
  {
    return _className;
  }
  
  /**
   * Adds the text value to the signature.
   */
  public void addText(String value)
  {
    _signature = value;
  }

  /**
   * Initialize the signature.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_signature == null)
      throw new ConfigException(L.l("A Signature requires the method signature."));

    parseSignature();
  }

  /**
   * Creates the object.
   */
  public Object create(Class targetClass)
    throws ClassNotFoundException,
           InstantiationException,
           IllegalAccessException,
           ConfigException,
           InvocationTargetException,
           ELException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    Class cl = Class.forName(_className, false, loader);

    if (targetClass != null && ! (targetClass.isAssignableFrom(cl)))
      throw new ConfigException(L.l("{0} is not assignable to {1}",
                                    targetClass.getName(), _className));


    Constructor constructor = getConstructor(cl);

    if (constructor == null)
      throw new ConfigException(L.l("Can't find a matching public constructor for '{0}'",
                                    _signature));

    Object []args = new Object[_args.size()];

    for (int i = 0; i < args.length; i++) {
      String arg = _args.get(i);

      args[i] = EL.evalObject(arg);
    }

    return constructor.newInstance(args);
  }

  private Constructor getConstructor(Class cl)
  {
    Constructor []cons = cl.getConstructors();

    for (int i = 0; i < cons.length; i++) {
      if (! Modifier.isPublic(cons[i].getModifiers()))
        continue;
      
      if (cons[i].getParameterTypes().length == _args.size())
        return cons[i];
    }

    return null;
  }
  

  /**
   * Parses the function signature.
   */
  private void parseSignature()
    throws ConfigException
  {
    _index = 0;

    _className = parseType(skipWhitespace(read()));

    CharBuffer cb = CharBuffer.allocate();
    int ch = skipWhitespace(read());

    if (ch < 0)
      return;
    
    if (ch != '(')
      throw new ConfigException(L.l("constructor syntax is `className(arg1, ..., argn)' in `{0}'",
                                        _signature));

    ch = skipWhitespace(read());
    while (ch > 0 && ch != ')') {
      cb.clear();

      while (ch > 0 && ch != ',' && ch != ')') {
        cb.append((char) ch);

        if (ch == '\'' || ch == '"') {
          int end = ch;

          for (ch = read(); ch > 0 && ch != end; ch = read()) {
            cb.append((char) ch);
          }

          if (ch > 0)
            cb.append((char) ch);
        }

        ch = read();
      }

      _args.add(cb.toString());
    }
    
    if (ch != ')')
      throw new ConfigException(L.l("constructor syntax is `className(arg1, ..., argn)' in `{0}'",
                                        _signature));

    ch = skipWhitespace(read());
    
    if (ch > 0)
      throw new ConfigException(L.l("constructor syntax is `className(arg1, ..., argn)' in `{0}'",
                                        _signature));
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
                                         _signature));

    while (true) {
      for (; Character.isWhitespace((char) ch); ch = read()) {
      }

      if (ch == '[') {
        ch = read();

        if (ch != ']')
          throw new ConfigException(L.l("function syntax is `ret-type name(arg1, ..., argn)' in `{0}'",
                                        _signature));

        cb.append("[]");

        ch = read();
      }
      else
        break;
    }

    

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
}

