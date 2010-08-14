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

package com.caucho.quercus.profile;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores the most recent profiles
 */
public class ProfileStore
{
  private static final AtomicLong _idSequence = new AtomicLong();
  
  private static final ArrayList<ProfileReport> _reportList
    = new ArrayList<ProfileReport>();

  public static long generateId()
  {
    return _idSequence.incrementAndGet();
  }

  /**
   * Adds a new report
   */
  public static void addReport(ProfileReport report)
  {
    synchronized (_reportList) {
      _reportList.add(0, report);

      while (_reportList.size() > 32)
        _reportList.remove(_reportList.size() - 1);
    }
  }

  /**
   * Returns the current reports
   */
  public static ArrayList<ProfileReport> getReports()
  {
    synchronized (_reportList) {
      return new ArrayList<ProfileReport>(_reportList);
    }
  }

  /**
   * Returns the report with the given index
   */
  public static ProfileReport findReport(long index)
  {
    synchronized (_reportList) {
      for (ProfileReport report : _reportList) {
        if (index == report.getId())
          return report;
      }
    }

    return null;
  }
}

