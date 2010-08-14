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

package com.caucho.filters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.caucho.server.http.AbstractResponseStream;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.ResponseWrapper;
import com.caucho.server.http.ResponseWriter;
import com.caucho.server.http.ServletOutputStreamImpl;
import com.caucho.vfs.FlushBuffer;

/**
 * Response wrapper that can take advantage of Resin's streams.
 */
public class CauchoResponseWrapper extends ResponseWrapper
  implements CauchoResponse {
  private FlushBuffer _flushBuffer;
  
  private final FilterWrapperResponseStream _originalStream;
  protected AbstractResponseStream _stream;

  private ResponseWriter _writer;
  private ServletOutputStreamImpl _os;
  
  private boolean _hasError;

  public CauchoResponseWrapper()
  {
    _os = new ServletOutputStreamImpl();
    _writer = new ResponseWriter();
    
    _originalStream = new FilterWrapperResponseStream();
  }

  public CauchoResponseWrapper(HttpServletResponse response)
  {
    this();
    
    init(response);
  }

  /**
   * Initialize the response.
   */
  public void init(HttpServletResponse response)
  {
    setResponse(response);

    _stream = _originalStream;
    _os.init(_originalStream);
    _writer.init(_originalStream);

    _hasError = false;

    _originalStream.init(this);

    _originalStream.start();
  } 

  /**
   * complete the response.
   */
  public void close()
    throws IOException
  {
    if (_stream != null)
      _stream.close();
    else
      _originalStream.close();
  }

  public ServletResponse getResponse()
  {
    return _response;
  }

  /**
   * Sets the response content type.
   */
  public void setContentType(String value)
  {
    _response.setContentType(value);
    
    try {
      _stream.setEncoding(getCharacterEncoding());
    } catch (UnsupportedEncodingException e) {
    }
  }

  /**
   * Sets the ResponseStream
   */
  public void setResponseStream(AbstractResponseStream stream)
  {
    _stream = stream;

    _os.init(stream);
    _writer.init(stream);
  }

  /**
   * Gets the response stream.
   */
  public AbstractResponseStream getResponseStream()
  {
    return _stream;
  }

  /**
   * Returns true for a caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  /**
   * Returns the servlet output stream.
   */
  public ServletOutputStream getOutputStream() throws IOException
  {
    return _os;
  }

  /**
   * Returns the print writer.
   */
  public PrintWriter getWriter() throws IOException
  {
    return _writer;
  }

  /**
   * Returns the output stream for this wrapper.
   */
  protected OutputStream getStream() throws IOException
  {
    return _response.getOutputStream();
  }

  /**
   * Sets the flush buffer
   */
  public void setFlushBuffer(FlushBuffer flushBuffer)
  {
    _flushBuffer = flushBuffer;
  }

  /**
   * Gets the flush buffer
   */
  public FlushBuffer getFlushBuffer()
  {
    return _flushBuffer;
  }

  public void flushBuffer()
    throws IOException
  {
    if (_flushBuffer != null)
      _flushBuffer.flushBuffer();
    
    _stream.flushBuffer();
    
    //_response.flushBuffer();
  }

  public void reset()
  {
    super.reset();

    resetBuffer();
  }
  
  public void resetBuffer()
  {
    if (_stream != null)
      _stream.clearBuffer();
    
    _response.resetBuffer();
  }

  public void clearBuffer()
  {
    resetBuffer();
  }

  public void setLocale(Locale locale)
  {
    _response.setLocale(locale);
    
    try {
      _stream.setLocale(_response.getLocale());
    } catch (UnsupportedEncodingException e) {
    }
  }

  /*
   * caucho
   */

  public String getHeader(String key)
  {
    return null;
  }
  
  public boolean disableHeaders(boolean disable)
  {
    return false;
  }

  public void setFooter(String key, String value)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setFooter(key, value);
  }

  public void addFooter(String key, String value)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).addFooter(key, value);
  }

  public int getRemaining()
  {
    /*
    if (_response instanceof CauchoResponse)
      return ((CauchoResponse) _response).getRemaining();
    else
      return 0;
    */
    return _stream.getRemaining();
  }

  /**
   * When set to true, RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  public void setForbidForward(boolean forbid)
  {
  }

  /**
   * Returns true if RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  public boolean getForbidForward()
  {
    return false;
  }

  public String getStatusMessage()
  {
    if (_response instanceof CauchoResponse)
      return ((CauchoResponse) _response).getStatusMessage();

    throw new UnsupportedOperationException();
  }

  /**
   * Set to true while processing an error.
   */
  public void setHasError(boolean hasError)
  {
    _hasError = hasError;
  }

  /**
   * Returns true if we're processing an error.
   */
  public boolean hasError()
  {
    return _hasError;
  }

  /**
   * Kills the cache for an error.
   */
  public void killCache()
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).killCache();
  }
  
  public void setSessionId(String id)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setSessionId(id);
  }
  
  public void setPrivateCache(boolean isPrivate)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setPrivateCache(isPrivate);
  }
  
  public void setNoCache(boolean isPrivate)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setNoCache(isPrivate);
  }

  public int getStatus()
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public Collection<String> getHeaders(String name)
  {
    return _response.getHeaders(name);
  }

  public Collection<String> getHeaderNames()
  {
    return _response.getHeaderNames();
  }

  public void setForwardEnclosed(boolean isForwardEnclosed) {
  }

  public boolean isForwardEnclosed() {
    return false;
  }

  public boolean isNoCacheUnlessVary()
  {
    return false;
  }
}
