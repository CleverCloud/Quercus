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
 * @author Reza Rahman
 */
package javax.ejb;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wraps the result of an asynchronous method call as a Future object preserving
 * compatability with the business interface signature. The value specified in
 * the constructor will be retrieved by the container and made available to the
 * client.
 */
public class AsyncResult<V> implements Future<V> {

  /**
   * Attempts to cancel execution of this task. This attempt will fail if the
   * task has already completed, already been cancelled, or could not be
   * cancelled for some other reason. If successful, and this task has not
   * started when cancel is called, this task should never run. If the task has
   * already started, then the mayInterruptIfRunning parameter determines
   * whether the thread executing this task should be interrupted in an attempt
   * to stop the task.
   * 
   * @param mayInterruptIfRunning
   *          true if the thread executing this task should be interrupted;
   *          otherwise, in-progress tasks are allowed to complete.
   * @return false if the task could not be cancelled, typically because it has
   *         already completed normally; true otherwise.
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Waits if necessary for the computation to complete, and then retrieves its
   * result.
   * 
   * @return The computed result.
   * @throws CancellationException
   *           If the computation was cancelled.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws ExecutionException
   *           If the computation threw an exception.
   */
  @Override
  public V get() throws CancellationException, InterruptedException,
      ExecutionException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Waits if necessary for at most the given time for the computation to
   * complete, and then retrieves its result, if available.
   * 
   * @param timeout
   *          The maximum time to wait.
   * @param unit
   *          The time unit of the timeout argument.
   * @throws CancellationException
   *           If the computation was cancelled.
   * @throws InterruptedException
   *           If the current thread was interrupted while waiting.
   * @throws ExecutionException
   *           If the computation threw an exception.
   * @throws TimeoutException
   *           If the wait timed out.
   */
  @Override
  public V get(long timeout, TimeUnit unit) throws CancellationException,
      InterruptedException, ExecutionException, TimeoutException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Returns true if this task was cancelled before it completed normally.
   * 
   * @return true if task was cancelled before it completed.
   */
  @Override
  public boolean isCancelled()
  {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Returns true if this task completed. Completion may be due to normal
   * termination, an exception, or cancellation -- in all of these cases, this
   * method will return true.
   * 
   * @return true if this task completed.
   */
  @Override
  public boolean isDone()
  {
    // TODO Auto-generated method stub
    return false;
  }
}