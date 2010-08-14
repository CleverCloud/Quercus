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

public class HtmlPanelGrid extends UIPanel
{
  public static final String COMPONENT_TYPE = "javax.faces.HtmlPanelGrid";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _bgcolor;
  private ValueExpression _bgcolorExpr;
  
  private Integer _border;
  private ValueExpression _borderExpr;
  
  private String _captionClass;
  private ValueExpression _captionClassExpr;
  
  private String _captionStyle;
  private ValueExpression _captionStyleExpr;
  
  private String _cellpadding;
  private ValueExpression _cellpaddingExpr;
  
  private String _cellspacing;
  private ValueExpression _cellspacingExpr;
  
  private String _columnClasses;
  private ValueExpression _columnClassesExpr;
  
  private Integer _columns;
  private ValueExpression _columnsExpr;
  
  private String _dir;
  private ValueExpression _dirExpr;
  
  private String _footerClass;
  private ValueExpression _footerClassExpr;
  
  private String _frame;
  private ValueExpression _frameExpr;
  
  private String _headerClass;
  private ValueExpression _headerClassExpr;
  
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

  private String _rowClasses;
  private ValueExpression _rowClassesExpr;

  private String _rules;
  private ValueExpression _rulesExpr;

  private String _style;
  private ValueExpression _styleExpr;

  private String _styleClass;
  private ValueExpression _styleClassExpr;

  private String _summary;
  private ValueExpression _summaryExpr;

  private String _title;
  private ValueExpression _titleExpr;

  private String _width;
  private ValueExpression _widthExpr;

  public HtmlPanelGrid()
  {
    setRendererType("javax.faces.Grid");
  }

  //
  // properties
  //

  public String getBgcolor()
  {
    if (_bgcolor != null)
      return _bgcolor;
    else if (_bgcolorExpr != null)
      return Util.evalString(_bgcolorExpr);
    else
      return null;
  }

  public void setBgcolor(String value)
  {
    _bgcolor = value;
  }

  public int getBorder()
  {
    if (_border != null)
      return _border;
    else if (_borderExpr != null)
      return Util.evalInt(_borderExpr);
    else
      return Integer.MIN_VALUE;
  }

  public void setBorder(int value)
  {
    _border = value;
  }
  
  public String getCaptionClass()
  {
    if (_captionClass != null)
      return _captionClass;
    else if (_captionClassExpr != null)
      return Util.evalString(_captionClassExpr);
    else
      return null;
  }

  public void setCaptionClass(String value)
  {
    _captionClass = value;
  }
  
  public String getCaptionStyle()
  {
    if (_captionStyle != null)
      return _captionStyle;
    else if (_captionStyleExpr != null)
      return Util.evalString(_captionStyleExpr);
    else
      return null;
  }

  public void setCaptionStyle(String value)
  {
    _captionStyle = value;
  }
  
  public String getCellpadding()
  {
    if (_cellpadding != null)
      return _cellpadding;
    else if (_cellpaddingExpr != null)
      return Util.evalString(_cellpaddingExpr);
    else
      return null;
  }

  public void setCellpadding(String value)
  {
    _cellpadding = value;
  }
  
  public String getCellspacing()
  {
    if (_cellspacing != null)
      return _cellspacing;
    else if (_cellspacingExpr != null)
      return Util.evalString(_cellspacingExpr);
    else
      return null;
  }

  public void setCellspacing(String value)
  {
    _cellspacing = value;
  }
  
  public String getColumnClasses()
  {
    if (_columnClasses != null)
      return _columnClasses;
    else if (_columnClassesExpr != null)
      return Util.evalString(_columnClassesExpr);
    else
      return null;
  }

  public void setColumnClasses(String value)
  {
    _columnClasses = value;
  }

  public int getColumns()
  {
    if (_columns != null)
      return _columns;
    else if (_columnsExpr != null)
      return Util.evalInt(_columnsExpr);
    else
      return Integer.MIN_VALUE;
  }

