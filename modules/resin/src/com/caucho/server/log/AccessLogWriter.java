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

package com.caucho.server.log;

import com.caucho.env.meter.MeterService;
import com.caucho.env.meter.SemaphoreMeter;
import com.caucho.env.thread.TaskWorker;
import com.caucho.env.thread.ThreadPool;
import com.caucho.log.AbstractRolloverLog;
import com.caucho.util.Alarm;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.concurrent.Semaphore;

/**
 * Represents an log of every top-level request to the server.
 */
public class AccessLogWriter extends AbstractRolloverLog
{
  protected static final L10N L = new L10N(AccessLogWriter.class);
  protected static final Logger log
    = Logger.getLogger(AccessLogWriter.class.getName());

  private static final int BUFFER_SIZE = 65536;
  private static final int BUFFER_GAP = 8 * 1024;

  private final AccessLog _log;

  private boolean _isAutoFlush;
  private final Object _bufferLock = new Object();

  private final Semaphore _logSemaphore = new Semaphore(16 * 1024);
  private final FreeList<LogBuffer> _freeList
    = new FreeList<LogBuffer>(512);

  private LogBuffer _logHead;
  private LogBuffer _logTail;
  private int _logQueueSize;

  private boolean _isFlushing;

  private final AtomicBoolean _hasFlushThread = new AtomicBoolean();
  private final AtomicBoolean _isThreadWake = new AtomicBoolean();

  private final LogWriterTask _logWriterTask = new LogWriterTask();
  private Thread _flushThread;

  private SemaphoreMeter _semaphoreProbe;

  AccessLogWriter(AccessLog log)
  {
    _log = log;

    /*
    _logBuffer = getLogBuffer();
    _buffer = _logBuffer.getBuffer();
    _length = 0;
    */

    _semaphoreProbe = MeterService.createSemaphoreMeter("Resin|Log|Semaphore");
  }

  @Override
  public void init()
    throws IOException
  {
    super.init();

    _isAutoFlush = _log.isAutoFlush();
  }

  void writeThrough(byte []buffer, int offset, int length)
    throws IOException
  {
    synchronized (_bufferLock) {
      write(buffer, offset, length);
    }

    flushStream();
  }

  void writeBuffer(LogBuffer buffer)
  {
    int queueSize = 0;

    synchronized (_bufferLock) {
      if (_logTail != null) {
        _logTail.setNext(buffer);
        _logTail = buffer;
        _logQueueSize++;
      }
      else {
        _logHead = buffer;
        _logTail = buffer;
        _logQueueSize = 1;
      }
    }

    if (_logQueueSize > 32 || _isAutoFlush) {
      _logWriterTask.wake();
    }
  }

  // must be synchronized by _bufferLock.
  @Override
  protected void flush()
  {
    // server/021g
    _logWriterTask.wake();

    /*
    boolean isFlush = false;

    synchronized (_bufferLock) {
      isFlush = flushBuffer();
    }

    if (isFlush) {
      scheduleThread();
    }
    */
  }

  protected void waitForFlush(long timeout)
  {
    long expire;

    expire = Alarm.getCurrentTimeActual() + timeout;

    while (true) {
      if (_logHead == null)
        return;

      long delta;
      delta = expire - Alarm.getCurrentTimeActual();

      if (delta < 0)
        return;

      if (delta > 50)
        delta = 50;

      try {
        _logWriterTask.wake();

        Thread.sleep(delta);
      } catch (Exception e) {
      }
    }
  }

  private boolean flushBuffer()
  {
    LogBuffer ptr = null;

    synchronized (_bufferLock) {
      ptr = _logHead;
      _logHead = null;
      _logTail = null;
      _logQueueSize = 0;
    }

    if (ptr == null)
      return false;

    while (ptr != null) {
      LogBuffer next = ptr.getNext();
      ptr.setNext(null);

      try {
        write(ptr.getBuffer(), 0, ptr.getLength());
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        freeBuffer(ptr);
      }

      ptr = next;
    }

    try {
      flushStream();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return true;
  }

  LogBuffer allocateBuffer()
  {
    try {
      Thread.interrupted();
      _logSemaphore.acquire();

      _semaphoreProbe.acquire();
    } catch (Exception e) {
      e.printStackTrace();
    }

    LogBuffer buffer = _freeList.allocate();

    if (buffer == null)
      buffer = new LogBuffer();

    return buffer;
  }

  void freeBuffer(LogBuffer logBuffer)
  {
    _logSemaphore.release();
    _semaphoreProbe.release();

    logBuffer.setNext(null);

    _freeList.free(logBuffer);
  }


  /**
   * Closes the log, flushing the results.
   */
  public void destroy()
    throws IOException
  {
    _logWriterTask.destroy();

    /*
    long expire = Alarm.getCurrentTime() + 5000;

    while (_writeQueue.size() > 0 && Alarm.getCurrentTime() < expire) {
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
      }
    }
    */
  }

  class LogWriterTask extends TaskWorker {
    public long runTask()
    {
      flushBuffer();
      
      return -1;
    }
  }
}
