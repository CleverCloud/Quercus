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

package javax.servlet;

/**
 * Asynchronous/comet servlet support.
 *
 * @since Servlet 3.0
 */
public interface AsyncContext {
  static final String ASYNC_CONTEXT_PATH = "javax.servlet.async.context_path";
  static final String ASYNC_PATH_INFO = "javax.servlet.async.path_info";
  static final String ASYNC_QUERY_STRING = "javax.servlet.async.query_string";
  static final String ASYNC_REQUEST_URI = "javax.servlet.async.request_uri";
  static final String ASYNC_SERVLET_PATH = "javax.servlet.async.servlet_path";

  public ServletRequest getRequest();

  public ServletResponse getResponse();

  public boolean hasOriginalRequestAndResponse();

  public void dispatch();

  public void dispatch(String path);

  public void dispatch(ServletContext context, String path);

  public void complete();

  public void start(Runnable run);

  public void addListener(AsyncListener listener);

  public void addListener(AsyncListener listener,
                          ServletRequest request,
                          ServletResponse response);

  public <T extends AsyncListener> T createListener(Class<T> cl)
    throws ServletException;

  public void setTimeout(long timeout);

  public long getTimeout();
}
