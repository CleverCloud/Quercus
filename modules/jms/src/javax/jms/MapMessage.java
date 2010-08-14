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

package javax.jms;

import java.util.Enumeration;

/**
 * The main message.
 */
public interface MapMessage extends Message {
  public boolean getBoolean(String name)
    throws JMSException;
  
  public byte getByte(String name)
    throws JMSException;
  
  public short getShort(String name)
    throws JMSException;
  
  public char getChar(String name)
    throws JMSException;
  
  public int getInt(String name)
    throws JMSException;
  
  public long getLong(String name)
    throws JMSException;
  
  public float getFloat(String name)
    throws JMSException;
  
  public double getDouble(String name)
    throws JMSException;
  
  public String getString(String name)
    throws JMSException;
  
  public byte []getBytes(String name)
    throws JMSException;
  
  public Object getObject(String name)
    throws JMSException;
  
  public Enumeration getMapNames()
    throws JMSException;
  
  public void setBoolean(String name, boolean value)
    throws JMSException;
  
  public void setByte(String name, byte value)
    throws JMSException;
  
  public void setShort(String name, short value)
    throws JMSException;
  
  public void setChar(String name, char value)
    throws JMSException;
  
  public void setInt(String name, int value)
    throws JMSException;
  
  public void setLong(String name, long value)
    throws JMSException;
  
  public void setFloat(String name, float value)
    throws JMSException;
  
  public void setDouble(String name, double value)
    throws JMSException;
  
  public void setString(String name, String value)
    throws JMSException;
  
  public void setBytes(String name, byte []value)
    throws JMSException;
  
  public void setBytes(String name, byte []value, int offset, int length)
    throws JMSException;
  
  public void setObject(String name, Object value)
    throws JMSException;

  public boolean itemExists(String name)
    throws JMSException;
}
