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

package com.caucho.network.listen;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.inject.Module;
import com.caucho.util.Alarm;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Public API to control a http upgrade connection.
 */
@Module
public class SocketLinkDuplexController extends AsyncController {
  private static final L10N L = new L10N(SocketLinkDuplexController.class);
  private static final Logger log
    = Logger.getLogger(SocketLinkDuplexController.class.getName());

  private ClassLoader _loader;

  private TcpSocketLink _conn;

  private ReadStream _is;
  private WriteStream _os;

  private SocketLinkDuplexListener _listener;
  private String _readThreadName;

  public SocketLinkDuplexController(TcpSocketLink conn,
                                    SocketLinkDuplexListener handler)
  {
    if (handler == null)
      throw new NullPointerException(L.l("handler is a required argument"));

    _conn = conn;
    _listener = handler;

    _loader = Thread.currentThread().getContextClassLoader();

    _is = _conn.getReadStream();
    _os = _conn.getWriteStream();

    _readThreadName = ("resin-" + _listener.getClass().getSimpleName()
                       + "-read-" + conn.getId());
  }

  /**
   * Returns true for a duplex controller
   */
  public boolean isDuplex()
  {
    return true;
  }

  /**
   * Sets the max idle time.
   */
  public void setIdleTimeMax(long idleTime)
  {
    if (idleTime < 0 || Long.MAX_VALUE / 2 < idleTime)
      idleTime = Long.MAX_VALUE / 2;

    TcpSocketLink conn = _conn;
    if (conn != null)
      conn.setIdleTimeout(idleTime);
  }

  /**
   * Gets the max idle time.
   */
  public long getIdleTimeMax()
  {
    TcpSocketLink conn = _conn;

    if (conn != null)
      return conn.getIdleTimeout();
    else
      return -1;
  }
  
  /**
   * Returns the socket link
   */
  public TcpSocketLink getSocketLink()
  {
    return _conn;
  }

  /**
   * Returns the read stream. The read stream should only be used by the read
   * handler.
   */
  public ReadStream getReadStream()
  {
    return _is;
  }

  /**
   * Returns the write stream. The write stream must be synchronized if multiple
   * threads can write to it.
   */
  public WriteStream getWriteStream()
  {
    return _os;
  }

  /**
   * Returns the handler
   */
  public SocketLinkDuplexListener getHandler()
  {
    return _listener;
  }

  public boolean serviceRead()
  {
    Thread thread = Thread.currentThread();

    boolean isValid = false;

    String oldName = thread.getName();

    try {
      thread.setName(_readThreadName);
      thread.setContextClassLoader(_loader);

      TcpSocketLink conn = _conn;
      ReadStream is = _is;
      SocketLinkDuplexListener handler = _listener;

      if (conn == null || is == null || handler == null) {
        return false;
      }

      if (is.getAvailable() > 0) {
        isValid = true;
        handler.onRead(this);
        return true;
      }
      else {
        handler.onComplete(this);
        return false;
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      thread.setName(oldName);

      if (! isValid)
        close();
    }

    return true;
  }

  @Override
  public void complete()
  {
    close();
  }
  
  @Override
  public void close()
  {
    closeImpl();
  }

  /**
   * Closes the connection.
   */
  @Override
  public void closeImpl()
  {
    ReadStream is = _is;
    _is = null;
    
    TcpSocketLink conn = _conn;
    _conn = null;
    
    SocketLinkDuplexListener listener = _listener;
    _listener = null;
    
    ClassLoader loader = _loader;
    _loader = null;
    
    _os = null;

    IoUtil.close(is);
    
    try {
      if (conn != null)
        conn.close();
    } catch (Exception e) {
    }

    super.closeImpl();
    
    if (listener != null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      
      try {
        thread.setContextClassLoader(loader);
        
        listener.onComplete(this);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }
  }

  @Override
  public String toString()
  {
    TcpSocketLink conn = _conn;

    if (conn == null)
      return getClass().getSimpleName() + "[" + _listener + ",closed]";
    else if (Alarm.isTest())
      return getClass().getSimpleName() + "[" + _listener + "]";
    else
      return (getClass().getSimpleName() + "[" + conn.getId() + "," + _listener + "]");
  }
}
