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
using System.Diagnostics;
using System.Net.Sockets;
using System.Net;
using System.Threading;

namespace Caucho.IIS
{
  public class Server
  {
    private Logger _log;

    private char _serverInternalId;

    private HmuxConnection[] _idle = new HmuxConnection[64];
    private volatile int _idleHead;
    private volatile int _idleTail;
    private int _idleSize = 16;

    private int _maxConnections = int.MaxValue / 2;

    private int _loadBalanceConnectTimeout;
    private int _loadBalanceIdleTime;
    private int _loadBalanceRecoverTime;
    private int _socketTimeout;

    private volatile int _activeCount;
    private volatile int _startingCount;

    private IPAddress _address;
    private int _port;
    private bool _isDebug = false;

    private long _successTime = 0;
    private long _failTime = 0;
    private long _busyTime = 0;

    private volatile int _traceId = 0;

    public Server(char serverInternalId,
      IPAddress address,
      int port,
      int loadBalanceConnectTimeout,
      int loadBalanceIdleTime,
      int loadBalanceRecoverTime,
      int socketTimeout)
    {
      _socketTimeout = socketTimeout;
      _loadBalanceConnectTimeout = loadBalanceConnectTimeout;
      _loadBalanceIdleTime = loadBalanceIdleTime;
      _loadBalanceRecoverTime = loadBalanceRecoverTime;
      _serverInternalId = serverInternalId;
      _address = address;
      _port = port;

      _log = Logger.GetLogger();
    }

    public void SetDebug(bool isDebug)
    {
      _isDebug = isDebug;
    }

    public bool IsActive()
    {
      return _failTime == 0 && _busyTime == 0;
    }

    public int GetPooledCount()
    {
      return _idleHead;
    }

    public HmuxConnection OpenConnection()
    {
      long now = Utils.CurrentTimeMillis();
      lock (this) {
        if (_failTime > 0 && (_failTime + _loadBalanceRecoverTime) > now) {
          return null;
        }

        if (_busyTime > 0 && (_busyTime + _loadBalanceRecoverTime) > now) {
          return null;
        }

        //only allow a single thread to open connection
        if ((_failTime > 0 || _busyTime > 0) && _startingCount > 0) {
          return null;
        }
      }

      HmuxConnection channel = OpenRecycle();

      if (channel != null)
        return channel;

      return Connect();
    }

    private HmuxConnection OpenRecycle()
    {
      long now = Utils.CurrentTimeMillis();
      HmuxConnection channel = null;

      lock (this) {
        if (_idleHead != _idleTail) {
          channel = _idle[_idleHead];
          long freeTime = channel.GetIdleStartTime();

          _idle[_idleHead] = null;
          _idleHead = (_idleHead + _idle.Length - 1) % _idle.Length;

          if (now < freeTime + _loadBalanceIdleTime) {
            _activeCount++;

            channel.ClearIdleStartTime();
            channel.ToActive();

            Trace.TraceInformation("OpenRecycle '{0}'", channel);

            return channel;
          }
        }
      }

      if (channel != null) {
        if (_log.IsLoggable(EventLogEntryType.Information))
          _log.Info(this + " close idle " + channel
                    + " expire=" + new DateTime(channel.GetIdleStartTime() * 10 + _loadBalanceIdleTime * 10));
        Trace.TraceInformation("closing expired channel '{0}'", channel);

        channel.CloseImpl();
      }

      Trace.TraceInformation("OpenRecyle return 'null'");
      return null;
    }

