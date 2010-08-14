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

package com.caucho.jstl.el;

import com.caucho.el.Expr;
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

public class CoreSetTag extends BodyTagSupport {
  private static L10N L = new L10N(CoreSetTag.class);
  private Expr valueExpr;
  private String var;
  private String scope;
  
  private Expr targetExpr;
  private Expr propertyExpr;

  /**
   * Sets the JSP-EL expression value.
   */
  public void setValue(Expr value)
  {
    this.valueExpr = value;
  }

  /**
   * Sets the variable to assign
   */
  public void setVar(String var)
  {
    this.var = var;
  }

  /**
   * Sets the scope of the variable.
   */
  public void setScope(String scope)
  {
    this.scope = scope;
  }

  /**
   * Sets the target to be set.
   */
  public void setTarget(Expr target)
  {
    this.targetExpr = target;
  }

  /**
   * Sets the property to be set.
   */
  public void setProperty(Expr property)
  {
    this.propertyExpr = property;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    try {
      if (valueExpr == null)
        return EVAL_BODY_BUFFERED;

      ELContext env = pageContext.getELContext();

      Object value = valueExpr.evalObject(env);
      if (var != null)
        setValue(value);
      else
        setProperty(value);

      return SKIP_BODY;
    } catch (Exception e) {
      throw new JspException(e);
    }
  }

  public int doEndTag() throws JspException
  {
    BodyContent body = (BodyContent) getBodyContent();

    if (body != null) {
      if (var != null)
        setValue(body.getString().trim());
      else
        setProperty(body.getString().trim());
    }

    return EVAL_PAGE;
  }

  private void setValue(Object value)
    throws JspException
  {
    if (scope == null) {
      if (value != null)
        pageContext.setAttribute(var, value);
      else
        pageContext.removeAttribute(var);
    }
    else if (scope.equals("page")) {
      if (value != null)
        pageContext.setAttribute(var, value);
      else
        pageContext.removeAttribute(var);
    }
    else if (scope.equals("request")) {
      if (value != null)
        pageContext.getRequest().setAttribute(var, value);
      else
        pageContext.getRequest().removeAttribute(var);
    }
    else if (scope.equals("session")) {
      if (value != null)
        pageContext.getSession().setAttribute(var, value);
      else
        pageContext.getSession().removeAttribute(var);
    }
    else if (scope.equals("application")) {
      if (value != null)
        pageContext.getServletContext().setAttribute(var, value);
      else
        pageContext.getServletContext().removeAttribute(var);
    }
    else
      throw new JspException(L.l("illegal scope value {0}", scope));
  }
  
  private void setProperty(Object value)
    throws JspException
  {
    try {
      ELContext env = pageContext.getELContext();

      Object target = targetExpr.evalObject(env);
      String property = propertyExpr.evalString(env);

      Expr.setProperty(target, property, value);
    } catch (Exception e) {
      throw new JspException(e);
    }
  }

  public static void setValue(PageContext pageContext,
                              String var, String scope, Object value)
    throws JspException
  {
    if (var == null) {
    }
    else if (scope == null || scope.equals("page")) {
      if (value != null)
        pageContext.setAttribute(var, value);
      else
        pageContext.removeAttribute(var);
    }
    else if (scope.equals("request")) {
      if (value != null)
        pageContext.getRequest().setAttribute(var, value);
      else
        pageContext.getRequest().removeAttribute(var);
    }
    else if (scope.equals("session")) {
      if (value != null)
        pageContext.getSession().setAttribute(var, value);
      else
        pageContext.getSession().removeAttribute(var);
    }
    else if (scope.equals("application")) {
      if (value != null)
        pageContext.getServletContext().setAttribute(var, value);
      else
        pageContext.getServletContext().removeAttribute(var);
    }
    else
      throw new JspException(L.l("illegal scope value {0}", scope));
  }
}
