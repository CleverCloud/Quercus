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

/**
 * Configuration for the taglib variable in the .tld
 */
public class TldVariable {
  private String _nameGiven;
  private String _nameFromAttribute;
  private String _alias;
  private String _variableClass;
  private boolean _declare = true;
  private String _scope = "NESTED";
  private String _description;

  /**
   * Sets a constant name of the variable.
   */
  public void setNameGiven(String nameGiven)
  {
    _nameGiven = nameGiven;
  }

  /**
   * Gets a constant name of the variable.
   */
  public String getNameGiven()
  {
    return _nameGiven;
  }

  /**
   * Sets a variable name determined from an attribute.
   */
  public void setNameFromAttribute(String nameFromAttribute)
  {
    _nameFromAttribute = nameFromAttribute;
  }

  /**
   * Gets a variable name determined from an attribute.
   */
  public String getNameFromAttribute()
  {
    return _nameFromAttribute;
  }

  /**
   * Sets the alias for tag files
   */
  public void setAlias(String alias)
  {
    _alias = alias;
  }

  /**
   * Gets the alias for tag files
   */
  public String getAlias()
  {
    return _alias;
  }

  /**
   * Sets the class of the variable object.
   */
  public void setVariableClass(String variableClass)
  {
    _variableClass = variableClass;
  }

  /**
   * Gets the variable class of the variable value.
   */
  public String getVariableClass()
  {
    if (_variableClass != null)
      return _variableClass;
    else
      return "java.lang.String";
  }

  /**
   * Sets whether the variable is declared or not.
   */
  public void setDeclare(boolean declare)
  {
    _declare = declare;
  }

  /**
   * Return true if the variable should be declared.
   */
  public boolean getDeclare()
  {
    return _declare;
  }

  /**
   * Sets the variable scope.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Return the variable scope
   */
  public String getScope()
  {
    return _scope;
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Return the descrption
   */
  public String getDescription()
  {
    return _description;
  }
}
