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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */
package javax.ejb;

import java.io.Serializable;
import java.util.Date;

/**
 * The Timer interface contains information about a timer that was created
 * through the EJB Timer Service.
 */
public interface Timer {

  /**
   * Cause the timer and all its associated expiration notifications to be
   * Canceled.
   * 
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public void cancel() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException;

  /**
   * Get a serializable handle to the timer. This handle can be used at a later
   * time to re-obtain the timer reference.
   * 
   * @return A serializable handle to the timer.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public TimerHandle getHandle() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException;

  /**
   * Get the information associated with the timer at the time of creation.
   * 
   * @return The Serializable object that was passed in at timer creation, or
   *         null if the info argument passed in at timer creation was null.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public Serializable getInfo() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException;

  /**
   * Get the point in time at which the next timer expiration is scheduled to
   * occur.
   * 
   * @return The point in time at which the next timer expiration is scheduled
   *         to occur.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public Date getNextTimeout() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException;

  /**
   * Get the number of milliseconds that will elapse before the next scheduled
   * timer expiration.
   * 
   * @return The number of milliseconds that will elapse before the next
   *         scheduled timer expiration.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public long getTimeRemaining() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException;

  /**
   * Get the schedule expression corresponding to this timer.
   * 
   * @return Schedule expression corresponding to this timer.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method. Also thrown if invoked on a
   *           timer that was created with one of the non-ScheduleExpression
   *           TimerService.createTimer APIs.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public ScheduleExpression getSchedule() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException;

  /**
   * Query whether this timer is a calendar-based timer.
   * 
   * @return true if this timer is a calendar-based timer.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been cancelled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public boolean isCalendarTimer() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException;

  /**
   * Query whether this timer has persistent semantics.
   * 
   * @return true if this timer has persistent guarantees.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public boolean isPersistent() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException;
}