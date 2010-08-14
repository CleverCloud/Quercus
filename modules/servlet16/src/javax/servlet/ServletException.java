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
 * A servlet exception.  ServletException can wrap another exception,
 * so the servlet can rethrow an exception to the servlet engine.
 *
 * <pre><code>
 * try {
 *   ...;
 * } catch (Exception e) {
 *   throw new ServletException(e);
 * }
 * </code></pre>
 */
public class ServletException extends Exception {
  /**
   * Basic exception constructor.
   */
  public ServletException()
  {
  }

  /**
   * Create a new servlet exception with a message.
   */
  public ServletException(String message)
  {
    super(message);
  }

  /**
   * Create a new servlet exception with a message and a wrapped exception.
   */
  public ServletException(String message, Throwable cause)
  {
    super(message, cause);
  }


  /**
   * Create a new servlet exception with a wrapped exception.
   */
  public ServletException(Throwable cause)
  {
    super(cause);
  }

  public Throwable getRootCause()
  {
    return getCause();
  }
}
