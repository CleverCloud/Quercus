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

import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.jstl.sql.SQLExecutionTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Looks up an i18n message from a bundle and prints it.
 */
public class SqlDateParamTag extends TagSupport {
  private static L10N L = new L10N(SqlDateParamTag.class);
  
  private Object _value;
  private String _type;

  /**
   * Sets the value
   *
   * @param value the sql value.
   */
  public void setValue(Object value)
  {
    _value = value;
  }

  /**
   * Sets the type
   *
   * @param type the type.
   */
  public void setType(String type)
  {
    _type = type;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    Object value = _value;

    long time = 0;

    Object result = null;

    if (value == null) {
    }
    else if (value instanceof Number)
      time = ((Number) value).longValue();
    else if (value instanceof java.util.Date)
      time = ((java.util.Date) value).getTime();
    else if (value instanceof java.sql.Date)
      time = ((java.sql.Date) value).getTime();
    else
      throw new JspException(L.l("sql:dateParam requires at date at '{0}'", value));

    if (value == null)
      result = null;
    else if (_type == null)
      result = new java.sql.Timestamp(time);
    else if (_type.equals("time"))
      result = new java.sql.Time(time);
    else if (_type.equals("date"))
      result = new java.sql.Date(time);
    else if (_type.equals("timestamp"))
      result = new java.sql.Timestamp(time);
    else
      throw new JspException(L.l("sql:dateParam type='{0}' is an unknown type",
                                 _type));

    Tag parent = getParent();
    for (;
         parent != null && ! (parent instanceof SQLExecutionTag);
         parent = parent.getParent()) {
    }

    if (parent == null)
      throw new JspException(L.l("sql:dateParam requires sql:query parent.")); 

    SQLExecutionTag tag = (SQLExecutionTag) parent;

    tag.addSQLParameter(result);
    
    return SKIP_BODY;
  }
}
