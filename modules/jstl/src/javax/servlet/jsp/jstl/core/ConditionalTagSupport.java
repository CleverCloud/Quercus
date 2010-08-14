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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

abstract public class ConditionalTagSupport extends TagSupport {
  private String _var;
  private String _scope;

  public void setVar(String var)
  {
    _var = var;
  }

  public void setScope(String scope)
  {
    _scope = scope;
  }

  protected abstract boolean condition() throws JspTagException;

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    boolean test = condition();
    Boolean value = test ? Boolean.TRUE : Boolean.FALSE;

    if (_var == null) {
    }
    else if (_scope == null || _scope.equals("") || _scope.equals("page"))
      pageContext.setAttribute(_var, value);
    else if (_scope.equals("request"))
      pageContext.getRequest().setAttribute(_var, value);
    else if (_scope.equals("session"))
      pageContext.getSession().setAttribute(_var, value);
    else if (_scope.equals("application"))
      pageContext.getServletContext().setAttribute(_var, value);
    else
      throw new JspException("unknown scope `" + _scope + "'");

    return test ? EVAL_BODY_INCLUDE : SKIP_BODY;
  }
}
