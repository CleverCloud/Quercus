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
 * @author Reza Rahman
 */
package com.caucho.config.timer;

import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Trigger;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

/**
 * Implements a cron-style trigger. This trigger is primarily intended for the
 * EJB calendar style timer service functionality.
 */
// TODO Is this class getting too large? Maybe separate into a
// parser/lexer/interpreter sub-component? Also, is parsing better done/more
// readable/maintainable via creating compiler style grammar tokens instead of
// direct String processing?
public class CronTrigger implements Trigger {
  private static final L10N L = new L10N(CronTrigger.class);
  // Order of search is important in the token maps.
  private static final String [][] MONTH_TOKEN_MAP = { { "january", "1" },
      { "february", "2" }, { "march", "3" }, { "april", "4" }, { "may", "5" },
      { "june", "6" }, { "july", "7" }, { "august", "8" },
      { "september", "9" }, { "october", "10" }, { "november", "11" },
      { "december", "12" }, { "jan", "1" }, { "feb", "2" }, { "mar", "3" },
      { "apr", "4" }, { "jun", "6" }, { "jul", "7" }, { "aug", "8" },
      { "sep", "9" }, { "oct", "10" }, { "nov", "11" }, { "dec", "12" } };
  private static final String [][] DAY_OF_WEEK_TOKEN_MAP = { { "sunday", "0" },
      { "monday", "1" }, { "tuesday", "2" }, { "wednesday", "3" },
      { "thursday", "4" }, { "friday", "5" }, { "saturday", "6" },
      { "sun", "0" }, { "mon", "1" }, { "tue", "2" }, { "wed", "3" },
      { "thu", "4" }, { "fri", "5" }, { "sat", "6" } };
  private static final String [][] RELATIVE_DAY_OF_WEEK_TOKEN_MAP = {
      { "last", "-0" }, { "1st", "1" }, { "2nd", "2" }, { "3rd", "3" },
      { "4th", "4" }, { "5th", "5" } };

  private AtomicReference<QDate> _localCalendar = new AtomicReference<QDate>();

  private boolean [] _seconds;
  private boolean [] _minutes;
  private boolean [] _hours;
  private boolean _isDaysFilterRelative;
  private boolean [] _days;
  private String _daysFilter;
  private boolean [] _months;
  private boolean [] _daysOfWeek;
  private YearsFilter _yearsFilter;

  private TimeZone _timezone = TimeZone.getTimeZone("GMT");

  private long _start = -1;
  private long _end = -1;

  /**
   * Creates new cron trigger.
   * 
   * @param cronExpression
   *          The cron expression to create the trigger from.
   * @param start
   *          The date the trigger should begin firing, in milliseconds. -1
   *          indicates that no start date should be enforced.
   * @param end
   *          The date the trigger should end firing, in milliseconds. -1
   *          indicates that no end date should be enforced.
   * @param string
   */
  public CronTrigger(final CronExpression cronExpression, final long start,
      final long end, TimeZone timezone)
  {
    if (cronExpression.getSecond() != null) {
      _seconds = parseRange(cronExpression.getSecond(), 0, 59, false);
    }

    if (cronExpression.getMinute() != null) {
      _minutes = parseRange(cronExpression.getMinute(), 0, 59, false);
    }

    if (cronExpression.getHour() != null) {
      _hours = parseRange(cronExpression.getHour(), 0, 23, false);
    }

    if (cronExpression.getDayOfWeek() != null) {
      _daysOfWeek = parseRange(
          tokenizeDayOfWeek(cronExpression.getDayOfWeek()), 0, 7, false);
    }

    if (_daysOfWeek[7]) { // Both 0 and 7 are Sunday, as in UNIX cron.
      _daysOfWeek[0] = _daysOfWeek[7];
    }

    if (cronExpression.getDayOfMonth() != null) {
      _daysFilter = tokenizeDayOfMonth(cronExpression.getDayOfMonth());
      _days = parseRange(_daysFilter, 1, 31, true);
    }

    if (cronExpression.getMonth() != null) {
      _months = parseRange(tokenizeMonth(cronExpression.getMonth()), 1, 12,
          false);
    }

    if (cronExpression.getYear() != null) {
      _yearsFilter = parseYear(cronExpression.getYear());
    }

    if (timezone != null) {
      _timezone = timezone;
    }

    _start = start;
    _end = end;
  }

