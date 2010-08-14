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

import com.caucho.jstl.NameValueTag;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

public class CoreRedirectTag extends TagSupport implements NameValueTag {
  private static L10N L = new L10N(CoreRedirectTag.class);
  
  private String _value;
  private String _context;

  private CharBuffer _url;

  /**
   * Sets the URL to be imported.
   */
  public void setURL(String value)
  {
    _value = value;
  }
  
  /**
   * Sets the external context for the import.
   */
  public void setContext(String context)
  {
    _context = context;
  }

  /**
   * Adds a parameter.
   */
  public void addParam(String name, String value)
  {
    String encoding = this.pageContext.getResponse().getCharacterEncoding();
    
    CoreUrlTag.addParam(_url, name, value, encoding);
  }

  public int doStartTag() throws JspException
  {
    _url = CoreUrlTag.normalizeURL(pageContext, _value, _context);

    return EVAL_BODY_INCLUDE;
  }
      
  public int doEndTag() throws JspException
  {
    String value = CoreUrlTag.encodeURL(pageContext, _url);

    try {
      HttpServletResponse response;
      ((HttpServletResponse) pageContext.getResponse()).sendRedirect(value);
    } catch (IOException e) {
      throw new JspException(e);
    }

    return SKIP_PAGE;
  }
}
