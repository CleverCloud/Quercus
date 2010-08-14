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
  * @author Alex Rojkov
  */


package javax.faces.webapp;

import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.JspException;
import javax.faces.convert.Converter;
import javax.faces.component.ValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

public abstract class ConverterELTag
  extends TagSupport
{

  public int doStartTag()
    throws JspException
  {
    UIComponentClassicTagBase parent
      = UIComponentClassicTagBase.getParentUIComponentClassicTagBase(pageContext);

    if (parent == null)
      throw new JspException(
        "ConverterELTag must be nested inside a UIComponent tag.");

    if (parent.getCreated()) {

      UIComponent component = parent.getComponentInstance();

      if (!(component instanceof ValueHolder))
        throw new JspException(
          "UIComponent parent of converter must be a ValueHolder.");

      Converter converter = createConverter();

      ValueHolder valueHolder = (ValueHolder) component;
      valueHolder.setConverter(converter);

      Object obj = valueHolder.getLocalValue();

      if (obj instanceof String) {
        FacesContext context = FacesContext.getCurrentInstance();
        Object value = converter.getAsObject(context,
                                             component,
                                             (String) obj);
        valueHolder.setValue(value);
      }
    }

    return Tag.SKIP_BODY;
  }

  protected abstract Converter createConverter()
    throws JspException;
}
