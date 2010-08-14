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

package com.caucho.jsf.html;

import java.io.*;
import java.util.*;

import javax.el.*;
import javax.faces.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.model.*;
import javax.faces.render.*;

/**
 * The base renderer
 */
abstract class BaseRenderer extends Renderer
{
  /**
   * True if the renderer is responsible for rendering the children.
   */
  @Override
  public boolean getRendersChildren()
  {
    return true;
  }

  @Override
  public Object getConvertedValue(FacesContext context,
                                  UIComponent component,
                                  Object submittedValue)
    throws ConverterException
  {
    if (context == null || component == null)
      throw new NullPointerException();

    if (component instanceof ValueHolder) {
      Converter converter = ((ValueHolder) component).getConverter();
      
      if (converter != null)
        return converter.getAsObject(context, component,
                                     (String) submittedValue);
    }

    ValueExpression valueExpr = component.getValueExpression("value");
      
    if (valueExpr != null) {
      Class type = valueExpr.getType(context.getELContext());

      if (type != null) {
        Converter converter = context.getApplication().createConverter(type);

        if (converter != null) {
          return converter.getAsObject(context,
                                       component,
                                       (String) submittedValue);
        }
      }
    }

    return submittedValue;
  }

  protected String toString(FacesContext context,
                            UIComponent component,
                            Object value)
  {
    if (component instanceof ValueHolder) {
      Converter converter = ((ValueHolder) component).getConverter();

      if (converter != null) {
        String result = converter.getAsString(context, component, value);

        return result;
      }
    }

    if (value != null)
      return value.toString();
    else
      return "";
  }

  public String toString()
  {
    return getClass().getName() + "[]";
  }
}
