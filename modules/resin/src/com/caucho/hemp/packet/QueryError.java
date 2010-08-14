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

import com.caucho.bam.ActorStream;
import com.caucho.bam.ActorError;
import java.io.Serializable;

/**
 * RPC error result from a get or set.  The "id" field is used
 * to match the query with the response.
 */
public class QueryError extends Packet {
  private final long _id;
  
  private final Serializable _value;
  private final ActorError _error;

  /**
   * zero-arg constructor for Hessian
   */
  private QueryError()
  {
    _id = 0;
    _value = null;
    _error = null;
  }

  /**
   * A query to a target
   *
   * @param id the query id
   * @param to the target jid
   * @param from the source jid
   * @param value copy the query request
   * @param error the query error
   */
  public QueryError(long id,
                    String to,
                    String from,
                    Serializable value,
                    ActorError error)
  {
    super(to, from);

    _id = id;
    _value = value;
    _error = error;
  }

  /**
   * Returns the id
   */
  public long getId()
  {
    return _id;
  }

  /**
   * Returns the query value
   */
  public Serializable getValue()
  {
    return _value;
  }

  /**
   * Returns the query error
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
    handler.queryError(getId(), getTo(), getFrom(),
                           getValue(), getError());
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    sb.append("id=");
    sb.append(_id);
    
    if (getTo() != null) {
      sb.append(",to=");
      sb.append(getTo());
    }
    
    if (getFrom() != null) {
      sb.append(",from=");
      sb.append(getFrom());
    }

    if (_value != null) {
      sb.append("," + _value.getClass().getName());
    }

    if (_error != null) {
      sb.append(",error=" + _error);
    }
    
    sb.append("]");
    
    return sb.toString();
  }
}
