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

package com.caucho.jsp;

import com.caucho.el.Expr;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.JspFragment;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * Abstract implementation for the fragment support.
 */
public class ELExprFragment extends JspFragment {
  private static final Logger log 
    = Logger.getLogger(ELExprFragment.class.getName());

  // the page context
  private PageContextImpl _pageContext;
  
  // the expression
  private Expr _expr;

  /**
   * Creates the fragment.
   */
  public ELExprFragment(PageContextImpl pageContext, Expr expr)
  {
    _pageContext = pageContext;
    _expr = expr;
  }

  /**
   * Returns the JSP context.
   */
  public JspContext getJspContext()
  {
    return _pageContext;
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
    JspWriter oldOut = null;

    if (out != null)
      oldOut = _pageContext.pushBody(out);

    try {
      Object value = _expr.getValue(_pageContext.getELContext());

      if (value instanceof JspFragment)
        ((JspFragment) value).invoke(out);
      else
        _expr.toStream(_pageContext.getOut(), value);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(e);
    } finally {
      if (oldOut != null)
        _pageContext.setWriter(oldOut);
    }
  }
}
