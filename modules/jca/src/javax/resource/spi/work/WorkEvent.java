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

package javax.resource.spi.work;

import java.util.EventObject;

/**
 * A work event.
 */
public class WorkEvent extends EventObject {
  public final static int WORK_ACCEPTED = 1;
  public final static int WORK_COMPLETED = 4;
  public final static int WORK_REJECTED = 2;
  public final static int WORK_STARTED = 3;

  private int type;
  private Work work;
  private WorkException exc;
  private long startDuration = -1;
  
  /**
   * Called with a Work instance has been accepted.
   */
  public WorkEvent(Object source, int type,
                   Work work, WorkException exn)
  {
    super(source);

    this.type = type;
    this.work = work;
    this.exc = exn;
  }
  
  /**
   * Called with a Work instance has been accepted.
   */
  public WorkEvent(Object source, int type,
                   Work work, WorkException exn,
                   long startDuration)
  {
    super(source);

    this.type = type;
    this.work = work;
    this.exc = exn;
    this.startDuration = startDuration;
  }

  /**
   * Returns the type of the event.
   */
  public int getType()
  {
    return this.type;
  }

  /**
   * Returns the work instance.
   */
  public Work getWork()
  {
    return this.work;
  }

  /**
   * Returns the start duration.
   */
  public long getStartDuration()
  {
    return this.startDuration;
  }

  /**
   * Returns the work exception
   */
  public WorkException getException()
  {
    return this.exc;
  }
}
