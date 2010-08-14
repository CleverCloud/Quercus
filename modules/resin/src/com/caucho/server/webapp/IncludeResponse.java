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

package com.caucho.server.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.caucho.server.cache.AbstractCacheFilterChain;
import com.caucho.server.http.AbstractResponseStream;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.CauchoResponseWrapper;
import com.caucho.server.http.ResponseWriter;
import com.caucho.server.http.ServletOutputStreamImpl;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

/**
 * Internal response for an include() or forward()
 */
class IncludeResponse extends CauchoResponseWrapper
{
  private static final L10N L = new L10N(IncludeResponse.class);
  
  private final IncludeResponseStream2 _originalStream
    = new IncludeResponseStream2(this);
  
  private final ServletOutputStreamImpl _responseOutputStream
    = new ServletOutputStreamImpl();
  private final ResponseWriter _responsePrintWriter
    = new ResponseWriter();

  private IncludeRequest _request;

  private QDate _calendar = new QDate();
  private AbstractResponseStream _stream;
  private AbstractCacheFilterChain _cacheInvocation;
  
  private boolean _isForwardEnclosed;

  IncludeResponse(IncludeRequest request)
  {
    super(request);
  }
  
  IncludeResponse(IncludeRequest request, HttpServletResponse response)
  {
    super(request, response);

    _request = request;
  }

  /**
   * Starts the request
   */
  void startRequest()
  {
    _originalStream.start();
    _stream = _originalStream;

    _responseOutputStream.init(_stream);
    _responsePrintWriter.init(_stream);

    _cacheInvocation = null;
  }

  /**
   * Finish request.
   */
  void finishRequest()
    throws IOException
  {
    _stream.close();
  }

  public IncludeRequest getRequest()
  {
    return _request;
  }

  /**
   * Sets the cache invocation to indicate that the response might be
   * cacheable.
   */
  public void setCacheInvocation(AbstractCacheFilterChain cacheInvocation)
  {
    // server/135q
    assert(_cacheInvocation == null || cacheInvocation == null);

    _cacheInvocation = cacheInvocation;
  }

  public final AbstractCacheFilterChain getCacheInvocation()
  {
    return _cacheInvocation;
  }

  @Override
  public void close()
  {
    // server/135q
    
    try {
      _stream.close();
    } catch (IOException e) {
    }
  }

  //
  // status and headers
  //

  @Override
  public void sendError(int code, String msg)
  {
  }
  
  @Override
  public void setHeader(String name, String value)
  {
    _originalStream.addHeader(name, value);
  }
  
  @Override
  public void addHeader(String name, String value)
  {
    _originalStream.addHeader(name, value);
  }

  @Override
  public Collection<String> getHeaders(String name)
  {
    Collection<String> headers = super.getHeaders(name);

    List<String> headerKeys = _originalStream.getHeaderKeys();
    List<String> headerValues = _originalStream.getHeaderValues();

    for (int i = 0; i < headerKeys.size(); i++) {
      String key = headerKeys.get(i);

      if (key.equals(name))
        headers.add(headerValues.get(i));
    }

    return headers;
  }

  @Override
  public Collection<String> getHeaderNames()
  {
    Collection<String> responseHeaders = super.getHeaderNames();

    final Set<String> headers;

    if (responseHeaders instanceof Set)
      headers = (Set) responseHeaders;
    else
      headers = new HashSet(responseHeaders);

    headers.addAll(_originalStream.getHeaderKeys());

    return headers;
  }

  @Override
  public boolean containsHeader(String name)
  {
    return false;
  }
  
  @Override
  public void setDateHeader(String name, long date)
  {
    _calendar.setGMTTime(date);

    setHeader(name, _calendar.printDate());
  }
  
  @Override
  public void addDateHeader(String name, long date)
  {
    _calendar.setGMTTime(date);

    addHeader(name, _calendar.printDate());
  }
  
  @Override
  public void setIntHeader(String name, int value)
  {
    setHeader(name, String.valueOf(value));
  }
  
  @Override
  public void addIntHeader(String name, int value)
  {
    addHeader(name, String.valueOf(value));
  }
  
  @Override
  public void addCookie(Cookie cookie)
  {
  }

  /**
   * included response can't set the content type.
   */
  @Override
  public void setContentType(String type)
  {
  }

  @Override
  public void setContentLength(int length)
  {
  }

  /**
   * Sets the ResponseStream
   */
  public void setResponseStream(AbstractResponseStream responseStream)
  {
    _stream = responseStream;

    _responseOutputStream.init(_stream);
    _responsePrintWriter.init(_stream);
  }

  /**
   * Gets the response stream.
   */
  public AbstractResponseStream getResponseStream()
  {
    return _stream;
  }

  /**
   * Gets the response stream.
   */
  public AbstractResponseStream getOriginalStream()
  {
    return _originalStream;
  }

  /**
   * Returns true for a Caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    return _stream.isCauchoResponseStream();
  }
  
  /**
   * Returns the ServletOutputStream for the response.
   */
  public ServletOutputStream getOutputStream() throws IOException
  {
    return _responseOutputStream;
  }

  /**
   * Returns a PrintWriter for the response.
   */
  public PrintWriter getWriter() throws IOException
  {
    /*
    if (! _hasWriter) {
      _hasWriter = true;

      if (_charEncoding != null && _responseStream != null)
        _responseStream.setEncoding(_charEncoding);
    }
    */
    
    return _responsePrintWriter;
  }

  /**
   * Returns the parent writer.
   */
  public PrintWriter getNextWriter()
  {
    return null;
  }

  public void setBufferSize(int size)
  {
    _stream.setBufferSize(size);
  }

  public int getBufferSize()
  {
    return _stream.getBufferSize();
  }

  public void flushBuffer()
    throws IOException
  {
    _stream.flush();
  }

  public void flushHeader()
    throws IOException
  {
    _stream.flushBuffer();
  }

  public void setDisableAutoFlush(boolean disable)
  {
    // XXX: _responseStream.setDisableAutoFlush(disable);
  }

  public void reset()
  {
    resetBuffer();
  }

  public void resetBuffer()
  {
    _stream.clearBuffer();

    // jsp/15ma
    // killCaching();
  }

  /**
   * Clears the response for a forward()
   *
   * @param force if not true and the response stream has committed,
   *   throw the IllegalStateException.
   */
  void reset(boolean force)
  {
    if (! force && _originalStream.isCommitted())
      throw new IllegalStateException(L.l("response cannot be reset() after committed"));
    
    _stream.clearBuffer();
  }

  public void clearBuffer()
  {
    _stream.clearBuffer();
  }

  @Override
  public void setForwardEnclosed(boolean isForwardEnclosed) {
    _isForwardEnclosed = isForwardEnclosed;
  }

  @Override
  public boolean isForwardEnclosed() {
    if (_isForwardEnclosed)
      return true;
    else if (getResponse() instanceof CauchoResponse)
      return ((CauchoResponse) getResponse()).isForwardEnclosed();
    else
      return false;
  }
}
