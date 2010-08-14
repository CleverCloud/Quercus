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

import java.io.*;
import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

public class ConverterTag extends TagSupport
{
  private String _id;
  private ValueExpression _binding;

  public void setConverterId(String id)
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
                                             Converter.class);
  }

  public int doStartTag()
    throws JspException
  {
    UIComponentClassicTagBase parent;
    
    parent = UIComponentClassicTagBase.getParentUIComponentClassicTagBase(this.pageContext);

    if (parent == null)
      throw new JspException("ConverterTag must be nested inside a UIComponent tag.");

    UIComponent comp = parent.getComponentInstance();

    if (parent.getCreated()) {
      if (! (comp instanceof ValueHolder))
        throw new JspException("UIComponent parent of converter must be a ValueHolder.");

      ValueHolder valueHolder = (ValueHolder) comp;

      Converter converter = createConverter();

      valueHolder.setConverter(converter);
    }
    
    return SKIP_BODY;
  }

  protected Converter createConverter()
    throws JspException
  {
    try {
      FacesContext context = FacesContext.getCurrentInstance();
      Converter converter;

      if (_binding != null) {
        converter = (Converter) _binding.getValue(context.getELContext());

        if (converter != null)
          return converter;
      }
    
      converter = context.getApplication().createConverter(_id);

      if (_binding != null)
        _binding.setValue(context.getELContext(), converter);

      return converter;
    } catch (Exception e) {
      throw new JspException(e);
    }
  }
}
