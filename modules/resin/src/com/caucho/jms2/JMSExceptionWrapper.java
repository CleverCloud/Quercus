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

package com.caucho.jms2;

import javax.jms.JMSException;

/**
 * Wraps the actual exception with an JMS exception
 */
public class JMSExceptionWrapper extends JMSException {
  private Throwable _rootCause;

  /**
   * Null constructor for beans
   */
  public JMSExceptionWrapper()
  {
    super("");
  }
  /**
   * Create a basic JMSExceptionWrapper with a message.
   *
   * @param msg the exception message.
   */
  public JMSExceptionWrapper(String msg)
  {
    super(msg);
  }

  /**
   * Create a JMSExceptionWrapper wrapping a root exception.
   *
   * @param rootCause the underlying wrapped exception.
   */
  public JMSExceptionWrapper(Throwable rootCause)
  {
    super(rootCause.getMessage());

    _rootCause = rootCause;
  }

  /**
   * Creates an JMSException from a throwable.
   */
  public static JMSException create(Throwable rootCause)
  {
    if (rootCause instanceof JMSException)
      return ((JMSException) rootCause);
    else
      return new JMSExceptionWrapper(rootCause);
  }

  /**
   * Returns the root exception if it exists.
   *
   * @return the underlying wrapped exception.
   */
  public Throwable getRootCause()
  {
    return _rootCause;
  }

  /**
   * Returns the root exception if it exists.
   *
   * @return the underlying wrapped exception.
   */
  public Throwable getCause()
  {
    return getRootCause();
  }
}

