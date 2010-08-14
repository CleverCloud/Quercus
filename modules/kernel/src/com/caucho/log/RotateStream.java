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

package com.caucho.log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.WeakAlarm;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamImpl;
import com.caucho.vfs.WriteStream;

/**
 * Automatically-rotating streams.  Normally, clients will call
 * getStream instead of using the StreamImpl interface.
 */
public class RotateStream extends StreamImpl implements AlarmListener {
  private static final Logger log
    = Logger.getLogger(RotateStream.class.getName());
  
  private static HashMap<Path,WeakReference<RotateStream>> _streams
    = new HashMap<Path,WeakReference<RotateStream>>();
  
  private static HashMap<String,WeakReference<RotateStream>> _formatStreams
    = new HashMap<String,WeakReference<RotateStream>>();

  private final AbstractRolloverLog _rolloverLog = new AbstractRolloverLog();

  private final Alarm _alarm = new WeakAlarm(this);

  private volatile boolean _isInit;
  private volatile boolean _isClosed;

  /**
   * Create rotate stream.
   *
   * @param path underlying log path
   */
  private RotateStream(Path path)
  {
    _rolloverLog.setPath(path);
  }

  /**
   * Create rotate stream.
   *
   * @param path underlying log path
   */
  private RotateStream(String formatPath)
    throws ConfigException
  {
    _rolloverLog.setPathFormat(formatPath);
  }

  /**
   * Returns the rotate stream corresponding to this path
   */
  public static RotateStream create(Path path)
  {
    synchronized (_streams) {
      WeakReference<RotateStream> ref = _streams.get(path);
      RotateStream stream = ref != null ? ref.get() : null;

      if (stream == null) {
        stream = new RotateStream(path);

        _streams.put(path, new WeakReference<RotateStream>(stream));
      }

      return stream;
    }
  }

  /**
   * Returns the rotate stream corresponding to this path
   */
  public static RotateStream create(String path)
    throws ConfigException
  {
    synchronized (_formatStreams) {
      WeakReference<RotateStream> ref = _formatStreams.get(path);
      RotateStream stream = ref != null ? ref.get() : null;

      if (stream == null) {
        stream = new RotateStream(path);

        _formatStreams.put(path, new WeakReference<RotateStream>(stream));
      }

      return stream;
    }
  }

  /**
   * Clears the streams.
   */
  public static void clear()
  {
    synchronized (_streams) {
      for (WeakReference<RotateStream> streamRef : _streams.values()) {
        try {
          RotateStream stream = streamRef.get();

          if (stream != null)
            stream.closeImpl();
        } catch (Throwable e) {
        }
      }
      
      _streams.clear();
    }
    
    synchronized (_formatStreams) {
      for (WeakReference<RotateStream> streamRef : _formatStreams.values()) {
        try {
          RotateStream stream = streamRef.get();

          if (stream != null)
            stream.closeImpl();
        } catch (Throwable e) {
        }
      }
      
      _formatStreams.clear();
    }
  }

  /**
   * Returns the rollover log.
   */
  public AbstractRolloverLog getRolloverLog()
  {
    return _rolloverLog;
  }

  /**
   * Sets the maximum number of rolled logs.
   */
  public void setMaxRolloverCount(int count)
  {
    _rolloverLog.setRolloverCount(count);
  }

  /**
   * Sets the log rollover period, rounded up to the nearest hour.
   *
   * @param period the new rollover period in milliseconds.
   */
  public void setRolloverPeriod(long period)
  {
    _rolloverLog.setRolloverPeriod(new Period(period));
  }

  /**
   * Sets the log rollover size in bytes.
   */
  public void setRolloverSize(long size)
  {
    _rolloverLog.setRolloverSize(new Bytes(size));
  }

  /**
   * Sets the archive format.
   *
   * @param format the archive format.
   */
  public void setArchiveFormat(String format)
  {
    _rolloverLog.setArchiveFormat(format);
  }

  /**
   * Initialize the stream, setting any logStream, System.out and System.err
   * as necessary.
   */
  public void init()
    throws IOException
  {
    synchronized (this) {
      if (_isInit)
        return;
      _isInit = true;
    }

    _rolloverLog.init();

    _alarm.queueAt(_rolloverLog.getNextRolloverCheckTime());
  }

  /**
   * Returns the Path associated with the stream.
   */
  @Override
  public Path getPath()
  {
    return _rolloverLog.getPath();
  }

  /**
   * True if the stream can write
   */
  @Override
  public boolean canWrite()
  {
    return true;
  }

  /**
   * Writes to the stream
   */
  @Override
  public void write(byte []buffer, int offset, int length, boolean isEnd)
    throws IOException
  {
    _rolloverLog.rollover();
    _rolloverLog.write(buffer, offset, length);

    // _alarm.queue(1000);
    _rolloverLog.rollover();
  }

  /**
   * Gets the current write stream
   */
  public WriteStream getStream()
  {
    return new WriteStream(this);
  }    

  /**
   * Flushes the underlying stream.
   */
  @Override
  public void flush()
    throws IOException
  {
    _rolloverLog.flush();
    _rolloverLog.rollover();

    _alarm.queueAt(_rolloverLog.getNextRolloverCheckTime());
  }

  /**
   * The close call does nothing since the rotate stream is shared for
   * many logs.
   */
  @Override
  public void close()
  {
  }

  public void handleAlarm(Alarm alarm)
  {
    try {
      _rolloverLog.flush();
      _rolloverLog.rollover();
    } catch (Throwable e) {
      e.printStackTrace();
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (! _isClosed) {
        long nextTime = _rolloverLog.getNextRolloverCheckTime();
        long now = Alarm.getCurrentTime();

        long delta = nextTime - now;

        if (delta < 60000)
          delta = 60000;

        _alarm.queue(delta);
      }
    }
  }

  /**
   * Closes the underlying stream.
   */
  private void closeImpl()
  {
    try {
      _isClosed = true;
      _rolloverLog.close();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * finalize.
   */
  @Override
  public void finalize()
  {
    closeImpl();
  }
}
