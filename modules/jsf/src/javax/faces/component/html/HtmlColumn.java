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

public class HtmlColumn extends UIColumn
{
  public static final String COMPONENT_TYPE = "javax.faces.HtmlColumn";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _footerClass;
  private ValueExpression _footerClassExpr;
  
  private String _headerClass;
  private ValueExpression _headerClassExpr;

  public HtmlColumn()
  {
    setRendererType(null);
  }

  //
  // properties
  //
  
  public String getHeaderClass()
  {
    if (_headerClass != null)
      return _headerClass;
    else if (_headerClassExpr != null)
      return Util.evalString(_headerClassExpr);
    else
      return null;
  }

  public void setHeaderClass(String headerClass)
  {
    _headerClass = headerClass;
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

  public void setFooterClass(String footerClass)
  {
    _footerClass = footerClass;
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
      case HEADER_CLASS:
        return _headerClassExpr;
      case FOOTER_CLASS:
        return _footerClassExpr;
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
      case HEADER_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _headerClass = Util.evalString(expr);
          return;
        }
        else
          _headerClassExpr = expr;
        break;

      case FOOTER_CLASS:
        if (expr != null && expr.isLiteralText()) {
          _footerClass = Util.evalString(expr);
          return;
        }
        else
          _footerClassExpr = expr;
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
      _headerClass,
      _footerClass,
    };
  }

  @Override
  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    int i = 0;

    if (state != null) 
      super.restoreState(context, state[i++]);

    _headerClass = (String) state[i++];
    _footerClass = (String) state[i++];
  }

  //
  // private impl
  //

  private enum PropEnum {
    HEADER_CLASS,
    FOOTER_CLASS,
  };

  static {
    _propMap.put("headerClass", PropEnum.HEADER_CLASS);
    _propMap.put("footerClass", PropEnum.FOOTER_CLASS);
  }
}
