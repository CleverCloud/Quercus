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

package javax.resource;

/**
 * The root resource exception.
 */
public class ResourceException extends Exception {
  private String _errorCode;

  public ResourceException()
  {
  }

  public ResourceException(String msg)
  {
    super(msg);
  }

  public ResourceException(String msg, String errorCode)
  {
    super(msg);

    _errorCode = errorCode;
  }

  public ResourceException(String msg, Throwable cause)
  {
    super(msg, cause);
  }

  public ResourceException(Throwable cause)
  {
    super(cause);
  }

  public String getErrorCode()
  {
    return _errorCode;
  }

  public void setErrorCode(String errorCode)
  {
    _errorCode = errorCode;
  }

  /**
   * @deprecated
   */
  public Exception getLinkedException()
  {
    return (Exception) getCause();
  }

  /**
   * @deprecated
   */
  public void setLinkedException(Exception exn)
  {
    initCause(exn);
  }
}
