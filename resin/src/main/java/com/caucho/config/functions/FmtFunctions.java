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
 * @author Sam 
 */

package com.caucho.config.functions;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.util.Sprintf;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

/**
 * An object to store in an EL Environment to provide utility methods.
 */
public class FmtFunctions {
  static protected final Logger log
    = Logger.getLogger(FmtFunctions.class.getName());
  static final L10N L = new L10N(FmtFunctions.class);

  static private QDate _calendar = new QDate(true);

  public FmtFunctions()
  {
  }

  /**
   * Make a timestamp for current date and time.
   */
  static public String timestamp(String format)
  {
    long now;

    if (CauchoSystem.isTesting())
      now = Alarm.getCurrentTime();
    else
      now = System.currentTimeMillis();

    return timestamp(format,now);
  }

  static protected String timestamp(String format, long t)
  {
    return _calendar.formatLocal(t, format);
  }

  public static String timestamp(String format, Date date)
  {
    return timestamp(format,date.getTime());
  }

  public static String timestamp(String format, Calendar date)
  {
    return timestamp(format,date.getTimeInMillis());
  }

  public static String timestamp(String format, QDate date)
  {
    return date.format(format);
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9)
  {
    return Sprintf.sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8)
  {
    return Sprintf.sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7)
  {
    return Sprintf.sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6)
  {
    return Sprintf.sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5, arg6 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5)
  {
    return Sprintf.sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4)
  {
    return Sprintf.sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3)
  {
    return Sprintf.sprintf(format, new Object[] { arg0, arg1, arg2, arg3 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2)
  {
    return Sprintf.sprintf(format, new Object[] { arg0, arg1, arg2 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1)
  {
    return Sprintf.sprintf(format, new Object[] { arg0, arg1 } );
  }

  public static String sprintf(String format, Object arg0)
  {
    return Sprintf.sprintf(format, new Object[] { arg0 } );
  }
}

