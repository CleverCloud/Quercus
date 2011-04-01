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

import java.io.IOException;
import java.util.ArrayList;

import com.caucho.inject.Module;
import com.caucho.loader.Environment;
import com.caucho.util.Alarm;
import com.caucho.util.QDate;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamImpl;
import com.caucho.vfs.WriteStream;

/**
 * Automatically-rotating streams.  Normally, clients will call
 * getStream instead of using the StreamImpl interface.
 */
@Module
public class TimestampFilter extends StreamImpl {

  static final String []DAY_NAMES = {
    "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
  };
  static final String []MONTH_NAMES = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  };

  private static final String []SHORT_WEEKDAY = {
    "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
  };
  private static final String []LONG_WEEKDAY = {
    "Sunday", "Monday", "Tuesday", "Wednesday",
    "Thursday", "Friday", "Saturday"
  };
  private static final String []SHORT_MONTH = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
  };
  private static final String []LONG_MONTH = {
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  };
  
  private WriteStream _stream;
  
  private TimestampBase []_timestamp;

  private boolean _isNullDelimited;
  
  private boolean _isLineBegin = true;
  private boolean _isRecordBegin = true;
  private int _timestampLength = 0;

  /**
   * Create listener.
   *
   * @param path underlying log path
   */
  public TimestampFilter()
  {
  }

  /**
   * Create listener.
   *
   * @param path underlying log path
   */
  public TimestampFilter(WriteStream out, String timestamp)
  {
    _stream = out;
    setTimestamp(timestamp);
  }

  /**
   * If null-delimited, the timestamp only applies after the cr/lf and a null
   */
  public void setNullDelimited(boolean isNullDelimited)
  {
    _isNullDelimited = isNullDelimited;
  }

  public void setTimestamp(String timestamp)
  {
    ArrayList<TimestampBase> timestampList = new ArrayList<TimestampBase>();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < timestamp.length(); i++) {
      char ch = timestamp.charAt(i);

      if (ch == '%') {
        ch = timestamp.charAt(i + 1);
        switch (ch) {
        case 'a': case 'A': case 'b': case 'B': case 'c': case 'd':
        case 'H': case 'I': case 'j': case 'm': case 'M': case 'p':
        case 'S': case 's': case 'W': case 'w': case 'x': case 'X':
        case 'y': case 'Y': case 'Z': case 'z':
          if (sb.length() > 0)
            timestampList.add(new Text(sb.toString()));
          sb.setLength(0);
          timestampList.add(new Code(ch));
          i++;
          break;

        case '{':
          if (sb.length() > 0)
            timestampList.add(new Text(sb.toString()));
          sb.setLength(0);
          for (i += 2;
               i < timestamp.length() && timestamp.charAt(i) != '}';
               i++) {
            sb.append((char) timestamp.charAt(i));
          }
          String type = sb.toString();
          sb.setLength(0);

          if ("thread".equals(type)) {
            timestampList.add(new ThreadTimestamp());
          }
          else if ("env".equals(type)) {
            timestampList.add(new EnvTimestamp());
          }
          else {
            sb.append("%{" + type + "}");
          }
          break;

        default:
          sb.append('%');
          break;
        }
      }
      else
        sb.append(ch);
    }

    if (sb.length() > 0)
      timestampList.add(new Text(sb.toString()));

    _timestamp = new TimestampBase[timestampList.size()];
    timestampList.toArray(_timestamp);
  }

  public void setStream(WriteStream stream)
  {
    _stream = stream;
  }

  @Override
  public Path getPath()
  {
    if (_stream != null)
      return _stream.getPath();
    else
      return super.getPath();
  }

  /**
   * Returns true if the stream can write.
   */
  @Override
  public boolean canWrite()
  {
    return _stream != null && _stream.canWrite();
  }

  /**
   * Write data to the stream.
   */
  @Override
  public void write(byte []buffer, int offset, int length, boolean isEnd)
    throws IOException
  {
    if (_stream == null)
      return;

    if (_timestamp == null) {
      _stream.write(buffer, offset, length);
      return;
    }

    if (length == 0)
      return;
    
    long now;

    now = Alarm.getExactTime();

    for (int i = 0; i < length; i++) {
      int ch = buffer[offset + i];

      if (ch == 0)
        continue;

      if (! _isLineBegin) {
      }
      else if (_isRecordBegin) {
        long start = _stream.getPosition();

        QDate localDate = QDate.allocateLocalDate();

        localDate.setGMTTime(now);

        int len = _timestamp.length;
        for (int j = 0; j < len; j++) {
          _timestamp[j].print(_stream, localDate);
        }

        QDate.freeLocalDate(localDate);

        _timestampLength = (int) (_stream.getPosition() - start);
        _isLineBegin = false;
        _isRecordBegin = false;
      }
      else {
        _isLineBegin = false;
        _isRecordBegin = false;

        for (int j = _timestampLength - 1; j >= 0; j--) {
          _stream.write(' ');
        }
      }

      _stream.write(ch);
      
      if (ch == '\n') {
        _isLineBegin = true;

        if (i + 1 < length && buffer[offset + i + 1] == 0) {
          _isRecordBegin = true;
        }

        // env/02d4
        /*
        if (! _isNullDelimited) {
          _isRecordBegin = true;
        }
        */
      }
      else if (ch == '\r'
               && i + 1 < length && buffer[offset + i + 1] != '\n') {
        _isLineBegin = true;

        if (i + 2 < length && buffer[offset + i + 2] == 0) {
          _isRecordBegin = true;
        }

        // env/02d4
        /*
        if (! _isNullDelimited) {
          _isRecordBegin = true;
        }
        */
      }
    }
    
    // env/02d4
    if (! _isNullDelimited) {
      _isRecordBegin = true;
    }
  }

  /**
   * Flushes the data.
   */
  public void flush()
    throws IOException
  {
    if (_stream != null)
      _stream.flush();
  }

  /**
   * Flushes the data.
   */
  public void close()
    throws IOException
  {
    if (_stream != null)
      _stream.close();
  }

  static class TimestampBase {
    public void print(WriteStream out, QDate cal)
      throws IOException
    {
    }
  }

  static class Text extends TimestampBase {
    private final char []_text;

    Text(String text)
    {
      _text = text.toCharArray();
    }
    
    public void print(WriteStream out, QDate cal)
      throws IOException
    {
      out.print(_text, 0, _text.length);
    }
  }

  static class Code extends TimestampBase {
    private final char _code;

    Code(char code)
    {
      _code = code;
    }
    
    public void print(WriteStream out, QDate cal)
      throws IOException
    {
      switch (_code) {
      case 'a':
        out.print(SHORT_WEEKDAY[cal.getDayOfWeek() - 1]);
        break;

      case 'A':
        out.print(LONG_WEEKDAY[cal.getDayOfWeek() - 1]);
        break;

      case 'b':
        out.print(SHORT_MONTH[cal.getMonth()]);
        break;

      case 'B':
        out.print(LONG_MONTH[cal.getMonth()]);
        break;

      case 'c':
        out.print(cal.printLocaleDate());
        break;

      case 'd':
        out.print((cal.getDayOfMonth()) / 10);
        out.print((cal.getDayOfMonth()) % 10);
        break;

      case 'H':
        int hour = (int) (cal.getTimeOfDay() / 3600000) % 24;
        out.print(hour / 10);
        out.print(hour % 10);
        break;

      case 'I':
        hour = (int) (cal.getTimeOfDay() / 3600000) % 12;
        if (hour == 0)
          hour = 12;
        out.print(hour / 10);
        out.print(hour % 10);
        break;

      case 'j':
        out.print((cal.getDayOfYear() + 1) / 100);
        out.print((cal.getDayOfYear() + 1) / 10 % 10);
        out.print((cal.getDayOfYear() + 1) % 10);
        break;

      case 'm':
        out.print((cal.getMonth() + 1) / 10);
        out.print((cal.getMonth() + 1) % 10);
        break;

      case 'M':
        out.print((cal.getTimeOfDay() / 600000) % 6);
        out.print((cal.getTimeOfDay() / 60000) % 10);
        break;

      case 'p':
        hour = (int) (cal.getTimeOfDay() / 3600000) % 24;
        if (hour < 12)
          out.print("am");
        else
          out.print("pm");
        break;

      case 'S':
        out.print((cal.getTimeOfDay() / 10000) % 6);
        out.print((cal.getTimeOfDay() / 1000) % 10);
        break;

      case 's':
        out.print((cal.getTimeOfDay() / 100) % 10);
        out.print((cal.getTimeOfDay() / 10) % 10);
        out.print(cal.getTimeOfDay() % 10);
        break;

      case 'W':
        int week = cal.getWeek();
        out.print((week + 1) / 10);
        out.print((week + 1) % 10);
        break;

      case 'w':
        out.print(cal.getDayOfWeek() - 1);
        break;

      case 'x':
        out.print(cal.printShortLocaleDate());
        break;
        
      case 'X':
        out.print(cal.printShortLocaleTime());
        break;
    
      case 'y':
        {
          int year = cal.getYear();
          out.print(year / 10 % 10);
          out.print(year % 10);
          break;
        }

      case 'Y':
        {
          int year = cal.getYear();
          out.print(year / 1000 % 10);
          out.print(year / 100 % 10);
          out.print(year / 10 % 10);
          out.print(year % 10);
          break;
        }

      case 'Z':
        if (cal.getZoneName() == null)
          out.print("GMT");
        else
          out.print(cal.getZoneName());
        break;

      case 'z':
        long offset = cal.getZoneOffset();

        if (offset < 0) {
          out.print("-");
          offset = - offset;
        }
        else
          out.print("+");

        out.print((offset / 36000000) % 10);
        out.print((offset / 3600000) % 10);
        out.print((offset / 600000) % 6);
        out.print((offset / 60000) % 10);
        break;
      }
    }
  }

  static class ThreadTimestamp extends TimestampBase {
    public void print(WriteStream out, QDate cal)
      throws IOException
    {
      out.print(Thread.currentThread().getName());
    }
  }

  static class EnvTimestamp extends TimestampBase {
    public void print(WriteStream out, QDate cal)
      throws IOException
    {
      out.print(Environment.getEnvironmentName());
    }
  }
}
