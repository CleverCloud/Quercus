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

import com.caucho.el.Expr;
import com.caucho.jsp.BodyContentImpl;
import com.caucho.jsp.PageContextImpl;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;

public class OutTag extends BodyTagSupport {
  private Object _value;
  private boolean _escapeXml = true;
  private String _defaultValue;

  /**
   * Sets the value.
   */
  public void setValue(Object value)
  {
    _value = value;
  }

  /**
   * Sets true if XML should be escaped.
   */
  public void setEscapeXml(boolean value)
  {
    _escapeXml = value;
  }

  /**
   * Sets the default value.
   */
  public void setDefault(String value)
  {
    _defaultValue = value;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      
      JspWriter out = pageContext.getOut();

      if (_value != null) {
        if (_escapeXml)
          Expr.toStreamEscaped(out, _value);
        else
          out.print(_value);
      }
      else if (_defaultValue != null) {
        if (_escapeXml)
          Expr.toStreamEscaped(out, _defaultValue);
        else
          out.print(_defaultValue);
      }
      else
        return EVAL_BODY_BUFFERED;
    } catch (IOException e) {
    }

    return SKIP_BODY;
  }

  public int doEndTag() throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      
      JspWriter out = pageContext.getOut();
      
      BodyContentImpl body = (BodyContentImpl) getBodyContent();

      if (body != null) {
        String s = body.getString().trim();

        if (_escapeXml)
          Expr.toStreamEscaped(out, s);
        else
          out.print(s);
      }
    } catch (Exception e) {
    }

    return EVAL_PAGE;
  }
}
