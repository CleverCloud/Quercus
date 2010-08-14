/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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

package com.caucho.web.webmail;

import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class WebmailServlet extends GenericServlet {
  private boolean parseMail(ReadStream is, Path dst)
    throws IOException
  {
    CharBuffer line = CharBuffer.allocate();
    String topId = null;
    int count = 1;
    WriteStream ws = null;
    IntMap messages = new IntMap();

    try {
      while (true) {
        do {
          line.clear();
          if (! is.readLine(line)) {
            if (ws != null)
              ws.println("</message>");
            return false;
          }
          if (ws != null && ! line.startsWith("From ")) {
            for (int i = 0; i < line.length(); i++) {
              char ch = line.charAt(i);
              if (ch == '<')
                ws.print("&lt;");
              else
                ws.print(ch);
            }
            ws.println();
          }
        } while (! line.startsWith("From "));

        if (ws != null) {
          ws.println("</message>");
          ws.close();
          ws = null;
        }


        String date = null;
        String subject = null;
        String from = null;
        String id = null;
        String references = null;

        do {
          line.clear();
          if (! is.readLine(line))
            return false;
          if (line.length() == 0)
            break;

          String lower = line.toString().toLowerCase();

          if (lower.startsWith("subject: ")) {
            subject = line.substring("subject: ".length()).trim();

            if (subject.toLowerCase().startsWith("re:"))
              subject = subject.substring(3).trim();
          }
          else if (lower.startsWith("from: ")) {
            from = line.substring("from: ".length());
          }
          else if (lower.startsWith("date: ")) {
            date = line.substring("from: ".length());
          }
        } while (line.length() > 0);

        int index = messages.get(subject);

        if (index >= 0) {
          ws = dst.lookup("" + index + ".xtp").openAppend();
        }
        else {
          if (subject != null && ! subject.equals(""))
            messages.put(subject, count);

          ws = dst.lookup("" + count++ + ".xtp").openWrite();
          ws.println("<title>" + subject + "</title>");
        }
        ws.println("<em>" + from + "</em>");
        ws.println("<date>" + date + "</date>");
        ws.println("<message>");
      }
    } finally {
      if (ws != null)
        ws.close();
    }
  }

  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    PrintWriter pw = response.getWriter();

    Path path = Vfs.lookup("/home/ferg/majordomo/archive/resin-interest.0006");
    Path dst = Vfs.lookup("/tmp/dst");
    dst.mkdirs();

    ReadStream is = path.openRead();
    try {
      parseMail(is, dst);
    } finally {
      is.close();
    }
    pw.println("done");
  }
}
