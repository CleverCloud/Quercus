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

/**
 * Information about an error for error pages.
 */
public final class ErrorData {
  private Throwable _throwable;
  private int _statusCode;
  private String _uri;
  private String _servletName;

  /**
   * Create a new errorData object.
   */
  public ErrorData(Throwable throwable,
                   int statusCode,
                   String uri,
                   String servletName)
  {
    _throwable = throwable;
    _statusCode = statusCode;
    _uri = uri;
    _servletName = servletName;
  }

  /**
   * Returns the error's request URI.
   */
  public String getRequestURI()
  {
    return _uri;
  }

  /**
   * Returns the error's servlet-name.
   */
  public String getServletName()
  {
    return _servletName;
  }

  /**
   * Returns the error's status code
   */
  public int getStatusCode()
  {
    return _statusCode;
  }

  /**
   * Returns the Throwable which caused the error
   */
  public Throwable getThrowable()
  {
    return _throwable;
  }
}
