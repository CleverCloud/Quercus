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

import java.io.Serializable;
import java.util.Date;

/**
 * A calendar-based timeout expression for an enterprise bean timer.
 * 
 * @author Reza Rahman
 */
public class ScheduleExpression implements Serializable {
  private static final long serialVersionUID = 1L;

  private String _second = "0";
  private String _minute = "0";
  private String _hour = "0";
  private String _dayOfWeek = "*";
  private String _dayOfMonth = "*";
  private String _month = "*";
  private String _year = "*";

  private String _timezone = "";

  private Date _start;
  private Date _end;

  /**
   * Constructs a new scheduled expression with defaults populated. The defaults
   * are the same as they are for the @Schedule annotation.
   */
  public ScheduleExpression()
  {
    super();
  }

  /**
   * Gets the seconds expression.
   * 
   * @return Seconds expression.
   */
  public String getSecond()
  {
    return _second;
  }

  /**
   * Gets the minutes expression.
   * 
   * @return Minutes expression.
   */
  public String getMinute()
  {
    return _minute;
  }

  /**
   * Gets the hour expression.
   * 
   * @return Hour expression.
   */
  public String getHour()
  {
    return _hour;
  }

  /**
   * Gets the day of week expression.
   * 
   * @return Day of week expression.
   */
  public String getDayOfWeek()
  {
    return _dayOfWeek;
  }

  /**
   * Gets the day of month expression.
   * 
   * @return Day of month expression.
   */
  public String getDayOfMonth()
  {
    return _dayOfMonth;
  }

  /**
   * Gets the month expression.
   * 
   * @return Month expression.
   */
  public String getMonth()
  {
    return _month;
  }

  /**
   * Gets the year expression.
   * 
   * @return Year expression.
   */
  public String getYear()
  {
    return _year;
  }

  /**
   * Gets the time zone for the schedule.
   * 
   * @return Time zone for the schedule.
   */
  public String getTimezone()
  {
    return _timezone;
  }

  /**
   * Gets the start date for the schedule.
   * 
   * @return Start date for the schedule.
   */
  public Date getStart()
  {
    return _start;
  }

  /**
   * Gets the end date for the schedule.
   * 
   * @return End date for the schedule.
   */
  public Date getEnd()
  {
    return _end;
  }

  /**
   * Sets the second expression.
   * 
   * @param second
   *          Second expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression second(String second)
  {
    _second = second;

    return this;
  }

  /**
   * Sets the second expression.
   * 
   * @param second
   *          Second expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression second(int second)
  {
    _second = String.valueOf(second);

    return this;
  }

  /**
   * Sets the minute expression.
   * 
   * @param minute
   *          Minute expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression minute(String minute)
  {
    _minute = minute;

    return this;
  }

  /**
   * Sets the minute expression.
   * 
   * @param minute
   *          Minute expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression minute(int minute)
  {
    _minute = String.valueOf(minute);

    return this;
  }

  /**
   * Sets the hour expression.
   * 
   * @param hour
   *          Hour expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression hour(String hour)
  {
    _hour = hour;

    return this;
  }

  /**
   * Sets the hour expression.
   * 
   * @param hour
   *          Hour expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression hour(int hour)
  {
    _hour = String.valueOf(hour);

    return this;
  }

  /**
   * Sets the day of week expression.
   * 
   * @param dayOfWeek
   *          Day of week expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression dayOfWeek(String dayOfWeek)
  {
    _dayOfWeek = dayOfWeek;

    return this;
  }

  /**
   * Sets the day of week expression.
   * 
   * @param dayOfWeek
   *          Day of week expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression dayOfWeek(int dayOfWeek)
  {
    _dayOfWeek = String.valueOf(dayOfWeek);

    return this;
  }

  /**
   * Sets the day of month expression.
   * 
   * @param dayOfMonth
   *          Day of month expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression dayOfMonth(String dayOfMonth)
  {
    _dayOfMonth = dayOfMonth;

    return this;
  }

  /**
   * Sets the day of month expression.
   * 
   * @param dayOfMonth
   *          Day of month expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression dayOfMonth(int dayOfMonth)
  {
    _dayOfMonth = String.valueOf(dayOfMonth);

    return this;
  }

  /**
   * Sets the month expression.
   * 
   * @param month
   *          Month expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression month(String month)
  {
    _month = month;

    return this;
  }

  /**
   * Sets the month expression.
   * 
   * @param month
   *          Month expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression month(int month)
  {
    _month = String.valueOf(month);

    return this;
  }

  /**
   * Sets the year expression.
   * 
   * @param year
   *          Year expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression year(String year)
  {
    _year = year;

    return this;
  }

  /**
   * Sets the year expression.
   * 
   * @param year
   *          Year expression.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression year(int year)
  {
    _year = String.valueOf(year);

    return this;
  }

  /**
   * Sets the time zone for this schedule.
   * 
   * @param timezoneId
   *          Time zone ID of this schedule.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression timezone(String timezoneId)
  {
    _timezone = timezoneId;

    return this;
  }

  /**
   * Sets the start date for this schedule.
   * 
   * @param start
   *          Start date of this schedule.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression start(Date start)
  {
    _start = start;

    return this;
  }

  /**
   * Sets the end date for this schedule.
   * 
   * @param end
   *          End date of this schedule.
   * @return Reference to the current object (this) for method chaining.
   */
  public ScheduleExpression end(Date end)
  {
    _end = end;

    return this;
  }

  @Override
  public String toString()
  {
    return String
        .format(
            "schedule expression[second: %s, minute: %s, hour: %s, day of week: %s, day of month: %s, month: %s, year: %s, timezone: %s, start: %s, end: %s]",
            _second, _minute, _hour, _dayOfWeek, _dayOfMonth, _month, _year,
            _timezone, _start, _end);
  }
}