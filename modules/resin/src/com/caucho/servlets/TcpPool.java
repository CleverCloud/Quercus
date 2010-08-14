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

package com.caucho.servlets;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.config.types.*;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCP connection pool for the HTTP proxy
 */
public class TcpPool {
  static protected final Logger log =
    Logger.getLogger(TcpPool.class.getName());
  static final L10N L = new L10N(TcpPool.class);

  private ArrayList<String> _hosts = new ArrayList<String>();
  private Server []_servers;
  private int _roundRobin;
  private long _failRecoverTime = 30 * 1000L;

  /**
   * Adds an address
   */
  public void addAddress(String address)
  {
    _hosts.add(address);
  }

  /**
   * Adds a host
   */
  public void addHost(String host)
  {
    _hosts.add(host);
  }

  /**
   * Sets the fail recover time.
   */
  public void setFailRecoverTime(Period period)
  {
    _failRecoverTime = period.getPeriod();
  }

  public int getServerCount()
  {
    return _hosts.size();
  }

  /**
   * Initialize the servlet with the server's sruns.
   */
  public void init()
    throws ServletException
  {
    if (_hosts.size() == 0)
      throw new ServletException(L.l("HttpProxyServlet needs at least one host."));

    _servers = new Server[_hosts.size()];

    for (int i = 0; i < _hosts.size(); i++) {
      String host = _hosts.get(i);

      _servers[i] = new Server(host);
    }
  }

  /**
   * Handle the request.
   */
  public Server nextServer()
  {
    synchronized (this) {
      long now = Alarm.getCurrentTime();
      
      int startIndex = _roundRobin;
      _roundRobin = (_roundRobin + 1) % _servers.length;

      int bestCost = Integer.MAX_VALUE;
      Server bestServer = null;

      for (int i = 0; i < _servers.length; i++) {
        int index = (startIndex + i) % _servers.length;

        Server server = _servers[index];

        if (_failRecoverTime < now - server.getLastFailTime()
            && server.getConnectionCount() < bestCost) {
          bestServer = server;
          bestCost = server.getConnectionCount();
        }
      }

      if (bestServer != null)
        return bestServer;
    
      int index = _roundRobin;
      _roundRobin = (_roundRobin + 1) % _servers.length;

      return _servers[index];
    }
  }

  class Server {
    private Path _path;
    private long _lastFailTime;

    private int _connectionCount;

    Server(String host)
    {
      if (host.startsWith("http"))
        _path = Vfs.lookup(host);
      else
        _path = Vfs.lookup("http://" + host);
    }

    String getURL()
    {
      return _path.getURL();
    }

    ReadWritePair open(String uri)
      throws IOException
    {
      try {
        Path path = _path.lookup(uri);

        ReadWritePair pair = path.openReadWrite();

        if (pair != null) {
          synchronized (this) {
            _connectionCount++;
          }
        }

        return pair;
      } catch (IOException e) {
        fail();

        throw e;
      }
    }

    long getLastFailTime()
    {
      return _lastFailTime;
    }

    int getConnectionCount()
    {
      return _connectionCount;
    }

    void fail()
    {
      _lastFailTime = Alarm.getCurrentTime();
    }

    void close()
    {
      synchronized (this) {
        _connectionCount--;
      }
    }
  }
}
