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

package com.caucho.jms.message;

import com.caucho.util.L10N;

import javax.jms.JMSException;
import javax.jms.MessageFormatException;
import java.util.logging.Logger;

/**
 * A basic message.
 */
public class ObjectConverter  {
  static final Logger log = Logger.getLogger(ObjectConverter.class.getName());
  static final L10N L = new L10N(ObjectConverter.class);
  
  /**
   * Returns an object converted to a boolean.
   */
  public static boolean toBoolean(Object obj)
    throws JMSException
  {
    if (obj == null) { 
      // jms/214c
      throw new NullPointerException();
    } else if (obj instanceof Boolean)
      return ((Boolean) obj).booleanValue();
    else if (obj instanceof String)
      return Boolean.valueOf((String) obj).booleanValue();
    else
      throw new MessageFormatException(L.l("can't convert '{0}' to boolean",
                                           obj.getClass().getName()));
  }

  /**
   * Returns a property as a byte
   */
  public static byte toByte(Object obj)
    throws JMSException
  {
    // jms/2252
    if (obj instanceof Byte)
      return ((Number) obj).byteValue();
    else if (obj == null || obj instanceof String)
      return (byte) Long.parseLong((String) obj);
    else
      throw new MessageFormatException(L.l("can't convert '{0}' to byte",
                                           obj.getClass().getName()));
  }

  /**
   * Returns a property as a short
   */
  public static short toShort(Object obj)
    throws JMSException
  {
    // jms/2252
    if (obj instanceof Short || obj instanceof Byte)
      return ((Number) obj).shortValue();
    else if (obj == null || obj instanceof String)
      return (short) Long.parseLong((String) obj);
    else
      throw new MessageFormatException(L.l("can't convert '{0}' to short",
                                           obj.getClass().getName()));
  }

  /**
   * Returns a property as an integer
   */
  public static int toInt(Object obj)
    throws JMSException
  {
    if (obj instanceof Integer
        || obj instanceof Short
        || obj instanceof Byte)
      return ((Number) obj).intValue();
    else if (obj == null || obj instanceof String)
      return (int) Long.parseLong((String) obj);
    else
      throw new MessageFormatException(L.l("can't convert '{0}' to int",
                                           obj.getClass().getName()));
  }

  /**
   * Returns a property as a long
   */
  public static long toLong(Object obj)
    throws JMSException
  {
    if (obj instanceof Long
        || obj instanceof Integer
        || obj instanceof Short
        || obj instanceof Byte)
      return ((Number) obj).longValue();
    else if (obj == null || obj instanceof String)
      return Long.parseLong((String) obj);
    else
      throw new MessageFormatException(L.l("can't convert '{0}' to long",
                                           obj.getClass().getName()));
  }

  /**
   * Returns a property as a float
   */
  public static float toFloat(Object obj)
    throws JMSException
  {
    if (obj == null || obj instanceof Float)
      return ((Number) obj).floatValue();
    else if (obj == null || obj instanceof String)
      return (float) Double.parseDouble((String) obj);
    else
      throw new MessageFormatException(L.l("can't convert '{0}' to float",
                                           obj.getClass().getName()));
  }

  /**
   * Returns a property as a double
   */
  public static double toDouble(Object obj)
    throws JMSException
  {
    if (obj == null || obj instanceof Float || obj instanceof Double)
      return ((Number) obj).doubleValue();
    else if (obj == null || obj instanceof String)
      return Double.parseDouble((String) obj);
    else
      throw new MessageFormatException(L.l("can't convert '{0}' to double",
                                           obj.getClass().getName()));
  }

  /**
   * Returns a property as a string
   */
  public static String toString(Object obj)
    throws JMSException
  {
    if (obj == null)
      return null;
    else if (! (obj instanceof byte[]))
      return obj.toString();
    else
      throw new MessageFormatException(L.l("can't convert '{0}' to String",
                                           obj.getClass().getName()));
  }

  /**
   * Returns a property as a char
   */
  public static char toChar(Object obj)
    throws JMSException
  {
    if (obj == null)
      throw new NullPointerException();
    else if (obj instanceof Character)
      return ((Character) obj).charValue();
    else if (obj instanceof String) {
      String s = (String) obj;

      if (s.length() != 1)
        throw new MessageFormatException(L.l("bad property {0}", obj));
        
      return s.charAt(0);
    }
    else
      throw new MessageFormatException(L.l("bad property {0}", obj));
    
  }

  /**
   * Returns a property as a byte[]
   */
  public static byte []toBytes(Object obj)
    throws JMSException
  {
    if (obj == null)
      return null;
    else if (obj instanceof byte[]) {
      byte []bytes = (byte []) obj;
      byte []newBytes = new byte[bytes.length];
      System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
      
      return newBytes;
    }
    /*
    else if (obj instanceof String) {
      String string = toString(obj);
      try {
        return string.getBytes("UTF-8");
      } catch (Exception e) {
        throw new MessageFormatException(e.toString());
      }
    }
    */
    else
      throw new MessageFormatException(L.l("can't convert {0} to byte[]",
                                           obj.getClass().getName()));
  }
}
