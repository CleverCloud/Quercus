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
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.TimeZone;

/**
 * Sets the time zone for the current page.
 */
public class FmtSetTimeZoneTag extends TagSupport {
  private static L10N L = new L10N(FmtSetTimeZoneTag.class);
  
  private Object _value;
  private String _var;
  private String _scope;

  /**
   * Sets the time zone value.
   */
  public void setValue(Object value)
  {
    _value = value;
  }

  /**
   * Sets the variable containing the time zone.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope to store the bundle.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    PageContextImpl pc = (PageContextImpl) pageContext;

    TimeZone timeZone = null;

    if (_value instanceof TimeZone) {
      timeZone = (TimeZone) _value;
    }
    else if (_value instanceof String) {
      String string = (String) _value;
      string = string.trim();

      if (string.equals(""))
        timeZone = TimeZone.getTimeZone("GMT");
      else
        timeZone = TimeZone.getTimeZone(string);
    }
    else
      timeZone = TimeZone.getTimeZone("GMT");

    String var = _var;
    if (var == null)
      var = Config.FMT_TIME_ZONE;
    
    CoreSetTag.setValue(pc, var, _scope, timeZone);

    return SKIP_BODY;
  }
}
