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

package com.caucho.ejb;

import javax.ejb.EJBException;
import java.lang.reflect.InvocationTargetException;

/**
 * Wraps the actual exception with an EJB exception
 */
public class EJBExceptionWrapper extends EJBException {
  private static final long serialVersionUID = 1L;

  private Throwable _rootCause;

  /**
   * Null constructor for beans
   */
  public EJBExceptionWrapper()
  {
  }

  /**
   * Create a basic EJBExceptionWrapper with a message.
   * 
   * @param msg
   *          the exception message.
   */
  public EJBExceptionWrapper(String msg)
  {
    super(msg);
  }

  /**
   * Create a EJBExceptionWrapper wrapping a root exception.
   * 
   * @param rootCause
   *          the underlying wrapped exception.
   */
  public EJBExceptionWrapper(Throwable rootCause)
  {
    super(rootCause.toString());

    _rootCause = rootCause;

    initCause(rootCause);
  }

  /**
   * Create a EJBExceptionWrapper wrapping a root exception.
   * 
   * @param rootCause
   *          the underlying wrapped exception.
   */
  public EJBExceptionWrapper(String msg, Throwable rootCause)
  {
    super(msg);

    _rootCause = rootCause;

    initCause(rootCause);
  }

  public Throwable getCause()
  {
    return _rootCause;
  }

  /**
   * Creates an EJBException from a throwable.
   */
  public static EJBException create(Throwable exn)
  {
    if (exn instanceof EJBException)
      return (EJBException) exn;
    else if (exn instanceof InvocationTargetException)
      return create(exn.getCause());
    else
      return new EJBExceptionWrapper(exn);
  }

  /**
   * Creates a runtime from a throwable.
   */
  public static RuntimeException createRuntime(Throwable rootCause)
  {
    if (rootCause instanceof RuntimeException)
      return ((RuntimeException) rootCause);
    else
      return new EJBExceptionWrapper(rootCause);
  }
}
