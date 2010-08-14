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

package com.caucho.db.sql;

import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class SubSelectEvalExpr extends Expr {
  protected static final L10N L = new L10N(SubSelectEvalExpr.class);

  private SubSelectExpr _subselect;
  
  SubSelectEvalExpr(SubSelectExpr subselect)
  {
    _subselect = subselect;
  }

  /**
   * Returns the expected result type of the expression.
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
    return _subselect.subCost(fromList);
  }

  /**
   * Evaluates the expression as a boolean.
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    _subselect.evaluate(context);
    
    return TRUE;
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "SubSelectEvalExpr[" + _subselect.getSubSelect() + "]";
  }
}