  private String tokenizeDayOfWeek(String dayOfWeek)
  {
    dayOfWeek = tokenize(dayOfWeek, DAY_OF_WEEK_TOKEN_MAP);

    return dayOfWeek;
  }

  private String tokenizeDayOfMonth(String dayOfMonth)
  {
    dayOfMonth = tokenize(dayOfMonth, RELATIVE_DAY_OF_WEEK_TOKEN_MAP);
    dayOfMonth = tokenize(dayOfMonth, DAY_OF_WEEK_TOKEN_MAP);

    return dayOfMonth;
  }

  private String tokenizeMonth(String month)
  {
    month = tokenize(month, MONTH_TOKEN_MAP);

    return month;
  }

  private String tokenize(String value, String [][] tokenMap)
  {
    // TODO The String processing is more resource intensive than necessary. See
    // if StringBuilder can work with regex?

    for (int i = 0; i < tokenMap.length; i++) {
      value = value.replaceAll("(?i)" + tokenMap[i][0], tokenMap[i][1]);
    }

    return value;
  }

  /**
   * parses a range, following cron rules.
   */
  // TODO This does not handle extra spaces between tokens, should it?
  private boolean [] parseRange(String range, int rangeMin, int rangeMax,
      boolean parseDayOfMonth) throws ConfigException
  {
    boolean [] values = new boolean[rangeMax + 1];

    int i = 0;
    while (i < range.length()) {
      char character = range.charAt(i);

      int min = 0;
      int max = 0;
      int increment = 1;

      if (character == '*') {
        min = rangeMin;
        max = rangeMax;
        i++;
      } else if ('0' <= character && character <= '9') {
        for (; i < range.length() && '0' <= (character = range.charAt(i))
            && character <= '9'; i++) {
          min = 10 * min + character - '0';
        }

        if (i < range.length() && character == '-') {
          for (i++; i < range.length() && '0' <= (character = range.charAt(i))
              && character <= '9'; i++) {
            max = 10 * max + character - '0';
          }
        } else if (parseDayOfMonth && (i < range.length())
            && (character == ' ')) {
          // This is just for further parsing validation, the filter value
          // cannot be processed right now.
          _isDaysFilterRelative = true; // This is the Nth weekday case.

          int dayOfWeek = 0;

          for (i++; i < range.length() && '0' <= (character = range.charAt(i))
              && character <= '9'; i++) {
            dayOfWeek = 10 * dayOfWeek + character - '0';
          }

          if ((dayOfWeek < 0) || (dayOfWeek > 6)) {
            throw new ConfigException(L.l(
                "'{0}' is an illegal cron range (day of week out of range)",
                range));
          }

          if ((min < 1) || (min > 5)) {
            throw new ConfigException(L.l(
                "'{0}' is an illegal cron range (invalid day of week)", range));
          }

          if (i < range.length()) {
            if ((i < (range.length() - 1)) && (character == ',')) {
              i++;
            } else {
              throw new ConfigException(L.l(
                  "'{0}' is an illegal cron range (invalid syntax)", range));
            }
          }

          continue;
        } else {
          max = min;
        }
      } else {
        if (parseDayOfMonth && (character == '-')) { // This is a -N days of
          // month case.
          _isDaysFilterRelative = true;

          // This is just for further parsing validation, the filter value
          // cannot be processed right now.

          int dayOfMonth = 0;

          for (i++; i < range.length() && '0' <= (character = range.charAt(i))
              && character <= '9'; i++) {
            // Don't need to do anything, evaluation will be done later, just
            // need to validate parsing for now.
            dayOfMonth = 10 * dayOfMonth + character - '0';
          }

          if ((dayOfMonth < 0) || (dayOfMonth > 30)) {
            throw new ConfigException(L.l(
                "'{0}' is an illegal cron range (day of month out of range)",
                range));
          }

          if ((i < range.length()) && ((character = range.charAt(i)) == '/')) {
            increment = 0;

            for (i++; i < range.length()
                && '0' <= (character = range.charAt(i)) && character <= '9'; i++) {
              increment = 10 * increment + character - '0';
            }

            if ((increment < rangeMin) && (increment > rangeMax)) {
              throw new ConfigException(
                  L
                      .l(
                          "'{0}' is an illegal cron range (increment value out of range)",
                          range));
            }
          }

          // The case of the last (-0) weekday
          if ((i < range.length()) && ((character = range.charAt(i)) == ' ')) {
            // Just need to do validation parsing, evaluation will be done
            // later.
            int dayOfWeek = 0;

            for (i++; i < range.length()
                && '0' <= (character = range.charAt(i)) && character <= '9'; i++) {
              dayOfWeek = 10 * dayOfWeek + character - '0';
            }

            if ((dayOfWeek < 0) || (dayOfWeek > 6)) {
              throw new ConfigException(L.l(
                  "'{0}' is an illegal cron range (day of week out of range)",
                  range));
            }

            if (min != 0) {
              throw new ConfigException(L.l(
                  "'{0}' is an illegal cron range (invalid syntax)", range));
            }
          }

          if (i < range.length()) {
            if ((i < (range.length() - 1)) && (character == ',')) {
              i++;
            } else {
              throw new ConfigException(L.l(
                  "'{0}' is an illegal cron range (invalid syntax)", range));
            }
          }

          continue;
        } else {
          throw new ConfigException(L.l(
              "'{0}' is an illegal cron range (invalid syntax)", range));
        }
      }

      if (min < rangeMin) {
        throw new ConfigException(L.l(
            "'{0}' is an illegal cron range (min value is too small)", range));
      } else if (max > rangeMax) {
        throw new ConfigException(L.l(
            "'{0}' is an illegal cron range (max value is too large)", range));
      }

      if ((i < range.length()) && ((character = range.charAt(i)) == '/')) {
        increment = 0;

        for (i++; i < range.length() && '0' <= (character = range.charAt(i))
            && character <= '9'; i++) {
          increment = 10 * increment + character - '0';
        }

        if (min == max) { // This is in the form of N/M, where N is the interval
          // start.
          max = rangeMax;
        }

        if ((increment < rangeMin) && (increment > rangeMax)) {
          throw new ConfigException(L.l(
              "'{0}' is an illegal cron range (increment value out of range)",
              range));
        }
      }

      if (i < range.length()) {
        if ((i < (range.length() - 1)) && (character == ',')) {
          i++;
        } else {
          throw new ConfigException(L.l(
              "'{0}' is an illegal cron range (invalid syntax)", range));
        }
      }

      for (; min <= max; min += increment) {
        values[min] = true;
      }
    }

    return values;
  }

