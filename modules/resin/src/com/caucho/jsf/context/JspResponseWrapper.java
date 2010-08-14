/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.context;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Locale;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.caucho.jsp.BodyResponseStream;
import com.caucho.server.http.AbstractResponseStream;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.CauchoResponseWrapper;
import com.caucho.server.http.ResponseWriter;
import com.caucho.server.http.ServletOutputStreamImpl;
import com.caucho.vfs.FlushBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

public class JspResponseWrapper extends CauchoResponseWrapper
{
  private static final Logger log
    = Logger.getLogger(JspResponseWrapper.class.getName());
  private boolean _hasError;
  private BodyResponseStream _bodyStream;
  private AbstractResponseStream _stream;

  private HttpServletResponse _response;
  
  private TempStream _tempStream = new TempStream();
  private WriteStream _out;
  
  private FlushBuffer _flushBuffer;

  private ResponseWriter _writer = new ResponseWriter();
  private ServletOutputStreamImpl _os = new ServletOutputStreamImpl();

  /**
   * Initialize the response.
   */
  public void init(HttpServletResponse response)
  {
    _bodyStream = new BodyResponseStream();
   _stream = _bodyStream;
    
    _out = new WriteStream(_tempStream);
    _bodyStream.setWriter(_out.getPrintWriter());
    
    setResponse(response);
    _response = response;

    _os.init(_stream);
    _writer.init(_stream);

    _hasError = false;
  } 

  /**
   * Sets the ResponseStream
   */
  public void setResponseStream(AbstractResponseStream stream)
  {
    try {
      _stream.flushBuffer();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
      
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
    //if (_flushBuffer != null)
    //   _flushBuffer.flushBuffer();
    
    //_stream.flushBuffer();
    
    //_response.flushBuffer();
  }

  public void resetBuffer()
  {
    if (_stream != null)
      _stream.clearBuffer();
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


  /**
   * Set to true while processing an error.
   */
  public void setHasError(boolean hasError)
  {
    _hasError = hasError;
  }

  public String getStatusMessage()
  {
    if (_response instanceof CauchoResponse)
      return ((CauchoResponse) _response).getStatusMessage();

    throw new UnsupportedOperationException();
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
  }
  
  public void setSessionId(String id)
  {

  }
  
  public void setPrivateCache(boolean isPrivate)
  {
  }
  
  public void setNoCache(boolean isPrivate)
  {
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

  /**
   * complete the response.
   */
  public void close()
    throws IOException
  {
    WriteStream out = _out;
    _out = null;

    if (out != null)
      out.close();
  }

  public void flushResponse()
    throws IOException
  {
    _out.flush();
    
    ReadStream rs = _tempStream.openRead();
    PrintWriter out = _response.getWriter();
    
    int ch;

    while ((ch = rs.readChar()) >= 0)
      out.write((char) ch);

    rs.close();
  }

  public String completeJsf()
    throws IOException
  {
    WriteStream out = _out;
    _out = null;

    if (out != null)
      out.flush();
    
    ReadStream rs = _tempStream.openRead();
    StringBuilder sb = new StringBuilder();
    
    int ch;

    while ((ch = rs.readChar()) >= 0)
      sb.append((char) ch);

    rs.close();

    return sb.toString();
  }
}

