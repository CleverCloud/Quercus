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

package javax.faces.convert;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;

public class EnumConverter
  implements Converter, StateHolder
{

  public static final java.lang.String CONVERTER_ID
    = "javax.faces.Enum";
  public static final java.lang.String ENUM_ID
    = "javax.faces.converter.EnumConverter.ENUM";
  public static final java.lang.String ENUM_NO_CLASS_ID
    = "javax.faces.converter.EnumConverter.ENUM_NO_CLASS";

  private Class<? extends Enum> _targetClass;

  private boolean _transient;

  public EnumConverter()
  {
  }

  public EnumConverter(Class targetClass)
  {
    _targetClass = targetClass;
  }

  public Object getAsObject(FacesContext context,
                            UIComponent component,
                            String value)
    throws ConverterException
  {
    if (context == null || component == null)
      throw new NullPointerException();

    if (_targetClass == null) {
      FacesMessage msg = createFacesMessageForEnumNoClass(context,
                                                          component,
                                                          value);
      throw new ConverterException(msg);
    }

    if (value == null)
      return null;

    value = value.trim();

    if (value.length() == 0)
      return null;


    try {
      return Enum.valueOf(_targetClass, value);
    }
    catch (Exception e) {
      FacesMessage msg = createFacesMessageForEnum(context, component, value);

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
    
    if (_targetClass == null) {
      FacesMessage msg = createFacesMessageForEnumNoClass(context,
                                                          component,
                                                          value);
      throw new ConverterException(msg);
    }

    if (value == null)
      return null;

    if (_targetClass.isAssignableFrom(value.getClass()))
      return value.toString();

    FacesMessage msg = createFacesMessageForEnum(context, component, value);

    throw new ConverterException(msg);
  }

  public Object saveState(FacesContext context)
  {
    return _targetClass;
  }

  public void restoreState(FacesContext context, Object state)
  {
    _targetClass = (Class<? extends Enum>) state;
  }

  public boolean isTransient()
  {
    return _transient;
  }

  public void setTransient(boolean isTransient)
  {
    _transient = isTransient;
  }

  public String toString()
  {
    return "EnumConverter[]";
  }

  private FacesMessage createFacesMessageForEnumNoClass(FacesContext context,
                                                        UIComponent component,
                                                        Object value)
  {
    String summary = Util.l10n(context,
                               ENUM_NO_CLASS_ID,
                               "{1}: \"{0}\" must be convertible to an enum from the enum, but no enum class provided.",
                               value,
                               Util.getLabel(context, component));

    String detail = Util.l10n(context,
                              ENUM_NO_CLASS_ID + "_detail",
                              "{1}: \"{0}\" must be convertible to an enum from the enum, but no enum class provided.",
                              value,
                              Util.getLabel(context, component));

    return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail);
  }

  private FacesMessage createFacesMessageForEnum(FacesContext context,
                                                 UIComponent component,
                                                 Object value)
  {
    String summary = Util.l10n(context,
                               ENUM_ID,
                               "{2}: \"{0}\" must be convertible to an enum.",
                               value,
                               getExample(),
                               Util.getLabel(context, component));

    String detail = Util.l10n(context,
                              ENUM_ID + "_detail",
                              "{2}: \"{0}\" must be convertible to an enum from the enum that contains the constant \"{1}\"",
                              value,
                              getExample(),
                              Util.getLabel(context, component));

    return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail);
  }

  private Object getExample()
  {
    Object[] enumConstants = _targetClass.getEnumConstants();

    if (enumConstants.length == 0) return "";

    return enumConstants[0];
  }

}
