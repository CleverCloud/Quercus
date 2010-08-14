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

package com.caucho.es;

import com.caucho.util.Alarm;
import com.caucho.util.QDate;

/**
 * JavaScript Date object
 */
class NativeDate extends Native {
  static final int NEW = 2;
  static final int UTC = 3;
  static final int VALUE_OF = 4;
  static final int TO_STRING = 6;
  static final int TO_UTC_STRING = 7;
  static final int TO_ISO_STRING = 8;
  static final int TO_UTC_ISO_STRING = 9;
  static final int TO_ISO_DATE = 10;
  static final int TO_UTC_ISO_DATE = 11;
  static final int TO_LOCALE_STRING = 12;
  static final int UTC_FORMAT = 13;
  static final int FORMAT = 14;
  static final int PARSE_DATE = 15;

  static final int GET_FULL_YEAR = 20;
  static final int GET_UTC_FULL_YEAR = 21;
  static final int GET_MONTH = 22;
  static final int GET_UTC_MONTH = 23;
  static final int GET_DATE = 24;
  static final int GET_UTC_DATE = 25;
  static final int GET_DAY = 26;
  static final int GET_UTC_DAY = 27;
  static final int GET_HOURS = 28;
  static final int GET_UTC_HOURS = 29;
  static final int GET_MINUTES = 30;
  static final int GET_UTC_MINUTES = 31;
  static final int GET_SECONDS = 32;
  static final int GET_UTC_SECONDS = 33;
  static final int GET_MILLISECONDS = 34;
  static final int GET_UTC_MILLISECONDS = 35;

  static final int GET_TIMEZONE_OFFSET = 36;

  static final int SET_FULL_YEAR = 50;
  static final int SET_UTC_FULL_YEAR = 51;
  static final int SET_MONTH = 52;
  static final int SET_UTC_MONTH = 53;
  static final int SET_DATE = 54;
  static final int SET_UTC_DATE = 55;
  static final int SET_HOURS = 56;
  static final int SET_UTC_HOURS = 57;
  static final int SET_MINUTES = 58;
  static final int SET_UTC_MINUTES = 59;
  static final int SET_SECONDS = 60;
  static final int SET_UTC_SECONDS = 61;
  static final int SET_MILLISECONDS = 62;
  static final int SET_UTC_MILLISECONDS = 63;

  static final int GET_MONTH_NAME = SET_UTC_MILLISECONDS + 1;
  static final int GET_UTC_MONTH_NAME = GET_MONTH_NAME + 1;

  private static String []monthNames = new String[] {
    "January", "February", "March", "April", "May", "June", 
      "July", "August", "September", "October", "November", "December"
      };

  QDate localCal = QDate.createLocal();
  QDate utcCal = new QDate();
  QDate cal = localCal;

  /**
   * Create a new object based on a prototype
   */
  private NativeDate(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
    // this.cal = cal;
  }

