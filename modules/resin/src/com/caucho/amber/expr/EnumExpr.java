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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.expr;

import com.caucho.amber.query.QueryParser;
import com.caucho.amber.type.PrimitiveIntType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;


/**
 * Enum expression for Amber.
 */
public class EnumExpr extends AbstractAmberExpr {
  private Class _javaType;

  // enum string value
  private String _name;

  // enum integer value
  private int _ordinal;

  private boolean _isOrdinal = true;

  /**
   * Creates a new enum expression.
   *
   * @param javaType the java type of the enum
   * @param name the string name of the enum value
   * @param ordinal the integer value of the enum
   */
  public EnumExpr(Class javaType,
                  String name,
                  int ordinal)
  {
    _javaType = javaType;
    _name = name;
    _ordinal = ordinal;
  }

  /**
   * Returns the expr type.
   */
  public AmberType getType()
  {
    return PrimitiveIntType.create();
  }

  /**
   * Returns the java type
   */
  public Class getJavaType()
  {
    return _javaType;
  }

  /**
   * Returns the enum value
   */
  public int getOrdinal()
  {
    return _ordinal;
  }

  /**
   * Returns true for ordinal
   */
  public boolean isOrdinal()
  {
    return _isOrdinal;
  }

  /**
   * Sets true for ordinal
   */
  public void setOrdinal(boolean isOrdinal)
  {
    _isOrdinal = isOrdinal;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    return this;
  }

  /**
   * Generates the enum.
   */
  public void generateWhere(CharBuffer cb)
  {
    if (_isOrdinal)
      cb.append(_ordinal);
    else {
      cb.append('\'');
      cb.append(_name);
      cb.append('\'');
    }
  }

  /**
   * Generates the (update) enum.
   */
  public void generateUpdateWhere(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Generates the having expression.
   */
  public void generateHaving(CharBuffer cb)
  {
    generateWhere(cb);
  }

  public String toString()
  {
    return "EnumExpr[" + _javaType.getName() + "." + _name + "(" + _ordinal + ")]";
  }
}
