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

import java.util.ArrayList;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.caucho.inject.Module;
import com.caucho.loader.Environment;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.QDate;

/**
 * Formats a timestamp
 */
@Module
public class TimestampFormatter extends Formatter {
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
  
  private TimestampBase []_timestamp;

  /**
   * Create formatter.
   */
  public TimestampFormatter()
  {
    setTimestamp("[%Y-%m-%d %H:%M:%S] %{level} {%{thread}} ");
  }

  public void setValue(String timestamp)
  {
    setTimestamp(timestamp);
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
          else if ("level".equals(type)) {
            timestampList.add(new LevelTimestamp());
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

  /**
   * Formats the record
   */
  @Override
  public String format(LogRecord log)
  {
    if (_timestamp == null) {
      return log.getMessage();
    }
    
    long now;

    if (CauchoSystem.isTesting())
      now = Alarm.getCurrentTime();
    else
      now = System.currentTimeMillis();

    StringBuilder sb = new StringBuilder();

    QDate localDate = QDate.allocateLocalDate();

    localDate.setGMTTime(now);

    int len = _timestamp.length;
    for (int j = 0; j < len; j++) {
      _timestamp[j].format(sb, localDate, log);
    }
    
    QDate.freeLocalDate(localDate);

    sb.append(log.getMessage());

    return sb.toString();
  }

  static class TimestampBase {
    public void format(StringBuilder sb, QDate cal, LogRecord log)
    {
    }
  }

  static class Text extends TimestampBase {
    private final char []_text;

    Text(String text)
    {
      _text = text.toCharArray();
    }
    
    @Override
    public void format(StringBuilder sb, QDate cal, LogRecord log)
    {
      sb.append(_text, 0, _text.length);
    }
  }

  static class Code extends TimestampBase {
    private final char _code;

    Code(char code)
    {
      _code = code;
    }
    
    @Override
    public void format(StringBuilder sb, QDate cal, LogRecord log)
    {
      switch (_code) {
      case 'a':
        sb.append(SHORT_WEEKDAY[cal.getDayOfWeek() - 1]);
        break;

      case 'A':
        sb.append(LONG_WEEKDAY[cal.getDayOfWeek() - 1]);
        break;

      case 'b':
        sb.append(SHORT_MONTH[cal.getMonth()]);
        break;

      case 'B':
        sb.append(LONG_MONTH[cal.getMonth()]);
        break;

      case 'c':
        sb.append(cal.printLocaleDate());
        break;

      case 'd':
        sb.append((cal.getDayOfMonth()) / 10);
        sb.append((cal.getDayOfMonth()) % 10);
        break;

      case 'H':
        int hour = (int) (cal.getTimeOfDay() / 3600000) % 24;
        sb.append(hour / 10);
        sb.append(hour % 10);
        break;

      case 'I':
        hour = (int) (cal.getTimeOfDay() / 3600000) % 12;
        if (hour == 0)
          hour = 12;
        sb.append(hour / 10);
        sb.append(hour % 10);
        break;

      case 'j':
        sb.append((cal.getDayOfYear() + 1) / 100);
        sb.append((cal.getDayOfYear() + 1) / 10 % 10);
        sb.append((cal.getDayOfYear() + 1) % 10);
        break;

      case 'm':
        sb.append((cal.getMonth() + 1) / 10);
        sb.append((cal.getMonth() + 1) % 10);
        break;

      case 'M':
        sb.append((cal.getTimeOfDay() / 600000) % 6);
        sb.append((cal.getTimeOfDay() / 60000) % 10);
        break;

      case 'p':
        hour = (int) (cal.getTimeOfDay() / 3600000) % 24;
        if (hour < 12)
          sb.append("am");
        else
          sb.append("pm");
        break;

      case 'S':
        sb.append((cal.getTimeOfDay() / 10000) % 6);
        sb.append((cal.getTimeOfDay() / 1000) % 10);
        break;

      case 's':
        sb.append((cal.getTimeOfDay() / 100) % 10);
        sb.append((cal.getTimeOfDay() / 10) % 10);
        sb.append(cal.getTimeOfDay() % 10);
        break;

      case 'W':
        int week = cal.getWeek();
        sb.append((week + 1) / 10);
        sb.append((week + 1) % 10);
        break;

      case 'w':
        sb.append(cal.getDayOfWeek() - 1);
        break;

      case 'x':
        sb.append(cal.printShortLocaleDate());
        break;
        
      case 'X':
        sb.append(cal.printShortLocaleTime());
        break;
    
      case 'y':
        {
          int year = cal.getYear();
          sb.append(year / 10 % 10);
          sb.append(year % 10);
          break;
        }

      case 'Y':
        {
          int year = cal.getYear();
          sb.append(year / 1000 % 10);
          sb.append(year / 100 % 10);
          sb.append(year / 10 % 10);
          sb.append(year % 10);
          break;
        }

      case 'Z':
        if (cal.getZoneName() == null)
          sb.append("GMT");
        else
          sb.append(cal.getZoneName());
        break;

      case 'z':
        long offset = cal.getZoneOffset();

        if (offset < 0) {
          sb.append("-");
          offset = - offset;
        }
        else
          sb.append("+");

        sb.append((offset / 36000000) % 10);
        sb.append((offset / 3600000) % 10);
        sb.append((offset / 600000) % 6);
        sb.append((offset / 60000) % 10);
        break;
      }
    }
  }

  static class ThreadTimestamp extends TimestampBase {
    @Override
    public void format(StringBuilder sb, QDate cal, LogRecord log)
    {
      sb.append(Thread.currentThread().getName());
    }
  }

  static class LevelTimestamp extends TimestampBase {
    @Override
    public void format(StringBuilder sb, QDate cal, LogRecord log)
    {
      sb.append(log.getLevel());
    }
  }

  static class EnvTimestamp extends TimestampBase {
    @Override
    public void format(StringBuilder sb, QDate cal, LogRecord log)
    {
      sb.append(Environment.getEnvironmentName());
    }
  }
}
