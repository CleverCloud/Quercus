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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.servlet;

import java.io.IOException;

/**
 * The RequestDispatcher gives servlets the capabilities of SSI includes.
 * The forwarded or included page is handled as a normal page request.
 *
 * <pre><code>
 * RequestDispatcher disp;
 * disp = request.getRequestDispatcher("inc.jsp?a=b");
 * disp.include(request, response);
 * </code></pre>
 *
 * <p>Servlets typically use <code>ServletRequest.setAttribute()</code>
 * to communicate between included pages.
 *
 * <p>A popular architecture uses servlets to process the initial request
 * and JSP files to format the results.  That template architecture uses
 * request attributes to communicate data from the servlet to the JSP page.
 * <code>disp.forward()</code> transfers the request to the JSP file.
 */
public interface RequestDispatcher {
  public static final String ERROR_MESSAGE = "javax.servlet.error.message";
  public static final String ERROR_EXCEPTION = "javax.servlet.error.exception";
  public static final String ERROR_EXCEPTION_TYPE
    = "javax.servlet.error.exception_type";
  public static final String ERROR_REQUEST_URI 
    = "javax.servlet.error.request_uri";
  public static final String ERROR_SERVLET_NAME
    = "javax.servlet.error.servlet_name";
  public static final String ERROR_STATUS_CODE
    = "javax.servlet.error.status_code";
  
  public static final String FORWARD_CONTEXT_PATH
    = "javax.servlet.forward.context_path";
  public static final String FORWARD_PATH_INFO
    = "javax.servlet.forward.path_info";
  public static final String FORWARD_QUERY_STRING
    = "javax.servlet.forward.query_string";
  public static final String FORWARD_REQUEST_URI
    = "javax.servlet.forward.request_uri";
  public static final String FORWARD_SERVLET_PATH
    = "javax.servlet.forward.servlet_path";
  
  public static final String INCLUDE_CONTEXT_PATH
    = "javax.servlet.include.context_path";
  public static final String INCLUDE_PATH_INFO
    = "javax.servlet.include.path_info";
  public static final String INCLUDE_QUERY_STRING
    = "javax.servlet.include.query_string";
  public static final String INCLUDE_REQUEST_URI
    = "javax.servlet.include.request_uri";
  public static final String INCLUDE_SERVLET_PATH
    = "javax.servlet.include.servlet_path";
  
  /**
   * Forwards the request to another page.  Forward may not be called
   * if data has been sent to the client.  Specifically,
   * forward calls the <code>response.reset()</code> method to clear
   * the output buffer.
   *
   * <p>Query parameters are added to the original query parameters.
   *
   * <p>The new URI values are based on the RequestDispatcher URI.
   * So getRequestURI(), getServletPath(), and getPathInfo() will reflect
   * the request dispatcher URI.
   *
   * @param request the original request
   * @param response the original response
   */
  public void forward(ServletRequest request, ServletResponse response)
    throws ServletException, IOException;
  /**
   * Includes the result of another page.
   *
   * <p>Query parameters are added to the original query parameters.
   *
   * <p>The included request's URI methods reflect the <em>original</em>
   * URI data.  So getRequestURI() will return the URI sent by
   * the browser.
   *
   * <p>Included pages should use request.getAttribute() to get the
   * new URI values:
   * <table>
   * <tr><td>getRequestURI<td>javax.servlet.include.request_uri
   * <tr><td>getContextPath<td>javax.servlet.include.context_path
   * <tr><td>getServletPath<td>javax.servlet.include.servlet_path
   * <tr><td>getPathInfo<td>javax.servlet.include.path_info
   * <tr><td>getQueryString<td>javax.servlet.include.query_string
   * </table>
   *
   * @param request the original request
   * @param response the original response
   */
  public void include(ServletRequest request, ServletResponse response)
    throws ServletException, IOException;
}
