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
public class AsyncEvent {
  private final AsyncContext _context;
  private final ServletRequest _request;
  private final ServletResponse _response;
  private final Throwable _throwable;

  public AsyncEvent(AsyncContext context)
  {
    this(context, null, null, null);
  }

  public AsyncEvent(AsyncContext context,
                    ServletRequest request,
                    ServletResponse response)
  {
    this(context, request, response, null);
  }

  public AsyncEvent(AsyncContext context, Throwable throwable)
  {
    this(context, null, null, throwable);
  }

  public AsyncEvent(AsyncContext context,
                    ServletRequest request,
                    ServletResponse response,
                    Throwable throwable)
  {
    _context = context;
    _request = request;
    _response = response;
    _throwable = throwable;
  }

  public AsyncContext getAsyncContext()
  {
    return _context;
  }

  public Throwable getThrowable()
  {
    return _throwable;
  }

  public ServletRequest getSuppliedRequest()
  {
    return _request;
  }

  public ServletResponse getSuppliedResponse()
  {
    return _response;
  }
}
