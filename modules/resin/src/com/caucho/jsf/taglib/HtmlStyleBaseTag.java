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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsf.taglib;

import java.io.*;
import java.util.*;

import javax.el.*;

import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.webapp.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * The base for html tags
 */
abstract public class HtmlStyleBaseTag extends UIComponentELTag {
  private HashMap<String,ValueExpression> _map;

  public void setAccept(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("accept", value);
  }

  public void setAccessKey(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("accesskey", value);
  }

  public void setAcceptcharset(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("acceptcharset", value);
  }

  /*
  public void setActionListener(MethodExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("actionListener", value);
  }
  */

  public void setAlt(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("alt", value);
  }

  public void setAutocomplete(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("autocomplete", value);
  }

  public void setBgcolor(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("bgcolor", value);
  }

  /*
  public void setBinding(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("binding", value);
  }
  */

  public void setBorder(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("border", value);
  }

  public void setCaptionClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("captionClass", value);
  }

  public void setCaptionStyle(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("captionStyle", value);
  }

  public void setCellpadding(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("cellpadding", value);
  }

  public void setCellspacing(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("cellspacing", value);
  }

  public void setCharset(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("charset", value);
  }

  public void setColumns(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("columns", value);
  }

  public void setColumnClasses(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("columnClasses", value);
  }

  public void setCols(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("cols", value);
  }

  public void setConverter(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("converter", value);
  }

  public void setCoords(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("coords", value);
  }

  public void setDir(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("dir", value);
  }

  public void setDisabled(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("disabled", value);
  }

  public void setDisabledClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("disabledClass", value);
  }

  public void setEnabledClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("enabledClass", value);
  }

  public void setEnctype(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("enctype", value);
  }

  public void setErrorClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("errorClass", value);
  }

  public void setErrorStyle(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("errorStyle", value);
  }

  public void setFatalClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("fatalClass", value);
  }

  public void setFatalStyle(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("fatalStyle", value);
  }

  public void setInfoClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("infoClass", value);
  }

  public void setInfoStyle(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("infoStyle", value);
  }

  public void setEscape(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("escape", value);
  }

  public void setFooterClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("footerClass", value);
  }

  public void setFor(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("for", value);
  }

  public void setFrame(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("frame", value);
  }

  public void setHeaderClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("headerClass", value);
  }

  public void setHeight(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("height", value);
  }

  public void setHreflang(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("hreflang", value);
  }

  public void setImage(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("image", value);
  }

  public void setIsmap(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("ismap", value);
  }

  public void setLang(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("lang", value);
  }

  public void setLayout(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("layout", value);
  }

  public void setLongdesc(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("longdesc", value);
  }

  public void setMaxlength(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("maxlength", value);
  }

  public void setName(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("name", value);
  }

  public void setOnblur(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onblur", value);
  }

  public void setOnchange(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onchange", value);
  }

  public void setOnclick(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onclick", value);
  }

  public void setOndblclick(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("ondblclick", value);
  }
  
  public void setOnfocus(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onfocus", value);
  }

  public void setOnkeydown(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onkeydown", value);
  }

  public void setOnkeypress(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onkeypress", value);
  }

  public void setOnkeyup(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onkeyup", value);
  }

  public void setOnmousedown(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onmousedown", value);
  }

  public void setOnmousemove(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onmousemove", value);
  }

  public void setOnmouseout(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onmouseout", value);
  }

  public void setOnmouseover(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onmouseover", value);
  }

  public void setOnmouseup(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onmouseup", value);
  }

  public void setOnreset(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onreset", value);
  }

  public void setOnselect(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onselect", value);
  }

  public void setOnsubmit(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("onsubmit", value);
  }

  public void setPrependId(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("prependId", value);
  }

  public void setReadonly(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("readonly", value);
  }

  public void setRedisplay(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("redisplay", value);
  }

  public void setRel(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("rel", value);
  }

  public void setRendered(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("rendered", value);
  }

  public void setRequired(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("required", value);
  }

  public void setRev(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("rev", value);
  }

  public void setRowClasses(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("rowClasses", value);
  }

  public void setRules(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("rules", value);
  }

  public void setRows(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("rows", value);
  }

  public void setShape(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("shape", value);
  }

  public void setShowDetail(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("showDetail", value);
  }

  public void setShowSummary(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("showSummary", value);
  }

  public void setSize(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("size", value);
  }

  public void setStyle(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("style", value);
  }

  public void setStyleClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("styleClass", value);
  }

  public void setSummary(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("summary", value);
  }

  public void setTabindex(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("tabindex", value);
  }

  public void setTarget(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("target", value);
  }

  public void setTitle(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("title", value);
  }

  public void setTooltip(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("tooltip", value);
  }

  public void setType(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("type", value);
  }

  public void setUrl(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("url", value);
  }

  public void setUsemap(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("usemap", value);
  }

  public void setValue(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("value", value);
  }

  public void setWarnClass(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("warnClass", value);
  }

  public void setWarnStyle(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("warnStyle", value);
  }

  public void setWidth(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String,ValueExpression>(8);
      
    _map.put("width", value);
  }
  
  /**
   * Sets the overridden properties of the tag
   */
  @Override
  protected void setProperties(UIComponent component)
  {
    super.setProperties(component);

    if (_map != null) {
      for (Map.Entry<String,ValueExpression> entry : _map.entrySet()) {
        component.setValueExpression(entry.getKey(), entry.getValue());
      }
    }
  }
}
