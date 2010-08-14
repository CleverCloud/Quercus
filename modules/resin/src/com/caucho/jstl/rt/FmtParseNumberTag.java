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

import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;

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
public class FmtParseNumberTag extends BodyTagSupport {
  private static L10N L = new L10N(FmtParseNumberTag.class);
  
  private String _value;
  private String _type;
  
  private String _pattern;
  
  private Object _parseLocale;
  private boolean _integerOnly = false;

  private String _var;
  private String _scope;

  /**
   * Sets the formatting value.
   *
   * @param value the value.
   */
  public void setValue(String value)
  {
    _value = value;
  }

  /**
   * Sets the formatting type.
   *
   * @param type the type.
   */
  public void setType(String type)
  {
    _type = type;
  }

  /**
   * Sets the number pattern.
   *
   * @param pattern the number pattern.
   */
  public void setPattern(String pattern)
  {
    _pattern = pattern;
  }

  /**
   * Sets the parse locale
   *
   * @param locale the locale
   */
  public void setParseLocale(Object locale)
  {
    _parseLocale = locale;
  }

  /**
   * Sets true if integer only parsing.
   *
   * @param integerOnly the JSP-EL expression for the number pattern.
   */
  public void setIntegerOnly(boolean integerOnly)
  {
    _integerOnly = integerOnly;
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
      PageContextImpl pc = (PageContextImpl) pageContext;
      
      JspWriter out = pc.getOut();

      NumberFormat format = getFormat();

      String string;

      if (_value != null)
        string = _value;
      else if (bodyContent != null)
        string = bodyContent.getString().trim();
      else
        string = null;

      Number value = null;

      if (string != null && ! "".equals(string))
        value = format.parse(string);

      if (_var == null) {
        if (_scope != null)
          throw new JspException(L.l("fmt:parseNumber var must not be null when scope '{0}' is set.",
                                     _scope));

        if (value != null)
          out.print(value);
      }
      else
        CoreSetTag.setValue(pageContext, _var, _scope, value);
    } catch (IOException e) {
    } catch (ParseException e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }

  protected NumberFormat getFormat()
    throws JspException
  {
    PageContextImpl pc = (PageContextImpl) pageContext;
      
    NumberFormat format = null;

    Locale locale = null;

    if (_parseLocale != null) {
      Object localeObj = _parseLocale;

      if (localeObj instanceof Locale)
        locale = (Locale) localeObj;
      else if (localeObj instanceof String)
        locale = pc.getLocale((String) localeObj, null);
    }
    
    if (locale == null)
      locale = pc.getLocale();

    if (_type == null || _type.equals("") || _type.equals("number")) {
      if (locale != null)
        format = NumberFormat.getInstance(locale);
      else
        format = NumberFormat.getInstance();
    }
    else if (_type.equals("percent")) {
      if (locale != null)
        format = NumberFormat.getPercentInstance(locale);
      else
        format = NumberFormat.getPercentInstance();
    }
    else if (_type.equals("currency")) {
      if (locale != null)
        format = NumberFormat.getCurrencyInstance(locale);
      else
        format = NumberFormat.getCurrencyInstance(locale);
    }
    else
      throw new JspException(L.l("unknown formatNumber type `{0}'", _type));

    if (_pattern != null) {
      DecimalFormat decimalFormat = (DecimalFormat) format;

      decimalFormat.applyPattern(_pattern);
    }

    format.setParseIntegerOnly(_integerOnly);

    return format;
  }
}
