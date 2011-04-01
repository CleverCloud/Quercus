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
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class SignaturePattern {
  private static L10N L = new L10N(SignaturePattern.class);

  private String _methodName;

  private ArrayList<String> _paramTypes;

  public void addText(String methodName)
    throws ConfigException
  {
    if (methodName.indexOf('(') < 0) {
      _methodName = methodName;
      return;
    }
    
    Signature sig = new Signature();
    sig.addText(methodName);
    sig.init();

    _methodName = sig.getName();

    String []params = sig.getParameterTypes();
    if (params != null) {
      _paramTypes = new ArrayList<String>();

      for (int i = 0; i < params.length; i++)
        _paramTypes.add(params[i]);
    }
  }

  /**
   * Adds a method parameter.
   */
  public void addParam(String typeName)
  {
    if (_paramTypes == null)
      _paramTypes = new ArrayList<String>();

    _paramTypes.add(typeName);
  }

  /**
   * Sets the parameters to zero to distinguish between
   * methods with zero arguments and methods which don't
   * specify the requirements.
   */
  public void setHasParams()
  {
    if (_paramTypes == null)
      _paramTypes = new ArrayList<String>();
  }

  public boolean isMatch(Method method)
  {
    return isMatch(method.getName(), method.getParameterTypes());
  }
  
  public boolean isMatch(String methodName, Class []params)
  {
    if (_methodName == null)
      return false;
    else if (_methodName.equals("*"))
      return true;
    else if (! _methodName.equals(methodName))
      return false;
    else if (_paramTypes == null)
      return true;

    if (params.length != _paramTypes.size())
      return false;

    for (int i = 0; i < params.length; i++) {
      String name = params[i].getName();
      String param = (String) _paramTypes.get(i);

      if (! name.equals(param) && ! name.endsWith("." + param))
        return false;
    }

    return true;
  }

  public int hashCode()
  {
    return _methodName.hashCode();
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof SignaturePattern))
      return false;

    SignaturePattern cfg = (SignaturePattern) o;

    if (! _methodName.equals(cfg._methodName))
      return false;

    if (_paramTypes == null || cfg._paramTypes == null)
      return _paramTypes == cfg._paramTypes;

    if (_paramTypes.size() != cfg._paramTypes.size())
      return false;

    for (int i = 0; i < _paramTypes.size(); i++)
      if (! _paramTypes.get(i).equals(cfg._paramTypes.get(i)))
        return false;

    return true;
  }

  public String toString()
  {
    CharBuffer cb = new CharBuffer();

    cb.append("SignaturePattern[");
    cb.append(_methodName);
    cb.append("(");
    for (int i = 0; _paramTypes != null && i < _paramTypes.size(); i++) {
      if (i != 0)
        cb.append(", ");
      cb.append(_paramTypes.get(i));
    }
    cb.append(")]");
    return cb.toString();
  }
}
