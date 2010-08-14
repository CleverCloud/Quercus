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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import javax.servlet.jsp.jstl.core.*;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.Locale;

/**
 * Sets the i18n localization bundle for the current page.
 */
public class SetBundleTag extends I18NSupport {
  private static L10N L = new L10N(SetBundleTag.class);
  
  private String _basename;
  private String _var = Config.FMT_LOCALIZATION_CONTEXT;
  private String _scope;

  /**
   * Sets the basename.
   */
  public void setBasename(String basename)
  {
    _basename = basename;
  }

  /**
   * Sets variable to store the bundle.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope to store the bundle.
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
    PageContextImpl pc = (PageContextImpl) pageContext;

    LocalizationContext bundle = pc.getBundle(_basename);

    if (_scope == null || _scope.equals("") || _scope.equals("page"))
      pc.setAttribute(_var, bundle);
    else if (_scope.equals("request"))
      pc.getRequest().setAttribute(_var, bundle);
    else if (_scope.equals("session"))
      pc.getSession().setAttribute(_var, bundle);
    else if (_scope.equals("application"))
      pc.getServletContext().setAttribute(_var, bundle);
    else
      throw new JspException(L.l("unknown scope `{0}' in fmt:setBundle",
                                 _scope));

    Locale locale = bundle.getLocale();

    if (locale != null)
      setResponseLocale(pageContext, locale);

    return SKIP_BODY;
  }
}
