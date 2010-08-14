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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jstl.el;

import com.caucho.el.Expr;
import com.caucho.jsp.PageContextImpl;
import com.caucho.jstl.ParamContainerTag;

import javax.el.ELContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.ArrayList;

/**
 * Looks up an i18n message from a bundle and prints it.
 */
public class MessageTag extends BodyTagSupport implements ParamContainerTag {
  private Expr _keyExpr;
  private Expr _bundleExpr;

  private ArrayList<Object> _params;

  private String _var;
  private String _scope;

  /**
   * Sets the message key.
   *
   * @param key the JSP-EL expression for the key.
   */
  public void setKey(Expr key)
  {
    _keyExpr = key;
  }

  /**
   * Sets the bundle.
   *
   * @param bundle the JSP-EL expression for the bundle.
   */
  public void setBundle(Expr bundle)
  {
    _bundleExpr = bundle;
  }

  /**
   * Sets the variable.
   *
   * @param var the variable
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope.
   *
   * @param scope the scope
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Add a parameter value to the message.
   */
  public void addParam(Object value)
  {
    if (_params == null)
      _params = new ArrayList<Object>();

    _params.add(value);
  }

  /**
   * Process the tag.
   */
  public int doEndTag()
    throws JspException
  {
    Object []args = null;

    if (_params != null) {
      args = _params.toArray(new Object[_params.size()]);
      _params = null;
    }
    
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      ELContext env = pageContext.getELContext();
      
      JspWriter out = pageContext.getOut();

      String key;

      if (_keyExpr != null)
        key = _keyExpr.evalString(env);
      else
        key = getBodyContent().getString().trim();

      String msg;
      
      if (_bundleExpr != null) {
        Object bundleObject = _bundleExpr.evalObject(env);

        msg = pageContext.getLocalizedMessage(bundleObject, key, args, null);
      }
      else {
        LocalizationContext lc;
        lc = (LocalizationContext) pageContext.getAttribute("caucho.bundle");

        if (lc == null)
          msg = pageContext.getLocalizedMessage(key, args, null);
        else
          msg = pageContext.getLocalizedMessage(lc, key, args, null);
      }

      if (_var != null)
        CoreSetTag.setValue(pageContext, _var, _scope, msg);
      else
        out.print(msg);
    } catch (Exception e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }
}
