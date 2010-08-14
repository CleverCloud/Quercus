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

public class ExistsExpr extends SubSelectExpr {
  protected static final L10N L = new L10N(ExistsExpr.class);
 
  private int _groupIndex;
  private ExistsQuery _exists;

  private Query _parentQuery;
  
  ExistsExpr(ExistsQuery query)
  {
    super(query);
    
    _exists = query;
  }

  /**
   * Binds the expression to the query.
   */
  public Expr bind(Query query)
    throws SQLException
  {
    if (_parentQuery != null)
      return this;
    
    _parentQuery = query;
    
    super.bind(query);
    
    _groupIndex = query.getDataFields();
    
    query.setDataFields(_groupIndex + 1);

    _exists.bind();
    
    return this;
  }

  /**
   * Returns the expected result type of the expression.
   */
  public Class getType()
  {
    return boolean.class;
  }
  
  ArrayList<SubSelectParamExpr> getParamExprs()
  {
    return _exists.getParamExprs();
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    ArrayList<SubSelectParamExpr> paramExprs = getParamExprs();
    
    long cost = 10;

    for (int i = 0; i < paramExprs.size(); i++)
      cost += paramExprs.get(i).getExpr().cost(fromList);

    return 2 * cost;
  }

  /**
   * Evaluates the subselect.
   */
  void evaluate(QueryContext context)
    throws SQLException
  {
    QueryContext subcontext = QueryContext.allocate();

    ArrayList<SubSelectParamExpr> paramExprs = getParamExprs();
    
    for (int i = 0; i < paramExprs.size(); i++) {
      paramExprs.get(i).eval(context, subcontext);
    }

    boolean exists = _exists.exists(subcontext, context.getTransaction());
    Data data = context.getGroupData(_groupIndex);

    data.setBoolean(exists);
    
    subcontext.close();
    
    QueryContext.free(subcontext);
  }

  /**
   * Evaluates the expression to check for null
   *
   * @param rows the current database tuple
   *
   * @return the null value
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    Data data = context.getGroupData(_groupIndex);

    return data.isNull();
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    Data data = context.getGroupData(_groupIndex);

    return data.getBoolean();
  }

  public String toString()
  {
    return "ExistsExpr[" + _exists + "]";
  }
}
