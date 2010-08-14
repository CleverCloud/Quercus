/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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
 * @author Sam 
 */

package com.caucho.doc;

import com.caucho.util.*;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Servlet to view a source file, with optional emphasis based on regular
 * expressions.
 */
public class ViewFileServlet extends GenericServlet {
  static private final Logger log = 
    Logger.getLogger(ViewFileServlet.class.getName());
  static final L10N L = new L10N(ViewFileServlet.class);

  static private final String PARAM_CONTEXTPATH = "contextpath";
  static private final String PARAM_SERVLETPATH = "servletpath";
  static private final String PARAM_FILE = "file";
  static private final String PARAM_RE_MARKER = "re-marker";
  static private final String PARAM_RE_START = "re-start";
  static private final String PARAM_RE_END = "re-end";

  ServletContext _context;

  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);
    _context = config.getServletContext();
  }

  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    try {
      viewFile(response.getWriter(), request);
    }
    catch (Exception ex) {
      throw new ServletException(ex);
    }
  }

  private void viewFile(PrintWriter out, ServletRequest request)
    throws Exception
  {
    String file = getFileName(request);
    Path path = getFilePath(request);

    if (path != null) {
      String re_mrk_str = request.getParameter(PARAM_RE_MARKER);
      String re_beg_str = request.getParameter(PARAM_RE_START);
      String re_end_str = request.getParameter(PARAM_RE_END);

      Pattern re_mrk = null; //re_mrk_str == null || re_mrk_str.length() == 0 ? null : Pattern.compile(re_mrk_str);
      Pattern re_beg = null; //re_beg_str == null || re_beg_str.length() == 0 ? null : Pattern.compile(re_beg_str);
      Pattern re_end = null; //re_end_str == null || re_end_str.length() == 0 ? null : Pattern.compile(re_end_str);

      /*
       * write the verbatim source to the browser.
       * if re.start (and optionally re.end) are specified, 
       * highlight the corresponding code sections
       */

      out.println("<html>");
      out.println("<head>");
      out.print("<title>");
      out.print(Html.escapeHtml(file));
      out.println("</title>");
      out.println("<style type='text/css'>");
      out.println("  .code-highlight { color: #1764FF; }");
      out.println("  .face-xmlelement { color: #003DB8; font-weight: bold }");
      out.println("</style>");
      out.println("</head>");
      out.println("<body bgcolor=white>");
      out.print("<code>");
      out.print("<b>");
      out.print(Html.escapeHtml(file));
      out.print("</b>");
      out.print("</code>");
      out.println("<p>");

      ReadStream is;
      try {
        is = path.openRead();
      } catch (java.io.FileNotFoundException ex) {
        out.println("<font color='red'><b>File not found: " + Html.escapeHtml(path.getPath()) + "</b></font>");
        out.println("</body>");
        out.println("</html>");
        return;
      }

      String line;
      out.print("<pre>");

      boolean h = false;  // true if currently highlighting
      boolean m = false;  // true if marked

      while ((line = is.readln()) != null) {
        // check for marker
        if (!m && re_mrk != null && re_mrk.matcher(line).matches()) {
          out.print("<a name='code-highlight'></a>");
          m = true;
        }

        // check for highlighting begin
        if (!h && re_beg != null && re_beg.matcher(line).matches()) {
          h = true;
          out.print("<b class='code-highlight'>");
          if  (!m && re_mrk == null) {
            out.print("<a name='code-highlight'></a>");
            m = true;
          }
        }

        // send string out
        // handle '<' character and '>' character
        int l = line.length();
        for (int i = 0; i < l; i++) {
          int ch = line.charAt(i);
          if (ch == '<') {
            if (h) out.print("<span class='face-xmlelement'>");
            out.print("&lt;");
          } else if (ch == '>') {
            out.print("&gt;");
            if (h) out.print("</span>");
          }
          else
            out.print((char) ch);
        }
        out.println();

        // check for highlighting end
        if (h && (re_end == null || (re_end != null && re_end.matcher(line).matches()))) {
          h = false;
          out.print("</b>");
        }
      }

      is.close();
      if (h)
        out.print("</b>");
      out.println("</pre>");
      out.println("</body>");
      out.println("</html>");
      return;
    }
  }

  private String getFileName(ServletRequest request)
  {
    String f = request.getParameter(PARAM_FILE);
    if (f != null && f.length() > 0 && f.indexOf("..") < 0) {
      return f;
    }
    return null;
  }

  private Path getFilePath(ServletRequest request)
  {
    String cp = request.getParameter(PARAM_CONTEXTPATH);
    String sp = request.getParameter(PARAM_SERVLETPATH);
    String f = getFileName(request);

    Path pwd = Vfs.lookup().createRoot();

    if (f != null) {
      ServletContext ctx = _context;

      String requestContext = ((HttpServletRequest) request).getContextPath();

      if (cp != null && cp.startsWith(requestContext))
        cp = cp.substring(requestContext.length());

      CharBuffer cb = new CharBuffer();

      if (cp != null)
        cb.append(cp);

      cb.append('/');
      cb.append(f);

      // return pwd.lookup(ctx.getRealPath(cb.toString()));
      return pwd.lookup(cb.toString());
    }

    return null;
  }

}

