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

package com.caucho.jsp;

import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.ErrorData;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.tagext.BodyContent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Logger;

public class PageContextWrapper extends PageContextImpl {
  private static final Logger log
    = Logger.getLogger(PageContextWrapper.class.getName());
  static final L10N L = new L10N(PageContextWrapper.class);

  private PageContextImpl _parent;

  public void init(PageContextImpl parent)
  {
    super.init();
    
    _parent = parent;
    clearAttributes();
    setOut(parent.getOut());
    _isFilled = true;
  }

  /**
   * Returns the underlying servlet for the page.
   */
  public Object getPage()
  {
    return _parent.getPage();
  }

  /**
   * Returns the servlet request for the page.
   */
  public HttpServletRequest getRequest()
  {
    return _parent.getRequest();
  }
  
  /**
   * Returns the servlet response for the page.
   */
  public HttpServletResponse getResponse()
  {
    return _parent.getResponse();
  }
  
  /**
   * Returns the servlet response for the page.
   */
  @Override
  public HttpServletRequest getCauchoRequest()
  {
    return _parent.getCauchoRequest();
  }
  
  /**
   * Returns the servlet response for the page.
   */
  @Override
  public CauchoResponse getCauchoResponse()
  {
    return _parent.getCauchoResponse();
  }

  @Override
  public HttpSession getSession()
  {
    return _parent.getSession();
  }

  @Override
  public ServletConfig getServletConfig()
  {
    return _parent.getServletConfig();
  }

  /**
   * Returns the page's servlet context.
   */
  @Override
  public ServletContext getServletContext()
  {
    return _parent.getServletContext();
  }

  /**
   * Returns the page's application.
   */
  @Override
  public WebApp getApplication()
  {
    return _parent.getApplication();
  }

  /**
   * Returns the page's error page.
   */
  @Override
  public String getErrorPage()
  {
    return _parent.getErrorPage();
  }

  /**
   * Sets the page's error page.
   */
  @Override
  public void setErrorPage(String errorPage)
  {
    _parent.setErrorPage(errorPage);
  }

  /**
   * Returns the Throwable stored by the error page.
   */
  public Throwable getThrowable()
  {
    return _parent.getThrowable();
  }

  /**
   * Returns the error data
   */
  public ErrorData getErrorData()
  {
    return _parent.getErrorData();
  }

  /**
   * Returns the variable resolver
   */
  // jsp/10c4
  /*
  public javax.servlet.jsp.el.VariableResolver getVariableResolver()
  {
    return _parent.getVariableResolver();
  }
  */

  /**
   * Returns the current out.
   */
  public JspWriter getOut()
  {
    return _parent.getOut();
  }

  /**
   * Pushes the page body.
   */
  public BodyContent pushBody()
  {
    return _parent.pushBody();
  }

  /**
   * Pushes the page body.
   */
  public JspWriter pushBody(Writer out)
  {
    return _parent.pushBody(out);
  }

  /**
   * Pops the BodyContent from the JspWriter stack.
   *
   * @return the enclosing writer
   */
  public JspWriter popAndReleaseBody()
    throws IOException
  {
    return _parent.popAndReleaseBody();
  }

  /**
   * Pops the page body.
   */
  public JspWriter popBody()
  {
    return _parent.popBody();
  }

  public void releaseBody(BodyContentImpl out)
    throws IOException
  {
    _parent.releaseBody(out);
  }

  /**
   * Pops the BodyContent from the JspWriter stack.
   *
   * @param oldWriter the old writer
   */
  public JspWriter setWriter(JspWriter oldWriter)
  {
    return _parent.setWriter(oldWriter);
  }

  /**
   * Returns the top writer.
   */
  public PrintWriter getTopWriter()
    throws IOException
  {
    return _parent.getTopWriter();
  }

  /**
   * Returns the expression evaluator
   */
  public ExpressionEvaluator getExpressionEvaluator()
  {
    return _parent.getExpressionEvaluator();
  }

  @Override
  public void forward(String relativeUrl, String query)
    throws ServletException, IOException
  {
    _parent.forward(relativeUrl, query);
  }

  @Override
  public void forward(String relativeUrl)
    throws ServletException, IOException
  {
    _parent.forward(relativeUrl);
  }

  public void include(String relativeUrl)
    throws ServletException, IOException
  {
    _parent.include(relativeUrl);
  }
  
  public void include(String relativeUrl, String query, boolean flush)
    throws ServletException, IOException
  {
    _parent.include(relativeUrl, query, flush);
  }
  
  @Override
  public void include(String relativeUrl, boolean flush)
    throws ServletException, IOException
  {
    _parent.include(relativeUrl, flush);
  }
}
