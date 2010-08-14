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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.Env;
import com.caucho.vfs.DatagramStream;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents read/write stream
 */
public class UdpInputOutput
  extends AbstractBinaryInputOutput
  implements SocketInputOutput
{
  private static final Logger log
    = Logger.getLogger(UdpInputOutput.class.getName());

  private DatagramSocket _socket;
  private Domain _domain;
  
  private DatagramStream _stream;
  private int _error;
  
  private byte _unread;
  
  public UdpInputOutput(Env env, String host, int port, Domain domain)
    throws IOException
  {
    super(env);
    
    _socket = new DatagramSocket();
    _socket.connect(InetAddress.getByName(host), port);
    
    _domain = domain;
  }
  
  public UdpInputOutput(Env env, DatagramSocket socket, Domain domain)
  {
    super(env);

    _socket = socket;
    _domain = domain;
  }

  public void bind(SocketAddress address)
    throws IOException
  {
    _socket.bind(address);
  }

  public void connect(SocketAddress address)
    throws IOException
  {
    _socket.connect(address);
    
    init();
  }

  public void init()
  {
    DatagramStream sock = new DatagramStream(_socket);
    sock.setThrowReadInterrupts(true);

    init(sock.getInputStream(), sock.getOutputStream());
  }

  public void setTimeout(long timeout)
  {
    try {
      if (_socket != null)
        _socket.setSoTimeout((int) timeout);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  public void setError(int error)
  {
    _error = error;
  }
  
  /**
   * Returns the current location in the file.
   */
  public long getPosition()
  {
    if (_stream != null)
      return _stream.getPosition();
    else
      return -1;
  }
  
  /**
   * Sets the current location in the file.
   */
  public boolean setPosition(long offset)
  {
    if (_stream == null)
      return false;

    boolean result = _stream.setPosition(offset);
    
    if (result)
      _isEOF = false;
    
    return result;
  }
  
  /**
   * Unread the last byte.
   */
  public void unread()
    throws IOException
  {
    if (_stream != null) {
      _stream.unread();
      _isEOF = false;
    }
  }
    
  /**
   * Implements the EnvCleanup interface.
   */
  public void cleanup()
  {
    DatagramSocket s = _socket;
    _socket = null;

    if (s != null)
      s.close();
  }

  public String toString()
  {
    if (_socket != null)
      return "UdpInputOutput["
          + _socket.getInetAddress() + "," + _socket.getPort() + "]";
    else
      return "UdpInputOutput[closed]";
  }
}

