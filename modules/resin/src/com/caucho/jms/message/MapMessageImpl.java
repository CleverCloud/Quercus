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

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageFormatException;
import java.util.*;
import java.io.*;
import com.caucho.vfs.*;
import com.caucho.hessian.io.*;

/**
 * A stream message.
 */
public class MapMessageImpl extends MessageImpl implements MapMessage  {
  private HashMap<String,Object> _map = new HashMap<String,Object>();

  public MapMessageImpl()
  {
  }

  MapMessageImpl(MapMessage map)
    throws JMSException
  {
    super(map);
    
    Enumeration e = map.getMapNames();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();

      _map.put(name, map.getObject(name));
    }
  }

  MapMessageImpl(MapMessageImpl map)
  {
    super(map);

    _map.putAll(map._map);
  }

  /**
   * Returns the type enumeration.
   */
  @Override
  public MessageType getType()
  {
    return MessageType.MAP;
  }

  /**
   * Returns true if the object exists.
   */
  public boolean itemExists(String name)
    throws JMSException
  {
    return _map.containsKey(name);
  }

  /**
   * Returns an enumeration of the map names.
   */
  public Enumeration getMapNames()
    throws JMSException
  {
    return Collections.enumeration(_map.keySet());
  }

  /**
   * Get a boolean from the stream.
   */
  public boolean getBoolean(String name)
    throws JMSException
  {
    return ObjectConverter.toBoolean(getObject(name));
  }

  /**
   * Get a byte from the stream.
   */
  public byte getByte(String name)
    throws JMSException
  {
    return ObjectConverter.toByte(getObject(name));
  }

  /**
   * Get a short from the stream.
   */
  public short getShort(String name)
    throws JMSException
  {
    return ObjectConverter.toShort(getObject(name));
  }

  /**
   * Get an integer from the stream.
   */
  public int getInt(String name)
    throws JMSException
  {
    return ObjectConverter.toInt(getObject(name));
  }

  /**
   * Get a long from the stream.
   */
  public long getLong(String name)
    throws JMSException
  {
    return ObjectConverter.toLong(getObject(name));
  }

  /**
   * Get a float from the stream.
   */
  public float getFloat(String name)
    throws JMSException
  {
    return ObjectConverter.toFloat(getObject(name));
  }

  /**
   * Get a double from the stream.
   */
  public double getDouble(String name)
    throws JMSException
  {
    return ObjectConverter.toDouble(getObject(name));
  }

  /**
   * Get a character object from the stream.
   */
  public char getChar(String name)
    throws JMSException
  {
    return ObjectConverter.toChar(getObject(name));
  }

  /**
   * Get a string from the stream.
   */
  public String getString(String name)
    throws JMSException
  {
    return ObjectConverter.toString(getObject(name));
  }

  /**
   * Get a byte array object from the stream.
   */
  public byte []getBytes(String name)
    throws JMSException
  {
    return ObjectConverter.toBytes(getObject(name));
  }

  /**
   * Get a byte array object from the stream.
   */
  public int getBytes(String name, byte []value)
    throws JMSException
  {
    byte []bytes = ObjectConverter.toBytes(getObject(name));

    if (bytes == null)
      return 0;

    int sublen = bytes.length;
    if (value.length < sublen)
      sublen = value.length;

    for (int i = 0; i < sublen; i++)
      value[i] = bytes[i];

    return sublen;
  }

  /**
   * Gets the next object.
   */
  public Object getObject(String name)
    throws JMSException
  {
    return _map.get(name);
  }

  /**
   * Clears the message and puts it into write mode.
   */
  public void clearBody()
    throws JMSException
  {
    super.clearBody();
    
    _map.clear();
  }

  /**
   * Sets a boolean to the stream.
   */
  public void setBoolean(String name, boolean b)
    throws JMSException
  {
    setObject(name, new Boolean(b));
  }

  /**
   * Sets a byte to the stream.
   */
  public void setByte(String name, byte b)
    throws JMSException
  {
    setObject(name, new Byte(b));
  }

  /**
   * Sets a short to the stream.
   */
  public void setShort(String name, short s)
    throws JMSException
  {
    setObject(name, new Short(s));
  }

  /**
   * Sets an integer to the stream.
   */
  public void setInt(String name, int i)
    throws JMSException
  {
    setObject(name, new Integer(i));
  }

  /**
   * Sets a long to the stream.
   */
  public void setLong(String name, long l)
    throws JMSException
  {
    setObject(name, new Long(l));
  }

  /**
   * Sets a float to the stream.
   */
  public void setFloat(String name, float f)
    throws JMSException
  {
    setObject(name, new Float(f));
  }

  /**
   * Sets a double to the stream.
   */
  public void setDouble(String name, double d)
    throws JMSException
  {
    setObject(name, new Double(d));
  }

  /**
   * Sets a string to the stream.
   */
  public void setString(String name, String s)
    throws JMSException
  {
    setObject(name, s);
  }

  /**
   * Sets a character to the stream.
   */
  public void setChar(String name, char ch)
    throws JMSException
  {
    setObject(name, new Character(ch));
  }

  /**
   * Sets a byte array to the stream.
   */
  public void setBytes(String name, byte []buf)
    throws JMSException
  {
    setBytes(name, buf, 0, buf.length);
  }

  /**
   * Sets a byte array to the stream.
   */
  public void setBytes(String name, byte []buf, int offset, int length)
    throws JMSException
  {
    byte []newBuf = new byte[length];

    System.arraycopy(buf, offset, newBuf, 0, length);
    
    setObject(name, newBuf);
  }

  /**
   * Sets the next object.
   */
  public void setObject(String name, Object obj)
    throws JMSException
  {
    checkBodyWriteable();
    
    if (obj == null) {
    }
    else if (obj instanceof byte[]) {
    }
    else if (! obj.getClass().getName().startsWith("java.lang."))
      throw new MessageFormatException(L.l("'{0}' is an invalid value for a map message.",
                                 obj.getClass().getName()));

    if (name == null || "".equals(name))
      throw new IllegalArgumentException(L.l("MapMessage.setXXX name may not be empty."));
    
    _map.put(name, obj);
  }

  public MessageImpl copy()
  {
    return new MapMessageImpl(this);
  }

  protected void copy(MapMessageImpl newMsg)
  {
    super.copy(newMsg);

    newMsg._map = new HashMap<String,Object>(_map);
  }

  /**
   * Serialize the body to an input stream.
   */
  @Override
  public InputStream bodyToInputStream()
    throws IOException
  {
    if (_map == null)
      return null;
    
    TempStream body = new TempStream();
    body.openWrite();
      
    StreamImplOutputStream ws = new StreamImplOutputStream(body);

    Hessian2Output out = new Hessian2Output(ws);

    out.writeObject(_map);

    out.close();
    
    ws.close();

    return body.openRead();
  }

  /**
   * Read the body from an input stream.
   */
  @Override
  public void readBody(InputStream is)
    throws IOException, JMSException
  {
    if (is != null) {
      Hessian2Input in = new Hessian2Input(is);

      _map = (HashMap<String,Object>) in.readObject();

      in.close();
    }
    
    setBodyReadOnly();
  }

  public String toString()
  {
    return "MapMessageImpl[]";
  }
}

