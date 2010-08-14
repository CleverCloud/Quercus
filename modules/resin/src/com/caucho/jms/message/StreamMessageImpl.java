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
import javax.jms.MessageEOFException;
import javax.jms.StreamMessage;
import java.util.ArrayList;
import java.io.*;

import com.caucho.vfs.*;
import com.caucho.hessian.io.*;

/**
 * A stream message.
 */
public class StreamMessageImpl extends MessageImpl implements StreamMessage
{
  private ArrayList<Object> _values = new ArrayList<Object>();
  private int _index;
  private byte []_bytes;
  private int _bytesOffset;

  public StreamMessageImpl()
  {
  }

  StreamMessageImpl(StreamMessage stream)
    throws JMSException
  {
    super(stream);

    try {
      stream.reset();
      
      Object value;
      while (true) {
        writeObject(stream.readObject());
      }
    } catch (MessageEOFException e) {
    }

    reset();
  }

  StreamMessageImpl(StreamMessageImpl stream)
  {
    super(stream);

    _values.addAll(stream._values);

    try {
      reset();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the type enumeration.
   */
  @Override
  public MessageType getType()
  {
    return MessageType.STREAM;
  }

  /**
   * Sets the body for reading.
   */
  public void setReceive()
    throws JMSException
  {
    super.setReceive();
    
    reset();
  }

  /**
   * Set the stream for reading.
   */
  public void reset()
    throws JMSException
  {
    setBodyReadOnly();
    
    _index = 0;
    _bytes = null;
    _bytesOffset = 0;
  }

  /**
   * Read a boolean from the stream.
   */
  public boolean readBoolean()
    throws JMSException
  {
    boolean value = ObjectConverter.toBoolean(readObjectImpl());

    _index++;
    
    return value;
  }

  /**
   * Read a byte from the stream.
   */
  public byte readByte()
    throws JMSException
  {
    byte value = ObjectConverter.toByte(readObjectImpl());

    _index++;

    return value;
  }

  /**
   * Read a short from the stream.
   */
  public short readShort()
    throws JMSException
  {
    short value = ObjectConverter.toShort(readObjectImpl());

    _index++;

    return value;
  }

  /**
   * Read an integer from the stream.
   */
  public int readInt()
    throws JMSException
  {
    int value = ObjectConverter.toInt(readObjectImpl());

    _index++;

    return value;
  }

  /**
   * Read a long from the stream.
   */
  public long readLong()
    throws JMSException
  {
    long value = ObjectConverter.toLong(readObjectImpl());

    _index++;

    return value;
  }

  /**
   * Read a float from the stream.
   */
  public float readFloat()
    throws JMSException
  {
    float value = ObjectConverter.toFloat(readObjectImpl());

    _index++;

    return value;
  }

  /**
   * Read a double from the stream.
   */
  public double readDouble()
    throws JMSException
  {
    double value = ObjectConverter.toDouble(readObjectImpl());

    _index++;

    return value;
  }

  /**
   * Read a character object from the stream.
   */
  public char readChar()
    throws JMSException
  {
    char value = ObjectConverter.toChar(readObjectImpl());

    _index++;

    return value;
  }

  /**
   * Read a string from the stream.
   */
  public String readString()
    throws JMSException
  {
    String value = ObjectConverter.toString(readObjectImpl());

    _index++;

    return value;
  }

  /**
   * Read a byte array object from the stream.
   */
  public int readBytes(byte []value)
    throws JMSException
  {
    byte []bytes;
    
    if (_bytes != null) {
      if (_bytesOffset == _bytes.length) {
        _bytes = null;
        _bytesOffset = 0;
        return -1;
      }
    }
    else {
      _bytes = ObjectConverter.toBytes(readObjectImpl());
      _index++;
    }

    if (_bytes == null)
      return -1;

    int sublen = _bytes.length - _bytesOffset;
    if (value.length < sublen)
      sublen = value.length;

    for (int i = 0; i < sublen; i++)
      value[i] = _bytes[_bytesOffset++];

    return sublen;
  }

  /**
   * Reads the next object.
   */
  public Object readObject()
    throws JMSException
  {
    Object value = readObjectImpl();

    _index++;

    return value;
  }

  /**
   * Reads the next object.
   */
  private Object readObjectImpl()
    throws JMSException
  {
    checkBodyReadable();

    if (_values.size() <= _index)
      throw new MessageEOFException(L.l("end of message in stream"));

    _bytes = null;
    _bytesOffset = 0;

    return _values.get(_index);
  }

  /**
   * Clears the message and puts it into write mode.
   */
  public void clearBody()
    throws JMSException
  {
    super.clearBody();
    
    _values.clear();
    _index = 0;
    _bytes = null;
    _bytesOffset = 0;
  }

  /**
   * Writes a boolean to the stream.
   */
  public void writeBoolean(boolean b)
    throws JMSException
  {
    writeObject(new Boolean(b));
  }

  /**
   * Writes a byte to the stream.
   */
  public void writeByte(byte b)
    throws JMSException
  {
    writeObject(new Byte(b));
  }

  /**
   * Writes a short to the stream.
   */
  public void writeShort(short s)
    throws JMSException
  {
    writeObject(new Short(s));
  }

  /**
   * Writes an integer to the stream.
   */
  public void writeInt(int i)
    throws JMSException
  {
    writeObject(new Integer(i));
  }

  /**
   * Writes a long to the stream.
   */
  public void writeLong(long l)
    throws JMSException
  {
    writeObject(new Long(l));
  }

  /**
   * Writes a float to the stream.
   */
  public void writeFloat(float f)
    throws JMSException
  {
    writeObject(new Float(f));
  }

  /**
   * Writes a double to the stream.
   */
  public void writeDouble(double d)
    throws JMSException
  {
    writeObject(new Double(d));
  }

  /**
   * Writes a string to the stream.
   */
  public void writeString(String s)
    throws JMSException
  {
    writeObject(s);
  }

  /**
   * Writes a character to the stream.
   */
  public void writeChar(char ch)
    throws JMSException
  {
    writeObject(new Character(ch));
  }

  /**
   * Writes a byte array to the stream.
   */
  public void writeBytes(byte []buf)
    throws JMSException
  {
    writeBytes(buf, 0, buf.length);
  }

  /**
   * Writes a byte array to the stream.
   */
  public void writeBytes(byte []buf, int offset, int length)
    throws JMSException
  {
    byte []newBuf = new byte[length];

    System.arraycopy(buf, offset, newBuf, 0, length);
    
    writeObject(newBuf);
  }

  /**
   * Writes the next object.
   */
  public void writeObject(Object obj)
    throws JMSException
  {
    checkBodyWriteable();
    
    _values.add(obj);
  }

  @Override
  public MessageImpl copy()
  {
    return new StreamMessageImpl(this);
  }

  protected void copy(StreamMessageImpl newMsg)
  {
    super.copy(newMsg);

    newMsg._values = new ArrayList(_values);
    newMsg._index = 0;
  }

  /**
   * Serialize the body to an input stream.
   */
  @Override
  public InputStream bodyToInputStream()
    throws IOException
  {
    if (_values == null || _values.size() == 0)
      return null;
    
    TempStream body = new TempStream();
    body.openWrite();
      
    StreamImplOutputStream ws = new StreamImplOutputStream(body);

    Hessian2Output out = new Hessian2Output(ws);

    out.writeObject(_values);

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

      _values = (ArrayList<Object>) in.readObject();

      in.close();
    }

    reset();
  }

  public String toString()
  {
    return "StreamMessageImpl[]";
  }
}

