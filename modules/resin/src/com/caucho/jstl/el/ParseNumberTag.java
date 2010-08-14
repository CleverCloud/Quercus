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
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.el.ELException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Looks up an i18n message from a bundle and prints it.
 */
public class ParseNumberTag extends BodyTagSupport {
  private static L10N L = new L10N(ParseNumberTag.class);
  
  private Expr _valueExpr;
  private Expr _typeExpr;
  
  private Expr _patternExpr;
  
  private Expr _parseLocaleExpr;
  private Expr _integerOnlyExpr;

  private String _var;
  private String _scope;

  /**
   * Sets the formatting value.
   *
   * @param value the JSP-EL expression for the value.
   */
  public void setValue(Expr value)
  {
    _valueExpr = value;
  }

  /**
   * Sets the formatting type.
   *
   * @param type the JSP-EL expression for the type.
   */
  public void setType(Expr type)
  {
    _typeExpr = type;
  }

  /**
   * Sets the number pattern.
   *
   * @param pattern the JSP-EL expression for the number pattern.
   */
  public void setPattern(Expr pattern)
  {
    _patternExpr = pattern;
  }

  /**
   * Sets the parse locale
   *
   * @param locale the JSP-EL expression for the number pattern.
   */
  public void setParseLocale(Expr locale)
  {
    _parseLocaleExpr = locale;
  }

  /**
   * Sets true if integer only parsing.
   *
   * @param integerOnly the JSP-EL expression for the number pattern.
   */
  public void setIntegerOnly(Expr integerOnly)
  {
    _integerOnlyExpr = integerOnly;
  }

  /**
   * Sets the variable name.
   *
   * @param var the variable name to store the value in.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the variable scope.
   *
   * @param scope the variable scope to store the value in.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Process the tag.
   */
  public int doEndTag()
    throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      
      JspWriter out = pageContext.getOut();

      NumberFormat format = getFormat();

      String string;

      if (_valueExpr != null)
        string = _valueExpr.evalString(pageContext.getELContext());
      else
        string = bodyContent.getString().trim();

      Number value = format.parse(string);

      if (_var == null)
        out.print(value);
      else
        CoreSetTag.setValue(pageContext, _var, _scope, value);
    } catch (IOException e) {
    } catch (ParseException e) {
      throw new JspException(e);
    } catch (ELException e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }

  protected NumberFormat getFormat()
    throws JspException, ELException
  {
    PageContextImpl pageContext = (PageContextImpl) this.pageContext;
    ELContext env = pageContext.getELContext();
      
    NumberFormat format = null;

    Locale locale = null;

    if (_parseLocaleExpr != null) {
      Object localeObj = _parseLocaleExpr.evalObject(env);

      if (localeObj instanceof Locale)
        locale = (Locale) localeObj;
      else if (localeObj instanceof String)
        locale = pageContext.getLocale((String) localeObj, null);
    }
    
    if (locale == null)
      locale = pageContext.getLocale();

    String type = null;
    if (_typeExpr != null)
      type = _typeExpr.evalString(env);

    if (type == null || type.equals("") || type.equals("number")) {
      if (locale != null)
        format = NumberFormat.getInstance(locale);
      else
        format = NumberFormat.getInstance();

      DecimalFormat decimalFormat = (DecimalFormat) format;

      if (_patternExpr != null)
        decimalFormat.applyPattern(_patternExpr.evalString(env));
    }
    else if (type.equals("percent")) {
      if (locale != null)
        format = NumberFormat.getPercentInstance(locale);
      else
        format = NumberFormat.getPercentInstance();
    }
    else if (type.equals("currency")) {
      if (locale != null)
        format = NumberFormat.getCurrencyInstance(locale);
      else
        format = NumberFormat.getCurrencyInstance(locale);
    }
    else
      throw new JspException(L.l("unknown formatNumber type `{0}'",
                                 type));

    if (_integerOnlyExpr != null)
      format.setParseIntegerOnly(_integerOnlyExpr.evalBoolean(env));

    return format;
  }
}
