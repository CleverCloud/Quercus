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

package com.caucho.jsf.taglib;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.NumberConverter;
import javax.faces.webapp.ConverterELTag;
import java.util.Locale;

public class ConvertNumberTag extends ConverterELTag
{
  private ValueExpression _bindingExpr;
  
  private ValueExpression _currencyCodeExpr;
  private ValueExpression _currencySymbolExpr;
  private ValueExpression _groupingUsedExpr;
  private ValueExpression _integerOnlyExpr;
  private ValueExpression _localeExpr;
  private ValueExpression _maxFractionDigitsExpr;
  private ValueExpression _maxIntegerDigitsExpr;
  private ValueExpression _minFractionDigitsExpr;
  private ValueExpression _minIntegerDigitsExpr;
  private ValueExpression _patternExpr;
  private ValueExpression _typeExpr;

  public void setBinding(ValueExpression expr)
  {
    _bindingExpr = expr;
  }

  public void setCurrencyCode(ValueExpression expr)
  {
    _currencyCodeExpr = expr;
  }

  public void setCurrencySymbol(ValueExpression expr)
  {
    _currencySymbolExpr = expr;
  }

  public void setGroupingUsed(ValueExpression expr)
  {
    _groupingUsedExpr = expr;
  }

  public void setIntegerOnly(ValueExpression expr)
  {
    _integerOnlyExpr = expr;
  }

  public void setLocale(ValueExpression expr)
  {
    _localeExpr = expr;
  }

  public void setMaxFractionDigits(ValueExpression expr)
  {
    _maxFractionDigitsExpr = expr;
  }

  public void setMaxIntegerDigits(ValueExpression expr)
  {
    _maxIntegerDigitsExpr = expr;
  }

  public void setMinFractionDigits(ValueExpression expr)
  {
    _minFractionDigitsExpr = expr;
  }

  public void setMinIntegerDigits(ValueExpression expr)
  {
    _minIntegerDigitsExpr = expr;
  }

  public void setPattern(ValueExpression expr)
  {
    _patternExpr = expr;
  }

  public void setType(ValueExpression expr)
  {
    _typeExpr = expr;
  }
  
  protected Converter createConverter()
  {
    FacesContext context = FacesContext.getCurrentInstance();

    Application app = context.getApplication();

    NumberConverter converter = null;

    String id = "javax.faces.Number";

    ELContext elContext = context.getELContext();
      
    if (_bindingExpr != null)
      converter = (NumberConverter) _bindingExpr.getValue(elContext);

    if (converter == null) {
      converter = (NumberConverter) app.createConverter(id);

      if (_bindingExpr != null)
        _bindingExpr.setValue(elContext, converter);
    }

    String type = null;

    if (_typeExpr != null)
      converter.setType((String) _typeExpr.getValue(elContext));

    if (_currencyCodeExpr != null)
      converter.setCurrencyCode((String) _currencyCodeExpr.getValue(elContext));

    if (_currencySymbolExpr != null)
      converter.setCurrencySymbol((String) _currencySymbolExpr.getValue(elContext));

    if (_groupingUsedExpr != null)
      converter.setGroupingUsed((Boolean) _groupingUsedExpr.getValue(elContext));

    if (_integerOnlyExpr != null)
      converter.setIntegerOnly((Boolean) _integerOnlyExpr.getValue(elContext));

    if (_localeExpr != null) {
      Object value = _localeExpr.getValue(elContext);

      if (value instanceof Locale)
        converter.setLocale((Locale) value);
      else if (value != null)
        converter.setLocale(new Locale(value.toString()));
    }

    if (_maxFractionDigitsExpr != null) {
      converter.setMaxFractionDigits((Integer) _maxFractionDigitsExpr.getValue(elContext));
    }

    if (_maxIntegerDigitsExpr != null) {
      converter.setMaxIntegerDigits((Integer) _maxIntegerDigitsExpr.getValue(elContext));
    }

    if (_minFractionDigitsExpr != null) {
      converter.setMinFractionDigits((Integer) _minFractionDigitsExpr.getValue(elContext));
    }

    if (_minIntegerDigitsExpr != null) {
      converter.setMinIntegerDigits((Integer) _minIntegerDigitsExpr.getValue(elContext));
    }

    if (_patternExpr != null) {
      converter.setPattern((String) _patternExpr.getValue(elContext));
    }

    return converter;
  }
}
