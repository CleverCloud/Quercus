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

package javax.faces.webapp;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

public class ValidatorTag extends TagSupport
{
  private String _id;
  private ValueExpression _binding;

  public void setValidatorId(String id)
  {
    _id = id;
  }

  public void setBinding(String binding)
    throws JspException
  {
    FacesContext context = FacesContext.getCurrentInstance();

    Application app = context.getApplication();
    ExpressionFactory factory = app.getExpressionFactory();

    _binding = factory.createValueExpression(context.getELContext(),
                                             binding,
                                             Validator.class);
  }

  public int doStartTag()
    throws JspException
  {
    UIComponentClassicTagBase parent;
    
    parent = UIComponentClassicTagBase.getParentUIComponentClassicTagBase(this.pageContext);

    if (parent == null)
      throw new JspException("ValidatorTag must be nested inside a UIComponent tag.");

    UIComponent comp = parent.getComponentInstance();

    if (parent.getCreated()) {
      if (! (comp instanceof EditableValueHolder))
        throw new JspException("UIComponent parent of validator must be a EditableValueHolder.");

      EditableValueHolder valueHolder = (EditableValueHolder) comp;

      Validator validator = createValidator();

      valueHolder.addValidator(validator);
    }
    
    return SKIP_BODY;
  }

  protected Validator createValidator()
    throws JspException
  {
    try {
      FacesContext context = FacesContext.getCurrentInstance();
      Validator validator;

      if (_binding != null) {
        validator = (Validator) _binding.getValue(context.getELContext());

        if (validator != null)
          return validator;
      }
    
      validator = context.getApplication().createValidator(_id);

      if (_binding != null)
        _binding.setValue(context.getELContext(), validator);

      return validator;
    } catch (Exception e) {
      throw new JspException(e);
    }
  }
}
