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

import java.io.Serializable;

/**
 * Cron expression.
 * 
 * @author Reza Rahman
 */
public class CronExpression implements Serializable {
  private static final long serialVersionUID = 1L;

  private String _second;
  private String _minute;
  private String _hour;
  private String _dayOfWeek;
  private String _dayOfMonth;
  private String _month;
  private String _year;

  /**
   * Constructs a new cron expression.
   * 
   * @param second
   *          Second part of cron expression.
   * @param minute
   *          Minute part of cron expression.
   * @param hour
   *          Hour part of cron expression.
   * @param dayOfWeek
   *          Day of week part of cron expression.
   * @param dayOfMonth
   *          Day of month part of cron expression.
   * @param month
   *          Month part of cron expression.
   * @param year
   *          Year part of cron expression.
   */
  public CronExpression(final String second, final String minute,
      final String hour, final String dayOfWeek, final String dayOfMonth,
      final String month, final String year)
  {
    _second = second;
    _minute = minute;
    _hour = hour;
    _dayOfWeek = dayOfWeek;
    _dayOfMonth = dayOfMonth;
    _month = month;
    _year = year;
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

  @Override
  public String toString()
  {
    return String
        .format(
            "cron expression[second: %s, minute: %s, hour: %s, day of week: %s, day of month: %s, month: %s, year: %s]",
            _second, _minute, _hour, _dayOfWeek, _dayOfMonth, _month, _year);
  }
}