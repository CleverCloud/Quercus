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

package com.caucho.jsp;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * Abstract implementation for the fragment support.
 */
public class StaticJspFragmentSupport extends JspFragment {
  private static final Logger log
    = Logger.getLogger(StaticJspFragmentSupport.class.getName());

  private String _value;

  private PageContext _pageContext;

  public static StaticJspFragmentSupport create(StaticJspFragmentSupport frag,
                                                PageContext pageContext,
                                                String value)
  {
    if (frag == null)
      frag = new StaticJspFragmentSupport();

    frag._value = value;
    frag._pageContext = pageContext;

    return frag;
  }

  /**
   * Returns the JSP context.
   */
  public JspContext getJspContext()
  {
    return _pageContext;
  }

  /**
   * Returns the fragment value.
   */
  public String getValue()
  {
    return _value;
  }

  /**
   * The public JspFragment interface.
   *
   * @param out optional location for the output
   * @param vars optional map to replace the page context
   */
  public void invoke(Writer out)
    throws JspException
  {
    try {
      if (out == null)
        _pageContext.getOut().write(_value);
      else
        out.write(_value);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(e);
    }
  }
}
