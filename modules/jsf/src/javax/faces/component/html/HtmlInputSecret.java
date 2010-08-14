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

public class HtmlInputSecret extends UIInput
{
  public static final String COMPONENT_TYPE = "javax.faces.HtmlInputSecret";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _accesskey;
  private ValueExpression _accesskeyExpr;
  
  private String _alt;
  private ValueExpression _altExpr;
  
  private String _autocomplete;
  private ValueExpression _autocompleteExpr;
  
  private String _dir;
  private ValueExpression _dirExpr;
  
  private Boolean _disabled;
  private ValueExpression _disabledExpr;
  
  private String _label;
  private ValueExpression _labelExpr;
  
  private String _lang;
  private ValueExpression _langExpr;
  
  private Integer _maxlength;
  private ValueExpression _maxlengthExpr;
  
  private String _onblur;
  private ValueExpression _onblurExpr;
  
  private String _onchange;
  private ValueExpression _onchangeExpr;
  
  private String _onclick;
  private ValueExpression _onclickExpr;
  
  private String _ondblclick;
  private ValueExpression _ondblclickExpr;
  
  private String _onfocus;
  private ValueExpression _onfocusExpr;

  private String _onkeydown;
  private ValueExpression _onkeydownExpr;

  private String _onkeypress;
  private ValueExpression _onkeypressExpr;

  private String _onkeyup;
  private ValueExpression _onkeyupExpr;

  private String _onmousedown;
  private ValueExpression _onmousedownExpr;

  private String _onmousemove;
  private ValueExpression _onmousemoveExpr;

  private String _onmouseout;
  private ValueExpression _onmouseoutExpr;

  private String _onmouseover;
  private ValueExpression _onmouseoverExpr;

  private String _onmouseup;
  private ValueExpression _onmouseupExpr;

  private String _onselect;
  private ValueExpression _onselectExpr;
  
  private Boolean _readonly;
  private ValueExpression _readonlyExpr;
  
  private Boolean _redisplay;
  private ValueExpression _redisplayExpr;

  private Integer _size;
  private ValueExpression _sizeExpr;

  private String _style;
  private ValueExpression _styleExpr;

  private String _styleClass;
  private ValueExpression _styleClassExpr;

  private String _tabindex;
  private ValueExpression _tabindexExpr;

  private String _title;
  private ValueExpression _titleExpr;

  public HtmlInputSecret()
  {
    setRendererType("javax.faces.Secret");
  }

  //
  // properties
  //

  public String getAccesskey()
  {
    if (_accesskey != null)
      return _accesskey;
    else if (_accesskeyExpr != null)
      return Util.evalString(_accesskeyExpr);
    else
      return null;
  }

  public void setAccesskey(String value)
  {
    _accesskey = value;
  }
  
  public String getAlt()
  {
    if (_alt != null)
      return _alt;
    else if (_altExpr != null)
      return Util.evalString(_altExpr);
    else
      return null;
  }

  public void setAlt(String value)
  {
    _alt = value;
  }
  
  public String getDir()
  {
    if (_dir != null)
      return _dir;
    else if (_dirExpr != null)
      return Util.evalString(_dirExpr);
    else
      return null;
  }

  public void setDir(String value)
  {
    _dir = value;
  }
  
  public boolean isDisabled()
  {
    if (_disabled != null)
      return _disabled;
    else if (_disabledExpr != null)
      return Util.evalBoolean(_disabledExpr);
    else
      return false;
  }

  public void setDisabled(boolean value)
  {
    _disabled = value;
  }
  
  public String getAutocomplete()
  {
    if (_autocomplete != null)
      return _autocomplete;
    else if (_autocompleteExpr != null)
      return Util.evalString(_autocompleteExpr);
    else
      return null;
  }

  public void setAutocomplete(String value)
  {
    _autocomplete = value;
  }
  
  public String getLabel()
  {
    if (_label != null)
      return _label;
    else if (_labelExpr != null)
      return Util.evalString(_labelExpr);
    else
      return null;
  }