    private HmuxConnection Connect()
    {
      lock (this) {
        if (_maxConnections <= _activeCount + _startingCount)
          return null;

        _startingCount++;
      }

      HmuxConnection connection = null;

      Object connectLock = new Object();
      Monitor.Enter(connectLock);
      try {
        Socket socket = new Socket(_address.AddressFamily, SocketType.Stream, ProtocolType.Tcp);

        AsyncCallback connectListener = delegate(IAsyncResult result)
        {
          Monitor.Enter(connectLock);
          try {
            Monitor.Pulse(connectLock);
          } catch (Exception) {
          } finally {
            Monitor.Exit(connectLock);
          }
        };

        socket.BeginConnect(_address, _port, connectListener, socket);
        Monitor.Wait(connectLock, _loadBalanceConnectTimeout);

        if (!socket.Connected) {
          throw new SocketException(10053);
        }

        socket.SendTimeout = _socketTimeout;
        socket.ReceiveTimeout = _socketTimeout;

        String traceId;
        if (_isDebug) {
          int i = _traceId++;
          traceId = i.ToString();
        } else {
          traceId = socket.Handle.ToInt32().ToString();
        }

        connection = new HmuxConnection(socket, this, _serverInternalId, traceId);

        lock (this) {
          _activeCount++;
        }

        if (_log.IsLoggable(EventLogEntryType.Information))
          _log.Info("Connect " + connection);

        Trace.TraceInformation("Connect '{0}'", connection);

        return connection;
      } catch (SocketException e) {
        String message = String.Format("Socket connection to {0}:{1} timed out on load-balance-connect-timeout {2}", _address, _port, _loadBalanceConnectTimeout);
        if (_log.IsLoggable(EventLogEntryType.Information))
          _log.Info(message);

        Trace.TraceInformation(message);
      } catch (Exception e) {
        String message = String.Format("Can't create HmuxChannel to '{0}:{1}' due to: {2} \t {3}", _address, _port, e.Message, e.StackTrace);
        if (_log.IsLoggable(EventLogEntryType.Information))
          _log.Info(message);

        Trace.TraceError(message);
      } finally {
        lock (this) {
          _startingCount--;
        }

        if (connection == null)
          FailConnect();

        Monitor.Exit(connectLock);
      }

      return null;
    }

    public void FailConnect()
    {
      lock (this) {
        _failTime = Utils.CurrentTimeMillis();
      }
    }

    internal void Busy()
    {
      lock (this) {
        _busyTime = Utils.CurrentTimeMillis();
      }
    }

    internal void FailSocket()
    {
      lock (this) {
        _failTime = Utils.CurrentTimeMillis();
      }
    }

    internal void SetCpuLoadAvg(double loadAvg)
    {
    }

    private void Success()
    {
      lock (this) {
        _failTime = 0;
        _successTime = Utils.CurrentTimeMillis();
      }
    }

    internal void ClearBusy()
    {
      lock (this) {
        _busyTime = 0;
      }
    }

    internal void Free(HmuxConnection channel)
    {
      Success();

      lock (this) {
        _activeCount--;

        bool failing = false;

        failing = _failTime > 0 || _busyTime > 0;

        int size = (_idleHead - _idleTail + _idle.Length) % _idle.Length;

        if (!failing && size < _idleSize) {
          Trace.TraceInformation("Returning channel '{0}' to pool", channel);
          _idleHead = (_idleHead + 1) % _idle.Length;
          _idle[_idleHead] = channel;

          channel = null;
        }
      }

      long now = Utils.CurrentTimeMillis();
      long maxIdleTime = _loadBalanceIdleTime;
      HmuxConnection oldChannel = null;

      do {
        oldChannel = null;

        lock (this) {
          if (_idleHead != _idleTail) {
            int nextTail = (_idleTail + 1) % _idle.Length;

            oldChannel = _idle[nextTail];
            if (oldChannel != null
                && (oldChannel.GetIdleStartTime() + maxIdleTime) < now) {
              _idle[nextTail] = null;
              _idleTail = nextTail;
            } else {
              oldChannel = null;
            }
          }
        }

        if (oldChannel != null) {
          oldChannel.CloseImpl();
          Trace.TraceInformation("closing expired channel '{0}'", oldChannel);
        }
      } while (oldChannel != null);

      if (channel != null) {
        channel.CloseImpl();
      }
    }

    internal void Close(HmuxConnection connection)
    {
      if (_log.IsLoggable(EventLogEntryType.Information))
        _log.Info("Close {0}", connection);

      Trace.TraceInformation("Close '{0}'", connection);

      lock (this) {
        _activeCount--;
      }
    }

    internal void Close()
    {
      if (_log.IsLoggable(EventLogEntryType.Information))
        _log.Info("Close {0}", this);

      Trace.TraceInformation("Close '{0}'", this);

      lock (this) {
        _idleHead = _idleTail = 0;
      }

      for (int i = 0; i < _idle.Length; i++) {
        HmuxConnection channel;

        lock (this) {
          channel = _idle[i];
          _idle[i] = null;
        }

        if (channel != null)
          channel.CloseImpl();
      }
    }

    internal string GetDebugId()
    {
      return _address + _port.ToString();
    }

    public String GetName()
    {
      return String.Format("{0}:{1}:{2}", _serverInternalId, _address, _port);
    }

    public override string ToString()
    {
      String format = this.GetType().Name + "'{0}:{1}:{2}'";
      return String.Format(format, _serverInternalId, _address, _port);
    }
  }
}
