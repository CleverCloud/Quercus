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
 * @author Alex Rojkov
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Sockets;
using System.Net;
using System.Diagnostics;

namespace Caucho.IIS
{
  public class LoadBalancer
  {
    private Logger _log;
    private Server[] _servers;
    private Random _random;
    private volatile int _roundRobinIdx;
    private int _loadBalanceConnectTimeout;
    private int _loadBalanceIdleTime;
    private int _loadBalanceRecoverTime;
    private int _loadBalanceSocketTimeout;
    private int _keepaliveTimeout;
    private int _socketTimeout;
    private bool _isDebug;

    //supports just one server for now
    public LoadBalancer(String servers,
      int loadBalanceConnectTimeout,
      int loadBalanceIdleTime,
      int loadBalanceRecoverTime,
      int loadBalanceSocketTimeout,
      int keepaliveTimeout,
      int socketTimeout,
      bool isDebug)
    {
      _log = Logger.GetLogger();

      _loadBalanceConnectTimeout = loadBalanceConnectTimeout;
      _loadBalanceIdleTime = loadBalanceIdleTime;
      _loadBalanceRecoverTime = loadBalanceRecoverTime;
      _loadBalanceSocketTimeout = loadBalanceSocketTimeout;
      _keepaliveTimeout = keepaliveTimeout;
      _socketTimeout = socketTimeout;
      _isDebug = isDebug;

      List<Server> pool = new List<Server>();
      String[] sruns = servers.Split(new char[] { ',', ' ' }, StringSplitOptions.RemoveEmptyEntries);

      for (int i = 0; i < sruns.Length; i++) {
        String server = sruns[i];
        int portIdx = server.LastIndexOf(':');
        String host = server.Substring(0, portIdx);
        IPAddress address = GetIPsForHost(host);
        int port = int.Parse(server.Substring(portIdx + 1, server.Length - portIdx - 1));
        char c = (char)('a' + i);
        _log.Info("Adding Server '{0}:{1}:{2}'", c, host, port);

        Server srun = new Server(c, address, port, _loadBalanceConnectTimeout, _loadBalanceIdleTime, _loadBalanceRecoverTime, _socketTimeout);
        srun.SetDebug(_isDebug);
        pool.Add(srun);
      }

      _servers = pool.ToArray();

      _random = new Random();
    }

    private IPAddress GetIPsForHost(String host)
    {
      IPAddress result = null;
      try {
        result = IPAddress.Parse(host);

        return result;
      } catch (Exception) {
      }

      try {
        IPHostEntry hostEntry = Dns.GetHostEntry(host);

        if (hostEntry != null && hostEntry.AddressList != null && hostEntry.AddressList.Length > 0) {
          foreach (IPAddress address in hostEntry.AddressList) {
            if (address.AddressFamily == AddressFamily.InterNetwork) {
              result = address;

              break;
            } else {
              result = address;
            }
          }
        }

        if (result == null) {
          String message = String.Format("Can't resolve ip for host '{0}'.", host);
          _log.Error(message);
          throw new ConfigurationException(message);
        }
      } catch (ConfigurationException e) {
        throw e;
      } catch (Exception e) {
        String message = String.Format("Can't resolve host '{0}'. Detailed error: {1}", host, e.Message);
        throw new ConfigurationException(message, e);
      }

      return result;
    }

    public void Init()
    {
    }

    public Server[] GetServers()
    {
      return _servers;
    }

    public int GetLoadBalanceConnectTimeout()
    {
      return _loadBalanceConnectTimeout;
    }

    public int GetLoadBalanceIdleTime()
    {
      return _loadBalanceIdleTime;
    }

    public int GetLoadBalanceRecoverTime()
    {
      return _loadBalanceRecoverTime;
    }

    public int GetLoadBalanceSocketTimeout()
    {
      return _loadBalanceSocketTimeout;
    }

    public int GetLoadBalanceKeepAliveTimeout()
    {
      return _keepaliveTimeout;
    }

    public int GetSocketTimeout()
    {
      return _socketTimeout;
    }

    public HmuxConnection OpenServer(String sessionId, Server xServer)
    {
      Trace.TraceInformation("{0}:{1}", _servers.Length, _servers[0]);

      HmuxConnection connection = null;
      if (sessionId != null)
        connection = OpenSessionServer(sessionId);

      if (connection == null)
        connection = OpenAnyServer(xServer);

      return connection;
    }

    public HmuxConnection OpenSessionServer(String sessionId)
    {
      char c = sessionId[0];

      Server server = _servers[(c - 'a')];

      HmuxConnection connection = null;

      connection = server.OpenConnection();

      return connection;
    }

    public HmuxConnection OpenAnyServer(Server xChannelFactory)
    {
      int serverCount = _servers.Length;

      Server server = null;
      HmuxConnection connection = null;

      int id = 0;

      lock (this) {
        _roundRobinIdx = _roundRobinIdx % serverCount;
        id = _roundRobinIdx;
        _roundRobinIdx++;
      }

      server = _servers[id];
      connection = server.OpenConnection();

      if (connection != null)
        return connection;

      lock (this) {
        _roundRobinIdx = _random.Next(serverCount);

        for (int i = 0; i < serverCount; i++) {
          id = (i + _roundRobinIdx) % serverCount;
          
          server = _servers[id];
          if (xChannelFactory != server)
            connection = server.OpenConnection();

          _roundRobinIdx = id;

          if (connection != null)
            break;
        }
      }

      return connection;
    }

    public void Destroy()
    {
      foreach (Server server in _servers) {
        server.Close();
      }
    }
  }
}
