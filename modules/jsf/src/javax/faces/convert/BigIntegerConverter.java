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

import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.component.*;

public class BigIntegerConverter implements Converter
{
  public static final String CONVERTER_ID
    = "javax.faces.BigInteger";
  public static final String BIGINTEGER_ID
    = "javax.faces.converter.BigIntegerConverter.BIGINTEGER";
  public static final String STRING_ID
    = "javax.faces.converter.STRING";
  
  public Object getAsObject(FacesContext context,
                            UIComponent component,
                            String value)
    throws ConverterException
  {
    // XXX: incorrect
    if (value == null)
      return null;

    value = value.trim();

    if (value.length() == 0)
      return null;

    try {
      return new java.math.BigInteger(value.toString());
    } catch (NumberFormatException e) {
      String summary = Util.l10n(context, BIGINTEGER_ID,
                                "{2}: \"{0}\" must be a number.",
                                 value,
                                 getExample(),
                                 Util.getLabel(context, component));
      
      String detail = Util.l10n(context, BIGINTEGER_ID + "_detail",
                                "{2}: \"{0}\" must be a number.  Example: {1}.",
                                value,
                                getExample(),
                                Util.getLabel(context, component));

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
    // XXX: incorrect
    if (value == null)
      return "";
    else if (value instanceof String)
      return (String) value;
    else
      return String.valueOf(value);
  }

  private String getExample()
  {
    return "1224";
  }

  public String toString()
  {
    return "BigIntegerConverter[]";
  }
}
