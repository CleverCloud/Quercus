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

package com.caucho.server.hmux;

import com.caucho.util.Alarm;
import com.caucho.vfs.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Underlying stream handling HTTP requests.
 */
class HmuxStream extends StreamImpl {
  private static final Logger log
    = Logger.getLogger(HmuxStream.class.getName());
  // reserved headers that should not be passed to the HTTP server
  private static HashMap<String,String> _reserved;

  private static final Object LOCK = new Object();
  
  // Saved keepalive stream for a new request.
  private static HmuxStream _savedStream;
  // Time the stream was saved
  private static long _saveTime;
  
  private long _socketTimeout = 30000L;

  private boolean _isSSL;

  private Socket _s;
  private InputStream _is;
  private OutputStream _os;
  private ReadStream _rs;
  private WriteStream _ws;

  // The server's host name
  private String _host;
  // The server's port
  private int _port;

  private String _virtualHost;

  // the method
  private String _method;
  // true for a HEAD stream
  private boolean _isHead;
  // true for a POST stream
  private boolean _isPost;

  // buffer containing the POST data
  private MemoryStream _tempStream;

  // true if keepalive is allowed
  private boolean _isKeepalive = true;
  // true after the request has been sent
  private boolean _didGet;
  // length of the current chunk, -1 on eof
  private int _chunkLength;
  // the request is done
  private boolean _isRequestDone;

  private HashMap<String,Object> _attributes;

  // Used to read unread bytes on recycle.
  private byte []_tempBuffer;

  /**
   * Create a new HTTP stream.
   */
  private HmuxStream(Path path, String host, int port, Socket s)
    throws IOException
  {
    _s = s;

    _host = host;
    _port = port;
    
    _is = _s.getInputStream();
    _os = _s.getOutputStream();

    _ws = VfsStream.openWrite(_os);
    _rs = VfsStream.openRead(_is, _ws);

    _attributes = new HashMap<String,Object>();

    init(path);
  }

  /**
   * Opens a new HTTP stream for reading, i.e. a GET request.
   *
   * @param path the URL for the stream
   *
   * @return the opened stream
   */
  static HmuxStreamWrapper openRead(HmuxPath path) throws IOException
  {
    HmuxStream stream = createStream(path);
    stream._isPost = false;

    return new HmuxStreamWrapper(stream);
  }

  /**
   * Opens a new HTTP stream for reading and writing, i.e. a POST request.
   *
   * @param path the URL for the stream
   *
   * @return the opened stream
   */
  static HmuxStreamWrapper openReadWrite(HmuxPath path) throws IOException
  {
    HmuxStream stream = createStream(path);
    stream._isPost = true;

    return new HmuxStreamWrapper(stream);
  }