  private YearsFilter parseYear(String year)
  {
    YearsFilter yearsFilter = new YearsFilter();

    int i = 0;
    while (i < year.length()) {
      YearsFilterValue filterValue = new YearsFilterValue();

      char character = year.charAt(i);

      if (character == '*') {
        filterValue.setAnyYear(true);
        i++;
      } else if ('0' <= character && character <= '9') {
        int startYear = 0;

        for (; i < year.length() && '0' <= (character = year.charAt(i))
            && character <= '9'; i++) {
          startYear = 10 * startYear + character - '0';
        }

        filterValue.setStartYear(startYear);

        if (i < year.length() && character == '-') {
          int endYear = 0;

          for (i++; i < year.length() && '0' <= (character = year.charAt(i))
              && character <= '9'; i++) {
            endYear = 10 * endYear + character - '0';
          }

          filterValue.setEndYear(endYear);
        } else {
          filterValue.setEndYear(startYear);
        }
      } else {
        throw new ConfigException(L.l("'{0}' is an illegal cron range", year));
      }

      if ((i < year.length()) && ((character = year.charAt(i)) == '/')) {
        filterValue.setAnyYear(false);
        int increment = 0;

        for (i++; i < year.length() && '0' <= (character = year.charAt(i))
            && character <= '9'; i++) {
          increment = 10 * increment + character - '0';
        }

        if (increment == 0) {
          throw new ConfigException(L.l("'{0}' is an illegal cron range", year));
        } else {
          filterValue.setIncrement(increment);
        }

        if (filterValue.getStartYear() == filterValue.getEndYear()) {
          // This is in the form of N/M, where N is the interval start.
          filterValue.setEndYear(Integer.MAX_VALUE);
        }
      }

      yearsFilter.addFilterValue(filterValue);

      if (i < year.length()) {
        if ((i < (year.length() - 1)) && (character == ',')) {
          i++;
        } else {
          throw new ConfigException(L.l("'{0}' is an illegal cron range", year));
        }
      }
    }

    return yearsFilter;
  }

