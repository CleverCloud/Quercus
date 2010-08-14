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
import com.caucho.jsp.PageContextImpl;
import com.caucho.jstl.NameValueTag;
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

/**
 * Looks up an i18n message from a bundle and prints it.
 */
public class CoreParamTag extends BodyTagSupport {
  private static L10N L = new L10N(CoreParamTag.class);
  
  private Expr _nameExpr;
  private Expr _valueExpr;

  /**
   * Sets the name
   *
   * @param name the JSP-EL expression for the name.
   */
  public void setName(Expr name)
  {
    _nameExpr = name;
  }

  /**
   * Sets the value
   *
   * @param value the JSP-EL expression for the value.
   */
  public void setValue(Expr value)
  {
    _valueExpr = value;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    try {
      if (_valueExpr == null)
        return EVAL_BODY_BUFFERED;

      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      ELContext env = pageContext.getELContext();
    
      String name = _nameExpr.evalString(env);

      if (name == null)
        return SKIP_BODY;
      
      String value = _valueExpr.evalString(env);
    
      Tag parent = getParent();
      for (; parent != null; parent = parent.getParent()) {
        if (parent instanceof NameValueTag) {
          NameValueTag tag = (NameValueTag) parent;

          if (value == null)
            tag.addParam(name, "");
          else
            tag.addParam(name, value);

          return SKIP_BODY;
        }
      }
      
      throw new JspException(L.l("c:param requires c:url or c:import parent.")); 
    } catch (JspException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(e);
    }
  }

  /**
   * Process the tag.
   */
  public int doEndTag()
    throws JspException
  {
    try {
      if (_valueExpr != null)
        return EVAL_PAGE;
      
      String value;

      if (bodyContent != null)
        value = bodyContent.getString().trim();
      else
        value = "";
    
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      ELContext env = pageContext.getELContext();
    
      String name = _nameExpr.evalString(env);

      Object parent = getParent();
      if (! (parent instanceof NameValueTag))
        throw new JspException(L.l("c:param requires c:url or c:import parent."));

      if (name == null)
        return EVAL_PAGE;

      NameValueTag tag = (NameValueTag) parent;

      if (value == null)
        tag.addParam(name, "");
      else
        tag.addParam(name, value);
    
      return EVAL_PAGE;
    } catch (Exception e) {
      throw new JspException(e);
    }
  }
}
