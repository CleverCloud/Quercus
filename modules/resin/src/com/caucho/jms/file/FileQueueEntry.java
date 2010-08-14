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

package com.caucho.jms.file;

import java.io.Serializable;
import java.lang.ref.SoftReference;

import com.caucho.jms.queue.QueueEntry;

/**
 * Entry in a file queue
 */
public class FileQueueEntry<E> extends QueueEntry<E>
{
  private final long _id;

  private SoftReference<E> _payloadRef;

  public FileQueueEntry(long id,
                        String msgId,
                        long leaseTimeout,
                        int priority,
                        long expiresTime,
                        E payload)
  {
    super(msgId, leaseTimeout, priority, expiresTime);
    
    _id = id;
    
    if (payload != null)
      _payloadRef = new SoftReference<E>(payload);
  }
  
  public long getId()
  {
    return _id;
  }

  public E getPayloadRef()
  {
    SoftReference<E> ref = _payloadRef;

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  public void setPayloadRef(E payload)
  {
    _payloadRef = new SoftReference<E>(payload);
  }
}