  /**
   * Creates the native Object object
   */
  static ESObject create(Global resin)
  {
    // QDate cal = QDate.createLocal();

    NativeDate nativeDate = new NativeDate("Date", NEW, 7);
    ESObject dateProto = new ESDate(Long.MAX_VALUE, resin.objProto);
    NativeWrapper date = new NativeWrapper(resin, nativeDate,
                                           dateProto, ESThunk.DATE_THUNK);
    nativeDate.newN = nativeDate.n;
    resin.dateProto = dateProto;

    put(dateProto, "toString", TO_STRING, 0);
    put(dateProto, "getTime", VALUE_OF, 0);
    put(dateProto, "valueOf", VALUE_OF, 0);
    put(dateProto, "toUTCString", TO_UTC_STRING, 0);
    put(dateProto, "toGMTString", TO_UTC_STRING, 0);
    put(dateProto, "toLocalISO8601", TO_ISO_STRING, 0);
    put(dateProto, "toISO8601", TO_UTC_ISO_STRING, 0);
    put(dateProto, "format", FORMAT, 0);
    put(dateProto, "UTCFormat", UTC_FORMAT, 0);

    put(dateProto, "toLocaleString", TO_LOCALE_STRING, 0);
    put(dateProto, "getUTCYear", GET_UTC_FULL_YEAR, 0);
    put(dateProto, "getUTCFullYear", GET_UTC_FULL_YEAR, 0);
    put(dateProto, "getUTCMonth", GET_UTC_MONTH, 0);
    put(dateProto, "getUTCDate", GET_UTC_DATE, 0);
    put(dateProto, "getUTCDay", GET_UTC_DAY, 0);
    put(dateProto, "getUTCHours", GET_UTC_HOURS, 0);
    put(dateProto, "getUTCMinutes", GET_UTC_MINUTES, 0);
    put(dateProto, "getUTCSeconds", GET_UTC_SECONDS, 0);
    put(dateProto, "getUTCMilliseconds", GET_UTC_MILLISECONDS, 0);

    put(dateProto, "setUTCYear", SET_UTC_FULL_YEAR, 1);
    put(dateProto, "setUTCFullYear", SET_UTC_FULL_YEAR, 1);
    put(dateProto, "setUTCMonth", SET_UTC_MONTH, 2);
    put(dateProto, "setUTCDate", SET_UTC_DATE, 3);
    put(dateProto, "setUTCHours", SET_UTC_HOURS, 4);
    put(dateProto, "setUTCMinutes", SET_UTC_MINUTES, 3);
    put(dateProto, "setUTCSeconds", SET_UTC_SECONDS, 2);
    put(dateProto, "setUTCMilliseconds", SET_UTC_MILLISECONDS, 1);

    put(dateProto, "getYear", GET_FULL_YEAR, 0);
    put(dateProto, "getFullYear", GET_FULL_YEAR, 0);
    put(dateProto, "getMonth", GET_MONTH, 0);
    put(dateProto, "getMonthName", GET_MONTH_NAME, 0);
    put(dateProto, "getDate", GET_DATE, 0);
    put(dateProto, "getDay", GET_DAY, 0);
    put(dateProto, "getHours", GET_HOURS, 0);
    put(dateProto, "getMinutes", GET_MINUTES, 0);
    put(dateProto, "getSeconds", GET_SECONDS, 0);
    put(dateProto, "getMilliseconds", GET_MILLISECONDS, 0);

    put(dateProto, "getTimezoneOffset", GET_TIMEZONE_OFFSET, 0);
    put(dateProto, "setYear", SET_FULL_YEAR, 3);
    put(dateProto, "setFullYear", SET_FULL_YEAR, 3);
    put(dateProto, "setMonth", SET_MONTH, 2);
    put(dateProto, "setDate", SET_DATE, 1);
    put(dateProto, "setHours", SET_HOURS, 4);
    put(dateProto, "setMinutes", SET_MINUTES, 3);
    put(dateProto, "setSeconds", SET_SECONDS, 2);
    put(dateProto, "setMilliseconds", SET_MILLISECONDS, 1);

    put(date, "UTC", UTC, 7);
    put(date, "parse", PARSE_DATE, 1);

    dateProto.setClean();
    date.setClean();

    return date;
  }

