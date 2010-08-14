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

package com.caucho.http.log;

import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.CompileException;
import com.caucho.util.ExceptionWrapper;
import com.caucho.util.QDate;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Represents an log of every error log request to the server.
 */
public class ErrorLog extends AbstractErrorLog {
  /**
   * Logs an error.
   *
   * @param message the error message
   * @param request the servlet request
   * @param response the servlet response
   * @param application the servlet context
   */
  public void log(String message,
                  HttpServletRequest request,
                  HttpServletResponse response,
                  ServletContext application)
    throws IOException
  {
    WriteStream logStream = getLogStream();

    if (logStream == null)
      return;

    CharBuffer cb = CharBuffer.allocate();

    QDate.formatLocal(cb, Alarm.getCurrentTime(), "[%Y/%m/%d %H:%M:%S] ");

    cb.append(message);

    logStream.log(cb.close());

    logStream.flush();
  }

  /**
   * Logs a message to the error log.
   *
   * @param log the error log to write the message.
   * @param message the message to write
   * @param e the exception to write
   */
  public void log(String message,
                  Throwable e,
                  HttpServletRequest request,
                  HttpServletResponse response,
                  ServletContext application)
    throws IOException
  {
    WriteStream logStream = getLogStream();

    if (logStream == null)
      return;

    Throwable t = e;
    while (t != null) {
      e = t;
        
      if (e instanceof ServletException)
        t = ((ServletException) e).getRootCause();
      else if (e instanceof ExceptionWrapper)
        t = ((ExceptionWrapper) e).getRootCause();
      else
        t = null;
    }

    CharBuffer cb = CharBuffer.allocate();

    QDate.formatLocal(cb, Alarm.getCurrentTime(), "[%Y/%m/%d %H:%M:%S] ");

    cb.append(message);

    logStream.log(cb.close());

    if (e != null && ! (e instanceof CompileException))
      logStream.log(e);

    logStream.flush();
  } 

  /**
   * Cleanup the log.
   */
  public void destroy()
    throws IOException
  {
  }
}
