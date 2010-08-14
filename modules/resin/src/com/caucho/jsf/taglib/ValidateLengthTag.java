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
import javax.faces.validator.LengthValidator;
import javax.faces.validator.Validator;
import javax.faces.webapp.ValidatorELTag;

public class ValidateLengthTag extends ValidatorELTag
{
  private ValueExpression _bindingExpr;
  
  private ValueExpression _minimumExpr;
  private ValueExpression _maximumExpr;

  public void setBinding(ValueExpression expr)
  {
    _bindingExpr = expr;
  }

  public void setMinimum(ValueExpression expr)
  {
    _minimumExpr = expr;
  }

  public void setMaximum(ValueExpression expr)
  {
    _maximumExpr = expr;
  }
  
  protected Validator createValidator()
  {
    FacesContext context = FacesContext.getCurrentInstance();

    Application app = context.getApplication();

    LengthValidator validator = null;

    ELContext elContext = context.getELContext();
      
    if (_bindingExpr != null)
      validator = (LengthValidator) _bindingExpr.getValue(elContext);

    if (validator == null) {
      String id = LengthValidator.VALIDATOR_ID;

      validator = (LengthValidator) app.createValidator(id);

      if (_bindingExpr != null)
        _bindingExpr.setValue(elContext, validator);
    }

    if (_minimumExpr != null)
      validator.setMinimum((Integer) _minimumExpr.getValue(elContext));

    if (_maximumExpr != null)
      validator.setMaximum((Integer) _maximumExpr.getValue(elContext));

    return validator;
  }
}