  /**
   * Creates a new HTTP stream.  If there is a saved connection to
   * the same host, use it.
   *
   * @param path the URL for the stream
   *
   * @return the opened stream
   */
  static private HmuxStream createStream(HmuxPath path) throws IOException
  {
    String host = path.getHost();
    int port = path.getPort();

    HmuxStream stream = null;
    long streamTime = 0;
    synchronized (LOCK) {
      if (_savedStream != null &&
          host.equals(_savedStream.getHost()) &&
          port == _savedStream.getPort()) {
        stream = _savedStream;
        streamTime = _saveTime;
        _savedStream = null;
      }
    }

    if (stream == null) {
    }
    // if the stream is still valid, use it
    else if (Alarm.getCurrentTime() < streamTime + 5000) {
      stream.init(path);
      return stream;
    }
    // if the stream has timed out, close it
    else {
      try {
        stream._isKeepalive = false;
        stream.close();
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    Socket s;

    try {
      s = new Socket(host, port);
    } catch (ConnectException e) {
      throw new ConnectException(path.getURL() + ": " + e.getMessage());
    } catch (Exception e) {
      throw new ConnectException(path.getURL() + ": " + e.toString());
    }

    int socketTimeout = 300 * 1000;

    try {
      s.setSoTimeout(socketTimeout);
    } catch (Exception e) {
    }
          
    return new HmuxStream(path, host, port, s);
  }

  /**
   * Initializes the stream for the next request.
   */
  private void init(Path path)
  {
    _isRequestDone = false;
    _didGet = false;
    _isPost = false;
    _isHead = false;
    _method = null;
    _attributes.clear();
    
    setPath(path);

    if (path instanceof HmuxPath)
      _virtualHost = ((HmuxPath) path).getVirtualHost();
  }

  /**
   * Set if this should be an SSL connection.
   */
  public void setSSL(boolean isSSL)
  {
    _isSSL = isSSL;
  }

  /**
   * Set if this should be an SSL connection.
   */
  public boolean isSSL()
  {
    return _isSSL;
  }

  /**
   * Sets the method
   */
  public void setMethod(String method)
  {
    _method = method;
  }

  /**
   * Sets true if we're only interested in the head.
   */
  public void setHead(boolean isHead)
  {
    _isHead = isHead;
  }

  /**
   * Returns the stream's host.
   */
  public String getHost()
  {
    return _host;
  }

  /**
   * Returns the stream's port.
   */
  public int getPort()
  {
    return _port;
  }
  
  /**
   * Returns a header from the response returned from the HTTP server.
   *
   * @param name name of the header
   * @return the header value.
   */
  public Object getAttribute(String name)
    throws IOException
  {
    if (! _didGet)
      getConnInput();

    return _attributes.get(name.toLowerCase());
  }

  /**
   * Returns an iterator of the returned header names.
   */
  public Iterator getAttributeNames()
    throws IOException
  {
    if (! _didGet)
      getConnInput();

    return _attributes.keySet().iterator();
  }

  /**
   * Sets a header for the request.
   */
  public void setAttribute(String name, Object value)
  {
    if (name.equals("method"))
      setMethod((String) value);
    else if (name.equals("socket-timeout")) {
      if (value instanceof Integer) {
        int socketTimeout = ((Integer) value).intValue();

        if (socketTimeout > 0) {
          try {
            if (_s != null)
              _s.setSoTimeout(socketTimeout);
          } catch (Exception e) {

          }
        }
      }
    }
    else
      _attributes.put(name.toLowerCase(), value);
  }

  /**
   * Remove a header for the request.
   */
  public void removeAttribute(String name)
  {
    _attributes.remove(name.toLowerCase());
  }

  /**
   * Sets the timeout.
   */
  public void setSocketTimeout(long timeout)
    throws SocketException
  {
    if (_s != null)
      _s.setSoTimeout((int) timeout);
  }

  /**
   * The stream is always writable (?)
   */
  public boolean canWrite()
  {
    return true;
  }

  /**
   * Writes a buffer to the underlying stream.
   *
   * @param buffer the byte array to write.
   * @param offset the offset into the byte array.
   * @param length the number of bytes to write.
   * @param isEnd true when the write is flushing a close.
   */
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    if (! _isPost)
      return;

    if (_tempStream == null)
      _tempStream = new MemoryStream();

    _tempStream.write(buf, offset, length, isEnd);
  }

  /**
   * The stream is readable.
   */
  public boolean canRead()
  {
    return true;
  }

  /**
   * Read data from the connection.  If the request hasn't yet been sent
   * to the server, send it.
   */
  public int read(byte []buf, int offset, int length) throws IOException
  {
    try {
      return readInt(buf, offset, length);
    } catch (IOException e) {
      _isKeepalive = false;
      throw e;
    } catch (RuntimeException e) {
      _isKeepalive = false;
      throw e;
    }
  }
  
  /**
   * Read data from the connection.  If the request hasn't yet been sent
   * to the server, send it.
   */
  public int readInt(byte []buf, int offset, int length) throws IOException
  {
    if (! _didGet)
      getConnInput();

    if (_isRequestDone)
      return -1;

    try {
      int len = length;

      if (_chunkLength == 0) {
        if (! readData())
          _chunkLength = -1;
      }
      
      if (_chunkLength < 0)
        return -1;
      
      if (_chunkLength < len)
        len = _chunkLength;

      len = _rs.read(buf, offset, len);

      if (len < 0) {
      }
      else
        _chunkLength -= len;
    
      return len;
    } catch (IOException e) {
      _isKeepalive = false;
      throw e;
    } catch (RuntimeException e) {
      _isKeepalive = false;
      throw e;
    }
  }

  /**
   * Sends the request and initializes the response.
   */
  private void getConnInput() throws IOException
  {
    if (_didGet)
      return;

    try {
      getConnInputImpl();
    } catch (IOException e) {
      _isKeepalive = false;
      throw e;
    } catch (RuntimeException e) {
      _isKeepalive = false;
      throw e;
    }
  }

  /**
   * Send the request to the server, wait for the response and parse
   * the headers.
   */
  private void getConnInputImpl() throws IOException
  {
    if (_didGet)
      return;

    _didGet = true;

    _ws.write('C');
    _ws.write(0);
    _ws.write(0);

    if (_method != null) {
      writeString(HmuxRequest.HMUX_METHOD, _method);
    }
    else if (_isPost) {
      writeString(HmuxRequest.HMUX_METHOD, "POST");
    }
    else if (_isHead)
      writeString(HmuxRequest.HMUX_METHOD, "HEAD");
    else
      writeString(HmuxRequest.HMUX_METHOD, "GET");
    
    if (_virtualHost != null)
      writeString(HmuxRequest.HMUX_SERVER_NAME, _virtualHost);
    else {
      writeString(HmuxRequest.HMUX_SERVER_NAME, _path.getHost());
      _ws.print(_path.getHost());
      if (_path.getPort() != 80) {
        writeString(HmuxRequest.CSE_SERVER_PORT,
                    String.valueOf(_path.getPort()));
      }
    }

    // Not splitting query? Also fullpath?
      writeString(HmuxRequest.HMUX_URI, _path.getPath());

    if (_path.getQuery() != null)
      writeString(HmuxRequest.CSE_QUERY_STRING, _path.getQuery());
    
    Iterator iter = getAttributeNames();
    while (iter.hasNext()) {
      String name = (String) iter.next();
      if (_reserved.get(name.toLowerCase()) == null) {
        writeString(HmuxRequest.HMUX_HEADER, name);
        writeString(HmuxRequest.HMUX_STRING, getAttribute(name));
      }
    }

    if (_isPost) {
      MemoryStream tempStream = _tempStream;
      _tempStream = null;
      if (tempStream != null) {
        TempBuffer tb = TempBuffer.allocate();
        byte []buffer = tb.getBuffer();
        int sublen;

        ReadStream postIn = tempStream.openReadAndSaveBuffer();

        while ((sublen = postIn.read(buffer, 0, buffer.length)) > 0) {
          _ws.write('D');
          _ws.write(sublen >> 8);
          _ws.write(sublen);
          _ws.write(buffer, 0, sublen);
        }

        tempStream.destroy();

        TempBuffer.free(tb);
        tb = null;
      }
    }

    _attributes.clear();

    _ws.write('Q');

    readData();

    if (_isHead)
      _isRequestDone = true;
  }

  private void writeString(int code, String string)
    throws IOException
  {
    WriteStream ws = _ws;

    ws.write((byte) code);
    int len = string.length();
    ws.write(len >> 8);
    ws.write(len);
    ws.print(string);
  }

  private void writeString(int code, Object obj)
    throws IOException
  {
    String string = String.valueOf(obj);
    
    WriteStream ws = _ws;

    ws.write((byte) code);
    int len = string.length();
    ws.write(len >> 8);
    ws.write(len);
    ws.print(string);
  }

  /**
   * Parse the headers returned from the server.
   */
  private boolean readData()
    throws IOException
  {
    boolean isDebug = log.isLoggable(Level.FINE);
    
    int code;

    ReadStream is = _rs;

    while ((code = is.read()) > 0) {
      switch (code) {
      case HmuxRequest.HMUX_CHANNEL:
        is.read();
        is.read();
        break;
      case HmuxRequest.HMUX_QUIT:
      case HmuxRequest.HMUX_EXIT:
        is.close();

        if (isDebug)
          log.fine("HMUX: " + (char) code);

        return false;

      case HmuxRequest.HMUX_YIELD:
        break;

      case HmuxRequest.HMUX_STATUS:
        String value = readString(is);
        _attributes.put("status", value.substring(0, 3));

        if (isDebug)
          log.fine("HMUX: " + (char) code + " " + value);
        break;

      case HmuxRequest.HMUX_DATA:
        _chunkLength = 256 * (is.read() & 0xff) + (is.read() & 0xff);

        if (isDebug)
          log.fine("HMUX: " + (char) code + " " + _chunkLength);

        return true;

      default:
        int len = 256 * (is.read() & 0xff) + (is.read() & 0xff);

        if (isDebug)
          log.fine("HMUX: " + (char) code + " " + len);

        is.skip(len);
        break;
      }
    }

    return false;
  }

  private String readString(ReadStream is)
    throws IOException
  {
    int len = 256 * (is.read() & 0xff) + is.read();

    char []buf = new char[len];

    is.readAll(buf, 0, len);

    return new String(buf);
  }

  /**
   * Returns the bytes still available.
   */
  public int getAvailable() throws IOException
  {
    if (! _didGet)
      getConnInput();

    return _rs.getAvailable();
  }

  /**
   * Close the connection.
   */
  public void close() throws IOException
  {
    if (_isKeepalive) {
      // If recycling, read any unread data
      if (! _didGet)
        getConnInput();

      if (! _isRequestDone) {
        if (_tempBuffer == null)
          _tempBuffer = new byte[256];

        try {
          while (read(_tempBuffer, 0, _tempBuffer.length) > 0) {
          }
        } catch (IOException e) {
          _isKeepalive = false;
        }
      }
    }

    if (com.caucho.server.util.CauchoSystem.isTesting())
      _isKeepalive = false; // XXX:
    
    if (_isKeepalive) {
      HmuxStream oldSaved;
      long now = Alarm.getCurrentTime();
      synchronized (LOCK) {
        oldSaved = _savedStream;
        _savedStream = this;
        _saveTime = now;
      }

      if (oldSaved != null && oldSaved != this) {
        oldSaved._isKeepalive = false;
        oldSaved.close();
      }

      return;
    }

    try {
      try {
        if (_ws != null)
          _ws.close();
      } catch (Throwable e) {
      }
      _ws = null;

      try {
        if (_rs != null)
          _rs.close();
      } catch (Throwable e) {
      }
      _rs = null;

      try {
        if (_os != null)
          _os.close();
      } catch (Throwable e) {
      }
      _os = null;

      try {
        if (_is != null)
          _is.close();
      } catch (Throwable e) {
      }
      _is = null;
    } finally {
      if (_s != null)
        _s.close();
      _s = null;
    }
  }

  static {
    _reserved = new HashMap<String,String>();
    _reserved.put("user-agent", "");
    _reserved.put("content-length", "");
    _reserved.put("content-encoding", "");
    _reserved.put("connection", "");
    _reserved.put("host", "");
  }
}
