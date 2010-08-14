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

package com.caucho.hemp.packet;

import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import java.io.Serializable;

/**
 * Unidirectional message with an error
 */
public class MessageError extends Packet {
  private final Serializable _value;
  private final ActorError _error;

  /**
   * An empty message to a destination
   *
   * @param to the target jid
   */
  public MessageError(String to,
                      String from,
                      Serializable value,
                      ActorError error)
  {
    super(to, from);

    _value = value;
    _error = error;
  }

  /**
   * Returns the message value
   */
  public Serializable getValue()
  {
    return _value;
  }

  /**
   * Returns the message error
   */
  public ActorError getError()
  {
    return _error;
  }

  /**
   * SPI method to dispatch the packet to the proper handler
   */
  @Override
  public void dispatch(ActorStream handler, ActorStream toSource)
  {
    handler.messageError(getTo(), getFrom(), getValue(), _error);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    
    if (getTo() != null) {
      sb.append("to=");
      sb.append(getTo());
    }
    
    if (getFrom() != null) {
      sb.append(",from=");
      sb.append(getFrom());
    }

    sb.append("," + _error);
    sb.append("]");
    
    return sb.toString();
  }
}
