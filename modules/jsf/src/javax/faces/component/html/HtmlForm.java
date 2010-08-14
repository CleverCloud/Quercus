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

public class HtmlForm extends UIForm
{
  public static final String COMPONENT_TYPE = "javax.faces.HtmlForm";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _accept;
  private ValueExpression _acceptExpr;
  
  private String _acceptcharset;
  private ValueExpression _acceptcharsetExpr;
  
  private String _dir;
  private ValueExpression _dirExpr;
  
  private String _enctype;
  private ValueExpression _enctypeExpr;
  
  private String _lang;
  private ValueExpression _langExpr;
  
  private String _onclick;
  private ValueExpression _onclickExpr;
  
  private String _ondblclick;
  private ValueExpression _ondblclickExpr;

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

  private String _onreset;
  private ValueExpression _onresetExpr;

  private String _onsubmit;
  private ValueExpression _onsubmitExpr;

  private String _style;
  private ValueExpression _styleExpr;

  private String _styleClass;
  private ValueExpression _styleClassExpr;

  private String _target;
  private ValueExpression _targetExpr;

  private String _title;
  private ValueExpression _titleExpr;

  public HtmlForm()
  {
    setRendererType("javax.faces.Form");
  }

  //
  // properties
  //

  public String getAccept()
  {
    if (_accept != null)
      return _accept;
    else if (_acceptExpr != null)
      return Util.evalString(_acceptExpr);
    else
      return null;
  }

  public void setAccept(String value)
  {
    _accept = value;
  }
  
  public String getAcceptcharset()
  {
    if (_acceptcharset != null)
      return _acceptcharset;
    else if (_acceptcharsetExpr != null)
      return Util.evalString(_acceptcharsetExpr);
    else
      return null;
  }

