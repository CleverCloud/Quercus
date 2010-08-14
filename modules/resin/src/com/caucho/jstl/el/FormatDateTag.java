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
 * @author Scott Ferguson
 */

package com.caucho.jstl.el;

import com.caucho.el.Expr;
import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.el.ELException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Formats an i18n date and prints it.
 */
public class FormatDateTag extends BodyTagSupport {
  private static L10N L = new L10N(FormatDateTag.class);
  
  private Expr _valueExpr;
  
  private Expr _typeExpr;
  private Expr _dateStyleExpr;
  private Expr _timeStyleExpr;
  
  private Expr _patternExpr;
  private Expr _timeZoneExpr;

  private String _var;
  private String _scope;

  /**
   * Sets the formatting value.
   *
   * @param value the JSP-EL expression for the value.
   */
  public void setValue(Expr value)
  {
    _valueExpr = value;
  }

  /**
   * Sets the date/time type.
   *
   * @param type the date/time type.
   */
  public void setType(Expr type)
  {
    _typeExpr = type;
  }

  /**
   * Sets the date style (full, short, etc.)
   *
   * @param style the date style
   */
  public void setDateStyle(Expr style)
  {
    _dateStyleExpr = style;
  }

  /**
   * Sets the time style (full, short, etc.)
   *
   * @param style the time style
   */
  public void setTimeStyle(Expr style)
  {
    _timeStyleExpr = style;
  }

  /**
   * Sets the formatting pattern.
   *
   * @param pattern the formatting pattern.
   */
  public void setPattern(Expr pattern)
  {
    _patternExpr = pattern;
  }

  /**
   * Sets the time zone.
   *
   * @param zone the time zone expression
   */
  public void setTimeZone(Expr zone)
  {
    _timeZoneExpr = zone;
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
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      
      JspWriter out = pageContext.getOut();

      Object value = _valueExpr.evalObject(pageContext.getELContext());

      if (value == null) {
        if (_var != null)
          CoreSetTag.setValue(pageContext, _var, _scope, null);
      
        return EVAL_PAGE;
      }

      long time = 0;

      if (value instanceof Number)
        time = ((Number) value).longValue();
      else if (value instanceof Date)
        time = ((Date) value).getTime();
      
      DateFormat format = null;

      Locale locale = pageContext.getLocale();

      String type = null;

      ELContext env = pageContext.getELContext();

      if (_typeExpr != null)
        type = _typeExpr.evalString(env);

      int dateStyle = DateFormat.DEFAULT;
      if (_dateStyleExpr != null)
        dateStyle = getDateStyle(_dateStyleExpr.evalString(env));

      int timeStyle = DateFormat.DEFAULT;
      if (_timeStyleExpr != null)
        timeStyle = getDateStyle(_timeStyleExpr.evalString(env));

      if (locale != null) {
        if (type == null || type.equals("date"))
          format = DateFormat.getDateInstance(dateStyle, locale);
        else if (type.equals("both"))
          format = DateFormat.getDateTimeInstance(dateStyle,
                                                  timeStyle,
                                                  locale);
        else if (type.equals("time"))
          format = DateFormat.getTimeInstance(timeStyle, locale);
        else
          throw new JspException(L.l("illegal type `{0}'", type));
      }
      else {
        if (type == null || type.equals("date"))
          format = DateFormat.getDateInstance(dateStyle);
        else if (type.equals("both"))
          format = DateFormat.getDateTimeInstance(dateStyle, timeStyle);
        else if (type.equals("time"))
          format = DateFormat.getTimeInstance(timeStyle);
        else
          throw new JspException(L.l("illegal type `{0}'", type));
      }

      if (format != null && _patternExpr != null) {
        String pattern = _patternExpr.evalString(env);
        try {
          ((SimpleDateFormat) format).applyPattern(pattern);
        } catch (ClassCastException e) {
          format = new SimpleDateFormat(pattern, locale);
        }
      }

      if (format != null) {
        TimeZone timeZone = getTimeZone();

        if (timeZone != null)
          format.setTimeZone(timeZone);

        value = format.format(new Date(time));
      }

      if (_var == null)
        out.print(value);
      else
        CoreSetTag.setValue(pageContext, _var, _scope, value);
    } catch (Exception e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }

  private TimeZone getTimeZone()
    throws ELException
  {
    if (_timeZoneExpr != null) {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      Object timeZoneObj = _timeZoneExpr.evalObject(pageContext.getELContext());

      TimeZone zone = getTimeZone(timeZoneObj);
      if (zone != null)
        return zone;
    }

    Object timeZoneObj = pageContext.getAttribute("com.caucho.time-zone");

    if (timeZoneObj != null)
      return (TimeZone) timeZoneObj;

    timeZoneObj = Config.find(pageContext, Config.FMT_TIME_ZONE);

    return getTimeZone(timeZoneObj);
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

