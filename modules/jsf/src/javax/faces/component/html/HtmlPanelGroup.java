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

package javax.faces.component.html;

import java.util.*;

import javax.el.*;

import javax.faces.component.*;
import javax.faces.context.*;

public class HtmlPanelGroup extends UIPanel
{
  public static final String COMPONENT_TYPE = "javax.faces.HtmlPanelGroup";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _layout;
  private ValueExpression _layoutExpr;

  private String _style;
  private ValueExpression _styleExpr;

  private String _styleClass;
  private ValueExpression _styleClassExpr;

  public HtmlPanelGroup()
  {
    setRendererType("javax.faces.Group");
  }

  //
  // properties
  //

  public String getLayout()
  {
    if (_layout != null)
      return _layout;
    else if (_layoutExpr != null)
      return Util.evalString(_layoutExpr);
    else
      return null;
  }

  public void setLayout(String value)
  {
    _layout = value;
  }
  
  public String getStyle()
  {
    if (_style != null)
      return _style;
    else if (_styleExpr != null)
      return Util.evalString(_styleExpr);
    else
      return null;
  }

  public void setStyle(String value)
  {
    _style = value;
  }
  
  public String getStyleClass()
  {
    if (_styleClass != null)
      return _styleClass;
    else if (_styleClassExpr != null)
      return Util.evalString(_styleClassExpr);
    else
      return null;
  }

  public void setStyleClass(String value)
  {
    _styleClass = value;
  }

  //
  // value expression override
  //

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (prop) {
      case LAYOUT:
        return _layoutExpr;
      case STYLE:
        return _styleExpr;
      case STYLE_CLASS:
        return _styleClassExpr;
      }
    }
    
    return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (prop) {
      case LAYOUT:
        if (expr != null && expr.isLiteralText()) {
          _layout = Util.evalString(expr);
          return;
        }
        else
          _layoutExpr = expr;
        break;

      case STYLE:
        if (expr != null && expr.isLiteralText()) {
          _style = Util.evalString(expr);
          return;
        }
        else
          _styleExpr = expr;
        break;

      case STYLE_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _styleClass = Util.evalString(expr);
          return;
        }
        else
          _styleClassExpr = expr;
        break;
      }
    }
    
    super.setValueExpression(name, expr);
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    Object parent = super.saveState(context);

    return new Object[] {
      parent,
      
      _layout,
      _style,
      _styleClass,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    int i = 0;

    if (state != null) 
      super.restoreState(context, state[i++]);

    _layout = (String) state[i++];
    _style = (String) state[i++];
    _styleClass = (String) state[i++];
  }

  //
  // utility
  //

  private enum PropEnum {
    LAYOUT,
    STYLE,
    STYLE_CLASS,
  }

  static {
    _propMap.put("layout", PropEnum.LAYOUT);
    _propMap.put("style", PropEnum.STYLE);
    _propMap.put("styleClass", PropEnum.STYLE_CLASS);
  }
}
