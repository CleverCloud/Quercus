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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.UnsupportedEncodingException;

/**
 * Sets the i18n locale bundle for the current page.
 */
public class FmtRequestEncodingTag extends TagSupport {
  private static L10N L = new L10N(FmtRequestEncodingTag.class);
  
  private String _value;

  /**
   * Sets the JSP-EL expression for the locale value.
   */
  public void setValue(String value)
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
    HttpServletRequest request = (HttpServletRequest) pc.getRequest();

    try {
      String value = null;
      if (_value != null)
        value = _value;
      else
        value = (String) Config.find(pageContext, "javax.servlet.jsp.jstl.fmt.request.charset");

      if (value != null && ! value.equals(""))
        request.setCharacterEncoding(value);
      else if (request.getCharacterEncoding() == null)
        request.setCharacterEncoding("ISO-8859-1");
    } catch (UnsupportedEncodingException e) {
      throw new JspException(e);
    }

    return SKIP_BODY;
  }
}