  /**
   * Gets the next time this trigger should be fired.
   * 
   * @param now
   *          The current time.
   * @return The next time this trigger should be fired.
   */
  @Override
  public long nextTime(long now)
  {
    // Jump to start time.
    if ((_start != -1) && (now < _start)) {
      now = _start;
    }

    QDate calendar = allocateCalendar();

    // Round up to seconds.
    long time = now + 1000 - now % 1000;

    calendar.setGMTTime(time);

    QDate nextTime = getNextTime(calendar);

    if (nextTime != null) {
      time = nextTime.getGMTTime();
      time -= _timezone.getRawOffset(); // Adjust for time zone specification.
    } else {
      time = Long.MAX_VALUE; // This trigger is inactive.
    }

    // Check for end date
    if ((_end != -1) && (time > _end)) {
      time = Long.MAX_VALUE; // This trigger is inactive.
    }

    freeCalendar(calendar);

    if (now < time)
      return time;
    else
      return nextTime(now + 3600000L); // Daylight savings time.
  }

  private QDate getNextTime(QDate currentTime)
  {
    int year = _yearsFilter.getNextMatch(currentTime.getYear());

    if (year == -1) {
      return null;
    } else {
      if (year > currentTime.getYear()) {
        currentTime.setSecond(0);
        currentTime.setMinute(0);
        currentTime.setHour(0);
        currentTime.setDayOfMonth(1);
        currentTime.setMonth(0); // The QDate implementation uses 0 indexed
        // months, but cron does not.
        currentTime.setYear(year);
      }

      QDate nextTime = getNextTimeInYear(currentTime);

      int count = 0;

      // Don't look more than approximately five years ahead.
      while ((count < 5) && (nextTime == null)) {
        count++;
        year++;
        year = _yearsFilter.getNextMatch(year);

        if (year == -1) {
          return null;
        } else {
          currentTime.setSecond(0);
          currentTime.setMinute(0);
          currentTime.setHour(0);
          currentTime.setDayOfMonth(1);
          currentTime.setMonth(0); // The QDate implementation uses 0 indexed
          // months, but cron does not.
          currentTime.setYear(year);

          nextTime = getNextTimeInYear(currentTime);
        }
      }

      return nextTime;
    }
  }

  private QDate getNextTimeInYear(QDate currentTime)
  {
    int month = getNextMatch(_months, (currentTime.getMonth() + 1));

    if (month == -1) {
      return null;
    } else {
      if (month > (currentTime.getMonth() + 1)) {
        currentTime.setSecond(0);
        currentTime.setMinute(0);
        currentTime.setHour(0);
        currentTime.setDayOfMonth(1);
        currentTime.setMonth(month - 1);
      }

      QDate nextTime = getNextTimeInMonth(currentTime);

      while ((month < _months.length) && (nextTime == null)) {
        month++;
        month = getNextMatch(_months, month);

        if (month == -1) {
          return null;
        } else {
          currentTime.setSecond(0);
          currentTime.setMinute(0);
          currentTime.setHour(0);
          currentTime.setDayOfMonth(1);
          currentTime.setMonth(month - 1);

          nextTime = getNextTimeInMonth(currentTime);
        }
      }

      return nextTime;
    }
  }

  private QDate getNextTimeInMonth(QDate currentTime)
  {
    // If the days filter is relative to particular months, the days map should
    // be re-calculated.
    if (_isDaysFilterRelative) {
      calculateDays(currentTime);
    }

    // Note, QDate uses a 1 indexed weekday, while cron uses a 0 indexed
    // weekday.
    int day = getNextDayMatch(currentTime.getDayOfMonth(), (currentTime
        .getDayOfWeek() - 1), currentTime.getDayOfMonth(), currentTime
        .getDaysInMonth());

    if (day == -1) {
      return null;
    } else {
      if (day > currentTime.getDayOfMonth()) {
        currentTime.setSecond(0);
        currentTime.setMinute(0);
        currentTime.setHour(0);
        currentTime.setDayOfMonth(day);
      }

      QDate nextTime = getNextTimeInDay(currentTime);

      if (nextTime == null) {
        day++;
        // Note, QDate uses a 1 indexed weekday, while cron uses a 0 indexed
        // weekday.
        day = getNextDayMatch(currentTime.getDayOfMonth(), (currentTime
            .getDayOfWeek() - 1), day, currentTime.getDaysInMonth());

        if (day == -1) {
          return null;
        } else {
          currentTime.setSecond(0);
          currentTime.setMinute(0);
          currentTime.setHour(0);
          currentTime.setDayOfMonth(day);

          return getNextTimeInDay(currentTime);
        }
      }

      return nextTime;
    }
  }

