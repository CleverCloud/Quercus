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

import com.caucho.jms.JmsExceptionWrapper;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import java.io.*;
import java.util.logging.Level;

/**
 * A byte-stream message.
 */
public class BytesMessageImpl extends MessageImpl implements BytesMessage {
  private TempStream _tempStream;
  private transient ReadStream _rs;
  private transient WriteStream _ws;

  public BytesMessageImpl()
  {
  }

  BytesMessageImpl(BytesMessage bytes)
    throws JMSException
  {
    super(bytes);

    bytes.reset();

    checkBodyWriteable();

    try {
      TempBuffer tempBuf = TempBuffer.allocate();
      byte []buffer = tempBuf.getBuffer();
      WriteStream out = getWriteStream();

      int sublen;
    
      while ((sublen = bytes.readBytes(buffer, buffer.length)) > 0) {
        out.write(buffer, 0, sublen);
      }

      TempBuffer.free(tempBuf);
      tempBuf = null;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      throw new JMSException(e.toString());
    }

    reset();
  }

  BytesMessageImpl(BytesMessageImpl bytes)
    throws JMSException
  {
    super(bytes);

    bytes.reset();

    _tempStream = bytes._tempStream;

    reset();
  }

  /**
   * Returns the type enumeration.
   */
  @Override
  public MessageType getType()
  {
    return MessageType.BYTES;
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
    
    try {
      // XXX: test for null
      if (_ws != null)
        _ws.close();

      if (_tempStream != null) {
        if (_rs != null)
          _rs.close();

        _rs = _tempStream.openReadAndSaveBuffer();
      }
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read a boolean from the stream.
   */
  public boolean readBoolean()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int value = is.read();

      if (value >= 0)
        return value == 1;
      else
        throw new MessageEOFException("BytesMessage EOF");
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read a byte from the stream.
   */
  public byte readByte()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int value = is.read();

      if (value >= 0)
        return (byte) value;
      else
        throw new MessageEOFException("BytesMessage EOF");
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read an unsigned byte from the stream.
   */
  public int readUnsignedByte()
    throws JMSException
  {
    ReadStream is = getReadStream();

    if (is == null)
      return -1;

    try {
      int value = is.read();

      if (value >= 0)
        return value;
      else
        throw new MessageEOFException("BytesMessage EOF");
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read a short from the stream.
   */
  public short readShort()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int d1 = is.read();
      int d2 = is.read();

      if (d2 >= 0)
        return (short) ((d1 << 8) + d2);
      else
        throw new MessageEOFException("BytesMessage EOF");
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read an unsigned short from the stream.
   */
  public int readUnsignedShort()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int d1 = is.read();    
      int d2 = is.read();    

      if (d2 >= 0)
        return ((d1 << 8) + d2);
      else
        throw new MessageEOFException("BytesMessage EOF");
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read an integer from the stream.
   */
  public int readInt()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int d1 = is.read();    
      int d2 = is.read();    
      int d3 = is.read();    
      int d4 = is.read();    

      if (d4 >= 0)
        return (d1 << 24) + (d2 << 16) + (d3 << 8) + d4;
      else
        throw new MessageEOFException("BytesMessage EOF");
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read a long from the stream.
   */
  public long readLong()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      long d1 = is.read();    
      long d2 = is.read();    
      long d3 = is.read();    
      long d4 = is.read();    
      long d5 = is.read();    
      long d6 = is.read();    
      long d7 = is.read();    
      long d8 = is.read();    

      if (d8 >= 0) {
        return ((d1 << 56)
                + (d2 << 48)
                + (d3 << 40)
                + (d4 << 32)
                + (d5 << 24)
                + (d6 << 16)
                + (d7 << 8)
                + (d8));
      }
      else
        throw new MessageEOFException("BytesMessage EOF");
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read a float from the stream.
   */
  public float readFloat()
    throws JMSException
  {
    return Float.intBitsToFloat(readInt());
  }

  /**
   * Read a double from the stream.
   */
  public double readDouble()
    throws JMSException
  {
    return Double.longBitsToDouble(readLong());
  }

  /**
   * Read a character object from the stream.
   */
  public char readChar()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int d1 = is.read();    
      int d2 = is.read();

      if (d2 >= 0)
        return (char) ((d1 << 8) + d2);
      else
        throw new MessageEOFException("BytesMessage EOF");
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read a string from the stream.
   */
  public String readUTF()
    throws JMSException
  {
    ReadStream is = getReadStream();
    CharBuffer cb = new CharBuffer();

    try {
      int len = readShort();

      int d1;
      
      while (len > 0) {
        d1 = is.read();
        
        if (d1 < 0x80) {
          cb.append((char) d1);
          len -= 1;
        }
        else if ((d1 & 0xe0) == 0xc0) {
          int d2 = is.read();

          cb.append((char) (((d1 & 0x1f) << 6) + (d2 & 0x3f)));

          len -= 2;
        }
        else if ((d1 & 0xf0) == 0xe0) {
          int d2 = is.read();
          int d3 = is.read();

          cb.append((char) (((d1 & 0xf) << 12)
                            + ((d2 & 0x3f) << 6)
                            + (d3 & 0x3f)));

          len -= 3;
        }
        else
          throw new MessageFormatException(L.l("invalid UTF-8 in bytes message"));
      }
    } catch (JMSException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new JmsExceptionWrapper(e);
    }

    return cb.toString();
  }

  /**
   * Read a byte array object from the stream.
   */
  public int readBytes(byte []value)
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      return is.read(value);
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Read a byte array object from the stream.
   */
  public int readBytes(byte []value, int length)
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      return is.read(value, 0, length);
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  protected ReadStream getReadStream()
    throws JMSException
  {
    checkBodyReadable();

    /* ejb/6a87
    if (_rs == null)
      throw new MessageEOFException(L.l("bytes message may not be read"));
    */
      
    return _rs;
  }

  /**
   * Clears the message and puts it into write mode.
   */
  public void clearBody()
    throws JMSException
  {
    super.clearBody();
    
    _ws = null;
    _tempStream = null;
    _rs = null;
  }

  /**
   * Writes a boolean to the stream.
   */
  public void writeBoolean(boolean b)
    throws JMSException
  {
    try {
      getWriteStream().write(b ? 1 : 0);
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Writes a byte to the stream.
   */
  public void writeByte(byte b)
    throws JMSException
  {
    try {
      getWriteStream().write(b);
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Writes a short to the stream.
   */
  public void writeShort(short s)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();
    
      ws.write(s >> 8);
      ws.write(s);
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Writes an integer to the stream.
   */
  public void writeInt(int i)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();
    
      ws.write(i >> 24);
      ws.write(i >> 16);
      ws.write(i >> 8);
      ws.write(i);
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Writes a long to the stream.
   */
  public void writeLong(long l)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();
    
      ws.write((int) (l >> 56));
      ws.write((int) (l >> 48));
      ws.write((int) (l >> 40));
      ws.write((int) (l >> 32));
      ws.write((int) (l >> 24));
      ws.write((int) (l >> 16));
      ws.write((int) (l >> 8));
      ws.write((int) l);
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Writes a float to the stream.
   */
  public void writeFloat(float f)
    throws JMSException
  {
    int i = Float.floatToIntBits(f);
    writeInt(i);
  }

  /**
   * Writes a double to the stream.
   */
  public void writeDouble(double d)
    throws JMSException
  {
    long l = Double.doubleToLongBits(d);
    writeLong(l);
  }

  /**
   * Writes a string to the stream.
   */
  public void writeUTF(String s)
    throws JMSException
  {
    try {
      WriteStream out = getWriteStream();

      int len = s.length();

      int byteLength = 0;

      for (int i = 0; i < len; i++) {
        int ch = s.charAt(0);

        if (ch == 0)
          byteLength += 2;
        else if (ch < 0x80)
          byteLength += 1;
        else if (ch < 0x800)
          byteLength += 2;
        else
          byteLength += 3;
      }

      out.write(byteLength >> 8);
      out.write(byteLength);

      for (int i = 0; i < len; i++) {
        int ch = s.charAt(i);

        if (ch == 0) {
          out.write(0xc0);
          out.write(0x80);
        }
        else if (ch < 0x80)
          out.write(ch);
        else if (ch < 0x800) {
          out.write(0xc0 + ((ch >> 6) & 0x1f));
          out.write(0x80 + (ch & 0x3f));
        }
        else if (ch < 0x8000) {
          out.write(0xe0 + ((ch >> 12) & 0x0f));
          out.write(0x80 + ((ch >> 6) & 0x3f));
          out.write(0x80 + (ch & 0x3f));
        }
      }
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Writes a character to the stream.
   */
  public void writeChar(char ch)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();

      ws.write(ch >> 8);
      ws.write(ch);
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
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
    try {
      WriteStream ws = getWriteStream();

      ws.write(buf, offset, length);
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    }
  }

  /**
   * Writes the next object.
   */
  public void writeObject(Object obj)
    throws JMSException
  {
    if (obj == null)
      throw new NullPointerException();
    else if (obj instanceof Boolean)
      writeBoolean(((Boolean) obj).booleanValue());
    else if (obj instanceof Byte)
      writeByte(((Byte) obj).byteValue());
    else if (obj instanceof Short)
      writeShort(((Short) obj).shortValue());
    else if (obj instanceof Character)
      writeChar(((Character) obj).charValue());
    else if (obj instanceof Integer)
      writeInt(((Integer) obj).intValue());
    else if (obj instanceof Long)
      writeLong(((Long) obj).longValue());
    else if (obj instanceof Float)
      writeFloat(((Float) obj).floatValue());
    else if (obj instanceof Double)
      writeDouble(((Double) obj).doubleValue());
    else if (obj instanceof String)
      writeUTF((String) obj);
    else if (obj instanceof byte[])
      writeBytes((byte[]) obj);
    else
      throw new MessageFormatException(obj.getClass().getName() + ": " + String.valueOf(obj));
  }

  public long getBodyLength()
    throws JMSException
  {
    checkBodyReadable();
    
    if (_tempStream == null)
      return 0;
    else
      return _tempStream.getLength();
  }

  protected WriteStream getWriteStream()
    throws JMSException
  {
    checkBodyWriteable();

    if (_tempStream == null)
      _tempStream = new TempStream();
    
    if (_ws == null)
      _ws = new WriteStream(_tempStream);

    return _ws;
  }

  public MessageImpl copy()
  {
    try {
      return new BytesMessageImpl(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected void copy(BytesMessageImpl newMsg)
  {
    super.copy(newMsg);

    try {
      if (_ws != null)
        _ws.flush();

      if (_tempStream != null)
        newMsg._tempStream = _tempStream.copy();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Serialize the body to an input stream.
   */
  @Override
  public InputStream bodyToInputStream()
    throws IOException
  {
    if (_tempStream != null)
      return _tempStream.openReadAndSaveBuffer();
    else
      return null;
  }

  /**
   * Read the body from an input stream.
   */
  @Override
  public void readBody(InputStream is)
    throws IOException, JMSException
  {
    if (is != null) {
      WriteStream out = getWriteStream();

      out.writeStream(is);

      out.close();
    }

    reset();
  }

  public String toString()
  {
    return "BytesMessageImpl[]";
  }
}

