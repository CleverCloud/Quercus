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

package com.caucho.server.http;

import com.caucho.server.cache.AbstractCacheEntry;
import com.caucho.server.cache.AbstractCacheFilterChain;
import com.caucho.vfs.FlushBuffer;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.PrintWriterImpl;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.Collection;
import java.util.Locale;
import java.util.Collection;

// Is there anything at all useful that could be put here?
public class StubServletResponse implements CauchoResponse {
  public StubServletResponse()
  {
  }

  protected AbstractResponseStream createResponseStream()
  {
    return new StubResponseStream();
  }
  
  public String getCharacterEncoding()
  {
    return "ISO-8859-1";
  }
  
  public void setLocale(Locale locale)
  {
  }
  
  public Locale getLocale()
  {
    return null;
  }
  
  public boolean writeHeadersInt(WriteStream out, int length, boolean isHead)
  {
    return false;
  }

  public void setBufferSize(int size)
  {
  }
  
  public int getBufferSize()
  {
    return 0;
  }
  
  public void flushBuffer()
  {
  }
  
  public boolean isCommitted()
  {
    return false;
  }
  
  public void reset()
  {
  }
  
  public void resetBuffer()
  {
  }
  
  public void setContentLength(int length)
  {
  }
  
  public void setContentType(String type)
  {
  }

  public void setStatus(int status)
  {
  }
  
  public void setStatus(int status, String messages)
  {
  }
  
  public void sendRedirect(String location)
  {
  }
  
  public void sendError(int i)
  {
  }
  
  public void sendError(int i, String message)
  {
  }
    
  public String encodeUrl(String url)
  {
    return url;
  }
  
  public String encodeURL(String url)
  {
    return url;
  }
  
  public String encodeRedirectUrl(String url)
  {
    return url;
  }
  
  public String encodeRedirectURL(String url)
  {
    return url;
  }

  public void addCookie(Cookie cookie)
  {
  }
  
  public boolean containsHeader(String header)
  {
    return false;
  }
  
  public void setHeader(String header, String value)
  {
  }
  
  public void setIntHeader(String header, int value)
  {
  }
  
  public void setDateHeader(String header, long value)
  {
  }
  
  public void addHeader(String header, String value)
  {
  }
  
  public void addIntHeader(String header, int value)
  {
  }
  
  public void addDateHeader(String header, long value)
  {
  }

  public String getHeader(String key)
  {
    return null;
  }
  
  public void clearBuffer()
  {
  }
  
  public void close() throws IOException
  {
  }

  public boolean disableHeaders(boolean disable)
  {
    return false;
  }

  public int getRemaining()
  {
    return 0;
  }

  public void setForbidForward(boolean forbid)
  {
  }
  
  public boolean getForbidForward()
  {
    return false;
  }
  
  public void setHasError(boolean hasError)
  {
  }
  
  public boolean hasError()
  {
    return true;
  }
  
  public void killCache()
  {
  }
  
  public void setPrivateCache(boolean isPrivate)
  {
  }
  
  public void setSessionId(String id)
  {
  }

  public int getStatus()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Collection<String> getHeaders(String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Collection<String> getHeaderNames()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public AbstractHttpResponse getAbstractHttpResponse()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public ServletResponse getResponse()
  {
    return null;
  }
  
  public void setNoCache(boolean killCache)
  {
  }

  public int getStatusCode()
  {
    return 200;
  }

  public String getStatusMessage()
  {
    return null;
  }
  
  public void setFooter(String key, String value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
    
  public void addFooter(String key, String value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void setFlushBuffer(FlushBuffer out)
  {
  }
  
  public FlushBuffer getFlushBuffer()
  {
    return null;
  }

  public boolean isCauchoResponseStream()
  {
    return true;
  }

  public void setResponseStream(AbstractResponseStream stream)
  {
  }

  public AbstractResponseStream getResponseStream()
  {
    return new WrapperResponseStream();
  }

  public boolean isDisabled()
  {
    return false;
  }

  public void enable()
  {
  }

  public void disable()
  {
  }

  public PrintWriter getWriter()
    throws IOException
  {
    return new PrintWriterImpl(new NullWriter());
  }

  public ServletOutputStream getOutputStream()
    throws IOException
  {
    ServletOutputStreamImpl out = new ServletOutputStreamImpl();

    out.init(NullOutputStream.NULL);

    return out;
  }

  public void setCharacterEncoding(String enc)
  {
  }

  public String getContentType()
  {
    return null;
  }

  public boolean isNoCacheUnlessVary()
  {
    return false;
  }
  
  public void setCacheInvocation(AbstractCacheFilterChain cacheFilterChain)
  {
  }

  public void setMatchCacheEntry(AbstractCacheEntry cacheEntry)
  {
  }

  public void setForwardEnclosed(boolean isForwardEnclosed) {
  }

  public boolean isForwardEnclosed() {
    return false;
  }

  static class NullWriter extends Writer {
    private static final NullWriter NULL = new NullWriter();

    public void write(int ch) {}
    public void write(char []buffer, int offset, int length) {}
    public void flush() {}
    public void close() {}
  }

  static class NullOutputStream extends OutputStream {
    private static final NullOutputStream NULL = new NullOutputStream();

    public void write(int ch) {}
    public void write(byte []buffer, int offset, int length) {}
    public void flush() {}
    public void close() {}
  }
}
