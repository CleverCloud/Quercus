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

import com.caucho.jstl.ParamContainerTag;
import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Looks up an i18n message from a bundle and prints it.
 */
public class FmtParamTag extends BodyTagSupport {
  private static L10N L = new L10N(FmtParamTag.class);
  
  private Object _value;
  private boolean _hasValue;

  /**
   * Sets the value
   *
   * @param key the JSP-EL expression for the key.
   */
  public void setValue(Object value)
  {
    _value = value;
    _hasValue = true;
  }

  /**
   * Process the tag.
   */
  public int doEndTag()
    throws JspException
  {
    Object value = null;
    
    if (_hasValue)
      value = _value;
    else
      value = bodyContent.getString().trim();

    Object parent = getParent();
    if (! (parent instanceof ParamContainerTag))
      throw new JspException(L.l("fmt:param requires fmt:message parent.")); 

    ParamContainerTag message = (ParamContainerTag) parent;

    message.addParam(value);
    
    return EVAL_PAGE;
  }
}
