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

package com.caucho.quercus.lib.date;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Date functions.
 */
public class DateModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(DateModule.class);
  private static final Logger log
    = Logger.getLogger(DateModule.class.getName());

  public static final int CAL_GREGORIAN = 0;
  public static final int CAL_JULIAN = 1;

  private static final String []_shortDayOfWeek = {
    "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
  };

  private static final String []_fullDayOfWeek = {
    "Sunday", "Monday", "Tuesday", "Wednesday",
    "Thursday", "Friday", "Saturday"
  };

  private static final String []_shortMonth = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
  };
  private static final String []_fullMonth = {
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  };

  private static final long MINUTE = 60000L;
  private static final long HOUR = 60 * MINUTE;
  private static final long DAY = 24 * HOUR;

  /**
   * Returns the days in a given month.
   */
  public static int cal_days_in_month(Env env,
                                      int cal, int month, int year)
  {
    QDate date = env.getDate();
    date.setGMTTime(env.getCurrentTime());
    
    date.setYear(year);
    date.setMonth(month - 1);

    return date.getDaysInMonth();
  }

  /**
   * Returns the days in a given month.
   */
  public static boolean checkdate(Env env, int month, int day, int year)
  {
    if (! (1 <= year && year <= 32767))
      return false;
    
    if (! (1 <= month && month <= 12))
      return false;

    return 1 <= day && day <= cal_days_in_month(env, 0, month, year);
  }

  /**
   * Returns the formatted date.
   */
  public String date(Env env,
                     String format,
                     @Optional("time()") long time)
  {
    return date(env, format, time, false);
  }
  
  /**
   * Returns the formatted date as an int.
   */
  public Value idate(Env env,
                     String format,
                     @Optional("time()") long time)
  {
    if (format.length() != 1) {
      log.log(Level.FINE,
          L.l(
              "idate format '{0}' needs to be of length one and only one",
              format));
      env.warning(L.l(
          "idate format '{0}' needs to be of length one and only one",
          format));

      return BooleanValue.FALSE;
    }
    
    switch (format.charAt(0)) {
    case 'B':
    case 'd':
    case 'h': case 'H':
    case 'i':
    case 'I':
    case 'L':
    case 'm':
    case 's':
    case 't':
    case 'U':
    case 'w':
    case 'W':
    case 'y':
    case 'Y':
    case 'z':
    case 'Z':
      String dateString = date(env, format, time, false);

      int sign = 1;
      long result = 0;
      int length = dateString.length();
        
      for (int i = 0; i < length; i++) {
        char ch = dateString.charAt(i);
          
        if ('0' <= ch && ch <= '9')
          result = result * 10 + ch - '0';
        else if (ch == '-' && i == 0)
          sign = -1;
        else {
          log.log(Level.FINEST, L.l(
              "error parsing idate string '{0}'", dateString));
          break;
        }
      }

      return LongValue.create(result * sign);

    default:
      log.log(Level.FINE, L.l("'{0}' is not a valid idate format", format));
      env.warning(L.l("'{0}' is not a valid idate format", format));
        
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the timestamp of easter.
   */
  public static long easter_date(Env env,
                                 @Optional("-1") int year)
  {
    long now = env.getCurrentTime();
    
    QDate date = env.getDate();
    date.setGMTTime(now);
    
    if (year < 0) {
      date.setGMTTime(now);
      
      year = date.getYear();
    }

    int y = year;

    int c = y / 100;
    int n = y - 19 * (y / 19);
    int k = (c - 17) / 25;
    int i = c - c / 4 - (c - k) / 3 + 19 * n + 15;
    i = i - 30 * (i / 30);
    i = i - (i / 28) * (1 - ((i / 28)
        * (29 / (i + 1))
        * ((21 - n) / 11)));

    int j = y + y / 4 + i + 2 - c + c / 4;
    j = j - 7 * (j / 7);
    int l = i - j;
    int m = 3 + (l + 40) / 44;
    int d = l + 28 - 31 * (m / 4);

    date.setYear(year);
    date.setMonth(m - 1);
    date.setDayOfMonth(d);

    return date.getGMTTime() / 1000;
  }

  /**
   * Returns the timestamp of easter.
   */
  public static long easter_days(Env env,
                                 @Optional("-1") int year,
                                 @Optional int method)
  {
    return easter_date(env, year);
  }

  /**
   * Returns an array of the current date.
   */
  public Value getdate(Env env, @Optional Value timeV)
  {
    QDate date = env.getDate();
    
    long time;
    
    if (timeV.isDefault())
      time = env.getCurrentTime();
    else
      time = timeV.toLong() * 1000L;
    
    date.setGMTTime(time);

    ArrayValue array = new ArrayValueImpl();

    array.put("seconds", date.getSecond());
    array.put("minutes", date.getMinute());
    array.put("hours", date.getHour());
    array.put("mday", date.getDayOfMonth());
    array.put("wday", date.getDayOfWeek() - 1);
    array.put("mon", date.getMonth() + 1);
    array.put("year", date.getYear());
    array.put("yday", date.getDayOfYear());
    array.put("weekday", _fullDayOfWeek[date.getDayOfWeek() - 1]);
    array.put("month", _fullMonth[date.getMonth()]);
    array.put(LongValue.ZERO, LongValue.create(time / 1000L));

    return array;
  }

  public Value gettimeofday(Env env, @Optional boolean isFloatReturn)
  {
    long gmtTime = env.getExactTime();

    if (isFloatReturn) {
      return new DoubleValue(((double) gmtTime) / 1000.0);
    }
    else {
      ArrayValueImpl result = new ArrayValueImpl();

      TimeZone localTimeZone = TimeZone.getDefault();

      long sec = gmtTime / 1000L;
      long microsec = (gmtTime - (sec * 1000)) * 1000L;
      long minutesWest = localTimeZone.getRawOffset() / 1000L / 60L * -1L;
      long dstTime = localTimeZone.useDaylightTime() ? 1 : 0;

      result.put("sec", sec);
      result.put("usec", microsec);
      result.put("minuteswest", minutesWest);
      result.put("dsttime", dstTime);

      return result;
    }
  }

  /**
   * Returns the formatted date.
   */
  public String gmdate(Env env,
                       String format,
                       @Optional("time()") long time)
  {
    return date(env, format, time, true);
  }

  /**
   * Returns the formatted date.
   */
  public long gmmktime(Env env,
                       @Optional() Value hourV,
                       @Optional() Value minuteV,
                       @Optional() Value secondV,
                       @Optional() Value monthV,
                       @Optional() Value dayV,
                       @Optional() Value yearV)
  {
    QDate date = env.getGmtDate();
    long now = env.getCurrentTime();

    date.setLocalTime(now);

    long gmtNow = date.getGMTTime();

    date.setGMTTime(gmtNow);

    setMktime(date, hourV, minuteV, secondV, monthV, dayV, yearV);

    return date.getGMTTime() / 1000L;
  }

  /**
   * Returns the formatted date.
   */
  public String gmstrftime(Env env,
                           String format,
                           @Optional("-1") long phpTime)
  {
    long time;

    if (phpTime == -1)
      time = env.getCurrentTime();
    else
      time = 1000 * phpTime;

    return QDate.formatGMT(time, format);
  }

  /**
   * Convert from a gregorian date to a julian day.
   */
  public double gregoriantojd(int month, int day, int year)
  {
    if (month <= 2) {
      year -= 1;
      month += 12;
    }

    long a = year / 100;
    long b = a / 4;
    long c = 2 - a + b;
    long e = (long) (365.25 * (year + 4716));
    long f = (long) (30.6001 * (month + 1));

    return (c + day + e + f - 1524.5);
  }

  private String date(Env env, String format, long time, boolean isGMT)
  {
    if (isGMT) {
      return dateImpl(format, time, env.getGmtDate());
    }
    else {
      QDate calendar = env.getDate();

      return dateImpl(format, time, calendar);
    }
  }
  
  /**
   * Returns the formatted date.
   */
  protected static String dateImpl(String format,
                                   long time,
                                   QDate calendar)
  {
    long now = 1000 * time;
    
    calendar.setGMTTime(now);

    CharBuffer sb = new CharBuffer();
    int len = format.length();

    for (int i = 0; i < len; i++) {
      char ch = format.charAt(i);

      switch (ch) {
        //
        // day
        //

        case 'd':
          {
            int day = calendar.getDayOfMonth();
            sb.append(day / 10);
            sb.append(day % 10);
            break;
          }

        case 'D':
          {
            // subtract 1 to be zero-based for array
            int day = calendar.getDayOfWeek() - 1;

            sb.append(_shortDayOfWeek[day]);
            break;
          }

        case 'j':
          {
            int day = calendar.getDayOfMonth();
            sb.append(day);
            break;
          }

        case 'l':
          {
            // subtract 1 to be zero-based for array
            int day = calendar.getDayOfWeek() - 1;

            sb.append(_fullDayOfWeek[day]);
            break;
          }

        case 'N':
          {
            int day = calendar.getDayOfWeek();

            // Mon=1, Sun=7
            day = day - 1;

            if (day == 0)
              day = 7;

            sb.append(day);
            break;
          }

        case 'S':
          {
            int day = calendar.getDayOfMonth();

            switch (day) {
              case 1: case 21: case 31:
                sb.append("st");
                break;
              case 2: case 22:
                sb.append("nd");
                break;
              case 3: case 23:
                sb.append("rd");
                break;
              default:
                sb.append("th");
                break;
            }
            break;
          }

        case 'w':
          {
            int day = calendar.getDayOfWeek() - 1;

            sb.append(day);
            break;
          }

        case 'z':
          {
            int day = calendar.getDayOfYear();

            sb.append(day);
            break;
          }

        //
        // week
        //

        case 'W':
          {
            int week = calendar.getWeek();

            sb.append(week / 10);
            sb.append(week % 10);
            break;
          }

        //
        // month
        //

        case 'F':
          {
            int month = calendar.getMonth();
            sb.append(_fullMonth[month]);
            break;
          }

        case 'm':
          {
            int month = calendar.getMonth() + 1;
            sb.append(month / 10);
            sb.append(month % 10);
            break;
          }

        case 'M':
          {
            int month = calendar.getMonth();
            sb.append(_shortMonth[month]);
            break;
          }

        case 'n':
          {
            int month = calendar.getMonth() + 1;
            sb.append(month);
            break;
          }

        case 't':
          {
            int days = calendar.getDaysInMonth();
            sb.append(days);
            break;
          }

        //
        // year
        //

        case 'L':
          {
            if (calendar.isLeapYear())
              sb.append(1);
            else
              sb.append(0);
            break;
          }

        case 'o':
          {
            int year = calendar.getYear();

            int week = calendar.getWeek();
            int month = calendar.getMonth();

            if (month > week)
              year++;
            else if (week == 53)
              year--;

            sb.append((year / 1000) % 10);
            sb.append((year / 100) % 10);
            sb.append((year / 10) % 10);
            sb.append((year) % 10);
            break;
          }

        case 'Y':
          {
            int year = calendar.getYear();

            sb.append((year / 1000) % 10);
            sb.append((year / 100) % 10);
            sb.append((year / 10) % 10);
            sb.append((year) % 10);
            break;
          }

        case 'y':
          {
            int year = calendar.getYear();

            sb.append((year / 10) % 10);
            sb.append((year) % 10);
            break;
          }

        //
        // time
        //

        case 'a':
          {
            int hour = calendar.getHour();

            if (hour < 12)
              sb.append("am");
            else
              sb.append("pm");
            break;
          }

        case 'A':
          {
            int hour = calendar.getHour();

            if (hour < 12)
              sb.append("AM");
            else
              sb.append("PM");
            break;
          }

        case 'g':
          {
            int hour = calendar.getHour() % 12;

            if (hour == 0)
              hour = 12;

            sb.append(hour);
            break;
          }

        case 'G':
          {
            int hour = calendar.getHour();

            sb.append(hour);
            break;
          }

        case 'h':
          {
            int hour = calendar.getHour() % 12;

            if (hour == 0)
              hour = 12;

            sb.append(hour / 10);
            sb.append(hour % 10);
            break;
          }

        case 'H':
          {
            int hour = calendar.getHour();

            sb.append(hour / 10);
            sb.append(hour % 10);
            break;
          }

        case 'i':
          {
            int minutes = calendar.getMinute();

            sb.append(minutes / 10);
            sb.append(minutes % 10);
            break;
          }

        case 's':
          {
            int seconds = calendar.getSecond();

            sb.append(seconds / 10);
            sb.append(seconds % 10);
            break;
          }

        //
        // timezone
        //

        case 'e':
          {
            TimeZone zone = calendar.getLocalTimeZone();

            sb.append(zone.getID());
            break;
          }

        case 'I':
          {
            if (calendar.isDST())
              sb.append('1');
            else
              sb.append('0');
            break;
          }

        case 'O':
          {
            long offset = calendar.getZoneOffset();

            int minute = (int) (offset / (60 * 1000));

            if (minute < 0) {
              sb.append('-');
              minute = -1 * minute;
            }
            else
              sb.append('+');

            sb.append((minute / 60) / 10);
            sb.append((minute / 60) % 10);
            sb.append((minute % 60) % 10);
            sb.append(minute % 10);
            break;
          }

        case 'P':
          {
            long offset = calendar.getZoneOffset();

            int minute = (int) (offset / (60 * 1000));

            if (minute < 0) {
              sb.append('-');
              minute = -1 * minute;
            }
            else
              sb.append('+');

            sb.append((minute / 60) / 10);
            sb.append((minute / 60) % 10);
            sb.append(':');
            sb.append((minute % 60) % 10);
            sb.append(minute % 10);
            break;
          }

        case 'T':
          {
            TimeZone zone = calendar.getLocalTimeZone();

            sb.append(zone.getDisplayName(calendar.isDST(), TimeZone.SHORT));
            break;
          }

        case 'Z':
          {
            long offset = calendar.getZoneOffset();

            sb.append(offset / (1000));
            break;
          }

        case 'c':
          {
            sb.append(calendar.printISO8601());
            break;
          }

        case 'r':
          {
            calendar.printRFC2822(sb);
            break;
          }

        case 'U':
          {
            sb.append(now / 1000);
            break;
          }

        case '\\':
          sb.append(format.charAt(++i));
          break;

        default:
          sb.append(ch);
          break;
      }
    }

    return sb.toString();
  }

  /**
   * Returns the time as an indexed or associative array
   */
  public ArrayValue localtime(Env env,
                              @NotNull @Optional("-1") long time,
                              @Optional("false") boolean isAssociative)
  {
    if (time < 0)
      time  = env.getCurrentTime();
    else
      time = time * 1000;

    long sec;
    long min;
    long hour;
    long mday;
    long mon;
    long year;
    long wday;
    long yday;
    long isdst;

    QDate localCalendar = env.getLocalDate();

    localCalendar.setGMTTime(time);

    sec = localCalendar.getSecond();
    min = localCalendar.getMinute();
    hour = localCalendar.getHour();
    mday = localCalendar.getDayOfMonth();
    mon = localCalendar.getMonth();
    year = localCalendar.getYear();
    wday = localCalendar.getDayOfWeek();
    yday = localCalendar.getDayOfYear();
    isdst = localCalendar.isDST() ? 1 : 0;

    year = year - 1900;
    wday = wday - 1;
    
    ArrayValue value = new ArrayValueImpl();

    if (isAssociative) {
      value.put("tm_sec", sec);
      value.put("tm_min", min);
      value.put("tm_hour", hour);
      value.put("tm_mday", mday);
      value.put("tm_mon", mon);
      value.put("tm_year", year);
      value.put("tm_wday", wday);
      value.put("tm_yday", yday);
      value.put("tm_isdst", isdst);
    }
    else {
      value.put(sec);
      value.put(min);
      value.put(hour);
      value.put(mday);
      value.put(mon);
      value.put(year);
      value.put(wday);
      value.put(yday);
      value.put(isdst);
    }

    return value;
  }

  /**
   * Returns the time including microseconds
   */
  public static Value microtime(Env env, @Optional boolean getAsFloat)
  {
    double now = env.getMicroTime() * 1E-6;

    if (getAsFloat) {
      return new DoubleValue(now);
    }
    else {
      return (env.createUnicodeBuilder()
              .append(String.format("%.6f", now - Math.floor(now)))
              .append(' ')
              .append((int) Math.floor(now)));
    }
  }

  /**
   * Returns the formatted date.
   */
  public long mktime(Env env,
                     @Optional() Value hourV,
                     @Optional() Value minuteV,
                     @Optional() Value secondV,
                     @Optional() Value monthV,
                     @Optional() Value dayV,
                     @Optional() Value yearV,
                     @Optional("-1") int isDST)
  {
    if (isDST != -1)
      env.deprecatedArgument("isDST");

    long now = env.getCurrentTime();
    
    QDate date = env.getDate();
    date.setGMTTime(now);
    
    setMktime(date, hourV, minuteV, secondV, monthV, dayV, yearV);

    return date.getGMTTime() / 1000L;
  }
  
  private static void setMktime(QDate date,
                                Value hourV,
                                Value minuteV,
                                Value secondV,
                                Value monthV,
                                Value dayV,
                                Value yearV)
  {
    if (! hourV.isDefault()) {
      int hour = hourV.toInt();
      
      date.setHour(hour);
    }

    if (! minuteV.isDefault()) {
      int minute = minuteV.toInt();
      
      date.setMinute(minute);
    }
    
    if (! secondV.isDefault()) {
      int second = secondV.toInt();
      
      date.setSecond(second);
    }

    if (! monthV.isDefault()) {
      int month = monthV.toInt();
      
      date.setMonth(month - 1);
    }

    if (! dayV.isDefault()) {
      int day = dayV.toInt();
      
      date.setDayOfMonth(day);
    }
    
    if (! yearV.isDefault()) {
      int year = yearV.toInt();
      
      if (year >= 1000) {
        date.setYear(year);
      }
      else if (year >= 70) {
        date.setYear(year + 1900);
      }
      else if (year >= 0) {
        date.setYear(year + 2000);
      }
      else if (year < 0) {
        date.setYear(1969);
      }
    }
  }

  /**
   * Returns the formatted date.
   */
  public String strftime(Env env,
                         String format,
                         @Optional("-1") long phpTime)
  {
    long time;

    if (phpTime == -1)
      time = env.getCurrentTime();
    else
      time = 1000 * phpTime;

    return QDate.formatLocal(time, format);
  }

  /**
   * Parses the time
   */
  public Value strtotime(Env env,
                         String timeString,
                         @Optional("-1") long now)
  {
    try {
      if (now >= 0)
        now = 1000L * now;
      else
        now = env.getCurrentTime();

      QDate date = env.getDate();
      date.setGMTTime(now);

      if (timeString.equals("")) {
        date.setHour(0);
        date.setMinute(0);
        date.setSecond(0);

        return new LongValue(date.getGMTTime() / 1000L);
      }

      DateParser parser = new DateParser(timeString, date);

      return new LongValue(parser.parse() / 1000L);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the current time in seconds.
   */
  public static long time(Env env)
  {
    return env.getCurrentTime() / 1000L;
  }

  /**
   * Convert from a julian day to unix
   */
  public long jdtounix(Env env, double jd)
  {
    long z = (long) (jd + 0.5);
    long w = (long) ((z - 1867216.25) / 36524.25);
    long x = (long) (w / 4);
    long a = (long) (z + 1 + w - x);
    long b = (long) (a + 1524);
    long c = (long) ((b - 122.1) / 365.25);
    long d = (long) (365.25 * c);
    long e = (long) ((b - d) / 30.6001);
    long f = (long) (30.6001 * e);

    long day = b - d - f;
    long month = e - 1;
    long year = c - 4716;

    if (month > 12) {
      month -= 12;
      year += 1;
    }

    QDate localCalendar = env.getLocalDate();

    localCalendar.setHour(0);
    localCalendar.setMinute(0);
    localCalendar.setSecond(0);
    localCalendar.setDayOfMonth((int) day);
    localCalendar.setMonth((int) (month - 1));
    localCalendar.setYear((int) year);

    return localCalendar.getLocalTime() / 1000L;
  }

  public static DateTime date_create(Env env,
                                     @Optional("now") String time,
                                     @Optional DateTimeZone dateTimeZone)
  {
    return DateTime.__construct(env, time, dateTimeZone);
  }
  
  public static void date_date_set(DateTime dateTime,
                                   int year,
                                   int month,
                                   int day)
  {
    dateTime.setDate(year, month, day);
  }
  
  public static String date_default_timezone_get(Env env)
  {
    TimeZone timeZone = env.getDefaultTimeZone();
    
    return timeZone.getID();
  }
  
  public static boolean date_default_timezone_set(Env env, String id)
  {
    env.setDefaultTimeZone(id);

    return true;
  }
  
  public static String date_format(DateTime dateTime, String format) 
  {
    return dateTime.format(format);
  }
  
  public static void date_isodate_set(DateTime dateTime,
                                      int year,
                                      int week,
                                      int day)
  {
    dateTime.setISODate(year, week, day);
  }
  
  public static void date_modify(DateTime dateTime, String modify)
  {
    dateTime.modify(modify);
  }
  
  public static long date_offset_get(DateTime dateTime)
  {
    return dateTime.getOffset();
  }
  
  public static Value date_parse(Env env, String date)
  {
    DateTime dateTime = new DateTime(env, date);
    QDate qDate = dateTime.getQDate();
    
    ArrayValue array = new ArrayValueImpl();
    
    array.put("year", qDate.getYear());
    array.put("month", qDate.getMonth() + 1);
    array.put("day", qDate.getDayOfMonth());
    array.put("hour", qDate.getHour());
    array.put("minute", qDate.getMinute());
    array.put("second", qDate.getSecond());
    array.put("fraction", qDate.getMillisecond() / 1000.0);
    
    //warning_count
    //warnings
    //error_count
    //errors
    //is_localtime
    
    return array;
  }
  
  public static ArrayValue date_sun_info(long time,
                                         double latitude,
                                         double longitude)
  {
    throw new UnimplementedException("date_sun_info");
  }
  
  public static Value date_sunrise(int timestamp,
                                   @Optional int format,
                                   @Optional double latitude,
                                   @Optional double longitude,
                                   @Optional double zenith,
                                   @Optional double gmtOffset)
  {
  //gmtOffset is specified in hours
    throw new UnimplementedException("date_sunrise");
  }
  
  public static Value date_sunset(int timestamp,
                                  @Optional int format,
                                  @Optional double latitude,
                                  @Optional double longitude,
                                  @Optional double zenith,
                                  @Optional double gmtOffset)
  {
    //gmtOffset is specified in hours
    throw new UnimplementedException("date_sunset");
  }
  
  public static void date_time_set(DateTime dateTime,
                                   int hour,
                                   int minute,
                                   @Optional int second)
  {
    dateTime.setTime(hour, minute, second);
  }

  @ReturnNullAsFalse
  public static DateTimeZone date_timezone_get(Env env,
                                               @NotNull DateTime dateTime)
  {
    if (dateTime == null) {
      env.warning("DateTime parameter must not be null");
      
      return null;
    }
    
    return dateTime.getTimeZone();
  }
  
  public static void date_timezone_set(Env env,
                                       @NotNull DateTime dateTime,
                                       @NotNull DateTimeZone dateTimeZone)
  {
    if (dateTime == null || dateTimeZone == null) {
      env.warning("parameters must not be null");
      
      return;
    }
    
    dateTime.setTimeZone(env, dateTimeZone);
  }
  
  public static ArrayValue timezone_abbreviations_list()
  {
    return DateTimeZone.listAbbreviations();
  }
  
  public static ArrayValue timezone_identifiers_list()
  {
    return DateTimeZone.listIdentifiers();
  }
  
  public static Value timezone_name_from_abbr(StringValue abbr,
                                              @Optional("-1") int gmtOffset,
                                              @Optional boolean isDST)
  {
    if (gmtOffset == -1)
      return DateTimeZone.findTimeZone(abbr);
    else
      return DateTimeZone.findTimeZone(abbr, gmtOffset, isDST);
  }
  
  public static String timezone_name_get(DateTimeZone dateTimeZone)
  {
    return dateTimeZone.getName();
  }
  
  public static long timezone_offset_get(DateTimeZone dateTimeZone,
                                         DateTime dateTime)
  {
    if (dateTimeZone == null)
      return 0;
    else
      return dateTimeZone.getOffset(dateTime);
  }
  
  public static DateTimeZone timezone_open(String timeZone)
  {
    return new DateTimeZone(timeZone);
  }
  
  /* commented out for wordpress-2.8.1
  public static Value timezone_transitions_get(DateTimeZone dateTimeZone)
  {
    return dateTimeZone.getTransitions();
  }
  */
  
  static class DateParser {
    private static final int INT = 1;
    private static final int PERIOD = 2;
    private static final int AGO = 3;
    private static final int AM = 4;
    private static final int PM = 5;
    private static final int MONTH = 6;
    private static final int WEEKDAY = 7;
    private static final int UTC = 8;

    private static final int UNIT_YEAR = 1;
    private static final int UNIT_MONTH = 2;
    private static final int UNIT_FORTNIGHT = 3;
    private static final int UNIT_WEEK = 4;
    private static final int UNIT_DAY = 5;
    private static final int UNIT_HOUR = 6;
    private static final int UNIT_MINUTE = 7;
    private static final int UNIT_SECOND = 8;
    private static final int UNIT_NOW = 9;

    private static final int NULL_VALUE = Integer.MAX_VALUE;

    private QDate _date;

    private String _s;
    private int _index;
    private int _length;

    private StringBuilder _sb = new StringBuilder();

    private int _peekToken;

    private int _value;
    private int _digits;
    private int _unit;
    private int _weekday;

    private boolean _hasDate;
    private boolean _hasTime;

    DateParser(String s, QDate date)
    {
      _date = date;
      _s = s;
      _length = s.length();
    }

    long parse()
    {
      _value = NULL_VALUE;
      _unit = 0;

      while (true) {
        int token = nextToken();

        if (token == '-') {
          token = nextToken();

          if (token == INT)
            _value = -_value;
          else {
            _peekToken = token;
            continue;
          }
        }

        if (token < 0) {
          if (_hasDate && ! _hasTime)
            _date.setTime(0, 0, 0, 0);

          return _date.getGMTTime();
        }
        else if (token == INT) {
          int digits = _digits;
          int value = _value;

          token = nextToken();

          if (token == PERIOD) {
            parsePeriod();
          }
          else if (token == ':') {
            parseTime();
            _hasTime = true;
          }
          else if (token == '-') {
            parseISODate(value);
            _hasDate = true;
          }
          else if (token == '/') {
            parseUSDate(value);
            _hasDate = true;
          }
          else if (token == MONTH) {
            parseDayMonthDate(value);
            _hasDate = true;
          }
          else {
            _peekToken = token;

            parseBareInt(value, digits);
          }
        }
        else if (token == PERIOD) {
          parsePeriod();
        }
        else if (token == WEEKDAY) {
          addWeekday(_value, _weekday);
          _value = NULL_VALUE;
        }
        else if (token == MONTH) {
          parseMonthDate(_value);
          _hasDate = true;
        }
        else if (token == '@') {
          token = nextToken();

          if (token == INT) {
            int value = _value;
            _value = NULL_VALUE;

            _date.setGMTTime(value * 1000L);

            token = nextToken();
            if (token == '.') {
              token = nextToken();

              if (token != INT)
                _peekToken = token;
            }
            else {
              _peekToken = token;
            }
          }
        }
      }
    }

    private void parsePeriod()
    {
      int value = _value;
      int unit = _unit;

      _value = NULL_VALUE;
      _unit = 0;

      int token = nextToken();
      if (token == AGO)
        value = -value;
      else
        _peekToken = token;

      addTime(value, unit);
    }

    private void parseISODate(int value1)
    {
      int year = _date.getYear();
      int month = 0;
      int day = 0;

      if (value1 < 0)
        value1 = - value1;

      int token = nextToken();

      int value2 = 0;

      if (token == INT) {
        value2 = _value;
        _value = NULL_VALUE;
      }
      else {
        _peekToken = token;
        return;
      }

      token = nextToken();

      if (token == '-') {
        token = nextToken();

        if (token == INT) {
          if (value1 < 0)
            _date.setYear(value1);
          else if (value1 <= 68)
            _date.setYear(2000 + value1);
          else if (value1 < 100)
            _date.setYear(1900 + value1);
          else
            _date.setYear(value1);

          _date.setMonth(value2 - 1);
          _date.setDayOfMonth(_value);
        }
        else {
          _date.setMonth(value1 - 1);
          _date.setDayOfMonth(value2);

          _peekToken = token;
        }
      }
      else {
        _date.setMonth(value1 - 1);
        _date.setDayOfMonth(value2);

        _peekToken = token;
      }
    }

    private void parseUSDate(int value1)
    {
      int year = _date.getYear();
      int month = 0;
      int day = 0;

      if (value1 < 0)
        value1 = - value1;

      int token = nextToken();

      int value2 = 0;

      if (token == INT) {
        value2 = _value;
      }
      else {
        _peekToken = token;
        return;
      }

      _value = NULL_VALUE;
      token = nextToken();

      if (token == '/') {
        token = nextToken();

        if (token == INT) {
          _date.setMonth(value1 - 1);
          _date.setDayOfMonth(value2);

          if (_value < 0)
            _date.setYear(_value);
          else if (_value <= 68)
            _date.setYear(2000 + _value);
          else if (_value < 100)
            _date.setYear(1900 + _value);
          else
            _date.setYear(_value);
        }
        else {
          _date.setMonth(value1 - 1);
          _date.setDayOfMonth(value2);

          _peekToken = token;
        }
        _value = NULL_VALUE;
      }
      else {
        _date.setMonth(value1 - 1);
        _date.setDayOfMonth(value2);

        _peekToken = token;
      }
    }

    private void parseDayMonthDate(int value1)
    {
      int year = _date.getYear();
      int month = 0;
      int day = 0;

      if (value1 < 0)
        value1 = - value1;

      int value2 = _value;

      _value = NULL_VALUE;
      int token = nextToken();

      if (token == '-') {
        _value = NULL_VALUE;
        token = nextToken();
      }

      // check that this is a real number (a year) rather than an ordinal
      if (token == INT && _digits > 0) {
        _date.setDayOfMonth(value1);
        _date.setMonth(value2 - 1);

        if (_value < 0)
          _date.setYear(_value);
        else if (_value <= 68)
          _date.setYear(2000 + _value);
        else if (_value < 100)
          _date.setYear(1900 + _value);
        else
          _date.setYear(_value);

        _value = NULL_VALUE;
      }
      else {
        _date.setDayOfMonth(value1);
        _date.setMonth(value2 - 1);

        _peekToken = token;
      }
    }

    private void parseMonthDate(int value1)
    {
      if (value1 < 0)
        value1 = - value1;

      _value = NULL_VALUE;
      int token = nextToken();

      if (token == '-') {
        _value = NULL_VALUE;
        token = nextToken();
      }

      if (token == INT) {
        int value2 = _value;

        _value = NULL_VALUE;
        token = nextToken();
        if (token == '-') {
          _value = NULL_VALUE;
          token = nextToken();
        }

        if (token == INT) {
          _date.setMonth(value1 - 1);
          _date.setDayOfMonth(value2);

          if (_value < 0)
            _date.setYear(_value);
          else if (_value <= 68)
            _date.setYear(2000 + _value);
          else if (_value < 100)
            _date.setYear(1900 + _value);
          else
            _date.setYear(_value);

          _value = NULL_VALUE;
        }
        else {
          _date.setMonth(value1 - 1);
          _date.setDayOfMonth(value2);

          _peekToken = token;
        }
      }
      else {
        _date.setMonth(value1 - 1);

        _peekToken = token;
      }
    }

    private void parseTime()
    {
      int hour = _value;
      _value = NULL_VALUE;

      if (hour < 0)
        hour = - hour;

      _date.setHour(hour);
      _date.setMinute(0);
      _date.setSecond(0);
      _date.setMillisecond(0);

      int token = nextToken();

      if (token == INT) {
        _date.setMinute(_value);
        _value = NULL_VALUE;
      }
      else {
        _peekToken = token;
        return;
      }

      token = nextToken();

      if (token == ':') {
        token = nextToken();

        if (token == INT) {
          _date.setSecond(_value);
          _value = NULL_VALUE;
        }
        else {
          _peekToken = token;
          return;
        }

        token = nextToken();

        if (token == '.') { // milliseconds
          token = nextToken();

          _value = NULL_VALUE;
          if (token != INT) {
            _peekToken = token;
            return;
          }
        }
      }

      if (token == AM) {
        hour = _date.getHour();

        if (hour == 12)
          _date.setHour(0);
      }
      else if (token == PM) {
        hour = _date.getHour();

        if (hour == 12)
          _date.setHour(12);
        else
          _date.setHour(hour + 12);
      }
      else
        _peekToken = token;

      parseTimezone();
    }

    private void parseTimezone()
    {
      int token = nextToken();
      int sign = 1;
      boolean hasUTC = false;

      if (token == UTC) {
        token = nextToken();

        hasUTC = true;
      }

      if (token == '-')
        sign = -1;
      else if (token == '+')
        sign = 1;
      else {
        _peekToken = token;

        if (hasUTC)
          _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());

        return;
      }

      int offset = 0;

      token = nextToken();
      if (token != INT) {
        _peekToken = token;

        if (hasUTC)
          _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());

        return;
      }
      else if (_digits == 4) {
        int hours = _value / 100;
        int minutes = _value % 100;

        int value = sign * (hours * 60 + minutes);
        _value = NULL_VALUE;

        // php/191g - php ignores any explicit
        // offset if the date specifies UTC/z/GMT
        if (hasUTC) 
          _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());
        else 
          _date.setGMTTime(
              _date.getGMTTime() - value * 60000L + _date.getZoneOffset());
        return;
      }
      else if (_digits == 2) {
        int value = _value;

        token = nextToken();

        if (token != ':') {
          _value = sign * _value;
          _peekToken = token;

          if (hasUTC)
            _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());
          return;
        }

        value = sign * (100 * value + _value);

        _date.setGMTTime(
            _date.getGMTTime() - value * 60000L + _date.getZoneOffset());
        return;
      }
      else {
        _value = sign * _value;
        _peekToken = token;

        if (hasUTC)
          _date.setGMTTime(_date.getGMTTime() + _date.getZoneOffset());

        return;
      }
    }

    private void addTime(int value, int unit)
    {
      if (value == NULL_VALUE)
        value = 1;
      else if (value == -NULL_VALUE)
        value = -1;

      switch (unit) {
        case UNIT_YEAR:
          _date.setYear(_date.getYear() + value);
          break;
        case UNIT_MONTH:
          _date.setMonth(_date.getMonth() + value);
          break;
        case UNIT_FORTNIGHT:
          _date.setGMTTime(_date.getGMTTime() + 14 * DAY * value);
          break;
        case UNIT_WEEK:
          _date.setGMTTime(_date.getGMTTime() + 7 * DAY * value);
          break;
        case UNIT_DAY:
          _date.setGMTTime(_date.getGMTTime() + DAY * value);
          break;
        case UNIT_HOUR:
          _date.setGMTTime(_date.getGMTTime() + HOUR * value);
          break;
        case UNIT_MINUTE:
          _date.setGMTTime(_date.getGMTTime() + MINUTE * value);
          break;
        case UNIT_SECOND:
          _date.setGMTTime(_date.getGMTTime() + 1000L * value);
          break;
      }
    }

    private void addWeekday(int value, int weekday)
    {
      if (value == NULL_VALUE)
        value = 0;
      else if (value == -NULL_VALUE)
        value = -1;

      _date.setDayOfMonth(_date.getDayOfMonth()
          + (8 + weekday - _date.getDayOfWeek()) % 7
          + 7 * value);
    }

    private void parseBareInt(int value, int digits)
    {
      if (digits == 8 && ! _hasDate) {
        _hasDate = true;

        _date.setYear(value / 10000);
        _date.setMonth((value / 100 % 12) - 1);
        _date.setDayOfMonth(value % 100);
      }
      else if (digits == 6 && ! _hasTime) {
        _hasTime = true;
        _date.setHour(value / 10000);
        _date.setMinute(value / 100 % 100);
        _date.setSecond(value % 100);

        parseTimezone();
      }
      else if (digits == 4 && ! _hasTime) {
        _hasTime = true;
        _date.setHour(value / 100);
        _date.setMinute(value % 100);
        _date.setSecond(0);
        parseTimezone();
      }
      else if (digits == 2 && ! _hasTime) {
        _hasTime = true;
        _date.setHour(value);
        _date.setMinute(0);
        _date.setSecond(0);
        parseTimezone();
      }

      int token = nextToken();
      if (token == '.') {
        _value = NULL_VALUE;
        token = nextToken();

        if (token == INT)
          _value = NULL_VALUE;
        else
          _peekToken = token;
      }
      else
        _peekToken = token;
    }

    int nextToken()
    {
      if (_peekToken > 0) {
        int token = _peekToken;
        _peekToken = 0;
        return token;
      }

      while (true) {
        skipSpaces();

        int ch = read();

        if (ch < 0)
          return -1;
        else if (ch == '-')
          return '-';
        else if (ch == '+')
          return '+';
        else if (ch == ':')
          return ':';
        else if (ch == '.')
          return '.';
        else if (ch == '/')
          return '/';
        else if (ch == '@')
          return '@';
        else if ('0' <= ch && ch <= '9') {
          int value = 0;
          int digits = 0;

          for (; '0' <= ch && ch <= '9'; ch = read()) {
            digits++;
            value = 10 * value + ch - '0';
          }

          _value = value;
          _digits = digits;

          unread();

          return INT;
        }
        else if ('a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z') {
          _sb.setLength(0);

          for (;
              'a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z' || ch == '.';
              ch = read()) {
            _sb.append(Character.toLowerCase((char) ch));
          }

          unread();

          String s = _sb.toString();

          return parseString(s);
        }
        else {
          // skip
        }
      }
    }

    private int parseString(String s)
    {
      if (s.endsWith("."))
        s = s.substring(0, s.length() - 1);

      if ("now".equals(s)
          || "today".equals(s)) {
        _value = 0;
        _unit = UNIT_NOW;
        return PERIOD;
      }
      else if ("last".equals(s)) {
        _value = -1;
        _digits = 0;
        return INT;
      }
      else if ("this".equals(s)) {
        _value = 0;
        _digits = 0;
        return INT;
      }
      else if ("am".equals(s) || "a.m".equals(s)) {
        return AM;
      }
      else if ("pm".equals(s) || "p.m".equals(s)) {
        return PM;
      }
      else if ("next".equals(s)) {
        _value = 1;
        _digits = 0;
        return INT;
      }
      else if ("third".equals(s)) {
        _value = 3;
        _digits = 0;
        return INT;
      }
      else if ("fourth".equals(s)) {
        _value = 4;
        _digits = 0;
        return INT;
      }
      else if ("fifth".equals(s)) {
        _value = 5;
        _digits = 0;
        return INT;
      }
      else if ("sixth".equals(s)) {
        _value = 6;
        _digits = 0;
        return INT;
      }
      else if ("seventh".equals(s)) {
        _value = 7;
        _digits = 0;
        return INT;
      }
      else if ("eighth".equals(s)) {
        _value = 8;
        _digits = 0;
        return INT;
      }
      else if ("ninth".equals(s)) {
        _value = 9;
        _digits = 0;
        return INT;
      }
      else if ("tenth".equals(s)) {
        _value = 10;
        _digits = 0;
        return INT;
      }
      else if ("eleventh".equals(s)) {
        _value = 11;
        _digits = 0;
        return INT;
      }
      else if ("twelfth".equals(s)) {
        _value = 12;
        _digits = 0;
        return INT;
      }
      else if ("yesterday".equals(s)) {
        _value = -1;
        _unit = UNIT_DAY;
        return PERIOD;
      }
      else if ("tomorrow".equals(s)) {
        _value = 1;
        _unit = UNIT_DAY;
        return PERIOD;
      }
      else if ("ago".equals(s)) {
        return AGO;
      }
      else if ("year".equals(s) || "years".equals(s)) {
        _unit = UNIT_YEAR;
        return PERIOD;
      }
      else if ("month".equals(s) || "months".equals(s)) {
        _unit = UNIT_MONTH;
        return PERIOD;
      }
      else if ("fortnight".equals(s) || "fortnights".equals(s)) {
        _unit = UNIT_FORTNIGHT;
        return PERIOD;
      }
      else if ("week".equals(s) || "weeks".equals(s)) {
        _unit = UNIT_WEEK;
        return PERIOD;
      }
      else if ("day".equals(s) || "days".equals(s)) {
        _unit = UNIT_DAY;
        return PERIOD;
      }
      else if ("hour".equals(s) || "hours".equals(s)) {
        _unit = UNIT_HOUR;
        return PERIOD;
      }
      else if ("minute".equals(s) || "minutes".equals(s)) {
        _unit = UNIT_MINUTE;
        return PERIOD;
      }
      else if ("second".equals(s) || "seconds".equals(s)) {
        // php/191s - if preceded by a value token, treat as a period, 
        // otherwise treat as an ordinal
        if (_value == NULL_VALUE) {
          _digits = 0;
          _value = 2;

          return INT;
        }

        _unit = UNIT_SECOND;
        return PERIOD;
      }
      else if ("january".equals(s) || "jan".equals(s)) {
        _value = 1;
        return MONTH;
      }
      else if ("february".equals(s) || "feb".equals(s)) {
        _value = 2;
        return MONTH;
      }
      else if ("march".equals(s) || "mar".equals(s)) {
        _value = 3;
        return MONTH;
      }
      else if ("april".equals(s) || "apr".equals(s)) {
        _value = 4;
        return MONTH;
      }
      else if ("may".equals(s)) {
        _value = 5;
        return MONTH;
      }
      else if ("june".equals(s) || "jun".equals(s)) {
        _value = 6;
        return MONTH;
      }
      else if ("july".equals(s) || "jul".equals(s)) {
        _value = 7;
        return MONTH;
      }
      else if ("august".equals(s) || "aug".equals(s)) {
        _value = 8;
        return MONTH;
      }
      else if ("september".equals(s) || "sep".equals(s) || "sept".equals(s)) {
        _value = 9;
        return MONTH;
      }
      else if ("october".equals(s) || "oct".equals(s)) {
        _value = 10;
        return MONTH;
      }
      else if ("november".equals(s) || "nov".equals(s)) {
        _value = 11;
        return MONTH;
      }
      else if ("december".equals(s) || "dec".equals(s)) {
        _value = 12;
        return MONTH;
      }
      else if ("sunday".equals(s) || "sun".equals(s)) {
        _weekday = 0;
        return WEEKDAY;
      }
      else if ("monday".equals(s) || "mon".equals(s)) {
        _weekday = 1;
        return WEEKDAY;
      }
      else if ("tuesday".equals(s) || "tue".equals(s) || "tues".equals(s)) {
        _weekday = 2;
        return WEEKDAY;
      }
      else if ("wednesday".equals(s) || "wed".equals(s) || "wednes".equals(s)) {
        _weekday = 3;
        return WEEKDAY;
      }
      else if ("thursday".equals(s) || "thu".equals(s)
          || "thur".equals(s) || "thurs".equals(s)) {
        _weekday = 4;
        return WEEKDAY;
      }
      else if ("friday".equals(s) || "fri".equals(s)) {
        _weekday = 5;
        return WEEKDAY;
      }
      else if ("saturday".equals(s) || "sat".equals(s)) {
        _weekday = 6;
        return WEEKDAY;
      }
      else if ("z".equals(s) || "gmt".equals(s) || "utc".equals(s)) {
        return UTC;
      }
      else
        return 0;
    }

    private void skipSpaces()
    {
      while (true) {
        int ch = read();

        if (Character.isWhitespace((char) ch)) {
          continue;
        }
        else if (ch == '(') {
          for (ch = read(); ch > 0 && ch != ')'; ch = read()) {
          }
        }
        else {
          unread();
          return;
        }
      }
    }

    int read()
    {
      if (_index < _length)
        return _s.charAt(_index++);
      else {
        _index++;
        return -1;
      }
    }

    void unread()
    {
      _index--;
    }
  }
}