  public void setColumns(int value)
  {
    _columns = value;
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
  
  public String getFooterClass()
  {
    if (_footerClass != null)
      return _footerClass;
    else if (_footerClassExpr != null)
      return Util.evalString(_footerClassExpr);
    else
      return null;
  }

  public void setFooterClass(String value)
  {
    _footerClass = value;
  }
  
  public String getFrame()
  {
    if (_frame != null)
      return _frame;
    else if (_frameExpr != null)
      return Util.evalString(_frameExpr);
    else
      return null;
  }

  public void setFrame(String value)
  {
    _frame = value;
  }
  
  public String getHeaderClass()
  {
    if (_headerClass != null)
      return _headerClass;
    else if (_headerClassExpr != null)
      return Util.evalString(_headerClassExpr);
    else
      return null;
  }

  public void setHeaderClass(String value)
  {
    _headerClass = value;
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
  
  public String getRowClasses()
  {
    if (_rowClasses != null)
      return _rowClasses;
    else if (_rowClassesExpr != null)
      return Util.evalString(_rowClassesExpr);
    else
      return null;
  }

  public void setRowClasses(String value)
  {
    _rowClasses = value;
  }
  
  public String getRules()
  {
    if (_rules != null)
      return _rules;
    else if (_rulesExpr != null)
      return Util.evalString(_rulesExpr);
    else
      return null;
  }

  public void setRules(String value)
  {
    _rules = value;
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
  
  public String getSummary()
  {
    if (_summary != null)
      return _summary;
    else if (_summaryExpr != null)
      return Util.evalString(_summaryExpr);
    else
      return null;
  }

  public void setSummary(String value)
  {
    _summary = value;
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
  
  public String getWidth()
  {
    if (_width != null)
      return _width;
    else if (_widthExpr != null)
      return Util.evalString(_widthExpr);
    else
      return null;
  }

  public void setWidth(String value)
  {
    _width = value;
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
      case BGCOLOR:
        return _bgcolorExpr;
      case BORDER:
        return _borderExpr;
      case CAPTION_CLASS:
        return _captionClassExpr;
      case CAPTION_STYLE:
        return _captionStyleExpr;
      case CELLPADDING:
        return _cellpaddingExpr;
      case CELLSPACING:
        return _cellspacingExpr;
      case COLUMN_CLASSES:
        return _columnClassesExpr;
      case COLUMNS:
        return _columnsExpr;
      case DIR:
        return _dirExpr;
      case FOOTER_CLASS:
        return _footerClassExpr;
      case FRAME:
        return _frameExpr;
      case HEADER_CLASS:
        return _headerClassExpr;
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
      case ROW_CLASSES:
        return _rowClassesExpr;
      case RULES:
        return _rulesExpr;
      case STYLE:
        return _styleExpr;
      case STYLE_CLASS:
        return _styleClassExpr;
      case SUMMARY:
        return _summaryExpr;
      case TITLE:
        return _titleExpr;
      case WIDTH:
        return _widthExpr;
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
      case BGCOLOR:
        if (expr != null && expr.isLiteralText()) {
          _bgcolor = Util.evalString(expr);
          return;
        }
        else
          _bgcolorExpr = expr;
        break;

      case BORDER:
        if (expr != null && expr.isLiteralText()) {
          _border = Util.evalInt(expr);
          return;
        }
        else
          _borderExpr = expr;
        break;

      case CAPTION_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _captionClass = Util.evalString(expr);
          return;
        }
        else
          _captionClassExpr = expr;
        break;

      case CAPTION_STYLE:
        if (expr != null && expr.isLiteralText()) {
          _captionStyle = Util.evalString(expr);
          return;
        }
        else
          _captionStyleExpr = expr;
        break;

      case CELLPADDING:
        if (expr != null && expr.isLiteralText()) {
          _cellpadding = Util.evalString(expr);
          return;
        }
        else
          _cellpaddingExpr = expr;
        break;

      case CELLSPACING:
        if (expr != null && expr.isLiteralText()) {
          _cellspacing = Util.evalString(expr);
          return;
        }
        else
          _cellspacingExpr = expr;
        break;

      case COLUMN_CLASSES:
        if (expr != null && expr.isLiteralText()) {
          _columnClasses = Util.evalString(expr);
          return;
        }
        else
          _columnClassesExpr = expr;
        break;

      case COLUMNS:
        if (expr != null && expr.isLiteralText()) {
          _columns = Util.evalInt(expr);
          return;
        }
        else
          _columnsExpr = expr;
        break;

      case DIR:
        if (expr != null && expr.isLiteralText()) {
          _dir = Util.evalString(expr);
          return;
        }
        else
          _dirExpr = expr;
        break;

      case FOOTER_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _footerClass = Util.evalString(expr);
          return;
        }
        else
          _footerClassExpr = expr;
        break;

      case FRAME:
        if (expr != null && expr.isLiteralText()) {
          _frame = Util.evalString(expr);
          return;
        }
        else
          _frameExpr = expr;
        break;

      case HEADER_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _headerClass = Util.evalString(expr);
          return;
        }
        else
          _headerClassExpr = expr;
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

      case ROW_CLASSES:
        if (expr != null && expr.isLiteralText()) {
          _rowClasses = Util.evalString(expr);
          return;
        }
        else
          _rowClassesExpr = expr;
        break;

      case RULES:
        if (expr != null && expr.isLiteralText()) {
          _rules = Util.evalString(expr);
          return;
        }
        else
          _rulesExpr = expr;
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

      case SUMMARY:
        if (expr != null && expr.isLiteralText()) {
          _summary = Util.evalString(expr);
          return;
        }
        else
          _summaryExpr = expr;
        break;

      case TITLE:
        if (expr != null && expr.isLiteralText()) {
          _title = Util.evalString(expr);
          return;
        }
        else
          _titleExpr = expr;
        break;

      case WIDTH:
        if (expr != null && expr.isLiteralText()) {
          _width = Util.evalString(expr);
          return;
        }
        else
          _widthExpr = expr;
        break;
      }
    }
    
