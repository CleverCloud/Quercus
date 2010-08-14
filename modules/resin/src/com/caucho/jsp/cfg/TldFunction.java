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

package com.caucho.jsp.cfg;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Signature;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.jsp.tagext.FunctionInfo;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Configuration for the taglib tag in the .tld
 */
public class TldFunction {
  private static L10N L = new L10N(TldFunction.class);
  
  private String _name;
  private Class _functionClass;
  private Signature _signature;
  private String _displayName;
  private String _description;
  private String _example;

  private Method _method;

  /**
   * Sets the function name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the function name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the function class.
   */
  public void setFunctionClass(Class functionClass)
  {
    _functionClass = functionClass;
  }

  /**
   * Gets the function class.
   */
  public Class getFunctionClass()
  {
    return _functionClass;
  }

  /**
   * Sets the function signature.
   */
  public void setFunctionSignature(Signature signature)
  {
    _signature = signature;
  }

  /**
   * Gets the function signature.
   */
  public Signature getFunctionSignature()
  {
    return _signature;
  }

  /**
   * Sets the display-name
   */
  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  /**
   * Gets the display-name
   */
  public String getDisplayName()
  {
    return _displayName;
  }

  /**
   * Sets the description
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Gets the description
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * Sets the example
   */
  public void setExample(String example)
  {
    _example = example;
  }

  /**
   * Gets the example
   */
  public String getExample()
  {
    return _example;
  }

  /**
   * Returns the underlying method.
   */
  public Method getMethod()
  {
    return _method;
  }

  /**
   * Initialize
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_name == null)
      throw new ConfigException(L.l("function needs <name>"));
    
    if (_signature == null)
      throw new ConfigException(L.l("function requires <signature>"));
    
    Method []methods = _functionClass.getMethods();

    loop:
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (! Modifier.isPublic(method.getModifiers()))
        continue;
      
      if (Modifier.isAbstract(method.getModifiers()))
        continue;

      if (_signature.matches(method)) {
        _method = method;

        if (! Modifier.isPublic(_method.getModifiers()))
          throw new ConfigException(L.l("function `{0}' must be public",
                                          _method));
        if (! Modifier.isStatic(_method.getModifiers()))
          throw new ConfigException(L.l("function `{0}' must be static",
                                          _method));
        if (Modifier.isAbstract(_method.getModifiers()))
          throw new ConfigException(L.l("function `{0}' must be concrete",
                                          _method));
        return;
      }
    }

    throw new ConfigException(L.l("No public method in `{0}' matches the signature `{1}'",
                                    _functionClass.getName(),
                                    _signature.getSignature()));
  }

  public FunctionInfo toFunctionInfo()
  {
    return new FunctionInfo(_name,
                            _functionClass.getName(),
                            _signature.getSignature());
  }

  public String toString()
  {
    return "TldFunction[" + _signature.getSignature() + "]";
  }
}
