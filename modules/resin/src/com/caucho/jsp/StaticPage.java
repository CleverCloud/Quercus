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

import com.caucho.util.CharBuffer;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempCharBuffer;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * A static page is a page that's just a static file.
 */
public class StaticPage extends Page {
  private Path _cacheEntry;
  private long _lastModified;
  private int _contentLength;
  private boolean _hasSession;

  /**
   * Create a new Static page.
   *
   * @param path the underlying file
   * @param hasSession if true, create a new session
   */
  StaticPage(Path path, boolean hasSession)
    throws IOException
  {
    _cacheEntry = path;
    _contentLength = (int) _cacheEntry.getLength();
    _hasSession = hasSession;

    _caucho_setCacheable();
  }

  public void init(Path path)
    throws ServletException
  {
  }

  /**
   * Returns true if the source has modified for this page.
   */
  public boolean _caucho_isModified()
  {
    return ! _cacheEntry.exists() || super._caucho_isModified();
  }

  void _caucho_setUncacheable()
  {
    _lastModified = 0;
  }

  public long getLastModified(HttpServletRequest request)
  {
    return _caucho_lastModified();
  }

  /**
   * Executes the JSP Page
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    
    _caucho_init(req, res);

    if (_hasSession) {
      req.getSession();
      res.setHeader("Cache-Control", "private");
    }

    // res.setContentLength(_contentLength);

    TempCharBuffer buf = TempCharBuffer.allocate();
    char []cBuf = buf.getBuffer();
    int len;

    PrintWriter out = response.getWriter();
    
    ReadStream rs = _cacheEntry.openRead();
    rs.setEncoding("UTF-8");
    try {
      while ((len = rs.read(cBuf, 0, cBuf.length)) > 0) {
        out.write(cBuf, 0, len);
      }
    } finally {
      rs.close();
    }

    TempCharBuffer.free(buf);
  }

  public boolean disableLog()
  {
    return true;
  }

  public static void writeDepend(Path dependPath,
                                 ArrayList<PersistentDependency> dependList)
    throws IOException
  {
    WriteStream os = dependPath.openWrite();
    try {
      for (int i = 0; i < dependList.size(); i++) {
        PersistentDependency dependency = dependList.get(i);

        if (dependency instanceof Depend) {
          Depend depend = (Depend) dependency;

          os.print('"');
          os.print(depend.getPath().getNativePath());
          os.print("\" \"");
          os.print(depend.getDigest());
          os.println("\"");
        }
      }
    } finally {
      os.close();
    }
  }

  static ArrayList<Depend> parseDepend(Path dependPath)
    throws IOException
  {
    ReadStream is = dependPath.openRead();
    try {
      ArrayList<Depend> dependList = new ArrayList<Depend>();
      
      String name;

      while ((name = parseName(is)) != null) {
        long digest = Long.parseLong(parseName(is));

        Depend depend = new Depend(dependPath.lookup(name), digest);

        dependList.add(depend);
      }

      return dependList;
    } finally {
      is.close();
    }
  }

  private static String parseName(ReadStream is)
    throws IOException
  {
    int ch;
    
    for (ch = is.read(); ch > 0 && ch != '"'; ch = is.read()) {
    }

    if (ch < 0)
      return null;

    CharBuffer cb = new CharBuffer();
    
    for (ch = is.read(); ch > 0 && ch != '"'; ch = is.read()) {
      cb.append((char) ch);
    }

    if (ch < 0)
      return null;

    return cb.toString();
  }
  
  public void destroy()
  {
    /*
    try {
      _cacheEntry.remove();
    } catch (IOException e) {
    }
    */
  }

  /**
   * Returns a printable version of the static page object.
   */
  public String toString()
  {
    return "StaticPage[" + _cacheEntry + "]";
  }
}
