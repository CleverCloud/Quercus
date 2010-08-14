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

public class UIGraphic extends UIComponentBase
{
  public static final String COMPONENT_FAMILY = "javax.faces.Graphic";
  public static final String COMPONENT_TYPE = "javax.faces.Graphic";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _url;
  private ValueExpression _urlExpr;
  
  private boolean _isSubmitted;

  public UIGraphic()
  {
    setRendererType("javax.faces.Image");
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

  public String getUrl()
  {
    if (_url != null)
      return _url;
    else if (_urlExpr != null)
      return Util.evalString(_urlExpr, getFacesContext());
    else
      return null;
  }

  public void setUrl(String url)
  {
    _url = url;
  }

  public Object getValue()
  {
    if (_url != null)
      return _url;
    else if (_urlExpr != null)
      return Util.eval(_urlExpr, getFacesContext());
    else
      return null;
  }

  public void setValue(Object url)
  {
    _url = (String) url;
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (_propMap.get(name)) {
      case URL:
      case VALUE:
        return _urlExpr;
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
      case URL:
      case VALUE:
        if (expr != null && expr.isLiteralText()) {
          _url = (String) expr.getValue(null);
          return;
        }
        else
          _urlExpr = expr;
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
    return new Object[] {
      super.saveState(context),
      _url,
    };
  }

  @Override
  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _url = (String) state[1];
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    URL,
    VALUE,
  }

  static {
    _propMap.put("url", PropEnum.URL);
    _propMap.put("value", PropEnum.VALUE);
  }
}
