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
 * @author Alex Rojkov
 */

package com.caucho.jsf.taglib;

import javax.el.*;

import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.validator.MethodExpressionValidator;
import javax.faces.validator.Validator;
import javax.faces.event.MethodExpressionValueChangeListener;
import javax.faces.event.ValueChangeListener;
import java.util.Map;
import java.util.HashMap;

/**
 * The h:inputText tag
 */
public class HtmlInputBaseTag
  extends HtmlStyleBaseTag
{
  private Map<String, ValueExpression> _map;

  private MethodExpression _validatorExpression;
  private MethodExpressionValidator _validator;

  private MethodExpression _valueChangeExpression;
  private MethodExpressionValueChangeListener _valueChangeListener;

  public String getComponentType()
  {
    return HtmlInputText.COMPONENT_TYPE;
  }

  public String getRendererType()
  {
    return "javax.faces.Text";
  }

  public ValueExpression getConverterMessage()
  {
    if (_map != null)
      return _map.get("converterMessage");

    return null;
  }

  public void setConverterMessage(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String, ValueExpression>();

    _map.put("converterMessage", value);
  }

  public ValueExpression getLabel()
  {
    if (_map != null)
      return _map.get("label");

    return null;
  }

  public void setLabel(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String, ValueExpression>();

    _map.put("label", value);
  }

  public ValueExpression getRequiredMessage()
  {
    if (_map != null)
      return _map.get("requiredMessage");

    return null;
  }

  public void setRequiredMessage(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String, ValueExpression>();

    _map.put("requiredMessage", value);
  }

  public ValueExpression getValidatorMessage()
  {
    if (_map != null)
      return _map.get("validatorMessage");

    return null;
  }

  public void setValidatorMessage(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String, ValueExpression>();

    _map.put("validatorMessage", value);
  }

  public MethodExpression getValidator()
  {
    return _validatorExpression;
  }

  public void setValidator(MethodExpression value)
  {
    _validatorExpression = value;

    if (_validatorExpression != null) {
      _validator = new MethodExpressionValidator(
        _validatorExpression);
    }
  }

  public MethodExpression getValueChangeListener()
  {
    return _valueChangeExpression;
  }

  public void setValueChangeListener(MethodExpression value)
  {
    _valueChangeExpression = value;

    if (_valueChangeExpression != null)
      _valueChangeListener = new MethodExpressionValueChangeListener(
        _valueChangeExpression);
  }

  public ValueExpression getImmediate()
  {
    if (_map != null)
      return _map.get("immediate");

    return null;
  }

    public void setImmediate(ValueExpression value)
  {
    if (_map == null)
      _map = new HashMap<String, ValueExpression>();

    _map.put("immediate", value);
  }

  @Override
  protected void setProperties(UIComponent component)
  {
    super.setProperties(component);

    if (_map != null)
      for (String attribute : _map.keySet()) {
        component.setValueExpression(attribute, _map.get(attribute));
      }

    UIInput input = (UIInput) component;

    if (_validatorExpression != null) {
      Validator[] validators = input.getValidators();

      MethodExpressionValidator validator = null;
      for (int i = 0; i < validators.length; i++)
        if (_validator == validators[i])
          validator = _validator;

      if (validator == null)
        input.addValidator(_validator);
    }

    if (_valueChangeExpression != null) {
      ValueChangeListener[] valueChangeListeners
        = input.getValueChangeListeners();

      MethodExpressionValueChangeListener listener = null;

      for (int i = 0; i < valueChangeListeners.length; i++)
        if (_valueChangeListener == valueChangeListeners[i])
          listener
            = (MethodExpressionValueChangeListener) valueChangeListeners[i];

      if (listener == null)
        input.addValueChangeListener(_valueChangeListener);
    }

  }

  public void release()
  {
    _map = null;

    _validatorExpression = null;
    _validator = null;

    _valueChangeExpression = null;
    _valueChangeListener = null;

    super.release();
  }
}