  private void calculateDays(QDate currentTime)
  {
    _days = new boolean[currentTime.getDaysInMonth() + 1];

    int i = 0;
    while (i < _daysFilter.length()) {
      char character = _daysFilter.charAt(i);

      int min = 0;
      int max = min;
      int increment = 1;

      if (character == '*') {
        min = 1;
        max = currentTime.getDaysInMonth();
        i++;
      } else if ('0' <= character && character <= '9') {
        for (; i < _daysFilter.length()
            && '0' <= (character = _daysFilter.charAt(i)) && character <= '9'; i++) {
          min = 10 * min + character - '0';
        }

        if (i < _daysFilter.length() && character == '-') {
          for (i++; i < _daysFilter.length()
              && '0' <= (character = _daysFilter.charAt(i)) && character <= '9'; i++) {
            max = 10 * max + character - '0';
          }
        } else if ((i < _daysFilter.length()) && (character == ' ')) {
          // This is the Nth weekday case.
          int dayOfWeek = 0;

          for (i++; i < _daysFilter.length()
              && '0' <= (character = _daysFilter.charAt(i)) && character <= '9'; i++) {
            dayOfWeek = 10 * dayOfWeek + character - '0';
          }

          int n = min;
          min = 1;
          int monthStartDayofWeek = ((currentTime.getDayOfWeek() - 1)
              - ((currentTime.getDayOfMonth() - min) % 7) + 7) % 7;

          min = min + ((dayOfWeek - monthStartDayofWeek + 7) % 7);

          min = min + ((n - 1) * 7);

          max = min;
        } else {
          max = min;
        }
      } else if (character == '-') { // This is a -N days from end of month
        // case.
        for (i++; i < _daysFilter.length()
            && '0' <= (character = _daysFilter.charAt(i)) && character <= '9'; i++) {
          min = 10 * min + character - '0';
        }

        min = currentTime.getDaysInMonth() - min;

        // The case of the last (-0) weekday case.
        if ((i < _daysFilter.length())
            && ((character = _daysFilter.charAt(i)) == ' ')) {
          int dayOfWeek = 0;

          for (i++; i < _daysFilter.length()
              && '0' <= (character = _daysFilter.charAt(i)) && character <= '9'; i++) {
            dayOfWeek = 10 * dayOfWeek + character - '0';
          }

          min = 1;
          int monthStartDayofWeek = ((currentTime.getDayOfWeek() - 1)
              - ((currentTime.getDayOfMonth() - min) % 7) + 7) % 7;

          min = min + ((dayOfWeek - monthStartDayofWeek + 7) % 7);

          // This is an integer division.
          min = min + (((currentTime.getDaysInMonth() - min) / 7) * 7);
        }

        max = min;
      }

      if ((i < _daysFilter.length())
          && ((character = _daysFilter.charAt(i)) == '/')) {
        for (i++; i < _daysFilter.length()
            && '0' <= (character = _daysFilter.charAt(i)) && character <= '9'; i++) {
          increment = 10 * increment + character - '0';
        }

        if (min == max) { // This is in the form of N/M, where N is the interval
          // start and the end is the max value.
          max = currentTime.getDaysInMonth();
        }
      }

      for (int day = min; ((day > 0) && (day <= max) && (day <= currentTime
          .getDaysInMonth())); day += increment) {
        _days[day] = true;
      }

      if (character == ',') {
        i++;
      }
    }
  }

  private int getNextDayMatch(int initialDayOfMonth, int initialDayOfWeek,
      int day, int daysInMonth)
  {
    while (day <= daysInMonth) {
      day = getNextMatch(_days, day, (daysInMonth + 1));

      if (day == -1) {
        return -1;
      }

      int dayOfWeek = ((initialDayOfWeek + ((day - initialDayOfMonth) % 7)) % 7);
      int nextDayOfWeek = getNextMatch(_daysOfWeek, dayOfWeek);

      if (nextDayOfWeek == dayOfWeek) {
        return day;
      } else if (nextDayOfWeek == -1) {
        day += (7 - dayOfWeek);
      } else {
        day += (nextDayOfWeek - dayOfWeek);
      }
    }

    return -1;
  }

