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

package com.caucho.jsp;

import com.caucho.util.ExceptionWrapper;

import javax.servlet.jsp.JspException;

/**
 * A JSP exception which includes the proper error page.
 */
public class QJspException extends JspException implements ExceptionWrapper {
  private String _errorPage;
  
  public QJspException()
  {
  }

  public QJspException(String msg)
  {
    super(msg);
  }

  public QJspException(String msg, String errorPage)
  {
    super(msg);
    
    _errorPage = errorPage;
  }

  public QJspException(Throwable e)
  {
    super(e);
  }

  public QJspException(Throwable e, String errorPage)
  {
    super(e);

    _errorPage = errorPage;
  }

  public static JspException createJspException(Throwable e)
  {
    if (e instanceof JspException)
      return (JspException) e;
    else
      return new QJspException(e);
  }

  public Throwable getRootCause()
  {
    return getCause();
  }

  public String getErrorPage()
  {
    return _errorPage;
  }
}



