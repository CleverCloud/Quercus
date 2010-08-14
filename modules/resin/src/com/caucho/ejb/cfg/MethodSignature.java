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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg;

import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Signature;
import com.caucho.inject.Module;
import com.caucho.util.CharBuffer;

@Module
public class MethodSignature {
  private String _ejbName;
  
  private String _methodName;
  private String _methodIntf;

  private Object _value;

  private ArrayList<String> _paramTypes;
  
  public MethodSignature()
  {
  }

  public void setEJBName(String ejbName)
  {
    _ejbName = ejbName;
  }

  public String getEJBName()
  {
    return _ejbName;
  }

  public void setMethodName(String name)
    throws ConfigException
  {
    setName(name);
  }
  
  public String getName()
  {
    return _methodName;
  }

  public void setName(String methodName)
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

  public void setDescription(String value)
  {
  }

  public void addText(String text)
    throws ConfigException
  {
    setName(text);
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
   * Adds a method parameter.
   */
  public MethodParams createMethodParams()
  {
    return new MethodParams();
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
  
  /**
   * Sets the method interface.
   */
  public void setMethodIntf(String intf)
  {
    _methodIntf = intf;
  }

  boolean isHome()
  {
    return _methodIntf == null || _methodIntf.equals("Home");
  }

  boolean isRemote()
  {
    return _methodIntf == null || _methodIntf.equals("Remote");
  }

  boolean isLocalHome()
  {
    return _methodIntf == null || _methodIntf.equals("LocalHome");
  }

  boolean isLocal()
  {
    return _methodIntf == null || _methodIntf.equals("Local");
  }

  int getCost()
  {
    int cost = _methodIntf == null ? 0 : 1;

    if (_methodName.equals("*"))
      return cost;
    else if (_paramTypes == null)
      return 2 + cost;
    else
      return 4 + cost;
  }

  public boolean isMatch(Method method, String intf)
  {
    if (method == null)
      return _methodName.equals("*");
    else
      return isMatch(method.getName(), method.getParameterTypes(), intf);
  }

  public boolean isMatch(AnnotatedMethod<?> annMethod, String intf)
  {
    if (annMethod == null)
      return _methodName.equals("*");
    
    Method method = annMethod.getJavaMember();

    return isMatch(method.getName(), method.getParameterTypes(), intf);
  }
  
  public boolean isMatch(String methodName, Class<?> []params, String intf)
  {
    if (_methodIntf != null && ! _methodIntf.equals(intf))
      return false;
    else
      return isMatch(methodName, params);
  }
  
  public boolean isMatch(String methodName, Class<?> []params)
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

  void setValue(Object value)
  {
    _value = value;
  }

  Object getValue()
  {
    return _value;
  }

  public int hashCode()
  {
    return _methodName.hashCode();
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof MethodSignature))
      return false;

    MethodSignature cfg = (MethodSignature) o;

    if (! _methodName.equals(cfg._methodName))
      return false;

    if (_paramTypes == null || cfg._paramTypes == null)
      return _paramTypes == cfg._paramTypes;

    if (_paramTypes.size() != cfg._paramTypes.size())
      return false;

    for (int i = 0; i < _paramTypes.size(); i++)
      if (! _paramTypes.get(i).equals(cfg._paramTypes.get(i)))
        return false;

    if (_methodIntf == cfg._methodIntf)
      return true;

    else if (_methodIntf == null || cfg._methodIntf == null)
      return false;

    else
      return _methodIntf.equals(cfg._methodIntf);
  }

  public String toSignatureString()
  {
    CharBuffer cb = new CharBuffer();

    cb.append(_methodName);
    cb.append("(");
    for (int i = 0; _paramTypes != null && i < _paramTypes.size(); i++) {
      if (i != 0)
        cb.append(", ");
      cb.append(_paramTypes.get(i));
    }
    cb.append(")");

    return cb.toString();
  }

  public String toString()
  {
    return ("MethodSignature[" + toSignatureString() + "]");
  }

  public class MethodParams {
    public void addMethodParam(String value)
    {
      addParam(value);
    }
  }
}
