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

package com.caucho.remote.websocket;

import com.caucho.servlet.WebSocketContext;
import com.caucho.servlet.WebSocketListener;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.net.Socket;
import java.util.logging.*;

/**
 * WebSocketClient
 */
public class WebSocketClient {
  private static final Logger log
    = Logger.getLogger(WebSocketClient.class.getName());
  private static final L10N L = new L10N(WebSocketClient.class);

  private String _url;

  private String _scheme;
  private String _host;
  private int _port;
  private String _path;
  
  private String _virtualHost;

  private WebSocketListener _listener;

  private Socket _s;
  private ReadStream _is;
  private WriteStream _os;
  private boolean _isClosed;

  private ClientContext _context;

  public WebSocketClient(String url)
  {
    _url = url;
    parseUrl(url);
  }
  
  public void setVirtualHost(String virtualHost)
  {
    _virtualHost = virtualHost;
  }

  public void connect(WebSocketListener listener)
    throws IOException
  {
    if (listener == null)
      throw new NullPointerException(L.l("listener is a required argument for connect()"));

    _listener = listener;

    connectImpl();
  }

  private void parseUrl(String url)
  {
    int p = url.indexOf("://");
    if (p < 0)
      throw new IllegalArgumentException(L.l("'{0}' is an illegal URL because it is missing a scheme",
                                             url));

    _scheme = url.substring(0, p);

    int q = url.indexOf('/', p + 3);

    String server;
    if (q < 0) {
      server = url.substring(p + 3);
      _path = "/";
    }
    else {
      server = url.substring(p + 3, q);
      _path = url.substring(q);
    }

    p = server.indexOf(':');

    if (p < 0) {
      _host = server;
      _port = 80;
    }
    else {
      _host = server.substring(0, p);
      _port = Integer.parseInt(server.substring(p + 1));
    }
  }

  protected void connectImpl()
    throws IOException
  {
    _s = new Socket(_host, _port);

    _is = Vfs.openRead(_s.getInputStream());
    _os = Vfs.openWrite(_s.getOutputStream());

    _os.print("GET " + _path + " HTTP/1.1\r\n");
    
    if (_virtualHost != null)
      _os.print("Host: " + _virtualHost + "\r\n");
    else if (_host != null)
      _os.print("Host: " + _host + "\r\n");
    else
      _os.print("Host: localhost\r\n");
    
    _os.print("Upgrade: WebSocket\r\n");
    _os.print("Connection: Upgrade\r\n");
    _os.print("Origin: Foo\r\n");
    _os.print("\r\n");
    _os.flush();

    parseHeaders(_is);

    // _wsOut = new WebSocketOutputStream(_out);
    // _wsIn = new WebSocketInputStream(_is);

    _context = new ClientContext();
    
    _listener.onStart(_context);
    
    // XXX: ThreadPool?
    Thread thread = new Thread(_context);
    thread.setDaemon(true);
    thread.start();
  }

  protected void parseHeaders(ReadStream in)
    throws IOException
  {
    String line = in.readln();

    if (! line.startsWith("HTTP/1.1 101"))
      throw new IOException(L.l("Unexpected response {0}", line));

    while ((line = in.readln()) != null && line.length() != 0) {
      int p = line.indexOf(':');

      if (p > 0) {
        String header = line.substring(0, p).trim();
        String value = line.substring(p + 1).trim();
      }
    }
  }

  public OutputStream getOutputStream()
  {
    return _os;
  }

  public void complete()
  {
    OutputStream os = _os;

    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  public boolean isClosed()
  {
    return _isClosed;
  }

  public void close()
  {
    _isClosed = true;
    
    OutputStream os = _os;
    _os = null;

    InputStream is = _is;
    _is = null;

    Socket s = _s;
    _s = null;

    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      if (is != null)
        is.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      if (s != null)
        s.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _url + "]";
  }

  class ClientContext implements Runnable, WebSocketContext
  {
    public InputStream getInputStream()
    {
      return _is;
    }

    public OutputStream getOutputStream()
    {
      return _os;
    }

    public void complete()
    {
      try {
        _os.close();
      } catch (IOException e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    public long getTimeout()
    {
      return 0;
    }

    public void setTimeout(long timeout)
    {
    }

    public void run()
    {
      try {
        handleRequests();
      } catch (Exception e) {
        if (_isClosed)
          log.log(Level.FINEST, e.toString(), e);
        else
          log.log(Level.WARNING, e.toString(), e);
      } finally {
        try {
          _listener.onComplete(this);
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }

    public void handleRequests()
      throws IOException
    {
      // server/2h20
      // _listener.onStart(this);

      while (! isClosed() && _is.waitForRead()) {
        _listener.onRead(this);
      }
    }
  }
}
