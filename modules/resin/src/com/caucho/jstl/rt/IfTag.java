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
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Tag representing an "if" condition.
 */
public class IfTag extends TagSupport {
  private static L10N L = new L10N(IfTag.class);
  
  private boolean _test;
  private String _var;
  private String _scope;

  /**
   * Sets the test value.
   */
  public void setTest(boolean test)
  {
    _test = test;
  }

  /**
   * Sets the variable which should contain the result of the test.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope for the variable.
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
    if (_var == null) {
    }
    else
      CoreSetTag.setValue(pageContext, _var, _scope, new Boolean(_test));

    return _test ? EVAL_BODY_INCLUDE : SKIP_BODY;
  }
}
