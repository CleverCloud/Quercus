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

package javax.script.http;

import javax.script.ScriptContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;

/**
 * Context information from an engine to the namespace.
 */
public interface HttpScriptContext extends ScriptContext {
  public static int REQUEST_SCOPE = 0;
  public static int APPLICATION_SCOPE = 175;
  public static int SESSION_SCOPE = 150;

  /**
   * Initialize the context with the current servlet.
   */
  public void initialize(Servlet servlet,
                         HttpServletRequest request,
                         HttpServletResponse response)
    throws ServletException;

  /**
   * Clears the state of the context.
   */
  public void release();

  /**
   * Returns an attribute from the given scope.
   */
  public Object getAttribute(String name, int scope);

  /**
   * Returns an attribute with the lowest score.
   */
  public Object getAttribute(String name);

  /**
   * Sets an attribute from the given scope.
   */
  public void setAttribute(String name, Object value, int scope);

  /**
   * Returns a reader from the source.
   */
  public Reader getScriptSource();

  /**
   * Returns the http request.
   */
  public HttpServletRequest getRequest();

  /**
   * Returns the http response
   */
  public HttpServletResponse getResponse();

  /**
   * Returns the servlet
   */
  public Servlet getServlet();

  /**
   * Forwards to the relative path.
   */
  public void forward(String path)
    throws ServletException, IOException;

  /**
   * Includes the relative path.
   */
  public void include(String path)
    throws ServletException, IOException;

  /**
   * Returns the script enable/disable state.
   */
  public boolean disableScript();

  /**
   * Returns the script session state.
   */
  public boolean useSession();

  /**
   * True if the servlet should display the results.
   */
  public boolean displayResults();

  /**
   * Returns the HTTP request methods allowed.
   */
  public String []getMethods();

  /**
   * Returns the allowed script languages.
   */
  public String []getAllowedLanguages();
}

