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

package com.caucho.sql;

import com.caucho.util.ExceptionWrapper;

import java.sql.SQLException;

/**
 * Wraps an exception in a SQLException wrapper.
 */
public class SQLExceptionWrapper extends java.sql.SQLException
  implements ExceptionWrapper {
  private Throwable _rootCause;

  /**
   * Creates the wrapper with a message.
   */
  public SQLExceptionWrapper(String message)
  {
    super(message);
  }

  /**
   * Creates the wrapper with a message and a root cause.
   *
   * @param message the message.
   * @param e the rootCause exception
   */
  public SQLExceptionWrapper(String message, Throwable e)
  {
    super(message);

    _rootCause = e;
  }
  
  /**
   * Creates the wrapper with a root cause.
   *
   * @param message the message.
   * @param e the rootCause exception
   */
  public SQLExceptionWrapper(Throwable e)
  {
    super(e.toString());

    _rootCause = e;
  }

  public static SQLException create(Throwable e)
  {
    if (e instanceof SQLException)
      return (SQLException) e;
    else
      return new SQLExceptionWrapper(e);
  }

  /**
   * Returns the root cause.
   */
  public Throwable getRootCause()
  {
    return _rootCause;
  }

  /**
   * Returns the root cause.
   */
  public Throwable getCause()
  {
    return _rootCause;
  }
}
