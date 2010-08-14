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

package com.caucho.jstl.rt;

import com.caucho.el.Expr;
import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.util.Locale;

public class CoreSetTag extends BodyTagSupport {
  private static L10N L = new L10N(CoreSetTag.class);
  
  private Object _value;
  private boolean _hasValue;
  private String _var;
  private String _scope;
  
  private Object _target;
  private String _property;

  /**
   * Sets the JSP-EL expression value.
   */
  public void setValue(Object value)
  {
    _value = value;
    _hasValue = true;
  }

  /**
   * Sets the variable to assign
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope of the variable.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Sets the target to be set.
   */
  public void setTarget(Object target)
  {
    _target = target;
  }

  /**
   * Sets the property to be set.
   */
  public void setProperty(String property)
  {
    _property = property;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    if (! _hasValue)
      return EVAL_BODY_BUFFERED;

    if (_value instanceof ValueExpression && _var != null)
      doMapVariable((ValueExpression) _value);
    else if (_var != null)
      doSetValue(_value);
    else
      doSetProperty(_value);

    return SKIP_BODY;
  }

  public int doEndTag() throws JspException
  {
    BodyContent body = (BodyContent) getBodyContent();

    if (body != null) {
      String value = body.getString().trim();
      
      if (_var != null)
        doSetValue(value);
      else
        doSetProperty(value);
    }

    return EVAL_PAGE;
  }

  private void doMapVariable(ValueExpression valueExpr) {
    VariableMapper mapper = pageContext.getELContext().getVariableMapper();

    mapper.setVariable(_var, valueExpr);
  }

  private void doSetValue(Object value)
    throws JspException
  {
    setValue(pageContext, _var, _scope, value);
  }
  
  private void doSetProperty(Object value)
    throws JspException
  {
    Expr.setProperty(_target, _property, value);
  }

  public static void setValue(PageContext pageContext,
                              String var, String scope, Object value)
    throws JspException
  {
    if (var == null) {
      if (scope != null && ! "".equals(scope))
        throw new JspException(L.l("var must not be null when scope '{0}' is set.",
                                   scope));
    }
    else if ("".equals(var)) {
      throw new JspException(L.l("var must not be ''"));
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
