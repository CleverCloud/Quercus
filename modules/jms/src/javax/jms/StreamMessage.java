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

/**
 * The main message.
 */
public interface StreamMessage extends Message {
  public boolean readBoolean()
    throws JMSException;

  public byte readByte()
    throws JMSException;

  public short readShort()
    throws JMSException;

  public char readChar()
    throws JMSException;

  public int readInt()
    throws JMSException;

  public long readLong()
    throws JMSException;

  public float readFloat()
    throws JMSException;

  public double readDouble()
    throws JMSException;

  public String readString()
    throws JMSException;

  public Object readObject()
    throws JMSException;

  public int readBytes(byte []value)
    throws JMSException;

  public void writeBoolean(boolean value)
    throws JMSException;

  public void writeByte(byte value)
    throws JMSException;

  public void writeShort(short value)
    throws JMSException;

  public void writeChar(char value)
    throws JMSException;

  public void writeInt(int value)
    throws JMSException;

  public void writeLong(long value)
    throws JMSException;

  public void writeFloat(float value)
    throws JMSException;

  public void writeDouble(double value)
    throws JMSException;

  public void writeString(String value)
    throws JMSException;

  public void writeBytes(byte []value)
    throws JMSException;

  public void writeBytes(byte []value, int offset, int length)
    throws JMSException;

  public void writeObject(Object value)
    throws JMSException;

  public void reset()
    throws JMSException;
}
