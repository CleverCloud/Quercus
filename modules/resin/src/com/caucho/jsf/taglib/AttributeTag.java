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

package com.caucho.jsf.taglib;

import java.io.*;
import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.render.*;
import javax.faces.webapp.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.caucho.util.*;

public class AttributeTag extends TagSupport
{
  private static final L10N L = new L10N(AttributeTag.class);
  
  private ValueExpression _name;
  private ValueExpression _value;

  public void setName(ValueExpression name)
  {
    _name = name;
  }

  public void setValue(ValueExpression value)
  {
    _value = value;
  }

  public int doStartTag()
    throws JspException
  {
    FacesContext context = FacesContext.getCurrentInstance();
    
    UIComponentClassicTagBase parent;
    
    parent = UIComponentClassicTagBase.getParentUIComponentClassicTagBase(this.pageContext);

    if (parent == null)
      throw new JspException(L.l("attribute:actionListener must be nested inside a UIComponent tag."));

    UIComponent parentComp = parent.getComponentInstance();

    String name = (String) _name.getValue(context.getELContext());

    if (_value.isLiteralText()) {
      Object value = _value.getValue(context.getELContext());
    
      parentComp.getAttributes().put(name, value);
    }
    else
      parentComp.setValueExpression(name, _value);
    
    return SKIP_BODY;
  }
}
