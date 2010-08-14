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

package com.caucho.es;

import com.caucho.java.LineMap;
import com.caucho.util.CharBuffer;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * JavaScript exception, filtered to get the line numbers right.
 */
public class ESException extends Exception {
  public ESException() {}
  public ESException(String name) { super(name); }
  public ESException(Throwable e) { super(e); }
  
  public static void staticPrintESTrace(Exception e, OutputStream os)
  {
    CharArrayWriter writer = new CharArrayWriter();
    PrintWriter pw = new PrintWriter(writer);
    
    e.printStackTrace(pw);

    pw.close();
    char []array = writer.toCharArray();

    CharBuffer cb = filter(array);

    if (os != null) {
      byte []b = cb.toString().getBytes();

      try {
        os.write(b, 0, b.length);
      } catch (IOException e1) {
      }
    } else
      System.out.println(cb);
  }

  public static void staticPrintESTrace(Exception e, PrintWriter os)
  {
    CharArrayWriter writer = new CharArrayWriter();
    PrintWriter pw = new PrintWriter(writer);
    
    e.printStackTrace(pw);

    pw.close();
    char []array = writer.toCharArray();

    CharBuffer cb = filter(array);

    if (os != null) {
      os.print(cb.toString());
    } else
      System.out.println(cb);
  }

  public void printESStackTrace(OutputStream os)
  {
    staticPrintESTrace(this, os);
  }

  public void printESStackTrace()
  {
    printESStackTrace(System.out);
  }

  public void printESStackTrace(PrintWriter out)
  {
    CharArrayWriter writer = new CharArrayWriter();
    PrintWriter pw = new PrintWriter(writer);
    
    printStackTrace(pw);

    pw.close();
    char []array = writer.toCharArray();

    CharBuffer cb = filter(array);

    out.print(cb.toString());
  }

  /**
   * Filter the exception trace to convert *.java line numbers back to
   * the *.js name.
   */
  private static CharBuffer filter(char []array)
  {
    CharBuffer buf = new CharBuffer();
    CharBuffer fun = new CharBuffer();
    CharBuffer file = new CharBuffer();

    boolean hasJavaScript = false;
    int i = 0;
    while (i < array.length) {
      fun.clear();
      file.clear();
      int start = i;
      int end;
      for (end = i; end < array.length && array[end] != '\n'; end++) {
      }

      for (; i < end && Character.isWhitespace(array[i]); i++) {
        fun.append(array[i]);
      }

      // skip 'at'
      for (; i < end && ! Character.isWhitespace(array[i]); i++) {
        fun.append(array[i]);
      }

      if (! fun.endsWith("at")) {
        for (i = start; i < end; i++) {
          buf.append(array[i]);
        }
        i = end + 1;

        buf.append('\n');

        continue;
      }

      for (; i < end && Character.isWhitespace(array[i]); i++) {
      }

      fun.clear();
      for (; i < end && ! Character.isWhitespace(array[i]) &&
             array[i] != '('; i++) {
        fun.append(array[i]);
      }

      if (fun.startsWith("com.caucho.es.")) {
        i = end + 1;
        continue;
      }

      /*
      if (! fun.startsWith("_js.")) {
        if (hasJavaScript) {
          i = end + 1;
          continue;
        }

        for (i = start; i < end; i++) {
          buf.append(array[i]);
        }
        i = end + 1;

        buf.append('\n');

        continue;
      }
      */

      if (i < end && array[i] == '(')
        i++;

      for (; i < end && array[i] != ')'; i++) {
        file.append(array[i]);
      }

      i = end + 1;

      if (fun.endsWith(".call"))
        continue;

      int p = fun.lastIndexOf('.');
      String className;
      String function;
      if (p > 0) {
        className = fun.substring(0, p);
        function = fun.substring(p + 1);
      }
      else {
        className = "";
        function = fun.toString();
      }

      Global global = Global.getGlobalProto();
      LineMap lineMap = global != null ? global.getLineMap(className) : null;
      String line = file.toString();

      if (lineMap != null) {
        p = file.indexOf(':');
        if (p > 0) {
          try {
            String filename = file.substring(0, p);
            int lineNo = Integer.parseInt(file.substring(p + 1));
            line = lineMap.convertLine(filename, lineNo);
          } catch (Exception e) {
          }
        }
        else
          line = lineMap.convertLine(file.toString(), 1);
      }

      buf.append("\tat ");
      buf.append(fun);
      buf.append("(");
      buf.append(line);
      buf.append(")");
      buf.append("\n");

      hasJavaScript = true;
    }

    return buf;
  }
}



