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

package com.caucho.jcr.base;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Information about property type.
 */
public class BasePropertyDefinition
  extends BaseItemDefinition
  implements PropertyDefinition {
  
  private final int _requiredType;
  
  private String []_valueConstraints;
  
  private Value []_defaultValues;
  
  private boolean _isMultiple;

  public BasePropertyDefinition(String name,
                                NodeType nodeType,
                                int requiredType)
  {
    super(name, nodeType);

    _requiredType = requiredType;
  }
  
  /**
   * Returns the property's type.
   */
  public int getRequiredType()
  {
    return _requiredType;
  }

  /**
   * Returns constraints on the property value.
   */
  public String[] getValueConstraints()
  {
    return _valueConstraints;
  }

  /**
   * Sets constraints on the property value.
   */
  public void setValueConstraints(String []constraints)
  {
    _valueConstraints = constraints;
  }

  /**
   * Returns the property's defaults.
   */
  public Value[] getDefaultValues()
  {
    return _defaultValues;
  }

  /**
   * Sets the property's defaults.
   */
  public void setDefaultValues(Value []defaultValues)
  {
    _defaultValues = defaultValues;
  }

  /**
   * Returns true if multiple values are allowed.
   */
  public boolean isMultiple()
  {
    return _isMultiple;
  }

  /**
   * Set true if multiple values are allowed.
   */
  public void setMultiple(boolean isMultiple)
  {
    _isMultiple = isMultiple;
  }
}