  private static void put(ESObject obj, String name, int n, int len)
  {
    obj.put(ESId.intern(name), new NativeDate(name, n, len), DONT_ENUM);
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    long time = 0;
    double value; 
    ESBase error;

    synchronized (cal) {

    int off = 0;
    switch (n) {
    case NEW:
      return ESDate.create(create(eval, length, n));

    case UTC:
      return ESNumber.create(create(eval, length, n));

    case TO_STRING:
    case TO_UTC_STRING:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESString.create(cal.printDate());

    case TO_ISO_STRING:
    case TO_UTC_ISO_STRING:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESString.create(cal.printISO8601());

    case TO_ISO_DATE:
    case TO_UTC_ISO_DATE:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESString.create(cal.printISO8601Date());

    case FORMAT:
    case UTC_FORMAT:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESString.create(cal.format(eval.getArgString(0, length)));

    case TO_LOCALE_STRING:
      if ((error = calculate(eval.getArg(-1), 1, TO_STRING)) != null)
        return error;

      return ESString.create(cal.printLocaleDate());

    case VALUE_OF:
      if (! (eval.getArg(-1) instanceof ESDate))
        throw new ESException("valueOf must be bound to date");
      
      value = (double) ((ESDate) eval.getArg(-1)).time;
      if (value > 8.64e15 || value < -8.64e15 || Double.isNaN(value))
        value = 0.0/0.0;

      return ESNumber.create(value);

    case PARSE_DATE:
      if (length < 0)
        return ESNumber.NaN;

      try {
        long lvalue = cal.parseDate(eval.getArg(0).toStr().toString());
        return ESNumber.create(millisToDouble(lvalue));
      } catch (Exception e) {
        throw new ESException(e.toString());
      }


    case GET_FULL_YEAR:
    case GET_UTC_FULL_YEAR:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESNumber.create((double) cal.get(cal.YEAR));

    case GET_MONTH:
    case GET_UTC_MONTH:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESNumber.create((double) cal.get(cal.MONTH));

    case GET_MONTH_NAME:
    case GET_UTC_MONTH_NAME:
    {
        if ((error = calculate(eval.getArg(-1), 1, n)) != null)
          return error;

        int month = (int) cal.get(cal.MONTH);

        return ESString.create(monthNames[month]);
    }

    case GET_DATE:
    case GET_UTC_DATE:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESNumber.create((double) cal.get(cal.DAY_OF_MONTH) + 1);

    case GET_DAY:
    case GET_UTC_DAY:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESNumber.create((double) cal.get(cal.DAY));

    case GET_HOURS:
    case GET_UTC_HOURS:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESNumber.create((double) (cal.get(cal.HOUR)));

    case GET_MINUTES:
    case GET_UTC_MINUTES:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESNumber.create((double) (cal.get(cal.MINUTE)));

    case GET_SECONDS:
    case GET_UTC_SECONDS:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESNumber.create((double) (cal.get(cal.SECOND)));

    case GET_MILLISECONDS:
    case GET_UTC_MILLISECONDS:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESNumber.create((double) cal.get(cal.MILLISECOND));

    case GET_TIMEZONE_OFFSET:
      if ((error = calculate(eval.getArg(-1), 1, n)) != null)
        return error;

      return ESNumber.create((double) (cal.getZoneOffset() / 60000));

    case SET_DATE:
    case SET_UTC_DATE:
      off--;

    case SET_MONTH:
    case SET_UTC_MONTH:
      off--;

    case SET_FULL_YEAR:
    case SET_UTC_FULL_YEAR:
      if ((error = calculate(eval.getArg(-1), length, n)) != null)
        return error;

      if (0 <= off)
        cal.set(cal.YEAR, (long) eval.getArg(off).toNum());

      if (0 <= off + 1 && off + 1 < length) {
        value = eval.getArg(off + 1).toNum();
        cal.set(cal.MONTH,
                (long) (Double.isNaN(value) ? Long.MAX_VALUE : value));
      }
      if (0 <= off + 2 && off + 2 < length) {
        value = eval.getArg(off + 2).toNum();
        cal.set(cal.DAY_OF_MONTH,
                (long) (Double.isNaN(value) ? Long.MAX_VALUE : value - 1));
      }

      return create(eval.getArg(-1), n);

    case SET_MILLISECONDS:
    case SET_UTC_MILLISECONDS:
      off--;

    case SET_SECONDS:
    case SET_UTC_SECONDS:
      off--;

    case SET_MINUTES:
    case SET_UTC_MINUTES:
      off--;

    case SET_HOURS:
    case SET_UTC_HOURS:
      if ((error = calculate(eval.getArg(-1), length, n)) != null)
        return error;

      if (0 <= off) {
        value = eval.getArg(off).toNum();
        cal.set(cal.HOUR,
                (long) (Double.isNaN(value) ? Long.MAX_VALUE : value));
      }

      if (0 <= off + 1 && off + 1 < length) {
        value = eval.getArg(off + 1).toNum();
        cal.set(cal.MINUTE,
                (long) (Double.isNaN(value) ? Long.MAX_VALUE : value));
      }
      if (0 <= off + 2 && off + 2 < length) {
        value = eval.getArg(off + 2).toNum();
        cal.set(cal.SECOND,
                (long) (Double.isNaN(value) ? Long.MAX_VALUE : value));
      }
      if (0 <= off + 3 && off + 3 < length) {
        value = eval.getArg(off + 3).toNum();
        cal.set(cal.MILLISECOND,
                (long) (Double.isNaN(value) ? Long.MAX_VALUE : value));
      }

      return create(eval.getArg(-1), n);

    default:
      throw new ESException("Unknown object function");
    }
    }
  }

