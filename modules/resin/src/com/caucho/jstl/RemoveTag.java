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

package com.caucho.jstl;

import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

public class RemoveTag extends TagSupport {
  private static L10N L = new L10N(RemoveTag.class);

  private String var;
  private String scope;

  /**
   * Sets the variable to assign
   */
  public void setVar(String var)
  {
    this.var = var;
  }

  /**
   * Sets the scope of the variable.
   */
  public void setScope(String scope)
  {
    this.scope = scope;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    if (scope == null) {
      pageContext.removeAttribute(var);
    }
    else if (scope.equals("page")) {
      pageContext.removeAttribute(var);
    }
    else if (scope.equals("request")) {
      pageContext.getRequest().removeAttribute(var);
    }
    else if (scope.equals("session")) {
      pageContext.getSession().removeAttribute(var);
    }
    else if (scope.equals("application")) {
      pageContext.getServletContext().removeAttribute(var);
    }
    else
      throw new JspException(L.l("illegal scope value {0}", scope));

    return SKIP_BODY;
  }
}
