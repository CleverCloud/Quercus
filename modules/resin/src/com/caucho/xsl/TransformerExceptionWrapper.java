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

package com.caucho.xsl;

import com.caucho.util.ExceptionWrapper;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

/**
 * Represents a exception when creating a transformation.
 */
public class TransformerExceptionWrapper extends TransformerException
  implements ExceptionWrapper {
  
  /**
   * Create a new exception with no error message.
   */
  public TransformerExceptionWrapper()
  {
    super("");
  }
  
  /**
   * Create a new exception with a string error message.
   */
  public TransformerExceptionWrapper(String msg)
  {
    super(msg);
  }
  
  /**
   * Create a new exception with a message and an error location.
   */
  public TransformerExceptionWrapper(String msg, SourceLocator locator)
  {
    super(msg, locator);
  }
  
  /**
   * Create a new exception with a message and an error location.
   */
  public TransformerExceptionWrapper(String msg, SourceLocator locator,
                                     Throwable e)
  {
    super(msg, locator, e);
  }
  
  /**
   * Create a new exception with a wrapped exception.
   */
  public TransformerExceptionWrapper(String msg, Throwable e)
  {
    super(msg, e);
  }
  
  /**
   * Create a new exception with a wrapped exception.
   */
  public TransformerExceptionWrapper(Throwable e)
  {
    super(String.valueOf(e), e);
  }

  /**
   * Returns the wrapped exception.
   */
  public Throwable getRootCause()
  {
    return getCause();
  }
}
