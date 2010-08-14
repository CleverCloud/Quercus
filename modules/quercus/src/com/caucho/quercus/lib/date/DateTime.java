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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.date;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.ClassImplementation;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.DefaultValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.date.DateModule.DateParser;
import com.caucho.util.Alarm;
import com.caucho.util.QDate;

/**
 * Date functions.
 */
public class DateTime
{
  public static final String ATOM = "Y-m-d\\TH:i:sP";
  public static final String COOKIE = "l, d-M-y H:i:s T";
  public static final String ISO8601 = "Y-m-d\\TH:i:sO";
  public static final String RFC822 = "D, d M y H:i:s O";
  public static final String RFC850 = "l, d-M-y H:i:s T";
  public static final String RFC1036 = "D, d M y H:i:s O";
  public static final String RFC1123 = "D, d M Y H:i:s O";
  public static final String RFC2822 = "D, d M Y H:i:s O";
  public static final String RFC3339 = "Y-m-d\\TH:i:sP";
  public static final String RSS = "D, d M Y H:i:s O";
  public static final String W3C = "Y-m-d\\TH:i:sP";
  
  private QDate _qDate;
  private DateTimeZone _dateTimeZone;
  
  protected DateTime(Env env, String timeString)
  {
    this(env, timeString, new DateTimeZone(env));
  }
  
  protected DateTime(Env env, String timeString, DateTimeZone dateTimeZone)
  {
    _qDate = new QDate(dateTimeZone.getTimeZone(), env.getCurrentTime());
    _dateTimeZone = dateTimeZone;
    
    init(env, timeString);
  }
  
  private void init(Env env, String timeString)
  {
    long now = env.getCurrentTime();
    _qDate.setGMTTime(now);

    if (timeString.equals("")) {
      _qDate.setHour(0);
      _qDate.setMinute(0);
      _qDate.setSecond(0);
    }

    DateParser parser = new DateParser(timeString, _qDate);
        
    long time = parser.parse();
    
    _qDate.setGMTTime(time);
  }
  
  public static DateTime __construct(Env env,
                                     @Optional("now") String time,
                                     @Optional DateTimeZone timeZone)
  {
    if (timeZone == null)
      return new DateTime(env, time);
    else
      return new DateTime(env, time, timeZone);
  }
  
  public String format(String format)
  {
    long time = _qDate.getGMTTime() / 1000;
    
    QDate calendar = new QDate(_qDate.getLocalTimeZone());

    return DateModule.dateImpl(format, time, calendar);
  }
  
  public void modify(String modify)
  {
    DateParser parser = new DateParser(modify, _qDate);
    
    long time = parser.parse();
    
    setTime(time);
  }
  
  public DateTimeZone getTimeZone()
  {
    return _dateTimeZone;
  }
  
  public void setTimeZone(Env env, DateTimeZone dateTimeZone)
  {
    _dateTimeZone = dateTimeZone;
    
    long time = _qDate.getGMTTime();
    
    _qDate = new QDate(dateTimeZone.getTimeZone(), env.getCurrentTime());
    _qDate.setGMTTime(time);
  }
  
  public long getOffset()
  {
    return _qDate.getZoneOffset() / 1000;
  }
  
  public void setTime(int hour,
                      int minute,
                      @Optional int second)
  {
    _qDate.setTime(hour, minute, second, 0);
  }
  
  public void setDate(int year,
                      int month,
                      int day)
  {
    _qDate.setDate(year, month - 1, day);
  }
  
  public void setISODate(int year,
                         int week, //yes, week, not month
                         @Optional int day)
  {
    throw new UnimplementedException("DateTime::setISODate()");
  }
  
  protected QDate getQDate()
  {
    return _qDate;
  }
  
  protected long getTime()
  {
    return _qDate.getLocalTime();
  }
  
  protected void setTime(long time)
  {
    _qDate.setLocalTime(time);
  }
  
  public String toString()
  {
    return format("now");
  }
}
