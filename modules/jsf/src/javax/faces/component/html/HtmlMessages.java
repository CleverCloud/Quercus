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

public class HtmlMessages extends UIMessages
{
  public static final String COMPONENT_TYPE = "javax.faces.HtmlMessages";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _dir;
  private ValueExpression _dirExpr;

  private String _errorClass;
  private ValueExpression _errorClassExpr;

  private String _errorStyle;
  private ValueExpression _errorStyleExpr;

  private String _fatalClass;
  private ValueExpression _fatalClassExpr;

  private String _fatalStyle;
  private ValueExpression _fatalStyleExpr;

  private String _infoClass;
  private ValueExpression _infoClassExpr;

  private String _infoStyle;
  private ValueExpression _infoStyleExpr;
  
  private String _lang;
  private ValueExpression _langExpr;
  
  private String _layout;
  private ValueExpression _layoutExpr;
  
  private String _style;
  private ValueExpression _styleExpr;
  
  private String _styleClass;
  private ValueExpression _styleClassExpr;
  
  private String _title;
  private ValueExpression _titleExpr;

  private String _warnClass;
  private ValueExpression _warnClassExpr;

  private String _warnStyle;
  private ValueExpression _warnStyleExpr;
  
  private Boolean _tooltip;
  private ValueExpression _tooltipExpr;

  public HtmlMessages()
  {
    setRendererType("javax.faces.Messages");
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
  
  public String getErrorClass()
  {
    if (_errorClass != null)
      return _errorClass;
    else if (_errorClassExpr != null)
      return Util.evalString(_errorClassExpr);
    else
      return null;
  }

  public void setErrorClass(String errorClass)
  {
    _errorClass = errorClass;
  }
  
  public String getErrorStyle()
  {
    if (_errorStyle != null)
      return _errorStyle;
    else if (_errorStyleExpr != null)
      return Util.evalString(_errorStyleExpr);
    else
      return null;
  }

  public void setErrorStyle(String errorStyle)
  {
    _errorStyle = errorStyle;
  }
  
  public String getFatalClass()
  {
    if (_fatalClass != null)
      return _fatalClass;
    else if (_fatalClassExpr != null)
      return Util.evalString(_fatalClassExpr);
    else
      return null;
  }

  public void setFatalClass(String fatalClass)
  {
    _fatalClass = fatalClass;
  }
  
  public String getFatalStyle()
  {
    if (_fatalStyle != null)
      return _fatalStyle;
    else if (_fatalStyleExpr != null)
      return Util.evalString(_fatalStyleExpr);
    else
      return null;
  }

  public void setFatalStyle(String fatalStyle)
  {
    _fatalStyle = fatalStyle;
  }
  
  public String getInfoClass()
  {
    if (_infoClass != null)
      return _infoClass;
    else if (_infoClassExpr != null)
      return Util.evalString(_infoClassExpr);
    else
      return null;
  }

  public void setInfoClass(String infoClass)
  {
    _infoClass = infoClass;
  }
  
  public String getInfoStyle()
  {
    if (_infoStyle != null)
      return _infoStyle;
    else if (_infoStyleExpr != null)
      return Util.evalString(_infoStyleExpr);
    else
      return null;
  }

  public void setInfoStyle(String infoStyle)
  {
    _infoStyle = infoStyle;
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

  public String getLayout()
  {
    if (_layout != null)
      return _layout;
    else if (_layoutExpr != null)
      return Util.evalString(_layoutExpr);
    else
      return null;
  }

  public void setLayout(String layout)
  {
    _layout = layout;
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
  
  public String getWarnClass()
  {
    if (_warnClass != null)
      return _warnClass;
    else if (_warnClassExpr != null)
      return Util.evalString(_warnClassExpr);
    else
      return null;
  }

  public void setWarnClass(String warnClass)
  {
    _warnClass = warnClass;
  }
  
  public String getWarnStyle()
  {
    if (_warnStyle != null)
      return _warnStyle;
    else if (_warnStyleExpr != null)
      return Util.evalString(_warnStyleExpr);
    else
      return null;
  }

  public void setWarnStyle(String warnStyle)
  {
    _warnStyle = warnStyle;
  }
  
  public boolean isTooltip()
  {
    if (_tooltip != null)
      return _tooltip;
    else if (_tooltipExpr != null)
      return Util.evalBoolean(_tooltipExpr);
    else
      return false;
  }

  public void setTooltip(boolean tooltip)
  {
    _tooltip = tooltip;
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
      case ERROR_CLASS:
        return _errorClassExpr;
      case ERROR_STYLE:
        return _errorStyleExpr;
      case FATAL_CLASS:
        return _fatalClassExpr;
      case FATAL_STYLE:
        return _fatalStyleExpr;
      case INFO_CLASS:
        return _infoClassExpr;
      case INFO_STYLE:
        return _infoStyleExpr;
      case LANG:
        return _langExpr;
      case LAYOUT:
        return _layoutExpr;
      case STYLE:
        return _styleExpr;
      case STYLE_CLASS:
        return _styleClassExpr;
      case TITLE:
        return _titleExpr;
      case WARN_CLASS:
        return _warnClassExpr;
      case WARN_STYLE:
        return _warnStyleExpr;
      case TOOLTIP:
        return _tooltipExpr;
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

      case ERROR_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _errorClass = Util.evalString(expr);
          return;
        }
        else
          _errorClassExpr = expr;
        break;

      case ERROR_STYLE:
        if (expr != null && expr.isLiteralText()) {
          _errorStyle = Util.evalString(expr);
          return;
        }
        else
          _errorStyleExpr = expr;
        break;

      case FATAL_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _fatalClass = Util.evalString(expr);
          return;
        }
        else
          _fatalClassExpr = expr;
        break;

      case FATAL_STYLE:
        if (expr != null && expr.isLiteralText()) {
          _fatalStyle = Util.evalString(expr);
          return;
        }
        else
          _fatalStyleExpr = expr;
        break;

      case INFO_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _infoClass = Util.evalString(expr);
          return;
        }
        else
          _infoClassExpr = expr;
        break;

      case INFO_STYLE:
        if (expr != null && expr.isLiteralText()) {
          _infoStyle = Util.evalString(expr);
          return;
        }
        else
          _infoStyleExpr = expr;
        break;

      case LANG:
        if (expr != null && expr.isLiteralText()) {
          _lang = Util.evalString(expr);
          return;
        }
        else
          _langExpr = expr;
        break;

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

      case TITLE:
        if (expr != null && expr.isLiteralText()) {
          _title = Util.evalString(expr);
          return;
        }
        else
          _titleExpr = expr;
        break;

      case TOOLTIP:
        if (expr != null && expr.isLiteralText()) {
          _tooltip = Util.evalBoolean(expr);
          return;
        }
        else
          _tooltipExpr = expr;
        break;

      case WARN_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _warnClass = Util.evalString(expr);
          return;
        }
        else
          _warnClassExpr = expr;
        break;

