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

package com.caucho.hmtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2StreamingInput;

/**
 * HmtpReader stream handles client packets received from the server.
 */
public class HmtpReader {
  private static final Logger log
    = Logger.getLogger(HmtpReader.class.getName());

  private String _id;
  
  private InputStream _is;
  private Hessian2StreamingInput _in;

  public HmtpReader()
  {
  }

  public HmtpReader(InputStream is)
  {
    init(is);
  }
  
  public void setId(String id)
  {
    _id = id;
  }

  public void init(InputStream is)
  {
    _is = is;
    _in = new Hessian2StreamingInput(is);
  }

  /**
   * Returns true if buffered read data is already available, i.e.
   * the read stream will not block.
   */
  public boolean isDataAvailable()
  {
    return _in != null && _in.isDataAvailable();
  }

  /**
   * Reads the next HMTP packet from the stream, returning false on
   * end of file.
   */
  public boolean readPacket(ActorStream actorStream)
    throws IOException
  {
    if (actorStream == null)
      throw new IllegalStateException("HmtpReader.readPacket requires a valid ActorStream for callbacks");

    Hessian2StreamingInput in = _in;

    if (in == null)
      return false;

    Hessian2Input hIn = null;

    try {
      hIn = in.startPacket();
    } catch (IOException e) {
      log.fine(this + " exception while reading HMTP packet\n  " + e);

      log.log(Level.FINER, e.toString(), e);
    }

    if (hIn == null) {
      close();
      return false;
    }

    int type = hIn.readInt();
    String to = hIn.readString();
    String from = hIn.readString();
    
    switch (HmtpPacketType.TYPES[type]) {
    case MESSAGE:
      {
        Serializable value = (Serializable) hIn.readObject();
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " message " + value
                    + " {to:" + to + ", from:" + from + "}");
        }

        actorStream.message(to, from, value);

        break;
      }

    case MESSAGE_ERROR:
      {
        Serializable value = (Serializable) hIn.readObject();
        ActorError error = (ActorError) hIn.readObject();
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " messageError " + error + " " + value
                    + " {to:" + to + ", from:" + from + "}");
        }

        actorStream.messageError(to, from, value, error);

        break;
      }

    case QUERY_GET:
      {
        long id = hIn.readLong();
        Serializable value = (Serializable) hIn.readObject();
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " queryGet " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.queryGet(id, to, from, value);

        break;
      }

    case QUERY_SET:
      {
        long id = hIn.readLong();
        Serializable value = (Serializable) hIn.readObject();
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " querySet " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.querySet(id, to, from, value);

        break;
      }

    case QUERY_RESULT:
      {
        long id = hIn.readLong();
        Serializable value = (Serializable) hIn.readObject();
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " queryResult " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.queryResult(id, to, from, value);

        break;
      }

    case QUERY_ERROR:
      {
        long id = hIn.readLong();
        Serializable value = (Serializable) hIn.readObject();
        ActorError error = (ActorError) hIn.readObject();
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " queryError " + error + " " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.queryError(id, to, from, value, error);

        break;
      }

    default:
      throw new UnsupportedOperationException("ERROR: " + HmtpPacketType.TYPES[type]);
    }

    return true;
  }

  public void close()
  {
    try {
      Hessian2StreamingInput in = _in;
      _in = null;

      if (in != null)
        in.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    // _client.close();
  }

  @Override
  public String toString()
  {
    if (_id != null)
      return getClass().getSimpleName() + "[" + _id + "]";
    else
      return getClass().getSimpleName() + "[" + _is + "]";
  }
}
