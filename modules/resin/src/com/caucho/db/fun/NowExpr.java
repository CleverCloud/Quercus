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

package com.caucho.db.fun;

import com.caucho.db.sql.Expr;
import com.caucho.db.sql.FunExpr;
import com.caucho.db.sql.QueryContext;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

import java.sql.SQLException;
import java.util.Date;

public class NowExpr extends FunExpr {
  protected static final L10N L = new L10N(NowExpr.class);
  
  protected void addArg(Expr expr)
    throws SQLException
  {
    throw new SQLException(L.l("now() has no arguments"));
  }

  /**
   * Returns the expected result type of the expression.
   */
  public Class getType()
  {
    return Date.class;
  }

  /**
   * Returns true for a null value.
   *
   * @param rows the current tuple being evaluated
   *
   * @return true if null
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return false;
  }

  /**
   * Evaluates the expression as a double.
   *
   * @param rows the current tuple being evaluated
   *
   * @return the double value
   */
  public double evalDouble(QueryContext context)
    throws SQLException
  {
    return evalLong(context);
  }

  /**
   * Evaluates the expression as a long.
   *
   * @param rows the current tuple being evaluated
   *
   * @return the long value
   */
  public long evalLong(QueryContext context)
    throws SQLException
  {
    return Alarm.getCurrentTime();
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param rows the current tuple being evaluated
   *
   * @return the string value
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    return QDate.formatLocal(evalLong(context));
  }
}