  public void setAcceptcharset(String value)
  {
    _acceptcharset = value;
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
  
  public String getEnctype()
  {
    if (_enctype != null)
      return _enctype;
    else if (_enctypeExpr != null)
      return Util.evalString(_enctypeExpr);
    else
      return null;
  }

  public void setEnctype(String value)
  {
    _enctype = value;
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
  
  public String getOnreset()
  {
    if (_onreset != null)
      return _onreset;
    else if (_onresetExpr != null)
      return Util.evalString(_onresetExpr);
    else
      return null;
  }

  public void setOnreset(String value)
  {
    _onreset = value;
  }
  
  public String getOnsubmit()
  {
    if (_onsubmit != null)
      return _onsubmit;
    else if (_onsubmitExpr != null)
      return Util.evalString(_onsubmitExpr);
    else
      return null;
  }

  public void setOnsubmit(String value)
  {
    _onsubmit = value;
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
  
  public String getTarget()
  {
    if (_target != null)
      return _target;
    else if (_targetExpr != null)
      return Util.evalString(_targetExpr);
    else
      return null;
  }

  public void setTarget(String value)
  {
    _target = value;
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
      case ACCEPT:
        return _acceptExpr;
      case ACCEPTCHARSET:
        return _acceptcharsetExpr;
      case DIR:
        return _dirExpr;
      case ENCTYPE:
        return _enctypeExpr;
      case LANG:
        return _langExpr;
      case ONCLICK:
        return _onclickExpr;
      case ONDBLCLICK:
        return _ondblclickExpr;
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
      case ONRESET:
        return _onresetExpr;
      case ONSUBMIT:
        return _onsubmitExpr;
      case STYLE:
        return _styleExpr;
      case STYLE_CLASS:
        return _styleClassExpr;
      case TARGET:
        return _targetExpr;
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
      case ACCEPT:
        if (expr != null && expr.isLiteralText()) {
          _accept = Util.evalString(expr);
          return;
        }
        else
          _acceptExpr = expr;
        break;

      case ACCEPTCHARSET:
        if (expr != null && expr.isLiteralText()) {
          _acceptcharset = Util.evalString(expr);
          return;
        }
        else
          _acceptcharsetExpr = expr;
        break;

      case DIR:
        if (expr != null && expr.isLiteralText()) {
          _dir = Util.evalString(expr);
          return;
        }
        else
          _dirExpr = expr;
        break;

      case ENCTYPE:
        if (expr != null && expr.isLiteralText()) {
          _enctype = Util.evalString(expr);
          return;
        }
        else
          _enctypeExpr = expr;
        break;

      case LANG:
        if (expr != null && expr.isLiteralText()) {
          _lang = Util.evalString(expr);
          return;
        }
        else
          _langExpr = expr;
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

      case ONRESET:
        if (expr != null && expr.isLiteralText()) {
          _onreset = Util.evalString(expr);
          return;
        }
        else
          _onresetExpr = expr;
        break;

      case ONSUBMIT:
        if (expr != null && expr.isLiteralText()) {
          _onsubmit = Util.evalString(expr);
          return;
        }
        else
          _onsubmitExpr = expr;
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

      case TARGET:
        if (expr != null && expr.isLiteralText()) {
          _target = Util.evalString(expr);
          return;
        }
        else
          _targetExpr = expr;
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
      _accept,
      _acceptcharset,
      _dir,
      _enctype,
      _lang,
      
      _onclick,
      _ondblclick,
      _onkeydown,
      _onkeypress,
      _onkeyup,
      
      _onmousedown,
      _onmousemove,
      _onmouseout,
      _onmouseover,
      _onmouseup,
      
      _onreset,
      _onsubmit,
      
      _style,
      _styleClass,
      _target,
      _title,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    int i = 0;

    if (state != null) 
      super.restoreState(context, state[i++]);

    _accept = (String) state[i++];
    _acceptcharset = (String) state[i++];
    _dir = (String) state[i++];
    _enctype = (String) state[i++];
    _lang = (String) state[i++];
    _onclick = (String) state[i++];
    _ondblclick = (String) state[i++];
    _onkeydown = (String) state[i++];
    _onkeypress = (String) state[i++];
    _onkeyup = (String) state[i++];
    _onmousedown = (String) state[i++];
    _onmousemove = (String) state[i++];
    _onmouseout = (String) state[i++];
    _onmouseover = (String) state[i++];
    _onmouseup = (String) state[i++];
    _onreset = (String) state[i++];
    _onsubmit = (String) state[i++];
    _style = (String) state[i++];
    _styleClass = (String) state[i++];
    _target = (String) state[i++];
    _title = (String) state[i++];
  }

  //
  // utility
  //

  private enum PropEnum {
    ACCEPT,
    ACCEPTCHARSET,
    DIR,
    ENCTYPE,
    LANG,
    ONCLICK,
    ONDBLCLICK,
    ONKEYDOWN,
    ONKEYPRESS,
    ONKEYUP,
    ONMOUSEDOWN,
    ONMOUSEMOVE,
    ONMOUSEOUT,
    ONMOUSEOVER,
    ONMOUSEUP,
    ONRESET,
    ONSUBMIT,
    STYLE,
    STYLE_CLASS,
    TARGET,
    TITLE,
  }

  static {
    _propMap.put("accept", PropEnum.ACCEPT);
    _propMap.put("acceptcharset", PropEnum.ACCEPTCHARSET);
    _propMap.put("dir", PropEnum.DIR);
    _propMap.put("enctype", PropEnum.ENCTYPE);
    _propMap.put("lang", PropEnum.LANG);
    _propMap.put("onclick", PropEnum.ONCLICK);
    _propMap.put("ondblclick", PropEnum.ONDBLCLICK);
    _propMap.put("onkeydown", PropEnum.ONKEYDOWN);
    _propMap.put("onkeypress", PropEnum.ONKEYPRESS);
    _propMap.put("onkeyup", PropEnum.ONKEYUP);
    _propMap.put("onmousedown", PropEnum.ONMOUSEDOWN);
    _propMap.put("onmousemove", PropEnum.ONMOUSEMOVE);
    _propMap.put("onmouseover", PropEnum.ONMOUSEOVER);
    _propMap.put("onmouseout", PropEnum.ONMOUSEOUT);
    _propMap.put("onmouseup", PropEnum.ONMOUSEUP);
    _propMap.put("onreset", PropEnum.ONRESET);
    _propMap.put("onsubmit", PropEnum.ONSUBMIT);
    _propMap.put("style", PropEnum.STYLE);
    _propMap.put("styleClass", PropEnum.STYLE_CLASS);
    _propMap.put("target", PropEnum.TARGET);
    _propMap.put("title", PropEnum.TITLE);
  }
}
