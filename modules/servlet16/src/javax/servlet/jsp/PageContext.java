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

package javax.servlet.jsp;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.tagext.BodyContent;
import java.io.IOException;
import java.util.Enumeration;

public abstract class PageContext extends JspContext {
  public final static int PAGE_SCOPE = 1;
  public final static int REQUEST_SCOPE = 2;
  public final static int SESSION_SCOPE = 3;
  public final static int APPLICATION_SCOPE = 4;

  public final static String PAGE = "javax.servlet.jsp.jspPage";
  public final static String PAGECONTEXT = "javax.servlet.jsp.jspPageContext";
  public final static String REQUEST = "javax.servlet.jsp.jspRequest";
  public final static String RESPONSE = "javax.servlet.jsp.jspResponse";
  public final static String CONFIG = "javax.servlet.jsp.jspConfig";
  public final static String SESSION = "javax.servlet.jsp.jspSession";
  public final static String OUT = "javax.servlet.jsp.jspOut";
  public final static String APPLICATION = "javax.servlet.jsp.jspApplication";
  public final static String EXCEPTION = "javax.servlet.jsp.jspException";

  /**
   * Gets the named page attribute.
   *
   * @param name of the attribute
   */
  public abstract Object getAttribute(String name);
  /**
   * Sets the named page attribute.
   *
   * @param name name of the attribute
   * @param attribute non-null attribute value.
   */
  public abstract void setAttribute(String name, Object attribute);
  /**
   * Removes the named page attribute.
   */
  public abstract void removeAttribute(String name);

  /**
   * Returns the current output for the page.
   */
  public abstract JspWriter getOut();
  /**
   * Returns the request's session.
   */
  public abstract HttpSession getSession();
  /**
   * Return the servlet object for the page.
   */
  public abstract Object getPage();
  /**
   * Returns the ServletRequest for the page.
   */
  public abstract ServletRequest getRequest();
  /**
   * Returns the ServletResponse for the page.
   */
  public abstract ServletResponse getResponse();
  /**
   * Returns the exception for error pages.
   */
  public abstract Exception getException();
  /**
   * Returns the servletConfig for the JSP page.
   */
  public abstract ServletConfig getServletConfig();
  /**
   * Returns the servletContext (application object) for the request.
   */
  public abstract ServletContext getServletContext();
  /**
   * Forwards the request relative to the current URL.
   */
  public abstract void forward(String relativeUrl)
    throws ServletException, IOException;

  /**
   * Includes the a page relative to the current URL.
   */
  public abstract void include(String relativeUrl)
    throws ServletException, IOException;

  /**
   * Includes the a page relative to the current URL.
   */
  public abstract void include(String relativeUrl, boolean flush)
    throws ServletException, IOException;
  /**
   * Internal routine to initialize the PageContext for a page.
   */
  public abstract void initialize(Servlet servlet,
                                  ServletRequest request,
                                  ServletResponse response,
                                  String errorPageURL,
                                  boolean needsSession,
                                  int bufferSize,
                                  boolean autoFlush)
    throws IOException, IllegalStateException, IllegalArgumentException;

  /**
   * Internal routine to support BodyTags.  Pushes the new bodyContent
   * to become the value of getOut().
   */
  public BodyContent pushBody()
  {
    return null;
  }

  /**
   * Internal routine to support errorPages.  Kept for backward
   * compatibility.
   */
  public abstract void handlePageException(Exception e)
    throws ServletException, IOException;
  /**
   * Internal routine to support errorPages
   */
  public abstract void handlePageException(Throwable t)
    throws ServletException, IOException;
  /**
   * Internal routine to free PageContext resources at the end of a page.
   */
  public abstract void release();

  /**
   * Returns an error data instance.
   */
  public ErrorData getErrorData() {
    return null;
  }

}
