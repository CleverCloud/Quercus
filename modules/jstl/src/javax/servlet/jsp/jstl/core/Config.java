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

package javax.servlet.jsp.jstl.core;

import java.util.logging.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

public class Config {
  private static final Logger log = Logger.getLogger(Config.class.getName());
  /**
   * The application's configured locale.
   */
  public static final String FMT_LOCALE
    = "javax.servlet.jsp.jstl.fmt.locale";

  /**
   * The fallback locale if none is found.
   */
  public static final String FMT_FALLBACK_LOCALE
    = "javax.servlet.jsp.jstl.fmt.fallbackLocale";
  
  /**
   * The current i18n localization context.
   */
  public static final String FMT_LOCALIZATION_CONTEXT
    = "javax.servlet.jsp.jstl.fmt.localizationContext";
  
  /**
   * The current time zone.
   */
  public static final String FMT_TIME_ZONE
    = "javax.servlet.jsp.jstl.fmt.timeZone";
  
  /**
   * The current Data Source.
   */
  public static final String SQL_DATA_SOURCE
    = "javax.servlet.jsp.jstl.sql.dataSource";

  /**
   * The maximum rows to read from the database.
   */
  public static final String SQL_MAX_ROWS
    = "javax.servlet.jsp.jstl.sql.maxRows";

  public static Object find(PageContext pageContext, String name)
  {
    Object object = pageContext.findAttribute(name);

    if (object != null)
      return object;

    return pageContext.getServletContext().getInitParameter(name);
  }

  /**
   * Returns an attribute from the page context.
   */
  public static Object get(PageContext pageContext, String name, int scope)
  {
    return pageContext.getAttribute(name, scope);
  }

  public static Object get(ServletRequest request, String name)
  {
    return request.getAttribute(name);
  }

  public static Object get(HttpSession session, String name)
  {
    try {
      if (session != null)
        return session.getAttribute(name);
    } catch (IllegalStateException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return null;
  }

  public static Object get(ServletContext context, String name)
  {
    return context.getAttribute(name);
  }

  /**
   * Returns an attribute from the page context.
   */
  public static void remove(PageContext pageContext, String name, int scope)
  {
    pageContext.removeAttribute(name, scope);
  }

  public static void remove(ServletRequest request, String name)
  {
    request.removeAttribute(name);
  }

  public static void remove(HttpSession session, String name)
  {
    session.removeAttribute(name);
  }

  public static void remove(ServletContext context, String name)
  {
    context.removeAttribute(name);
  }

  public static void set(ServletRequest request,
                         String name,
                         Object var)
  {
    request.setAttribute(name, var);
  }

  public static void set(HttpSession session,
                         String name,
                         Object var)
  {
    try {
      if (session != null)
        session.setAttribute(name, var);
    } catch (IllegalStateException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public static void set(ServletContext context,
                         String name,
                         Object var)
  {
    context.setAttribute(name, var);
  }

  public static void set(PageContext pageContext,
                         String name,
                         Object var,
                         int scope)
  {
    pageContext.setAttribute(name, var, scope);
  }
}
