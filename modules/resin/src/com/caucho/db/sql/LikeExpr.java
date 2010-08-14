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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

class LikeExpr extends Expr {
  private static final Logger log
    = Logger.getLogger(LikeExpr.class.getName());
  
  private Expr _expr;
  private String _pattern;
  private Pattern _regexp;
  private boolean _isNot;

  LikeExpr(Expr expr, String pattern, boolean isNot)
  {
    _expr = expr;
    _pattern = pattern;
    _isNot = isNot;

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < pattern.length(); i++) {
      char ch = pattern.charAt(i);

      switch (ch) {
      case '%':
        sb.append(".*");
        break;
      case '_':
        sb.append(".");
        break;
      case '.': case '\\': case '*': case '+': case '(': case ')':
      case '[': case ']': case '?': case '^': case '$': case '|':
        sb.append("\\");
        sb.append(ch);
        break;
      default:
        sb.append(ch);
      }
    }

    try {
      _regexp = Pattern.compile(sb.toString());
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public Expr bind(Query query)
    throws SQLException
  {
    _expr = _expr.bind(query);

    return this;
  }

  /**
   * Returns the type of the expression.
   */
  public Class getType()
  {
    return boolean.class;
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    return (_expr.subCost(fromList));
  }

  /**
   * Evaluates the expression as a boolean.
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    if (_expr.isNull(context))
      return UNKNOWN;

    String value = _expr.evalString(context);

    if (_regexp.matcher(value).matches())
      return _isNot ? FALSE : TRUE;
    else
      return _isNot ? TRUE : FALSE;
  }

  public String evalString(QueryContext context)
    throws SQLException
  {
    throw new SQLException("can't convert string to boolean");
  }

  /**
   * Evaluates aggregate functions during the group phase.
   *
   * @param state the current database tuple
   */
  public void evalGroup(QueryContext context)
    throws SQLException
  {
    _expr.evalGroup(context);
  }

  public String toString()
  {
    return "(" + _expr + " LIKE " + _pattern + ")";
  }
}
