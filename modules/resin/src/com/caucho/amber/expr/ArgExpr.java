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
import com.caucho.amber.type.*;
import com.caucho.util.CharBuffer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parameter argument expression.
 */
public class ArgExpr extends AbstractAmberExpr {
  private QueryParser _parser;

  // argument index
  private int _index;

  private int _sqlIndex;

  private String _name;

  private AmberType _type;

  /**
   * Creates a new argument expression.
   *
   * @param index the argument index
   */
  public ArgExpr(QueryParser parser, int index)
  {
    _parser = parser;

    _index = index;

    _sqlIndex = -1;
  }

  /**
   * Creates a new named argument expression.
   *
   * @param String the argument name
   */
  public ArgExpr(QueryParser parser, String name, int index)
  {
    _parser = parser;

    _name = name;

    _index = index;

    _sqlIndex = -1;
  }

  /**
   * Returns the index value
   */
  public int getIndex()
  {
    return _index;
  }

  /**
   * Returns the expr type.
   */
  public AmberType getType()
  {
    return _type;
  }

  /**
   * Sets the expr type.
   */
  public void setType(AmberType type)
  {
    _type = type;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    parser.addArg(this);

    return this;
  }

  /**
   * Returns true if the expression must exist
   */
  @Override
  public boolean exists()
  {
    // ejb/0h1k
    return true;
  }

  /**
   * Generates the literal.
   */
  public void generateWhere(CharBuffer cb)
  {
    generateInternalWhere(cb, true);
  }

  /**
   * Generates the (update) literal.
   */
  public void generateUpdateWhere(CharBuffer cb)
  {
    generateInternalWhere(cb, false);
  }

  /**
   * Generates the having expression.
   */
  public void generateHaving(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Returns the argument name, or null
   * if it is a positional parameter.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the parameter.
   */
  public void setParameter(PreparedStatement pstmt, int i,
                           AmberType []argTypes, Object []argValues)
    throws SQLException
  {
    try {
      if (_name == null) {

        // jpa/141d (enum type)
        if (getType() != null) {
          if (! ((getType() instanceof UtilDateType) ||
                 (getType() instanceof CalendarType))) {
            argTypes[_index - 1] = getType();
          }
        }

        if (argTypes[_index - 1] != null) {
          argTypes[_index - 1].setParameter(pstmt, _sqlIndex + 1,
                                            argValues[_index - 1]);
          // jpa/141e
        }
        else
          pstmt.setString(_sqlIndex + 1, null);
      }
      else {
        // jpa/141d (enum type)
        if (getType() != null) {
          // jpa/1410, jpa/1413
          if (! ((getType() instanceof UtilDateType) ||
                 (getType() instanceof CalendarType))) {
            argTypes[i - 1] = getType();
          }
        }

        if (argTypes[i - 1] != null) {
          // jpa/141g

          // jpa/1217 argTypes[i - 1].setParameter(pstmt, _sqlIndex + 1, argValues[i - 1]);
          argTypes[i - 1].setParameter(pstmt, i, argValues[i - 1]);
        }
        else
          pstmt.setString(_sqlIndex + 1, null);
      }
    } catch (Exception e) {
      // jpa/141h
      throw new IllegalArgumentException(e);
    }
  }

  public String toString()
  {
    if (_name == null)
      return "?" + _index;
    else
      return ":" + _name;
  }

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    if (_sqlIndex < 0)
      _sqlIndex = _parser.generateSQLArg();

    cb.append("?");
  }
}
