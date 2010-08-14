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
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.*;

/**
 * Configuration for a function signature.
 */
public class Signature {
  private static final L10N L = new L10N(Signature.class);
  private static final Logger log
    = Logger.getLogger(Signature.class.getName());

  private String _signature;
  private String _className;
  private String _name;
  private String []_parameterTypes;
  private String _returnType;

  private int _index;

  public Signature()
  {
  }

  public Signature(String sig)
  {
    addText(sig);
    init();
  }

  /**
   * Returns the signature.
   */
  public String getSignature()
  {
    return _signature;
  }

  /**
   * Returns the method name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the class name
   */
  public String getClassName()
  {
    return _className;
  }

  /**
   * Returns the method.
   */
  public Method getMethod()
  {
    if (_className == null)
      return null;

    try {
      Class cl = CauchoSystem.loadClass(_className);
      if (cl == null)
        return null;

      Method []methods = cl.getMethods();
      for (int i = 0; i < methods.length; i++) {
        if (matches(methods[i])) {
          return methods[i];
        }
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    return null;
  }

  /**
   * Returns the return type.
   */
  public String getReturnType()
  {
    return _returnType;
  }
  
  /**
   * Returns the method parameters.  If null, then the parameters
   * were not specified.
   */
  public String []getParameterTypes()
  {
    return _parameterTypes;
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
    // jsp/18v2
    /*
    if (signature == null)
      throw new ConfigException(L.l("A Signature requires the method signature."));
    */

    if (_signature != null && ! "".equals(_signature)) // <-jsp/18v2 (ConfigContext.getTextValue(node) no longer returns null)
      parseSignature();
  }

  /**
   * Returns true if the method matches the signature.
   */
  public boolean matches(Method method)
  {
    if (! method.getName().equals(getName()))
      return false;

    Class []parameterTypes = method.getParameterTypes();
    String []sigTypes = getParameterTypes();

    if (parameterTypes.length != sigTypes.length)
      return false;

    for (int i = 0; i < parameterTypes.length; i++) {
      String param = getName(parameterTypes[i]);

      if (! param.equals(sigTypes[i])
          && ! param.endsWith("." + sigTypes[i]))
        return false;
    }

    return true;
  }

  private String getName(Class cl)
  {
    if (cl.isArray())
      return getName(cl.getComponentType()) + "[]";
    else
      return cl.getName();
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

    if (ch == '(' || ch < 0) {
      _name = _returnType;
      _returnType = null;
    }
    else {
      for (; Character.isJavaIdentifierPart((char) ch) || ch == '.'; ch = read())
        cb.append((char) ch);

      if (cb.length() == 0)
        throw new ConfigException(L.l("unexpected empty function name in `{0}'",
                                      _signature));

      _name = cb.toString();

      int p = _name.lastIndexOf('.');
      if (p > 0) {
        _className = _name.substring(0, p);
        _name = _name.substring(p + 1);
      }

      ch = skipWhitespace(ch);
    }

    if (ch != '(')
      throw new ConfigException(L.l("function syntax is `ret-type name(arg1, ..., argn)' in `{0}'",
                                        _signature));

    ArrayList<String> argList = new ArrayList<String>();
    
    ch = read();
    while (Character.isJavaIdentifierPart((char) (ch = skipWhitespace(ch))) ||
           ch == '.') {
      String type = parseType(ch);

      argList.add(type);

      ch = skipWhitespace(read());

      for (;
           Character.isJavaIdentifierPart((char) ch) || ch == '.';
           ch = read()) {
      }
      
      if (ch == ',')
        ch = read();
    }

    _parameterTypes = (String []) argList.toArray(new String[argList.size()]);

    if (ch != ')')
      throw new ConfigException(L.l("function syntax is `ret-type name(arg1, ..., argn)' in `{0}'",
                                        _signature));

    ch = skipWhitespace(read());

    if (ch != -1)
      throw new ConfigException(L.l("function syntax is `ret-type name(arg1, ..., argn)' in `{0}'",
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

  public String toString()
  {
    return "Signature[" + _signature + "]";
  }
}

