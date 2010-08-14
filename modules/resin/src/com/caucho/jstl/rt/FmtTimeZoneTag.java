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

package com.caucho.jstl.rt;

import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;
import java.util.TimeZone;

/**
 * Sets the i18n localization bundle for a context.
 */
public class FmtTimeZoneTag extends TagSupport implements TryCatchFinally {
  private static L10N L = new L10N(FmtTimeZoneTag.class);
  
  private Object _value;

  private Object _oldTimeZone;
  
  /**
   * Sets the JSP-EL expression for the basename.
   */
  public void setValue(Object value)
  {
    _value = value;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    PageContextImpl pc = (PageContextImpl) pageContext;

    _oldTimeZone = pc.getAttribute("com.caucho.time-zone");

    Object valueObj = _value;
    TimeZone timeZone = null;

    if (valueObj instanceof TimeZone) {
      timeZone = (TimeZone) valueObj;
    }
    else if (valueObj instanceof String) {
      String string = (String) valueObj;
      string = string.trim();

      if (string.equals(""))
        timeZone = TimeZone.getTimeZone("GMT");
      else
        timeZone = TimeZone.getTimeZone(string);
    }
    else
      timeZone = TimeZone.getTimeZone("GMT");

    pc.setAttribute("com.caucho.time-zone", timeZone);

    return EVAL_BODY_INCLUDE;
  }

  /**
   * Handle the catch
   */
  public void doCatch(Throwable t)
    throws Throwable
  {
    throw t;
  }
  
  /**
   * Handle the finaly
   */
  public void doFinally()
  {
    if (_oldTimeZone == null)
      pageContext.removeAttribute("com.caucho.time-zone");
    else
      pageContext.setAttribute("com.caucho.time-zone", _oldTimeZone);
  }
}
