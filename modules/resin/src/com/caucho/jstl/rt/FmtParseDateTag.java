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
 * @author Scott Ferguson
 */

package com.caucho.jstl.rt;

import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Formats an i18n date and prints it.
 */
public class FmtParseDateTag extends BodyTagSupport {
  private static L10N L = new L10N(FmtParseDateTag.class);

  private String _value;
  
  private String _type;
  private String _dateStyle;
  private String _timeStyle;
  
  private Object _parseLocale;
  
  private String _pattern;
  private Object _timeZone;
  
  private String _var;
  private String _scope;

  /**
   * Sets the formatting value.
   *
   * @param value the string value.
   */
  public void setValue(String value)
  {
    _value = value;
  }

  /**
   * Sets the date/time type.
   *
   * @param type the date/time type.
   */
  public void setType(String type)
  {
    _type = type;
  }

  /**
   * Sets the date style (full, short, etc.)
   *
   * @param style the date style
   */
  public void setDateStyle(String style)
  {
    _dateStyle = style;
  }

  /**
   * Sets the time style (full, short, etc.)
   *
   * @param style the time style
   */
  public void setTimeStyle(String style)
  {
    _timeStyle = style;
  }

  /**
   * Sets the formatting pattern.
   *
   * @param pattern the formatting pattern.
   */
  public void setPattern(String pattern)
  {
    _pattern = pattern;
  }

  /**
   * Sets the time zone.
   *
   * @param zone the time zone expression
   */
  public void setTimeZone(Object zone)
  {
    if ("".equals(zone))
      zone = null;
    
    _timeZone = zone;

  }

  /**
   * Sets the parse locale
   *
   * @param locale the locale
   */
  public void setParseLocale(Object locale)
  {
    _parseLocale = locale;
  }

  /**
   * Sets the variable name.
   *
   * @param var the variable name to store the value in.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the variable scope.
   *
   * @param scope the variable scope to store the value in.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Process the tag.
   */
  public int doEndTag()
    throws JspException
  {
    try {
      PageContextImpl pc = (PageContextImpl) pageContext;
      
      JspWriter out = pc.getOut();

      String string = null;

      if (_value != null)
        string = _value;
      else if (bodyContent != null) {
        string = bodyContent.getString();
        
        if (string != null)
          string = string.trim();
      }
      
      DateFormat format = getFormat();

      Object value;

      if (string == null || "".equals(string))
        value = null;
      else
        value = format.parse(string);

      if (_var == null) {
        if (_scope != null)
          throw new JspException(L.l("fmt:parseDate var must not be null when scope '{0}' is set.",
                                     _scope));

        if (value != null)
          out.print(value);
      }
      else
        CoreSetTag.setValue(pageContext, _var, _scope, value);
    } catch (IOException e) {
    } catch (ParseException e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }

  protected DateFormat getFormat()
    throws JspException
  {
    PageContextImpl pc = (PageContextImpl) pageContext;
    
    DateFormat format = null;
    Locale locale = null;

    if (_parseLocale != null) {
      Object localeObj = _parseLocale;

      if (localeObj instanceof Locale)
        locale = (Locale) localeObj;
      else if (localeObj instanceof String)
        locale = pc.getLocale((String) localeObj, null);
    }
    
    if (locale == null)
      locale = pc.getLocale();

    int dateStyle = DateFormat.DEFAULT;
    if (_dateStyle != null)
      dateStyle = getDateStyle(_dateStyle);

    int timeStyle = DateFormat.DEFAULT;
    if (_timeStyle != null)
      timeStyle = getDateStyle(_timeStyle);

    if (locale != null) {
      if (_type == null || _type.equals("date"))
        format = DateFormat.getDateInstance(dateStyle, locale);
      else if (_type.equals("both"))
        format = DateFormat.getDateTimeInstance(dateStyle,
                                                timeStyle,
                                                locale);
      else if (_type.equals("time"))
        format = DateFormat.getTimeInstance(timeStyle, locale);
      else
        throw new JspException(L.l("illegal type `{0}'", _type));
    }
    else {
      if (_type == null || _type.equals("date"))
        format = DateFormat.getDateInstance(dateStyle);
      else if (_type.equals("both"))
        format = DateFormat.getDateTimeInstance(dateStyle, timeStyle);
      else if (_type.equals("time"))
        format = DateFormat.getTimeInstance(timeStyle);
      else
        throw new JspException(L.l("illegal type `{0}'", _type));
    }

    if (format == null)
      return null;

    if (_pattern != null) {
      try {
        ((SimpleDateFormat) format).applyPattern(_pattern);
      } catch (ClassCastException e) {
        format = new SimpleDateFormat(_pattern, locale);
      }
    }
    
    TimeZone timeZone = getTimeZone(_timeZone);

    if (timeZone == null)
      timeZone = (TimeZone) pageContext.getAttribute("com.caucho.time-zone");
        
    if (timeZone == null)
      timeZone = getTimeZone(Config.find(pageContext, Config.FMT_TIME_ZONE));

    if (timeZone != null)
      format.setTimeZone(timeZone);

    return format;
  }

  private TimeZone getTimeZone(Object timeZoneObj)
  {
    if (timeZoneObj instanceof TimeZone)
      return (TimeZone) timeZoneObj;
    else if (timeZoneObj instanceof String) {
      String timeZoneString = (String) timeZoneObj;

      return TimeZone.getTimeZone(timeZoneString);
    }

    return null;
  }

  public static int getDateStyle(String style)
    throws JspException
  {
    if (style == null || style.equals("default"))
      return DateFormat.DEFAULT;
    else if (style.equals("short"))
      return DateFormat.SHORT;
    else if (style.equals("medium"))
      return DateFormat.MEDIUM;
    else if (style.equals("long"))
      return DateFormat.LONG;
    else if (style.equals("full"))
      return DateFormat.FULL;
    else
      throw new JspException(L.l("illegal date style `{0}'", style));
  }
}
