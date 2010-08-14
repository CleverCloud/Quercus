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

import com.caucho.config.types.Signature;

import javax.servlet.jsp.tagext.JspFragment;

/**
 * Configuration for the taglib attribute in the .tld
 */
public class TldAttribute {
  private String _name;
  private Boolean _required;
  private boolean _rtexprvalue;
  private Class _type;
  private String _description;
  private boolean _isFragment;

  private DeferredMethod _deferredMethod;
  private DeferredValue _deferredValue;

  /**
   * Sets the attribute name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the attribute name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  public String getDescription()
  {
    return _description;
  }

  /**
   * Sets true if the attribute is required.
   */
  public void setRequired(boolean required)
  {
    _required = required;
  }

  /**
   * Returns true if the attribute is required.
   */
  public boolean getRequired()
  {
    return _required != null && _required;
  }

  /**
   * Returns true if the attribute is required.
   */
  public Boolean getRequiredVar()
  {
    return _required;
  }

  /**
   * Sets true if the attribute allows runtime expressions
   */
  public void setRtexprvalue(boolean rtexprvalue)
  {
    _rtexprvalue = rtexprvalue;
  }

  /**
   * Returns true if runtime expressions are required.
   */
  public boolean getRtexprvalue()
  {
    return _rtexprvalue;
  }

  /**
   * Sets true if the attribute allows runtime expressions
   */
  public void setFragment(boolean isFragment)
  {
    _isFragment = isFragment;
  }

  /**
   * Sets true if the attribute allows runtime expressions
   */
  public boolean isFragment()
  {
    return _isFragment;
  }

  /**
   * Sets the type of the attribute.
   */
  public void setType(Class type)
  {
    _type = type;
  }

  /**
   * Returns the type of the attribute.
   */
  public Class getType()
  {
    if (_type != null)
      return _type;
    else if (isFragment())
      return JspFragment.class;
    else
      return String.class;
  }

  /**
   * Sets the deferred value.
   */
  public void setDeferredValue(DeferredValue v)
  {
    _deferredValue = v;
  }

  /**
   * Sets the deferred value.
   */
  public DeferredValue getDeferredValue()
  {
    return _deferredValue;
  }

  /**
   * Sets the deferred method.
   */
  public void setDeferredMethod(DeferredMethod defer)
  {
    _deferredMethod = defer;
  }

  public String getExpectedType()
  {
    if (_deferredValue != null)
      return _deferredValue.getType();
    else
      return null;
  }

  public DeferredMethod getDeferredMethod()
  {
    return _deferredMethod;
  }
  
  public String getDeferredMethodSignature()
  {
    if (_deferredMethod != null) {
      Signature sig = _deferredMethod.getMethodSignature();

      if (sig != null)
        return sig.getSignature();
    }
    
    return null;
  }

  public static class DeferredMethod {
    private Signature _signature;

    public void setMethodSignature(Signature sig)
    {
      _signature = sig;
    }

    public Signature getMethodSignature()
    {
      return _signature;
    }
  }

  public static class DeferredValue {
    private String _type;

    public void setId(String id)
    {
    }

    public void setType(String type)
    {
      _type = type;
    }

    public String getType()
    {
      return _type;
    }
  }
}