  public ESBase construct(Call eval, int length) throws Throwable
  {
    if (cal == null)
      cal = new QDate();

    if (n != NEW)
      return super.construct(eval, length);
    
    synchronized (cal) {
      return ESDate.create(create(eval, length, NEW));
    }
  }

  private long create(Call eval, int length, int code)
    throws Throwable
  {
    boolean isLocal = (code & 1) == 0;
    long value = 0;

    if (length == 0) {
      return Alarm.getCurrentTime();
    }
    else if (length == 1)
      value = (long) (eval.getArg(0).toNum());
    else if (length >= 3) {
      long year = (long) eval.getArg(0).toNum();
      long month = (long) eval.getArg(1).toNum();
      long day = (long) eval.getArg(2).toNum() - 1;
      
      long hour = 0;
      if (length >= 4)
        hour = (long) eval.getArg(3).toNum();

      long minute = 0;
      if (length >= 5)
        minute = (long) eval.getArg(4).toNum();

      long second = 0;
      if (length >= 6)
        second = (long) eval.getArg(5).toNum();

      long ms = 0;
      if (length >= 7)
        ms = (long) eval.getArg(6).toNum();

      cal.setDate(year, month, day);
      cal.setTime(hour, minute, second, ms);

      value = cal.get(cal.TIME);

      if (isLocal)
        value -= cal.getZoneOffset();
    }
    else
      value = Long.MIN_VALUE;

    return value;
  }

  private double millisToDouble(long millis)
  {
    double dvalue = millis;
    if (dvalue > 8.64e15 || dvalue < -8.64e15 || Double.isNaN(dvalue))
      dvalue = 0.0/0.0;

    return dvalue;
  }

  private ESBase create(ESBase obj, int code) throws ESException
  {
    boolean isLocal = (code & 1) == 0;

    long value = cal.get(cal.TIME);

    if (isLocal)
      value -= cal.getZoneOffset();

    double dvalue = value;
    if (dvalue > 8.64e15 || dvalue < -8.64e15 || Double.isNaN(dvalue))
      dvalue = 0.0/0.0;

    if (! (obj instanceof ESDate))
      return ESNumber.create(dvalue);

    ESNumber newValue  = ESNumber.create(dvalue);
    ((ESDate) obj).time = (long) dvalue;

    return newValue;
  }

  private ESBase calculate(ESBase arg, int length, int code) 
    throws Throwable
  {
    boolean isLocal = (code & 1) == 0;

    double value = arg.toNum();
    if (Double.isNaN(value) || value > 8.64e15 || value < -8.64e15 || 
        length < 1)
      return ESNumber.NaN;

    long time = (long) value;

    if (isLocal)
      cal = localCal;
    else
      cal = utcCal;

    cal.setGMTTime(time);

    return null;
  }
}
