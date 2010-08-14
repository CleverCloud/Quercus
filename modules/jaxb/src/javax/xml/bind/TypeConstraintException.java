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

package javax.xml.bind;
import java.io.PrintStream;

public class TypeConstraintException extends RuntimeException {

  private String _errorCode;
  private Throwable _exception;

  public TypeConstraintException(String message)
  {
    super(message);
  }

  public TypeConstraintException(String message, String errorCode)
  {
    super(message);
    _errorCode = errorCode;
  }

  public TypeConstraintException(String message, String errorCode,
                                 Throwable exception)
  {
    super(message);
    _errorCode = errorCode;
    _exception = exception;
  }

  public TypeConstraintException(String message, Throwable exception)
  {
    super(message);
    _exception = exception;
  }

  public TypeConstraintException(Throwable exception)
  {
    super();
    _exception = exception;
  }

  public String getErrorCode()
  {
    return _errorCode;
  }

  public Throwable getLinkedException()
  {
    return _exception;
  }

  public Throwable getCause()
  {
    return _exception;
  }

  public void setLinkedException(Throwable exception)
  {
    _exception = exception;
  }

  public void printStackTrace(PrintStream s)
  {
    super.printStackTrace(s);

    if (_exception != null) {
      s.print("Linked Exception: ");
      _exception.printStackTrace(s);
    }
  }

  public void printStackTrace()
  {
    printStackTrace(System.err);
  }
}

