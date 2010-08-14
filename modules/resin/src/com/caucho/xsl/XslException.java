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

import com.caucho.java.LineMap;
import com.caucho.util.ExceptionWrapper;

import javax.xml.transform.TransformerException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Exception rewriting.
 */
class XslException extends TransformerException implements ExceptionWrapper {
  private Throwable cause;
  private LineMap lineMap;

  public XslException(Throwable cause, LineMap lineMap)
  {
    super(cause);
    this.cause = cause;
    this.lineMap = lineMap;
  }

  public Throwable getRootCause()
  {
    return cause;
  }

  public String getMessage()
  {
    return cause.getMessage();
  }

  public void printStackTrace()
  {
    if (lineMap == null)
      cause.printStackTrace();
    else
      lineMap.printStackTrace(cause, System.out);
  }

  public void printStackTrace(PrintStream os)
  {
    if (lineMap == null)
      cause.printStackTrace(os);
    else
      lineMap.printStackTrace(cause, os);
  }

  public void printStackTrace(PrintWriter pw)
  {
    if (lineMap == null)
      cause.printStackTrace(pw);
    else
      lineMap.printStackTrace(cause, pw);
  }

  public String toString()
  {
    if (cause != null)
      return cause.toString();
    else
      return super.toString();
  }
}