  private QDate getNextTimeInDay(QDate currentTime)
  {
    int hour = getNextMatch(_hours, currentTime.getHour());

    if (hour == -1) {
      return null;
    } else {
      if (hour > currentTime.getHour()) {
        currentTime.setSecond(0);
        currentTime.setMinute(0);
        currentTime.setHour(hour);
      }

      QDate nextTime = getNextTimeInHour(currentTime);

      if (nextTime == null) {
        hour++;
        hour = getNextMatch(_hours, hour);

        if (hour == -1) {
          return null;
        } else {
          currentTime.setSecond(0);
          currentTime.setMinute(0);
          currentTime.setHour(hour);

          return getNextTimeInHour(currentTime);
        }
      }

      return nextTime;
    }
  }

  private QDate getNextTimeInHour(QDate currentTime)
  {
    int minute = getNextMatch(_minutes, currentTime.getMinute());

    if (minute == -1) {
      return null;
    } else {
      if (minute > currentTime.getMinute()) {
        currentTime.setSecond(0);
        currentTime.setMinute(minute);
      }

      QDate nextTime = getNextTimeInMinute(currentTime);

      if (nextTime == null) {
        minute++;
        minute = getNextMatch(_minutes, minute);

        if (minute == -1) {
          return null;
        } else {
          currentTime.setSecond(0);
          currentTime.setMinute(minute);

          return getNextTimeInMinute(currentTime);
        }
      }

      return nextTime;
    }
  }

  private QDate getNextTimeInMinute(QDate currentTime)
  {
    int second = getNextMatch(_seconds, currentTime.getSecond());

    if (second == -1) {
      return null;
    } else {
      currentTime.setSecond(second);

      return currentTime;
    }
  }

  private int getNextMatch(boolean [] range, int start)
  {
    return getNextMatch(range, start, range.length);
  }

  private int getNextMatch(boolean [] range, int start, int end)
  {
    for (int match = start; match < end; match++) {
      if (range[match]) {
        return match;
      }
    }

    return -1;
  }

  private QDate allocateCalendar()
  {
    QDate calendar = _localCalendar.getAndSet(null);

    if (calendar == null) {
      calendar = QDate.createLocal();
    }

    return calendar;
  }

  private void freeCalendar(QDate cal)
  {
    _localCalendar.set(cal);
  }

  private class YearsFilter {
    private List<YearsFilterValue> _filterValues = new LinkedList<YearsFilterValue>();
    private boolean _anyYear = false;
    private int _endYear = 0;

    private void addFilterValue(YearsFilterValue filterValue)
    {
      if (filterValue.isAnyYear()) {
        _anyYear = true;
      } else if (filterValue.getEndYear() > _endYear) {
        _endYear = filterValue.getEndYear();
      }

      _filterValues.add(filterValue);
    }

    private int getNextMatch(int year)
    {
      if (_anyYear) {
        return year;
      }

      while (year <= _endYear) {
        for (YearsFilterValue filterValue : _filterValues) {
          if ((year >= filterValue.getStartYear())
              && (year <= filterValue.getEndYear())
              && ((year % filterValue.getIncrement()) == 0)) {
            return year;
          }
        }

        year++;
      }

      return -1;
    }
  }

  private class YearsFilterValue {
    private boolean _anyYear = false;
    private int _startYear = 0;
    private int _endYear = 0;
    private int _increment = 1;

    public boolean isAnyYear()
    {
      return _anyYear;
    }

    public void setAnyYear(boolean anyYear)
    {
      _anyYear = anyYear;
    }

    public int getStartYear()
    {
      return _startYear;
    }

    public void setStartYear(int startYear)
    {
      _startYear = startYear;
    }

    public int getEndYear()
    {
      return _endYear;
    }

    public void setEndYear(int endYear)
    {
      _endYear = endYear;
    }

    public void setIncrement(int increment)
    {
      _increment = increment;
    }

    public int getIncrement()
    {
      return _increment;
    }
  }
}