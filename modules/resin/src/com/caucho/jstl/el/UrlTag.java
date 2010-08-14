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
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

public class UrlTag extends TagSupport implements NameValueTag {
  private static final L10N L = new L10N(ImportTag.class);

  private Expr _valueExpr;
  private Expr _contextExpr;

  private String _var;
  private String _scope;

  private CharBuffer _url;

  /**
   * Sets the URL to be imported.
   */
  public void setValue(Expr value)
  {
    _valueExpr = value;
  }
  
  /**
   * Sets the external context for the import.
   */
  public void setContext(Expr context)
  {
    _contextExpr = context;
  }

  /**
   * Sets the variable for the import.
   */
  public void setVar(String var)
  {
    _var = var;
  }
  
  /**
   * Sets the scope for the result variable for the output.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Adds a parameter.
   */
  public void addParam(String name, String value)
  {
    String encoding = this.pageContext.getResponse().getCharacterEncoding();
    
    com.caucho.jstl.rt.CoreUrlTag.addParam(_url, name, value, encoding);
  }

  public int doStartTag() throws JspException
  {
    PageContextImpl pageContext = (PageContextImpl) this.pageContext;
  
    String value = _valueExpr.evalString(pageContext.getELContext());
    String context = null;

    if (_contextExpr != null)
      context = _contextExpr.evalString(pageContext.getELContext());
    
    _url = normalizeURL(pageContext, value, context);

    return EVAL_BODY_INCLUDE;
  }
      
  public int doEndTag() throws JspException
  {
    PageContextImpl pageContext = (PageContextImpl) this.pageContext;
  
    String value = encodeURL(pageContext, _url);

    try {
      if (_var == null) {
        JspWriter out = pageContext.getOut();

        out.print(value);
      }
      else
        CoreSetTag.setValue(pageContext, _var, _scope, value);
    } catch (IOException e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }

  public static CharBuffer normalizeURL(PageContext pageContext,
                                        String url, String context)
    throws JspException
  {
    if (url == null)
      url = "";

    CharBuffer value = new CharBuffer();

    int slash = url.indexOf('/');
    int colon = url.indexOf(':');

    if (colon > 0 && colon < slash) {
      value.append(url);
    }
    else if (slash == 0) {
      HttpServletRequest request;
      request = (HttpServletRequest) pageContext.getRequest();

      if (context != null) {
        value.append(context);

        if (context.endsWith("/"))
          value.append(url, 1, url.length() - 1);
        else
          value.append(url);
      }
      else {
        value.append(request.getContextPath());
        value.append(url);
      }
    }
    else {
      if (context != null) {
        value.append(context);
        value.append(url);
      }
      else
        value.append(url);
    }

    return value;
  }

  public static String encodeURL(PageContext pageContext, CharBuffer url)
  {
    String value = url.toString();

    int colon = value.indexOf(':');
    int slash = value.indexOf('/');

    if (colon < slash && slash > 0)
      return value;
    else
      return ((HttpServletResponse) pageContext.getResponse()).encodeURL(value);
    /*
    if (value.startsWith("/"))
      return ((HttpServletResponse) pageContext.getResponse()).encodeURL(value);
    else
      return value;
    */
  }
}
