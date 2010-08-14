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

package javax.faces.convert;

import java.util.*;
import java.text.*;

import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.component.*;

public class NumberConverter
  implements Converter, StateHolder
{
  public static final String CONVERTER_ID = "javax.faces.Number";
  public static final String CURRENCY_ID
    = "javax.faces.converter.NumberConverter.CURRENCY";
  public static final String NUMBER_ID
    = "javax.faces.converter.NumberConverter.NUMBER";
  public static final String PATTERN_ID
    = "javax.faces.converter.NumberConverter.PATTERN";
  public static final String PERCENT_ID
    = "javax.faces.converter.NumberConverter.PERCENT";
  public static final String STRING_ID
    = "javax.faces.converter.STRING";
  
  private String _currencyCode;
  private String _currencySymbol;
  private Locale _locale;

  private Integer _maxFractionDigits;
  private Integer _minFractionDigits;

  private Integer _maxIntegerDigits;
  private Integer _minIntegerDigits;

  private String _pattern;
  private String _type = "number";
  private boolean _isGroupingUsed = true;
  private boolean _isIntegerOnly;
  private boolean _isTransient;

  private NumberFormat _format;

  public String getCurrencyCode()
  {
    return _currencyCode;
  }

  public void setCurrencyCode(String value)
  {
    _currencyCode = value;
    _format = null;
  }

  public String getCurrencySymbol()
  {
    return _currencySymbol;
  }

  public void setCurrencySymbol(String value)
  {
    _currencySymbol = value;
    _format = null;
  }

  public Locale getLocale()
  {
    if (_locale != null)
      return _locale;

    FacesContext context = FacesContext.getCurrentInstance();
    
    return context.getViewRoot().getLocale();
  }

  public void setLocale(Locale locale)
  {
    _locale = locale;
    _format = null;
  }

  public int getMaxFractionDigits()
  {
    if (_maxFractionDigits != null)
      return _maxFractionDigits;
    else
      return 0;
  }

  public void setMaxFractionDigits(int value)
  {
    _maxFractionDigits = value;
    _format = null;
  }

  public int getMinFractionDigits()
  {
    if (_minFractionDigits != null)
      return _minFractionDigits;
    else
      return 0;
  }

  public void setMinFractionDigits(int value)
  {
    _minFractionDigits = value;
    _format = null;
  }

  public int getMaxIntegerDigits()
  {
    if (_maxIntegerDigits != null)
      return _maxIntegerDigits;
    else
      return 0;
  }

  public void setMaxIntegerDigits(int value)
  {
    _maxIntegerDigits = value;
    _format = null;
  }

  public int getMinIntegerDigits()
  {
    if (_minIntegerDigits != null)
      return _minIntegerDigits;
    else
      return 0;
  }

  public void setMinIntegerDigits(int value)
  {
    _minIntegerDigits = value;
    _format = null;
  }

  public void setIntegerOnly(boolean isIntegerOnly)
  {
    _isIntegerOnly = isIntegerOnly;
  }

  public boolean isIntegerOnly()
  {
    return _isIntegerOnly;
  }

  public String getPattern()
  {
    return _pattern;
  }

  public void setPattern(String value)
  {
    _pattern = value;
    _format = null;
  }

  public String getType()
  {
    return _type;
  }

  public void setType(String value)
  {
    _type = value;
    _format = null;
  }

  public boolean isGroupingUsed()
  {
    return _isGroupingUsed;
  }
  
  public void setGroupingUsed(boolean value)
  {
    _isGroupingUsed = value;
    _format = null;
  }

  public boolean isTransient()
  {
    return _isTransient;
  }
  
  public void setTransient(boolean value)
  {
    _isTransient = value;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object []values = (Object []) state;
    _currencyCode = (String) values [0];
    _currencySymbol = (String) values [1];
    _locale = (Locale) values [2];
    _maxFractionDigits = (Integer) values [3];
    _minFractionDigits = (Integer) values [4];
    _maxIntegerDigits = (Integer) values [5];
    _minIntegerDigits = (Integer) values [6];
    _pattern = (String) values [7];
    _type = (String) values [8];
    _isGroupingUsed = ((Boolean) values [9]).booleanValue();
    _isIntegerOnly = ((Boolean) values [10]).booleanValue();
  }

  public Object saveState(FacesContext context)
  {
    Object []state = new Object [11];
    state [0] = _currencyCode;
    state [1] = _currencySymbol;
    state [2] = _locale;
    state [3] = _maxFractionDigits;
    state [4] = _minFractionDigits;
    state [5] = _maxIntegerDigits;
    state [6] = _minIntegerDigits;
    state [7] = _pattern;
    state [8] = _type;
    state [9] = _isGroupingUsed ? Boolean.TRUE : Boolean.FALSE;
    state [10] = _isIntegerOnly ? Boolean.TRUE : Boolean.FALSE;
    return state;
  }
  
  public Object getAsObject(FacesContext context,
                            UIComponent component,
                            String value)
    throws ConverterException
  {
    if (context == null || component == null)
      throw new NullPointerException();
    
    // XXX: incorrect
    if (value == null)
      return null;

    if (value.length() == 0)
      return null;

    value = value.trim();

    UIViewRoot viewRoot = context.getViewRoot();
    Locale locale = null;
    
    if (viewRoot != null)
      locale = viewRoot.getLocale();

    NumberFormat format = getFormat(locale);

    try {
      synchronized (format) {
        return format.parse(value);
      }
    } catch (ParseException e) {
      String summary;
      String detail;
      
      if ("percent".equals(_type)) {
        summary = Util.l10n(context, PERCENT_ID,
                            "{2}: \"{0}\" could not be understood as a percentage.",
                            value,
                            getExample(context),
                            Util.getLabel(context, component));
      
        detail = Util.l10n(context, PERCENT_ID + "_detail",
                           "{2}: \"{0}\" could not be understood as a percentage. Example: {1}.",
                           value,
                           getExample(context),
                           Util.getLabel(context, component));
      }
      else if ("currency".equals(_type)) {
        summary = Util.l10n(context, CURRENCY_ID,
                            "{2}: \"{0}\" could not be understood as a currency value.",
                            value,
                            getExample(context),
                            Util.getLabel(context, component));
      
        detail = Util.l10n(context, CURRENCY_ID + "_detail",
                           "{2}: \"{0}\" could not be understood as a currency value. Example: {1}.",
                           value,
                           getExample(context),
                           Util.getLabel(context, component));
      }
      else {
        summary = Util.l10n(context, NUMBER_ID,
                            "{2}: \"{0}\" could not be understood as a number.",
                            value,
                            getExample(context),
                            Util.getLabel(context, component));
      
        detail = Util.l10n(context, NUMBER_ID + "_detail",
                           "{2}: \"{0}\" could not be understood as a number. Example: {1}.",
                           value,
                           getExample(context),
                           Util.getLabel(context, component));
      }

      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                          summary,
                                          detail);
      
      throw new ConverterException(msg, e);
    }
  }
  
  public String getAsString(FacesContext context,
                            UIComponent component,
                            Object value)
    throws ConverterException
  {
    if (context == null || component == null)
      throw new NullPointerException();
    
    if (value == null)
      return "";
    else if (value instanceof Number) {
      NumberFormat format = getFormat(context.getViewRoot().getLocale());

      synchronized (format) {
        return format.format((Number) value);
      }
    }
    else
      return String.valueOf(value);
  }

  private NumberFormat getFormat(Locale locale)
  {
    synchronized (this) {
      if (_locale == null)
        return createFormat(locale);
      else if (_format == null) {
        _format = createFormat(_locale);
      }

      return _format;
    }
  }

  private NumberFormat createFormat(Locale locale)
  {
    NumberFormat format;
    
    if (_type == null || "number".equals(_type)) {
      if (locale != null)
        format = NumberFormat.getNumberInstance(locale);
      else
        format = NumberFormat.getNumberInstance();
    }
    else if ("currency".equals(_type)) {
      if (locale != null)
        format = NumberFormat.getCurrencyInstance(locale);
      else
        format = NumberFormat.getCurrencyInstance();

      if (_currencyCode != null)
        format.setCurrency(Currency.getInstance(_currencyCode));
      else if (_currencySymbol != null) {
        if (format instanceof DecimalFormat) {
          DecimalFormat decimalFormat = (DecimalFormat) format;
          DecimalFormatSymbols symbols
            = decimalFormat.getDecimalFormatSymbols();

          symbols.setCurrencySymbol(_currencySymbol);
          decimalFormat.setDecimalFormatSymbols(symbols);
        }
      }
    }
    else if ("percent".equals(_type)) {
      if (locale != null)
        format = NumberFormat.getPercentInstance(locale);
      else
        format = NumberFormat.getPercentInstance();
    }
    else {
      throw new ConverterException("'" + _type + "' is an illegal converter type.");
    }

    format.setGroupingUsed(_isGroupingUsed);
    format.setParseIntegerOnly(_isIntegerOnly);

    if (_maxFractionDigits != null)
      format.setMaximumFractionDigits(_maxFractionDigits);
    if (_minFractionDigits != null)
      format.setMinimumFractionDigits(_minFractionDigits);
    if (_maxIntegerDigits != null)
      format.setMaximumIntegerDigits(_maxIntegerDigits);
    if (_minIntegerDigits != null)
      format.setMinimumIntegerDigits(_minIntegerDigits);

    if (_pattern != null && format instanceof DecimalFormat)
      ((DecimalFormat) format).applyPattern(_pattern);

    return format;
  }

  private String getExample(FacesContext context)
  {
    UIViewRoot viewRoot = context.getViewRoot();
    Locale locale = null;

    if (viewRoot != null)
      locale = viewRoot.getLocale();
    
    NumberFormat format = getFormat(locale);

    synchronized (format) {
      if ("percentage".equals(_type))
        return format.format(new Double(0.75));
      else
        return format.format(new Double(10125.25));
    }
  }

  public String toString()
  {
    return "NumberConverter[]";
  }
}
