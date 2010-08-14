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

package com.caucho.jstl.el;

import com.caucho.el.Expr;
import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

/**
 * Sets the i18n localization bundle for a context.
 */
public class BundleTag extends TagSupport implements TryCatchFinally {
  private static L10N L = new L10N(BundleTag.class);
  
  private Expr _basenameExpr;
  private Expr _prefixExpr;

  private Object _oldBundle;
  private Object _oldPrefix;

  /**
   * Sets the JSP-EL expression for the basename.
   */
  public void setBasename(Expr basename)
  {
    _basenameExpr = basename;
  }

  /**
   * Sets a prefix for message keys.
   */
  public void setPrefix(Expr prefix)
  {
    _prefixExpr = prefix;
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    PageContextImpl pc = (PageContextImpl) pageContext;

    String basename = _basenameExpr.evalString(pc.getELContext());

    _oldBundle = pc.getAttribute("caucho.bundle");
    _oldPrefix = pc.getAttribute("caucho.bundle.prefix");
    
    LocalizationContext bundle = pc.getBundle(basename);

    pc.setAttribute("caucho.bundle", bundle);

    if (_prefixExpr != null) {
      String prefix = _prefixExpr.evalString(pc.getELContext());

      pc.setAttribute("caucho.bundle.prefix", prefix);
    }
    else if (_oldPrefix != null)
      pc.removeAttribute("caucho.bundle.prefix");

    return EVAL_BODY_INCLUDE;
  }

  /**
   * Handle the catch
   */
  public void doCatch(Throwable t)
    throws Throwable
  {
    throw t;
  }
  
  /**
   * Handle the finaly
   */
  public void doFinally()
  {
    if (_oldBundle == null)
      pageContext.removeAttribute("caucho.bundle");
    else
      pageContext.setAttribute("caucho.bundle", _oldBundle);
    
    if (_oldPrefix == null)
      pageContext.removeAttribute("caucho.bundle.prefix");
    else
      pageContext.setAttribute("caucho.bundle.prefix", _oldPrefix);
  }

  public static Object setBundle(String baseName, PageContextImpl pc)
  {
    Object oldBundle = pc.getAttribute("caucho.bundle");
    
    LocalizationContext bundle = pc.getBundle(baseName);

    pc.setAttribute("caucho.bundle", bundle);

    return oldBundle;
  }
}
