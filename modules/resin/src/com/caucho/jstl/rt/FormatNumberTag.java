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

import com.caucho.jsp.BodyContentImpl;
import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Looks up an i18n message from a bundle and prints it.
 */
public class FormatNumberTag extends BodyTagSupport {
  private static L10N L = new L10N(FormatNumberTag.class);
  
  private Double _value;
  private boolean _hasValue;
  
  private String _type;
  
  private String _pattern;
  
  private String _currencyCode;
  private String _currencySymbol;
  
  private boolean _groupingUsed = true;
  
  private int _maxIntegerDigits = -1;
  private int _minIntegerDigits = -1;
  private int _maxFractionDigits = -1;
  private int _minFractionDigits = -1;

  private String _var;
  private String _scope;

  /**
   * Sets the formatting value.
   *
   * @param value the value.
   */
  public void setValue(Object value)
    throws JspException
  {
    try {
      if (value == null || "".equals(value)) {
      }
      else if (value instanceof Number)
        _value = ((Number) value).doubleValue();
      else if (value instanceof String) {
        _value = Double.valueOf((String) value);
      }
      else {
        throw new NumberFormatException(L.l("Expected number at '{0}'",
                                            value));
      }
    } catch (NumberFormatException e) {
      throw new JspException(new IllegalArgumentException(e.getMessage()));
    }
    
    _hasValue = true;
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
   * Sets the currency code.
   *
   * @param currencyCode the currency code.
   */
  public void setCurrencyCode(String currency)
  {
    _currencyCode = currency;
  }

  /**
   * Sets the currency symbol.
   *
   * @param currencySymbol the currency symbol.
   */
  public void setCurrencySymbol(String currencySymbol)
  {
    _currencySymbol = currencySymbol;
  }

  /**
   * Sets the groupingUsed expression
   *
   * @param groupingUsed true if grouping is used
   */
  public void setGroupingUsed(boolean groupingUsed)
  {
    _groupingUsed = groupingUsed;
  }

  /**
   * Sets the minimum digits allowed in the integer portion.
   *
   * @param minIntegerDigits the digits.
   */
  public void setMinIntegerDigits(int minIntegerDigits)
  {
    _minIntegerDigits = minIntegerDigits;
  }

  /**
   * Sets the maximum digits allowed in the integer portion.
   *
   * @param maxIntegerDigits the digits.
   */
  public void setMaxIntegerDigits(int maxIntegerDigits)
  {
    _maxIntegerDigits = maxIntegerDigits;
  }

  /**
   * Sets the minimum digits allowed in the fraction portion.
   *
   * @param minFractionDigits the digits.
   */
  public void setMinFractionDigits(int minFractionDigits)
  {
    _minFractionDigits = minFractionDigits;
  }

  /**
   * Sets the maximum digits allowed in the fraction portion.
   *
   * @param maxFractionDigits the JSP-EL expression for the digits.
   */
  public void setMaxFractionDigits(int maxFractionDigits)
  {
    _maxFractionDigits = maxFractionDigits;
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

      Double rawValue = null;
      BodyContentImpl body = (BodyContentImpl) getBodyContent();

      if (_hasValue)
        rawValue = _value;
      else if (body != null) {
        String value = body.getTrimString();

        if (! value.equals(""))
          rawValue = Double.parseDouble(value);
      }

      if (rawValue != null && Double.isNaN(rawValue))
        rawValue = 0.0;

      String value;

      if (rawValue == null)
        value = null;
      else if (format != null)
        value = format.format(rawValue);
      else
        value = String.valueOf(rawValue);

      if (_var == null) {
        if (_scope != null)
          throw new JspException(L.l("fmt:formatDate var must not be null when scope '{0}' is set.",
                                     _scope));

        if (value != null)
          out.print(value);
      }
      else
        CoreSetTag.setValue(pageContext, _var, _scope, value);
    } catch (IOException e) {
    }

    return EVAL_PAGE;
  }

  protected NumberFormat getFormat()
    throws JspException
  {
    PageContextImpl pc = (PageContextImpl) pageContext;
      
    NumberFormat format = null;

    Locale locale = pc.getLocale();

    String type = _type;

    if (type == null || type.equals("") || type.equals("number")
        || _pattern != null && ! "".equals(_pattern)) {
      if (locale != null)
        format = NumberFormat.getInstance(locale);
      else
        format = NumberFormat.getInstance();

      DecimalFormat decimalFormat = (DecimalFormat) format;

      if (_pattern != null)
        decimalFormat.applyPattern(_pattern);
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
        format = NumberFormat.getCurrencyInstance();

      if ((_currencyCode != null || _currencySymbol != null)
          && format instanceof DecimalFormat) {
        DecimalFormat dFormat = (DecimalFormat) format;
        DecimalFormatSymbols dSymbols;

        dSymbols = dFormat.getDecimalFormatSymbols();

        if (_currencyCode != null && dSymbols != null)
          dSymbols.setInternationalCurrencySymbol(_currencyCode);
        else if (_currencySymbol != null && dSymbols != null)
          dSymbols.setCurrencySymbol(_currencySymbol);

        dFormat.setDecimalFormatSymbols(dSymbols);
      }
    }
    else
      throw new JspException(L.l("unknown formatNumber type `{0}'",
                                 type));

    format.setGroupingUsed(_groupingUsed);

    if (_minIntegerDigits > 0)
      format.setMinimumIntegerDigits(_minIntegerDigits);
      
    if (_maxIntegerDigits > 0)
      format.setMaximumIntegerDigits(_maxIntegerDigits);

    if (_minFractionDigits > 0)
      format.setMinimumFractionDigits(_minFractionDigits);
      
    if (_maxFractionDigits > 0)
      format.setMaximumFractionDigits(_maxFractionDigits);

    return format;
  }
}
