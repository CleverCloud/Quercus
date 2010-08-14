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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;

/**
 * Resin's application implementation.
 */
public class ErrorPage {
  static L10N L = new L10N(ErrorPage.class);

  // The page location
  private String _location;
  
  // The exception
  private String _exceptionType;
  
  // The error code
  private int _errorCode = -1;

  /**
   * Creates the error page.
   */
  public ErrorPage()
  {
  }

  /**
   * Sets the location.
   */
  public void setLocation(String location)
  {
    _location = location;
  }

  /**
   * Gets the location.
   */
  public String getLocation()
  {
    return _location;
  }

  /**
   * Sets the exception type
   */
  public void setExceptionType(String exceptionType)
  {
    _exceptionType = exceptionType;
  }

  /**
   * Gets the exception type
   */
  public String getExceptionType()
  {
    return _exceptionType;
  }

  /**
   * Sets the error code.
   */
  public void setErrorCode(int code)
  {
    _errorCode = code;
  }

  /**
   * Gets the error code.
   */
  public int getErrorCode()
  {
    return _errorCode;
  }

  /**
   * Init
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_location == null)
      throw new ServletException(L.l("error-page needs 'location' attribute."));
  }
}
