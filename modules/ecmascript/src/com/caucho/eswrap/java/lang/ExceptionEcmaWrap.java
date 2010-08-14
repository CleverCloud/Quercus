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
 *
 * $Id: ExceptionEcmaWrap.java,v 1.2 2004/09/29 00:13:05 cvs Exp $
 */

package com.caucho.eswrap.java.lang;

import com.caucho.es.ESException;
import com.caucho.vfs.WriteStream;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

public class ExceptionEcmaWrap {
  public static void printStackTrace(Exception e, Object o)
  {
    if (o instanceof PrintStream)
      e.printStackTrace((PrintStream) o);
    else if (o instanceof PrintWriter)
      e.printStackTrace((PrintWriter) o);
    else if (o instanceof WriteStream)
      e.printStackTrace(((WriteStream) o).getPrintWriter());
    else
      e.printStackTrace();
  }

  public static void printESStackTrace(Exception e, Object o)
  {
    if (o instanceof OutputStream)
      ESException.staticPrintESTrace(e, (OutputStream) o);
    else if (o instanceof PrintWriter)
      ESException.staticPrintESTrace(e, (PrintWriter) o);
    else
      printStackTrace(e, o);
  }
}