    super.setValueExpression(name, expr);
  }

  //
  // state
  //

  @Override
  public Object saveState(FacesContext context)
  {
    Object parent = super.saveState(context);

    return new Object[] {
      parent,
      _bgcolor,
      _border,
      _captionClass,
      _captionStyle,
      _cellpadding,
      _cellspacing,
      _columnClasses,
      _columns,
      _dir,
      _footerClass,
      _frame,
      _headerClass,
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
      
      _rowClasses,
      _rules,
      
      _style,
      _styleClass,
      _summary,
      _title,
      _width,
    };
  }

  @Override
  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    int i = 0;

    if (state != null) 
      super.restoreState(context, state[i++]);

    _bgcolor = (String) state[i++];
    _border = (Integer) state[i++];
    _captionClass = (String) state[i++];
    _captionStyle = (String) state[i++];
    _cellpadding = (String) state[i++];
    _cellspacing = (String) state[i++];
    _columnClasses = (String) state[i++];
    _columns = (Integer) state[i++];
    _dir = (String) state[i++];
    _footerClass = (String) state[i++];
    _frame = (String) state[i++];
    _headerClass = (String) state[i++];
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
    _rowClasses = (String) state[i++];
    _rules = (String) state[i++];
    _style = (String) state[i++];
    _styleClass = (String) state[i++];
    _summary = (String) state[i++];
    _title = (String) state[i++];
    _width = (String) state[i++];
  }

  //
  // utility
  //

  private enum PropEnum {
    BGCOLOR,
    BORDER,
    CAPTION_CLASS,
    CAPTION_STYLE,
    CELLPADDING,
    CELLSPACING,
    COLUMN_CLASSES,
    COLUMNS,
    DIR,
    FOOTER_CLASS,
    FRAME,
    HEADER_CLASS,
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
    ROW_CLASSES,
    RULES,
    STYLE,
    STYLE_CLASS,
    SUMMARY,
    TITLE,
    WIDTH,
  }

  static {
    _propMap.put("bgcolor", PropEnum.BGCOLOR);
    _propMap.put("border", PropEnum.BORDER);
    _propMap.put("captionClass", PropEnum.CAPTION_CLASS);
    _propMap.put("captionStyle", PropEnum.CAPTION_STYLE);
    _propMap.put("cellpadding", PropEnum.CELLPADDING);
    _propMap.put("cellspacing", PropEnum.CELLSPACING);
    _propMap.put("columnClasses", PropEnum.COLUMN_CLASSES);
    _propMap.put("columns", PropEnum.COLUMNS);
    _propMap.put("dir", PropEnum.DIR);
    _propMap.put("footerClass", PropEnum.FOOTER_CLASS);
    _propMap.put("frame", PropEnum.FRAME);
    _propMap.put("headerClass", PropEnum.HEADER_CLASS);
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
    _propMap.put("rowClasses", PropEnum.ROW_CLASSES);
    _propMap.put("rules", PropEnum.RULES);
    _propMap.put("style", PropEnum.STYLE);
    _propMap.put("styleClass", PropEnum.STYLE_CLASS);
    _propMap.put("summary", PropEnum.SUMMARY);
    _propMap.put("title", PropEnum.TITLE);
    _propMap.put("width", PropEnum.WIDTH);
  }
}
