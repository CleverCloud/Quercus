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
 * @author Scott Ferguson
 */

package com.caucho.amber.expr;

import com.caucho.amber.query.QueryParser;
import com.caucho.util.CharBuffer;

/**
 * Literal expression for Amber.
 */
public class LiteralExpr extends AbstractAmberExpr {
  private QueryParser _parser;

  // literal value
  private String _value;

  private Class _javaType;

  /**
   * Creates a new literal expression.
   *
   * @param value the string value of the literal
   * @param type the java type of the literal
   */
  public LiteralExpr(QueryParser parser,
                     String value,
                     Class javaType)
  {
    _parser = parser;
    _value = value;
    _javaType = javaType;
  }

  /**
   * Returns the java type
   */
  public Class getJavaType()
  {
    return _javaType;
  }

  /**
   * Returns the literal value
   */
  public String getValue()
  {
    return _value;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    return this;
  }

  /**
   * Returns true if the expression must exist
   */
  @Override
  public boolean exists()
  {
    return true;
  }

  /**
   * Generates the literal.
   */
  public void generateWhere(CharBuffer cb)
  {
    if ((_javaType != null) && _javaType.equals(boolean.class)) {
      if (! _parser.isPostgresDBMS()) {

        // Derby and MySql.

        if (_value.equalsIgnoreCase("false"))
          cb.append("0");
        else
          cb.append("1");

        return;
      }
    }

    cb.append(_value);
  }

  /**
   * Generates the (update) literal.
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
    return _value;
  }
}