  public void setLabel(String value)
  {
    _label = value;
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

  public void setLang(String value)
  {
    _lang = value;
  }
  
  public int getMaxlength()
  {
    if (_maxlength != null)
      return _maxlength;
    else if (_maxlengthExpr != null)
      return Util.evalInt(_maxlengthExpr);
    else
      return Integer.MIN_VALUE;
  }

  public void setMaxlength(int value)
  {
    _maxlength = value;
  }
  
  public String getOnblur()
  {
    if (_onblur != null)
      return _onblur;
    else if (_onblurExpr != null)
      return Util.evalString(_onblurExpr);
    else
      return null;
  }

  public void setOnblur(String value)
  {
    _onblur = value;
  }
  
  public String getOnchange()
  {
    if (_onchange != null)
      return _onchange;
    else if (_onchangeExpr != null)
      return Util.evalString(_onchangeExpr);
    else
      return null;
  }

  public void setOnchange(String value)
  {
    _onchange = value;
  }
  
  public String getOnclick()
  {
    if (_onclick != null)
      return _onclick;
    else if (_onclickExpr != null)
      return Util.evalString(_onclickExpr);
    else
      return null;
  }

  public void setOnclick(String value)
  {
    _onclick = value;
  }
  
  public String getOndblclick()
  {
    if (_ondblclick != null)
      return _ondblclick;
    else if (_ondblclickExpr != null)
      return Util.evalString(_ondblclickExpr);
    else
      return null;
  }

  public void setOndblclick(String value)
  {
    _ondblclick = value;
  }
  
  public String getOnfocus()
  {
    if (_onfocus != null)
      return _onfocus;
    else if (_onfocusExpr != null)
      return Util.evalString(_onfocusExpr);
    else
      return null;
  }

  public void setOnfocus(String value)
  {
    _onfocus = value;
  }
  
  public String getOnkeydown()
  {
    if (_onkeydown != null)
      return _onkeydown;
    else if (_onkeydownExpr != null)
      return Util.evalString(_onkeydownExpr);
    else
      return null;
  }

  public void setOnkeydown(String value)
  {
    _onkeydown = value;
  }
  
  public String getOnkeypress()
  {
    if (_onkeypress != null)
      return _onkeypress;
    else if (_onkeypressExpr != null)
      return Util.evalString(_onkeypressExpr);
    else
      return null;
  }

  public void setOnkeypress(String value)
  {
    _onkeypress = value;
  }
  
  public String getOnkeyup()
  {
    if (_onkeyup != null)
      return _onkeyup;
    else if (_onkeyupExpr != null)
      return Util.evalString(_onkeyupExpr);
    else
      return null;
  }

  public void setOnkeyup(String value)
  {
    _onkeyup = value;
  }
  
  public String getOnmousedown()
  {
    if (_onmousedown != null)
      return _onmousedown;
    else if (_onmousedownExpr != null)
      return Util.evalString(_onmousedownExpr);
    else
      return null;
  }

  public void setOnmousedown(String value)
  {
    _onmousedown = value;
  }
  
  public String getOnmousemove()
  {
    if (_onmousemove != null)
      return _onmousemove;
    else if (_onmousemoveExpr != null)
      return Util.evalString(_onmousemoveExpr);
    else
      return null;
  }

  public void setOnmousemove(String value)
  {
    _onmousemove = value;
  }
  
  public String getOnmouseout()
  {
    if (_onmouseout != null)
      return _onmouseout;
    else if (_onmouseoutExpr != null)
      return Util.evalString(_onmouseoutExpr);
    else
      return null;
  }

  public void setOnmouseout(String value)
  {
    _onmouseout = value;
  }
  
  public String getOnmouseover()
  {
    if (_onmouseover != null)
      return _onmouseover;
    else if (_onmouseoverExpr != null)
      return Util.evalString(_onmouseoverExpr);
    else
      return null;
  }

  public void setOnmouseover(String value)
  {
    _onmouseover = value;
  }
  
  public String getOnmouseup()
  {
    if (_onmouseup != null)
      return _onmouseup;
    else if (_onmouseupExpr != null)
      return Util.evalString(_onmouseupExpr);
    else
      return null;
  }

  public void setOnmouseup(String value)
  {
    _onmouseup = value;
  }
  
  public String getOnselect()
  {
    if (_onselect != null)
      return _onselect;
    else if (_onselectExpr != null)
      return Util.evalString(_onselectExpr);
    else
      return null;
  }

  public void setOnselect(String value)
  {
    _onselect = value;
  }
  
  public boolean isReadonly()
  {
    if (_readonly != null)
      return _readonly;
    else if (_readonlyExpr != null)
      return Util.evalBoolean(_readonlyExpr);
    else
      return false;
  }

  public void setReadonly(boolean value)
  {
    _readonly = value;
  }
  
  public boolean isRedisplay()
  {
    if (_redisplay != null)
      return _redisplay;
    else if (_redisplayExpr != null)
      return Util.evalBoolean(_redisplayExpr);
    else
      return false;
  }

  public void setRedisplay(boolean value)
  {
    _redisplay = value;
  }
  
  public int getSize()
  {
    if (_size != null)
      return _size;
    else if (_sizeExpr != null)
      return Util.evalInt(_sizeExpr);
    else
      return Integer.MIN_VALUE;
  }

  public void setSize(int value)
  {
    _size = value;
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
  
  public String getTabindex()
  {
    if (_tabindex != null)
      return _tabindex;
    else if (_tabindexExpr != null)
      return Util.evalString(_tabindexExpr);
    else
      return null;
  }

  public void setTabindex(String value)
  {
    _tabindex = value;
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

  public void setTitle(String value)
  {
    _title = value;
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
      case ACCESSKEY:
        return _accesskeyExpr;
      case ALT:
        return _altExpr;
      case AUTOCOMPLETE:
        return _autocompleteExpr;
      case DIR:
        return _dirExpr;
      case DISABLED:
        return _disabledExpr;
      case LABEL:
        return _labelExpr;
      case LANG:
        return _langExpr;
      case MAXLENGTH:
        return _maxlengthExpr;
      case ONBLUR:
        return _onblurExpr;
      case ONCHANGE:
        return _onchangeExpr;
      case ONCLICK:
        return _onclickExpr;
      case ONDBLCLICK:
        return _ondblclickExpr;
      case ONFOCUS:
        return _onfocusExpr;
      case ONKEYDOWN:
        return _onkeydownExpr;
      case ONKEYPRESS:
        return _onkeypressExpr;
      case ONKEYUP:
        return _onkeyupExpr;
      case ONMOUSEDOWN:
        return _onmousedownExpr;
      case ONMOUSEMOVE:
        return _onmousemoveExpr;
      case ONMOUSEOUT:
        return _onmouseoutExpr;
      case ONMOUSEOVER:
        return _onmouseoverExpr;
      case ONMOUSEUP:
        return _onmouseupExpr;
      case ONSELECT:
        return _onselectExpr;
      case READONLY:
        return _readonlyExpr;
      case REDISPLAY:
        return _redisplayExpr;
      case SIZE:
        return _sizeExpr;
      case STYLE:
        return _styleExpr;
      case STYLE_CLASS:
        return _styleClassExpr;
      case TABINDEX:
        return _tabindexExpr;
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
      case ACCESSKEY:
        if (expr != null && expr.isLiteralText()) {
          _accesskey = Util.evalString(expr);
          return;
        }
        else
          _accesskeyExpr = expr;
        break;

      case ALT:
        if (expr != null && expr.isLiteralText()) {
          _alt = Util.evalString(expr);
          return;
        }
        else
          _altExpr = expr;
        break;

      case AUTOCOMPLETE:
        if (expr != null && expr.isLiteralText()) {
          _autocomplete = Util.evalString(expr);
          return;
        }
        else
          _autocompleteExpr = expr;
        break;

      case DIR:
        if (expr != null && expr.isLiteralText()) {
          _dir = Util.evalString(expr);
          return;
        }
        else
          _dirExpr = expr;
        break;

      case DISABLED:
        if (expr != null && expr.isLiteralText()) {
          _disabled = Util.evalBoolean(expr);
          return;
        }
        else
          _disabledExpr = expr;
        break;

      case LABEL:
        if (expr != null && expr.isLiteralText()) {
          _label = Util.evalString(expr);
          return;
        }
        else
          _labelExpr = expr;
        break;

      case LANG:
        if (expr != null && expr.isLiteralText()) {
          _lang = Util.evalString(expr);
          return;
        }
        else
          _langExpr = expr;
        break;

      case MAXLENGTH:
        if (expr != null && expr.isLiteralText()) {
          _maxlength = Util.evalInt(expr);
          return;
        }
        else
          _maxlengthExpr = expr;
        break;

      case ONBLUR:
        if (expr != null && expr.isLiteralText()) {
          _onblur = Util.evalString(expr);
          return;
        }
        else
          _onblurExpr = expr;
        break;

      case ONCHANGE:
        if (expr != null && expr.isLiteralText()) {
          _onchange = Util.evalString(expr);
          return;
        }
        else
          _onchangeExpr = expr;
        break;

      case ONCLICK:
        if (expr != null && expr.isLiteralText()) {
          _onclick = Util.evalString(expr);
          return;
        }
        else
          _onclickExpr = expr;
        break;

      case ONDBLCLICK:
        if (expr != null && expr.isLiteralText()) {
          _ondblclick = Util.evalString(expr);
          return;
        }
        else
          _ondblclickExpr = expr;
        break;

      case ONFOCUS:
        if (expr != null && expr.isLiteralText()) {
          _onfocus = Util.evalString(expr);
          return;
        }
        else
          _onfocusExpr = expr;
        break;

      case ONKEYDOWN:
        if (expr != null && expr.isLiteralText()) {
          _onkeydown = Util.evalString(expr);
          return;
        }
        else
          _onkeydownExpr = expr;
        break;

      case ONKEYPRESS:
        if (expr != null && expr.isLiteralText()) {
          _onkeypress = Util.evalString(expr);
          return;
        }
        else
          _onkeypressExpr = expr;
        break;

      case ONKEYUP:
        if (expr != null && expr.isLiteralText()) {
          _onkeyup = Util.evalString(expr);
          return;
        }
        else
          _onkeyupExpr = expr;
        break;

      case ONMOUSEDOWN:
        if (expr != null && expr.isLiteralText()) {
          _onmousedown = Util.evalString(expr);
          return;
        }
        else
          _onmousedownExpr = expr;
        break;

      case ONMOUSEMOVE:
        if (expr != null && expr.isLiteralText()) {
          _onmousemove = Util.evalString(expr);
          return;
        }
        else
          _onmousemoveExpr = expr;
        break;

      case ONMOUSEOUT:
        if (expr != null && expr.isLiteralText()) {
          _onmouseout = Util.evalString(expr);
          return;
        }
        else
          _onmouseoutExpr = expr;
        break;

      case ONMOUSEOVER:
        if (expr != null && expr.isLiteralText()) {
          _onmouseover = Util.evalString(expr);
          return;
        }
        else
          _onmouseoverExpr = expr;
        break;

      case ONMOUSEUP:
        if (expr != null && expr.isLiteralText()) {
          _onmouseup = Util.evalString(expr);
          return;
        }
        else
          _onmouseupExpr = expr;
        break;

      case ONSELECT:
        if (expr != null && expr.isLiteralText()) {
          _onselect = Util.evalString(expr);
          return;
        }
        else
          _onselectExpr = expr;
        break;

      case READONLY:
        if (expr != null && expr.isLiteralText()) {
          _readonly = Util.evalBoolean(expr);
          return;
        }
        else
          _readonlyExpr = expr;
        break;

      case REDISPLAY:
        if (expr != null && expr.isLiteralText()) {
          _redisplay = Util.evalBoolean(expr);
          return;
        }
        else
          _redisplayExpr = expr;
        break;

      case SIZE:
        if (expr != null && expr.isLiteralText()) {
          _size = Util.evalInt(expr);
          return;
        }
        else
          _sizeExpr = expr;
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

      case TABINDEX:
        if (expr != null && expr.isLiteralText()) {
          _tabindex = Util.evalString(expr);
          return;
        }
        else
          _tabindexExpr = expr;
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
      
      _accesskey,
      _alt,
      _autocomplete,
      _dir,
      _disabled,
      _label,
      _lang,
      _maxlength,
      
      _onblur,
      _onchange,
      _onclick,
      _ondblclick,
      _onfocus,
      
      _onkeydown,
      _onkeypress,
      _onkeyup,
      
      _onmousedown,
      _onmousemove,
      _onmouseout,
      _onmouseover,
      _onmouseup,
      
      _onselect,
      
      _readonly,
      _redisplay,
      _size,
      _style,
      _styleClass,
      _tabindex,
      _title,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    int i = 0;

    if (state != null) 
      super.restoreState(context, state[i++]);

    _accesskey = (String) state[i++];
    _alt = (String) state[i++];
    _autocomplete = (String) state[i++];
    _dir = (String) state[i++];
    _disabled = (Boolean) state[i++];
    _label = (String) state[i++];
    _lang = (String) state[i++];
    _maxlength = (Integer) state[i++];
    _onblur = (String) state[i++];
    _onchange = (String) state[i++];
    _onclick = (String) state[i++];
    _ondblclick = (String) state[i++];
    _onfocus = (String) state[i++];
    _onkeydown = (String) state[i++];
    _onkeypress = (String) state[i++];
    _onkeyup = (String) state[i++];
    _onmousedown = (String) state[i++];
    _onmousemove = (String) state[i++];
    _onmouseout = (String) state[i++];
    _onmouseover = (String) state[i++];
    _onmouseup = (String) state[i++];
    _onselect = (String) state[i++];
    _readonly = (Boolean) state[i++];
    _redisplay = (Boolean) state[i++];
    _size = (Integer) state[i++];
    _style = (String) state[i++];
    _styleClass = (String) state[i++];
    _tabindex = (String) state[i++];
    _title = (String) state[i++];
  }

  //
  // utility
  //

  private enum PropEnum {
    ACCESSKEY,
    ALT,
    AUTOCOMPLETE,
    DIR,
    DISABLED,
    LABEL,
    LANG,
    MAXLENGTH,
    ONBLUR,
    ONCHANGE,
    ONCLICK,
    ONDBLCLICK,
    ONFOCUS,
    ONKEYDOWN,
    ONKEYPRESS,
    ONKEYUP,
    ONMOUSEDOWN,
    ONMOUSEMOVE,
    ONMOUSEOUT,
    ONMOUSEOVER,
    ONMOUSEUP,
    ONSELECT,
    READONLY,
    REDISPLAY,
    SIZE,
    STYLE,
    STYLE_CLASS,
    TABINDEX,
    TITLE,
  }

  static {
    _propMap.put("accesskey", PropEnum.ACCESSKEY);
    _propMap.put("alt", PropEnum.ALT);
    _propMap.put("autocomplete", PropEnum.AUTOCOMPLETE);
    _propMap.put("dir", PropEnum.DIR);
    _propMap.put("disabled", PropEnum.DISABLED);
    _propMap.put("label", PropEnum.LABEL);
    _propMap.put("lang", PropEnum.LANG);
    _propMap.put("maxlength", PropEnum.MAXLENGTH);
    _propMap.put("onblur", PropEnum.ONBLUR);
    _propMap.put("onchange", PropEnum.ONCHANGE);
    _propMap.put("onclick", PropEnum.ONCLICK);
    _propMap.put("ondblclick", PropEnum.ONDBLCLICK);
    _propMap.put("onfocus", PropEnum.ONFOCUS);
    _propMap.put("onkeydown", PropEnum.ONKEYDOWN);
    _propMap.put("onkeypress", PropEnum.ONKEYPRESS);
    _propMap.put("onkeyup", PropEnum.ONKEYUP);
    _propMap.put("onmousedown", PropEnum.ONMOUSEDOWN);
    _propMap.put("onmousemove", PropEnum.ONMOUSEMOVE);
    _propMap.put("onmouseover", PropEnum.ONMOUSEOVER);
    _propMap.put("onmouseout", PropEnum.ONMOUSEOUT);
    _propMap.put("onmouseup", PropEnum.ONMOUSEUP);
    _propMap.put("onselect", PropEnum.ONSELECT);
    _propMap.put("readonly", PropEnum.READONLY);
    _propMap.put("redisplay", PropEnum.REDISPLAY);
    _propMap.put("size", PropEnum.SIZE);
    _propMap.put("style", PropEnum.STYLE);
    _propMap.put("styleClass", PropEnum.STYLE_CLASS);
    _propMap.put("tabindex", PropEnum.TABINDEX);
    _propMap.put("title", PropEnum.TITLE);
  }
}
