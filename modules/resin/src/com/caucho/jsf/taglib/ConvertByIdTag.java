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
import javax.faces.webapp.ConverterELTag;

public class ConvertByIdTag extends ConverterELTag
{
  private ValueExpression _bindingExpr;
  
  private ValueExpression _converterIdExpr;

  public void setBinding(ValueExpression expr)
  {
    _bindingExpr = expr;
  }

  public void setConverterId(ValueExpression expr)
  {
    _converterIdExpr = expr;
  }
  
  protected Converter createConverter()
  {
    FacesContext context = FacesContext.getCurrentInstance();

    Application app = context.getApplication();

    Converter converter = null;

    ELContext elContext = context.getELContext();
      
    if (_bindingExpr != null)
      converter = (Converter) _bindingExpr.getValue(elContext);

    if (converter == null) {
      String id = (String) _converterIdExpr.getValue(elContext);

      converter = app.createConverter(id);

      if (_bindingExpr != null)
        _bindingExpr.setValue(elContext, converter);
    }

    return converter;
  }
}
