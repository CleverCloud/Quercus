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

package com.caucho.log;

import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Resin's rotating path-based log.
 */
public class StreamHandler extends Handler {
  private static final L10N L = new L10N(StreamHandler.class);
  
  private WriteStream _os;

  private Formatter _formatter;

  private String _timestamp;
  private boolean _isNullDelimited;

  public StreamHandler()
  {
  }

  public StreamHandler(WriteStream os)
  {
    _os = os;
  }

  /**
   * Sets the timestamp.
   */
  public void setTimestamp(String timestamp)
  {
    _timestamp = timestamp;
  }

  /**
   * Sets the formatter.
   */
  public void setFormatter(Formatter formatter)
  {
    _formatter = formatter;
  }

  public void setNullDelimited(boolean isNullDelimited)
  {
    _isNullDelimited = isNullDelimited;
  }

  /**
   * Publishes the record.
   */
  public void publish(LogRecord record)
  {
    if (! isLoggable(record))
      return;

    try {
      if (record == null) {
        synchronized (_os) {
          _os.println("no record");
          
          if (_isNullDelimited)
            _os.write(0);
            
          _os.flush();
        }
        return;
      }

      if (_formatter != null) {
        String value = _formatter.format(record);

        synchronized (_os) {
          _os.println(value);
          if (_isNullDelimited)
            _os.write(0);
          
          _os.flush();
        }
        
        return;
      }
      
      String message = record.getMessage();
      Throwable thrown = record.getThrown();

      synchronized (_os) {
        if (_timestamp != null) {
          _os.print(_timestamp);
        }
          
        if (thrown != null) {
          if (message != null
              && ! message.equals(thrown.toString()) 
              && ! message.equals(thrown.getMessage()))
            _os.println(message);
        
          record.getThrown().printStackTrace(_os.getPrintWriter());
        }
        else {
          _os.println(record.getMessage());
        }
        
        if (_isNullDelimited)
          _os.write(0);
        
        _os.flush();
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Flushes the buffer.
   */
  public void flush()
  {
  }

  /**
   * Closes the handler.
   */
  public void close()
  {
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    if (_os == null || _os.getPath() == null)
      return super.hashCode();
    else
    return _os.getPath().hashCode();
  }

  /**
   * Test for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (getClass() != o.getClass())
      return false;

    StreamHandler handler = (StreamHandler) o;

    if (_os == null || handler._os == null)
      return false;
    else
      return _os.getPath().equals(handler._os.getPath());
  }

  public String toString()
  {
    if (_os == null)
      return "StreamHandler@" + System.identityHashCode(this) + "[]";
    else
      return ("StreamHandler@" + System.identityHashCode(this)
              + "[" + _os.getPath() + "]");
  }
}
