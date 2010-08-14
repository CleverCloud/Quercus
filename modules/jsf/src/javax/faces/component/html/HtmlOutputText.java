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

import javax.faces.context.*;
import javax.faces.component.*;

public class HtmlOutputText extends UIOutput
{
  public static final String COMPONENT_TYPE = "javax.faces.HtmlOutputText";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _dir;
  private ValueExpression _dirExpr;
  
  private String _lang;
  private ValueExpression _langExpr;
  
  private String _style;
  private ValueExpression _styleExpr;
  
  private String _styleClass;
  private ValueExpression _styleClassExpr;
  
  private String _title;
  private ValueExpression _titleExpr;
  
  private Boolean _escape;
  private ValueExpression _escapeExpr;

  public HtmlOutputText()
  {
    setRendererType("javax.faces.Text");
  }

  //
  // properties
  //
  
  public String getDir()
  {
    if (_dir != null)
      return _dir;
    else if (_dirExpr != null)
      return Util.evalString(_dirExpr);
    else
      return null;
  }

  public void setDir(String dir)
  {
    _dir = dir;
  }

  public boolean isEscape()
  {
    if (_escape != null)
      return _escape;
    else if (_escapeExpr != null)
      return Util.evalBoolean(_escapeExpr);
    else
      return true;
  }

  public void setEscape(boolean isEscape)
  {
    _escape = isEscape;
  }

  public String getLang()
  {
    if (_lang != null)
      return _lang;
    else if (_langExpr != null)
      return Util.evalString(_langExpr);
    else
      return null;
  }

  public void setLang(String lang)
  {
    _lang = lang;
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

  public void setStyle(String style)
  {
    _style = style;
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

  public void setStyleClass(String styleClass)
  {
    _styleClass = styleClass;
  }

  public String getTitle()
  {
    if (_title != null)
      return _title;
    else if (_titleExpr != null)
      return Util.evalString(_titleExpr);
    else
      return null;
  }

  public void setTitle(String title)
  {
    _title = title;
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
      case DIR:
        return _dirExpr;
      case ESCAPE:
        return _escapeExpr;
      case LANG:
        return _langExpr;
      case STYLE:
        return _styleExpr;
      case STYLE_CLASS:
        return _styleClassExpr;
      case TITLE:
        return _titleExpr;
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
      case DIR:
        if (expr != null && expr.isLiteralText()) {
          _dir = Util.evalString(expr);
          return;
        }
        else
          _dirExpr = expr;
        break;

      case ESCAPE:
        if (expr != null && expr.isLiteralText()) {
          _escape = Util.evalBoolean(expr);
          return;
        }
        else
          _escapeExpr = expr;
        break;

      case LANG:
        if (expr != null && expr.isLiteralText()) {
          _lang = Util.evalString(expr);
          return;
        }
        else
          _langExpr = expr;
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

      case TITLE:
        if (expr != null && expr.isLiteralText()) {
          _title = Util.evalString(expr);
          return;
        }
        else
          _titleExpr = expr;
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
      _dir,
      _escape,
      _lang,
      _style,
      _styleClass,
      _title,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    if (state != null) 
      super.restoreState(context, state[0]);

    _dir = (String) state[1];
    _escape = (Boolean) state[2];
    _lang = (String) state[3];
    _style = (String) state[4];
    _styleClass = (String) state[5];
    _title = (String) state[6];
  }

  //
  // private impl
  //

  private enum PropEnum {
    DIR,
    ESCAPE,
    LANG,
    STYLE,
    STYLE_CLASS,
    TITLE,
  }

  static {
    _propMap.put("dir", PropEnum.DIR);
    _propMap.put("escape", PropEnum.ESCAPE);
    _propMap.put("lang", PropEnum.LANG);
    _propMap.put("style", PropEnum.STYLE);
    _propMap.put("styleClass", PropEnum.STYLE_CLASS);
    _propMap.put("title", PropEnum.TITLE);
  }
}