      case WARN_STYLE:
        if (expr != null && expr.isLiteralText()) {
          _warnStyle = Util.evalString(expr);
          return;
        }
        else
          _warnStyleExpr = expr;
        break;
      }
    }

    super.setValueExpression(name, expr);
  }

  //
  // state

  public Object saveState(FacesContext context)
  {
    Object parent = super.saveState(context);

    return new Object[] {
      parent,
      _dir,
      _errorClass,
      _errorStyle,
      _fatalClass,
      _fatalStyle,
      _infoClass,
      _infoStyle,
      _lang,
      _layout,
      _style,
      _styleClass,
      _title,
      _tooltip,
      _warnClass,
      _warnStyle,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    if (state != null) 
      super.restoreState(context, state[0]);

    int i = 1;
    
    _dir = (String) state[i++];
    _errorClass = (String) state[i++];
    _errorStyle = (String) state[i++];
    _fatalClass = (String) state[i++];
    _fatalStyle = (String) state[i++];
    _infoClass = (String) state[i++];
    _infoStyle = (String) state[i++];
    _lang = (String) state[i++];
    _layout = (String) state[i++];
    _style = (String) state[i++];
    _styleClass = (String) state[i++];
    _title = (String) state[i++];
    _tooltip = (Boolean) state[i++];
    _warnClass = (String) state[i++];
    _warnStyle = (String) state[i++];
  }

  //
  // private impl
  //

  private enum PropEnum {
    DIR,
    ERROR_CLASS,
    ERROR_STYLE,
    FATAL_CLASS,
    FATAL_STYLE,
    INFO_CLASS,
    INFO_STYLE,
    LANG,
    LAYOUT,
    STYLE,
    STYLE_CLASS,
    TITLE,
    TOOLTIP,
    WARN_CLASS,
    WARN_STYLE,
  }

  static {
    _propMap.put("dir", PropEnum.DIR);
    _propMap.put("errorClass", PropEnum.ERROR_CLASS);
    _propMap.put("errorStyle", PropEnum.ERROR_STYLE);
    _propMap.put("fatalClass", PropEnum.FATAL_CLASS);
    _propMap.put("fatalStyle", PropEnum.FATAL_STYLE);
    _propMap.put("infoClass", PropEnum.INFO_CLASS);
    _propMap.put("infoStyle", PropEnum.INFO_STYLE);
    _propMap.put("lang", PropEnum.LANG);
    _propMap.put("layout", PropEnum.LAYOUT);
    _propMap.put("style", PropEnum.STYLE);
    _propMap.put("styleClass", PropEnum.STYLE_CLASS);
    _propMap.put("title", PropEnum.TITLE);
    _propMap.put("tooltip", PropEnum.TOOLTIP);
    _propMap.put("warnClass", PropEnum.WARN_CLASS);
    _propMap.put("warnStyle", PropEnum.WARN_STYLE);
  }
}
