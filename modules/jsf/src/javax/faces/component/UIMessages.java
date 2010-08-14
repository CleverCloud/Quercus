/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package javax.faces.component;

import java.util.*;

import javax.el.*;
import javax.faces.context.*;

public class UIMessages extends UIComponentBase
{
  public static final String COMPONENT_FAMILY = "javax.faces.Messages";
  public static final String COMPONENT_TYPE = "javax.faces.Messages";

  private Boolean _globalOnly;
  private ValueExpression _globalOnlyExpr;

  private Boolean _showDetail;
  private ValueExpression _showDetailExpr;

  private Boolean _showSummary;
  private ValueExpression _showSummaryExpr;

  public UIMessages()
  {
    setRendererType("javax.faces.Messages");
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  //
  // properties
  //
  
  public boolean isGlobalOnly()
  {
    if (_globalOnly != null)
      return _globalOnly;
    else if (_globalOnlyExpr != null)
      return Util.evalBoolean(_globalOnlyExpr, getFacesContext());
    else
      return false;
  }

  public void setGlobalOnly(boolean value)
  {
    _globalOnly = value;
  }
  
  public boolean isShowDetail()
  {
    if (_showDetail != null)
      return _showDetail;
    else if (_showDetailExpr != null)
      return Util.evalBoolean(_showDetailExpr, getFacesContext());
    else
      return false;
  }

  public void setShowDetail(boolean value)
  {
    _showDetail = value;
  }

  public boolean isShowSummary()
  {
    if (_showSummary != null)
      return _showSummary;
    else if (_showSummaryExpr != null)
      return Util.evalBoolean(_showSummaryExpr, getFacesContext());
    else
      return true;
  }

  public void setShowSummary(boolean value)
  {
    _showSummary = value;
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("globalOnly".equals(name))
      return _globalOnlyExpr;
    else if ("showDetail".equals(name))
      return _showDetailExpr;
    else if ("showSummary".equals(name))
      return _showSummaryExpr;
    else
      return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("globalOnly".equals(name)) {
      if (expr != null && expr.isLiteralText()) {
        _globalOnly = Util.booleanValueOf(expr.getValue(null));
        return;
      }
      else
        _globalOnlyExpr = expr;
    }
    else if ("showDetail".equals(name)) {
      if (expr != null && expr.isLiteralText()) {
        _showDetail = Util.booleanValueOf(expr.getValue(null));
        return;
      }
      else
        _showDetailExpr = expr;
    }
    else if ("showSummary".equals(name)) {
      if (expr != null && expr.isLiteralText()) {
        _showSummary = Util.booleanValueOf(expr.getValue(null));
        return;
      }
      else
        _showSummaryExpr = expr;
    }

    super.setValueExpression(name, expr);
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    return new Object[] {
      super.saveState(context),
      _globalOnly,
      _showDetail,
      _showSummary,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _globalOnly = (Boolean) state[1];
    _showDetail = (Boolean) state[2];
    _showSummary = (Boolean) state[3];
  }
}
