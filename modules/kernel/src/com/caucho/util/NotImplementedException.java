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
 * A better name for stubbed out interfaces.
 */
public class NotImplementedException extends UnsupportedOperationException
  implements ExceptionWrapper {
  private Throwable rootCause;
  
  public NotImplementedException(String message)
  {
    super(message);
  }
  
  public NotImplementedException(String message, Throwable e)
  {
    super(message);

    rootCause = e;
  }
  
  public NotImplementedException(Throwable e)
  {
    super(e.toString());

    rootCause = e;
  }

  public static UnsupportedOperationException create(Exception e)
  {
    if (e instanceof UnsupportedOperationException)
      return (UnsupportedOperationException) e;
    else
      return new NotImplementedException(e);
  }
  
  public Throwable getRootCause()
  {
    return rootCause;
  }

  /**
   * Prints the stack trace.
   */
  public void printStackTrace(PrintWriter out)
  {
    super.printStackTrace(out);
    
    Throwable rootCause = getRootCause();

    if (rootCause != null) {
      out.println("Root cause is:");
      rootCause.printStackTrace(out);
    }
  }
  
  /**
   * Prints the stack trace.
   */
  public void printStackTrace(PrintStream out)
  {
    super.printStackTrace(out);
    
    Throwable rootCause = getRootCause();

    if (rootCause != null) {
      out.println("Root cause is:");
      rootCause.printStackTrace(out);
    }
  }
}
