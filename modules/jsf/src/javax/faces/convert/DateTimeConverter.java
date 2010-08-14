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

import java.util.*;
import java.text.*;

import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.component.*;

public class DateTimeConverter
  implements Converter, StateHolder
{
  public static final String CONVERTER_ID
    = "javax.faces.DateTime";
  public static final String DATE_ID
    = "javax.faces.converter.DateTimeConverter.DATE";
  public static final String DATETIME_ID
    = "javax.faces.converter.DateTimeConverter.DATETIME";
  public static final String STRING_ID
    = "javax.faces.converter.STRING";
  public static final String TIME_ID
    = "javax.faces.converter.DateTimeConverter.TIME";

  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

  private String _dateStyle = "default";
  private String _timeStyle = "default";
  private Locale _locale;

  private String _pattern;
  private TimeZone _timeZone = GMT;
  private String _type = "date";

  private boolean _isTransient;

  private DateFormat _format;

  public String getDateStyle()
  {
    return _dateStyle;
  }

  public void setDateStyle(String value)
  {
    _dateStyle = value;
    _format = null;
  }

  public String getTimeStyle()
  {
    return _timeStyle;
  }

  public void setTimeStyle(String value)
  {
    _timeStyle = value;
    _format = null;
  }

  public Locale getLocale()
  {
    if (_locale != null)
      return _locale;

    FacesContext context = FacesContext.getCurrentInstance();
    
    return context.getViewRoot().getLocale();
  }

  public void setLocale(Locale locale)
  {
    _locale = locale;
    _format = null;
  }

  public String getPattern()
  {
    return _pattern;
  }

  public void setPattern(String value)
  {
    _pattern = value;
    _format = null;
  }

  public String getType()
  {
    return _type;
  }

  public void setType(String value)
  {
    _type = value;
    _format = null;
  }

  public TimeZone getTimeZone()
  {
    return _timeZone;
  }

  public void setTimeZone(TimeZone value)
  {
    _timeZone = value;
    _format = null;
  }
  
  public void setTransient(boolean value)
  {
    _isTransient = value;
  }
  
  public boolean isTransient()
  {
    return _isTransient;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object []values = (Object []) state;
    _dateStyle = (String) values [0];
    _timeStyle = (String) values [1];
    _locale = (Locale) values [2];
    _pattern = (String) values [3];
    _timeZone = (TimeZone) values [4];
    _type = (String) values [5];
  }

  public Object saveState(FacesContext context)
  {
    Object []state = new Object [6];
    state[0] = _dateStyle;
    state[1] = _timeStyle;
    state[2] = _locale;
    state[3] = _pattern;
    state[4] = _timeZone;
    state[5] = _type;
    return state;
  }
  
  public Object getAsObject(FacesContext context,
                            UIComponent component,
                            String value)
    throws ConverterException
  {
    if (context == null || component == null)
      throw new NullPointerException();
    
    if (value == null)
      return null;

    value = value.trim();
    
    if (value.length() == 0)
      return null;

    DateFormat format = getFormat(context);

    try {
      synchronized (format) {
        return format.parse(value);
      }
    } catch (ParseException e) {
      String summary;
      String detail;
      
      if ("date".equals(_type)) {
        summary = Util.l10n(context, DATE_ID,
                            "{2}: \"{0}\" could not be understood as a date.",
                            value,
                            getExample(context),
                            Util.getLabel(context, component));
      
        detail = Util.l10n(context, DATE_ID + "_detail",
                           "{2}: \"{0}\" could not be understood as a percentage. Example: {1}.",
                           value,
                           getExample(context),
                           Util.getLabel(context, component));
      }
      else if ("time".equals(_type)) {
        summary = Util.l10n(context, TIME_ID,
                            "{2}: \"{0}\" could not be understood as a time.",
                            value,
                            getExample(context),
                            Util.getLabel(context, component));
      
        detail = Util.l10n(context, TIME_ID + "_detail",
                           "{2}: \"{0}\" could not be understood as a time. Example: {1}.",
                           value,
                           getExample(context),
                           Util.getLabel(context, component));
      }
      else {
        summary = Util.l10n(context, DATETIME_ID,
                            "{2}: \"{0}\" could not be understood as a date and time.",
                            value,
                            getExample(context),
                            Util.getLabel(context, component));
      
        detail = Util.l10n(context, DATETIME_ID + "_detail",
                           "{2}: \"{0}\" could not be understood as a date and time. Example: {1}.",
                           value,
                           getExample(context),
                           Util.getLabel(context, component));
      }

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
    if (context == null || component == null)
      throw new NullPointerException();
    
    if (value == null)
      return "";
    else if (value instanceof Date) {
      DateFormat format = getFormat(context);

      synchronized (format) {
        return format.format((Date) value);
      }
    }
    else
      return String.valueOf(value);
  }

  private DateFormat getFormat(FacesContext context)
  {
    synchronized (this) {
      if (_locale == null) {
        Locale locale = context.getViewRoot().getLocale();

        return createFormat(locale);
      }
      else if (_format == null) {
        _format = createFormat(_locale);
      }

      return _format;
    }
  }

  private DateFormat createFormat(Locale locale)
  {
    DateFormat format;

    int dateStyle = DateFormat.DEFAULT;
    int timeStyle = DateFormat.DEFAULT;

    if ("short".equals(_dateStyle)) {
      dateStyle = DateFormat.SHORT;
    }
    else if ("medium".equals(_dateStyle)) {
      dateStyle = DateFormat.MEDIUM;
    }
    else if ("long".equals(_dateStyle)) {
      dateStyle = DateFormat.LONG;
    }
    else if ("full".equals(_dateStyle)) {
      dateStyle = DateFormat.FULL;
    }
    else if ("default".equals(_dateStyle)) {
      dateStyle = DateFormat.DEFAULT;
    }
    else if (_dateStyle != null)
      throw new ConverterException("'" + _dateStyle + "' is an unknown dateStyle");
    
    if ("short".equals(_timeStyle)) {
      timeStyle = DateFormat.SHORT;
    }
    else if ("medium".equals(_timeStyle)) {
      timeStyle = DateFormat.MEDIUM;
    }
    else if ("long".equals(_timeStyle)) {
      timeStyle = DateFormat.LONG;
    }
    else if ("full".equals(_timeStyle)) {
      timeStyle = DateFormat.FULL;
    }
    else if ("default".equals(_timeStyle)) {
      timeStyle = DateFormat.DEFAULT;
    }
    else if (_timeStyle != null)
      throw new ConverterException("'" + _timeStyle + "' is an unknown timeStyle");
    

    if (_type == null || "date".equals(_type)) {
      if (locale != null)
        format = DateFormat.getDateInstance(dateStyle, locale);
      else
        format = DateFormat.getDateInstance(dateStyle);
    }
    else if ("time".equals(_type)) {
      if (locale != null)
        format = DateFormat.getTimeInstance(timeStyle, locale);
      else
        format = DateFormat.getTimeInstance(timeStyle);
    }
    else if ("both".equals(_type)) {
      if (locale != null)
        format = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
      else
        format = DateFormat.getDateTimeInstance(dateStyle, timeStyle);
    }
    else
      throw new ConverterException("'" + _type + "' is an unknown type");

    try {
      if (_pattern != null && format instanceof SimpleDateFormat)
        ((SimpleDateFormat) format).applyPattern(_pattern);
    } catch (Exception e) {
      throw new ConverterException(e);
    }

    if (_timeZone != null)
      format.setTimeZone(_timeZone);

    return format;
  }

  private String getExample(FacesContext context)
  {
    DateFormat format = getFormat(context);

    synchronized (format) {
      Date date = new Date(894621091000L);
      
      return format.format(date);
    }
  }

  public String toString()
  {
    return "DateTimeConverter[]";
  }
}
