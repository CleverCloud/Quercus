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
import javax.faces.convert.*;

public class UIForm extends UIComponentBase implements NamingContainer
{
  public static final String COMPONENT_FAMILY = "javax.faces.Form";
  public static final String COMPONENT_TYPE = "javax.faces.Form";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private Boolean _isPrependId;
  private ValueExpression _isPrependIdExpr;
  
  private boolean _isSubmitted;

  public UIForm()
  {
    setRendererType("javax.faces.Form");
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

  public boolean isPrependId()
  {
    if (_isPrependId != null)
      return _isPrependId;
    else if (_isPrependIdExpr != null)
      return Util.evalBoolean(_isPrependIdExpr, getFacesContext());
    else
      return true;
  }

  public void setPrependId(boolean value)
  {
    _isPrependId = value;
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("prependId".equals(name)) {
      return _isPrependIdExpr;
    }
    else
      return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("prependId".equals(name)) {
      if (expr != null && expr.isLiteralText()) {
        _isPrependId = Util.booleanValueOf(expr.getValue(null));
        return;
      }
      else
        _isPrependIdExpr = expr;
    }

    super.setValueExpression(name, expr);
  }

  //
  // render properties
  //

  public boolean isSubmitted()
  {
    return _isSubmitted;
  }

  public void setSubmitted(boolean isSubmitted)
  {
    _isSubmitted = isSubmitted;
  }

  /**
   * @Since 1.2
   */
  @Override
  public String getContainerClientId(FacesContext context)
  {
    if (isPrependId())
      return getClientId(context);
    else {
      for (UIComponent comp = getParent();
           comp != null;
           comp = comp.getParent()) {
        if (comp instanceof NamingContainer)
          return comp.getContainerClientId(context);
      }
      
      return null;
    }
  }

  //
  // decode
  //

  /**
   * Recursively calls the decodes for any children, then calls
   * decode().
   */
  public void processDecodes(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;
    
    decode(context);

    if (isSubmitted()) {
      Iterator iter = getFacetsAndChildren();
      while (iter.hasNext()) {
        UIComponent child = (UIComponent) iter.next();

        child.processDecodes(context);
      }
    }
  }

  //
  // validators
  //

  /**
   * Recursively calls the validators for any children, then calls
   * decode().
   */
  public void processValidators(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (isSubmitted()) {
      super.processValidators(context);
    }
  }

  //
  // updates
  //

  /**
   * Recursively calls the updates for any children, then calls
   * update().
   */
  @Override
  public void processUpdates(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (isSubmitted()) {
      super.processUpdates(context);
    }
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    return new Object[] {
      super.saveState(context),
      _isPrependId,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _isPrependId = (Boolean) state[1];
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    PREPEND_ID,
  }

  static {
    _propMap.put("prependId", PropEnum.PREPEND_ID);
  }
}
