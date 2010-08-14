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

package javax.servlet.jsp;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * JspFactory objects should not be used by JSP authors.
 */
public abstract class JspFactory {
  private static JspFactory _factory;

  public static JspFactory getDefaultFactory()
  {
    return _factory;
  }

  public static void setDefaultFactory(JspFactory deflt)
  {
    _factory = deflt;
  }

  public abstract PageContext getPageContext(Servlet servlet,
                                             ServletRequest request,
                                             ServletResponse response,
                                             String errorPageURL,
                                             boolean needsSession,
                                             int buffer,
                                             boolean autoflush);

  public abstract void releasePageContext(PageContext pc);

  public abstract JspEngineInfo getEngineInfo();

  public abstract JspApplicationContext
    getJspApplicationContext(ServletContext context);
}
