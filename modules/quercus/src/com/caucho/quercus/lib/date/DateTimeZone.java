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

import java.util.TimeZone;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

/**
 * Date functions.
 */
public class DateTimeZone
{
  private TimeZone _timeZone;
  
  protected DateTimeZone(Env env)
  {
    TimeZone timeZone = env.getDefaultTimeZone();
    
    if (timeZone == null)
      timeZone = TimeZone.getDefault();
    
    _timeZone = timeZone;
  }
  
  protected DateTimeZone(String id)
  {
    _timeZone = TimeZone.getTimeZone(id);
  }
  
  public static DateTimeZone __construct(String id)
  {
    return new DateTimeZone(id);
  }
  
  public static ArrayValue listAbbreviations()
  {
    ArrayValue array = new ArrayValueImpl();
    
    String []ids = TimeZone.getAvailableIDs();
    
    for (int i = 0; i < ids.length; i++) {
      TimeZone tz = TimeZone.getTimeZone(ids[i]);
      
      addAbbreviation(array, tz, false);
      
      if (tz.useDaylightTime())
        addAbbreviation(array, tz, true);
    }
    
    return array;
  }
  
  private static void addAbbreviation(ArrayValue array,
                                      TimeZone tz,
                                      boolean isDST)
  {
    ArrayValueImpl zone = new ArrayValueImpl();
    
    zone.put("dst", isDST);
    
    int offset = tz.getRawOffset() / 1000;
    
    if (isDST)
      offset += tz.getDSTSavings() / 1000;
      
    zone.put("offset", offset);
    zone.put("timezone_id", tz.getID());
    
    String name = tz.getDisplayName(isDST, TimeZone.SHORT);
    Value nameV = StringValue.create(name.toLowerCase());
    
    Value zones = array.get(nameV);
    
    if (zones.isNull()) {
      zones = new ArrayValueImpl();
      
      array.put(nameV, zones);
    }
    
    zones.put(zone);
  }
  
  public static ArrayValue listIdentifiers()
  {
    ArrayValue array = new ArrayValueImpl();
    
    String []ids = TimeZone.getAvailableIDs();
    
    java.util.Arrays.sort(ids);
    
    for (int i = 0; i < ids.length; i++) {
      array.put(ids[i]);
    }

    return array;
  }
  
  public String getName()
  {
    return _timeZone.getID();
  }
  
  public long getOffset(DateTime dateTime)
  {
    return _timeZone.getOffset(dateTime.getTime()) / 1000L;
  }

  /* commented out for wordpress-2.8.1
  public Value getTransitions(@Optional int timestampBegin,
                              @Optional int timestampEnd)
  {
    throw new UnimplementedException("DateTimeZone->getTransitions()");
  }
  */
  
  protected TimeZone getTimeZone()
  {
    return _timeZone;
  }
  
  protected static Value findTimeZone(StringValue abbr)
  {
    // Can't use TimeZone.getTimeZone() because that function returns
    // GMT timezone by default if not found
    
    ArrayValue array = listAbbreviations();
    
    Value zones = array.get(abbr.toLowerCase());

    if (zones.isset())
      return zones.get(LongValue.ZERO).get(StringValue.create("timezone_id"));
    else
      return BooleanValue.FALSE;
  }
  
  protected static Value findTimeZone(StringValue abbr,
                                      int offset,
                                      boolean isDST)
  {
    ArrayValue array = listAbbreviations();
    
    Value zones = array.get(abbr.toLowerCase());
    
    if (zones.isset() && zones.isArray()) {
      Value offsetStr = StringValue.create("offset");
      
      for (Value zone : ((ArrayValue)zones).values()) {
        if (zone.get(offsetStr).toInt() == offset)
          return zone.get(StringValue.create("timezone_id"));
      }
    }

    return findTimeZone(offset, isDST);
  }
  
  protected static Value findTimeZone(int offset,
                                      boolean isDST)
  {
    String []zoneIDs = TimeZone.getAvailableIDs(offset * 1000);
    
    for (int i = 0; i < zoneIDs.length; i++) {
      TimeZone zone = TimeZone.getTimeZone(zoneIDs[i]);

      if (isDST == zone.useDaylightTime())
        return StringValue.create(zoneIDs[i]);
    }

    return BooleanValue.FALSE;
  }
  
  public String toString()
  {
    return _timeZone.getID();
  }
}
