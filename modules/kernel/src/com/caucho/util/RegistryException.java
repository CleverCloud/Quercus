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

package com.caucho.util;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Base class for configuration exceptions.  Thrown by RegistryNode
 * classes.
 */
public class RegistryException extends java.io.IOException
  implements ExceptionWrapper {
  private Throwable _rootCause;

  /**
   * Create a null exception
   */
  public RegistryException()
  {
  }

  /**
   * Creates an exception with a message
   */
  public RegistryException(String msg)
  {
    super(msg);
  }

  /**
   * Wraps an exception in the config exception
   */
  public RegistryException(String msg, Throwable e)
  {
    super(msg);

    _rootCause = e;
  }

  /**
   * Wraps an exception in the config exception
   */
  public RegistryException(Throwable e)
  {
    _rootCause = e;
  }

  /**
   * Returns the root cause, if any.
   */
  public Throwable getRootCause()
  {
    return _rootCause;
  }

  /**
   * Prints the stack trace, preferring the root cause if it exists.
   */
  public void printStackTrace()
  {
    if (_rootCause != null)
      _rootCause.printStackTrace();
    else
      super.printStackTrace();
  }

  /**
   * Prints the stack trace, preferring the root cause if it exists.
   */
  public void printStackTrace(PrintStream os)
  {
    if (_rootCause != null)
      _rootCause.printStackTrace(os);
    else
      super.printStackTrace(os);
  }

  /**
   * Prints the stack trace, preferring the root cause if it exists.
   */
  public void printStackTrace(PrintWriter os)
  {
    if (_rootCause != null)
      _rootCause.printStackTrace(os);
    else
      super.printStackTrace(os);
  }
}


