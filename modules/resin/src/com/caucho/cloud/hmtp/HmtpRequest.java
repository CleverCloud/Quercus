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

package com.caucho.cloud.hmtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.cloud.bam.BamService;
import com.caucho.hemp.broker.HempMemoryQueue;
import com.caucho.hessian.io.HessianDebugInputStream;
import com.caucho.hmtp.HmtpReader;
import com.caucho.hmtp.HmtpWriter;
import com.caucho.network.listen.AbstractProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Handles HMTP requests from a peer server.
 * 
 * If the request does not match one of the HMTP codes, it the HmtpRequest
 * will forward to the HMUX request for compatibility.
 */
public class HmtpRequest extends AbstractProtocolConnection
{
  private static final L10N L = new L10N(HmtpRequest.class);
  private static final Logger log
    = Logger.getLogger(HmtpRequest.class.getName());

  public static final int HMUX_TO_UNIDIR_HMTP = '7';
  public static final int HMUX_SWITCH_TO_HMTP = '8';
  public static final int HMUX_HMTP_OK        = '9';
  
  private SocketLink _conn;
  private BamService _bamService;
  
  private ReadStream _rawRead;
  private WriteStream _rawWrite;
  
  private boolean _isFirst;

  private HmtpReader _hmtpReader;
  private HmtpWriter _hmtpWriter;
  private ActorStream _linkStream;
  
  private HmtpLinkActor _linkActor;

  public HmtpRequest(SocketLink conn,
                     BamService bamService)
  {
    _conn = conn;
    _bamService = bamService;

    _rawRead = conn.getReadStream();
    _rawWrite = conn.getWriteStream();
  }

  @Override
  public boolean isWaitForRead()
  {
    return true;
  }
  
  @Override
  public void onStartConnection()
  {
    _isFirst = true;
  }

  @Override
  public boolean handleRequest()
    throws IOException
  {
    try {
      if (_isFirst) {
        return handleInitialRequest();
      }
      else {
        return dispatchHmtp();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    }
  }

  private boolean handleInitialRequest()
    throws IOException
  {
    _isFirst = false;
    
    ReadStream is = _rawRead;
    
    int ch = is.read();
    
    if (ch < 0)
      return false;
    
    boolean isUnidir = false;
    
    if (ch == HMUX_TO_UNIDIR_HMTP)
      isUnidir = true;
    else if (ch == HMUX_SWITCH_TO_HMTP)
      isUnidir = false;
    else
      throw new UnsupportedOperationException(L.l("0x{0} is an invalid HMUX code.",
                                                  Integer.toHexString(ch)));

    int len = (is.read() << 8) + is.read();
    boolean isAdmin = is.read() != 0;

    InputStream rawIs = is;

    if (log.isLoggable(Level.FINEST)) {
      HessianDebugInputStream dIs
        = new HessianDebugInputStream(is, log, Level.FINEST);
      dIs.startStreaming();
      rawIs = dIs;
    }

    if (_hmtpReader != null)
      _hmtpReader.init(rawIs);
    else {
      _hmtpReader = new HmtpReader(rawIs);
      _hmtpReader.setId(getRequestId());
    }

    if (_hmtpWriter != null)
      _hmtpWriter.init(_rawWrite);
    else {
      _hmtpWriter = new HmtpWriter(_rawWrite);
      // _hmtpWriter.setId(getRequestId());
      _hmtpWriter.setAutoFlush(true);
    }

    Broker broker = _bamService.getBroker();
    ActorStream brokerStream = broker.getBrokerStream();

    _hmtpWriter.setJid("hmtp-server-" + _conn.getId() + "-hmtp");

    _linkStream = new HempMemoryQueue(_hmtpWriter, brokerStream, 1);

    _linkActor = new HmtpLinkActor(_linkStream,
                                   broker,
                                   _bamService.getLinkManager(),
                                   _conn.getRemoteHost(),
                                   isUnidir);

    return dispatchHmtp();
  }

  private boolean dispatchHmtp()
    throws IOException
  {
    HmtpReader in = _hmtpReader;

    do {
      ActorStream brokerStream = _linkActor.getBrokerStream();
      
      if (! in.readPacket(brokerStream)) {
        return false;
      }
    } while (in.isDataAvailable());
    
    return true;
  }

  /**
   * Close when the socket closes.
   */
  @Override
  public void onCloseConnection()
  {
    HmtpLinkActor linkActor = _linkActor;
    _linkActor = null;

    ActorStream linkStream = _linkStream;
    _linkStream = null;

    if (linkActor!= null)
      linkActor.onCloseConnection();

    if (linkStream != null)
      linkStream.close();

    HmtpWriter writer = _hmtpWriter;

    if (writer != null)
      writer.close();
  }

  protected String getRequestId()
  {
    return "hmtp" + ":" + _conn.getId();
  }

  public final String dbgId()
  {
    return "Hmtp[" + _conn.getId() + "] ";
  }

  @Override
  public String toString()
  {
    return "HmuxRequest" + dbgId();
  }
}
