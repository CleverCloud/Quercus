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

package com.caucho.quercus.env;

import java.io.*;
import java.util.*;

import com.caucho.vfs.*;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.util.*;

/**
 * Represents a 8-bit PHP 5 style binary builder (unicode.semantics = off),
 * used for large data like file reads.
 */
public class LargeStringBuilderValue
  extends StringValue
{
  public static final StringValue EMPTY = StringBuilderValue.EMPTY;
  
  public static final int SIZE = 4 * 1024;
  
  protected byte [][]_bufferList;
  protected int _length;

  private int _hashCode;
  private String _value;

  public LargeStringBuilderValue()
  {
    _bufferList = new byte[32][];
    _bufferList[0] = new byte[SIZE];
  }

  public LargeStringBuilderValue(byte [][]bufferList, int length)
  {
    _bufferList = bufferList;
    _length = length;
  }
  
  public LargeStringBuilderValue(StringValue s)
  {
    this();
    
    s.appendTo(this);
  }
  
  /**
   * Creates an empty string builder of the same type.
   */
  public StringValue createEmptyStringBuilder()
  {
    return new StringBuilderValue();
  }

  /**
   * Returns the value.
   */
  public String getValue()
  {
    return toString();
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return "string";
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return StringBuilderValue.getValueType(_bufferList[0], 0, _length);
  }

  /**
   * Returns true for a long
   */
  @Override
  public boolean isLongConvertible()
  {
    return false;
  }

  /**
   * Returns true for a double
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Returns true for a number
   */
  @Override
  public boolean isNumber()
  {
    return false;
  }

  /**
   * Returns true for a scalar
   */
  @Override
  public boolean isScalar()
  {
    return true;
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    if (_length == 0)
      return false;
    else if (_length == 1 && _bufferList[0][0] == '0')
      return false;
    else
      return true;
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return parseLong(_bufferList[0], 0, _length);
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return StringBuilderValue.toDouble(_bufferList[0], 0, _length);
  }

  /**
   * Convert to an input stream.
   */
  @Override
  public InputStream toInputStream()
  {
    return new BuilderInputStream();
  }

  /**
   * Converts to a string.
   */
  @Override
  public String toString()
  {
    char []buffer = new char[_length];

    byte [][]bufferList = _bufferList;
    for (int i = _length - 1; i >= 0; i--) {
      buffer[i] = (char) bufferList[i / SIZE][i % SIZE];
    }

    return new String(buffer, 0, _length);
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject()
  {
    return toString();
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    // XXX: can this just return this, or does it need to return a copy?
    
    return new LargeStringBuilderValue(_bufferList, _length);
  }

  /**
   * Converts to a BinaryValue.
   */
  @Override
  public StringValue toBinaryValue(Env env)
  {
    return this;
  }

  /**
   * Converts to a BinaryValue in desired charset.
   */
  @Override
  public StringValue toBinaryValue(String charset)
  {
    return this;
  }
  
  /**
   * Append to a string builder.
   */
  public void appendTo(StringValue bb)
  {
    int tail = _length % SIZE;
    int fixedLength = (_length - tail) / SIZE;

    int i = 0;
    for (; i < fixedLength; i++) {
      bb.append(_bufferList[i], 0, SIZE);
    }

    bb.append(_bufferList[i], 0, tail);
  }

  /**
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    if (getValueType().isLongAdd())
      return LongValue.create(toLong());
    else
      return this;
  }

  /**
   * Converts to a byte array, with no consideration of character encoding.
   * Each character becomes one byte, characters with values above 255 are
   * not correctly preserved.
   */
  public byte[] toBytes()
  {
    byte[] bytes = new byte[_length];

    byte [][]bufferList = _bufferList;
    for (int i = _length - 1; i >= 0; i--) {
      bytes[i] = bufferList[i / SIZE][i % SIZE];
    }

    return bytes;
  }

  //
  // Operations
  //

  /**
   * Returns the character at an index
   */
  public Value get(Value key)
  {
    return charValueAt(key.toLong());
  }

  /**
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
  {
    int len = _length;

    if (index < 0 || len <= index)
      return UnsetStringValue.UNSET;
    else {
      int data = _bufferList[(int) (index / SIZE)][(int) (index % SIZE)];
      
      return StringBuilderValue.create((char) (data & 0xff));
    }
  }

  //
  // CharSequence
  //

  /**
   * Returns the length of the string.
   */
  @Override
  public int length()
  {
    return _length;
  }

  /**
   * Returns the character at a particular location
   */
  @Override
  public char charAt(int index)
  {
    int data = _bufferList[index / SIZE][index % SIZE] & 0xff;
    
    return (char) data;
  }

  /**
   * Returns a subsequence
   */
  @Override
  public CharSequence subSequence(int start, int end)
  {
    if (end <= start)
      return StringBuilderValue.EMPTY;

    StringValue stringValue;
    
    if (end - start < 1024)
      stringValue = new StringBuilderValue(end - start);
    else
      stringValue = new LargeStringBuilderValue();

    int endChunk = end / SIZE;
      
    while (start < end) {
      int startChunk = start / SIZE;
      int startOffset = start % SIZE;

      if (startChunk == endChunk) {
        stringValue.append(_bufferList[startChunk],
                           startOffset,
                           (end - start));

        return stringValue;
      }
      else {
        int len = SIZE - startOffset;

        stringValue.append(_bufferList[startChunk], startOffset, len);

        start += len;
      }
    }

    return stringValue;
  }

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toLowerCase()
  {
    int length = _length;
    
    StringValue string = new LargeStringBuilderValue();

    byte [][]bufferList = _bufferList;

    for (int i = 0; i < length; i++) {
      int ch = bufferList[i / SIZE][i % SIZE] & 0xff;

      if ('A' <= ch && ch <= 'Z')
        ch = (ch + 'a' - 'A');

      string.append((char) ch);
    }

    return string;
  }
  
  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toUpperCase()
  {
    int length = _length;
    
    StringValue string = new LargeStringBuilderValue();

    byte [][]bufferList = _bufferList;

    for (int i = 0; i < length; i++) {
      int ch = bufferList[i / SIZE][i % SIZE] & 0xff;

      if ('a' <= ch && ch <= 'z')
        ch = (ch + 'A' - 'a');

      string.append((char) ch);
    }

    return string;
  }

  //
  // append code
  //

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder()
  {
    return new StringBuilderValue();
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder(int length)
  {
    return new StringBuilderValue(length);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return new LargeStringBuilderValue(_bufferList, _length);
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue appendUnicode(char []buf, int offset, int length)
  {
    return append(buf, offset, length);
  }
  
  /**
   * Append a Java string to the value.
   */
  @Override
  public StringValue append(String s)
  {
    int len = s.length();
    
    ensureCapacity(_length + len);
    
    for (int i = 0; i < len; i++) {
      _bufferList[_length / SIZE][_length % SIZE] = (byte) s.charAt(i);
      
      _length++;
    }
    
    return this;
  }
  
  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(CharSequence buf, int head, int tail)
  {
    int len = tail - head;
    
    ensureCapacity(_length + len);
    
    for (int i = 0; i < len; i++) {
      _bufferList[_length / SIZE][_length % SIZE] = (byte) buf.charAt(i);
      
      _length++;
    }
    
    return this;
  }
  
  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(char []buf, int offset, int length)
  {
    ensureCapacity(_length + length);

    for (int i = offset; i < length + offset; i++) {
      _bufferList[_length / SIZE][_length % SIZE] = (byte) buf[i];
      
      _length++;
    }

    return this;
  }

  /**
   * Append a buffer to the value.
   */
  public final StringValue append(byte []buf, int offset, int length)
  {
    ensureCapacity(_length + length);

    while (length > 0) {
      int chunk = _length / SIZE;
      int chunkOffset = _length % SIZE;

      int sublen = SIZE - chunkOffset;
      if (length < sublen)
        sublen = length;

      System.arraycopy(buf, offset, _bufferList[chunk], chunkOffset, sublen);

      offset += sublen;
      length -= sublen;
      _length += sublen;
    }
      
    return this;
  }

  /**
   * Append a double to the value.
   */
  public final StringValue append(byte []buf)
  {
    return append(buf, 0, buf.length);
  }

  /**
   * Append a Java byte to the value without conversions.
   */
  @Override
  public final StringValue append(char v)
  {
    if (_length % SIZE == 0)
      ensureCapacity(_length + 1);

    _bufferList[_length / SIZE][_length % SIZE] = (byte) v;

    _length += 1;

    return this;
  }

  /**
   * Append a Java byte to the value without conversions.
   */
  public final StringValue append(byte v)
  {
    if (_length % SIZE == 0)
      ensureCapacity(_length + 1);

    _bufferList[_length / SIZE][_length % SIZE] = (byte) v;

    _length += 1;

    return this;
  }

  /**
   * Append a Java boolean to the value.
   */
  @Override
  public final StringValue append(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  @Override
  public StringValue append(long v)
  {
    // XXX: this probably is frequent enough to special-case
    
    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  @Override
  public StringValue append(double v)
  {
    return append(String.valueOf(v));
  }
  
  /**
   * Append a Java value to the value.
   */
  @Override
  public final StringValue append(Value v)
  {
    v.appendTo(this);

    return this;
  }

  /**
   * Append from an input stream, using InputStream.read semantics,
   * i.e. just call is.read once even if more data is available.
   */
  public int appendRead(InputStream is, long length)
  {
    try {
      int offset = _length % SIZE;

      if (offset == 0) {
        ensureCapacity(_length + SIZE);
      }
      
      byte []buffer = _bufferList[_length / SIZE];
      int sublen = SIZE - offset;

      if (length < sublen)
        sublen = (int) length;

      sublen = is.read(buffer, 0, sublen);

      if (sublen > 0) {
        _length += sublen;

        return sublen;
      }
      else
        return -1;
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Append from an input stream, reading from the input stream until
   * end of file or the length is reached.
   */
  public int appendReadAll(InputStream is, long length)
  {
    int readLength = 0;
    
    while (length > 0) {
      int sublen = appendRead(is, length);

      if (sublen < 0)
        return readLength <= 0 ? -1 : readLength;

      length -= sublen;
      readLength += sublen;
    }

    return readLength;
  }
  
  /**
   * Append to a string builder.
   */
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    if (length() == 0)
      return sb;
    
    Env env = Env.getInstance();

    try {
      Reader reader = env.getRuntimeEncodingFactory().create(toInputStream());
      
      if (reader != null) {
        sb.append(reader);

        reader.close();
      }

      return sb;
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
  }

  //
  // Java generator code
  //

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    for (int i = 0; i < _length; i += SIZE) {
      int chunk = i / SIZE;
      int sublen = _length - i;

      if (SIZE < sublen)
        sublen = SIZE;

      env.write(_bufferList[chunk], 0, sublen);
    }
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env, WriteStream out)
  {
    try {
      for (int i = 0; i < _length; i += SIZE) {
        int chunk = i / SIZE;
        int sublen = _length - i;

        if (SIZE < sublen)
          sublen = SIZE;

        out.write(_bufferList[chunk], 0, sublen);
      }
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Serializes the value.
   */
  public void serialize(Env env, StringBuilder sb)
  {
    sb.append("s:");
    sb.append(_length);
    sb.append(":\"");
    sb.append(toString());
    sb.append("\";");
  }

  /**
   * Returns an OutputStream.
   */
  public OutputStream getOutputStream()
  {
    return new BuilderOutputStream();
  }

  private void ensureCapacity(int newCapacity)
  {
    if (newCapacity > 10000000) {
      Thread.dumpStack();
      throw new IllegalStateException();
    }
    
    int chunk = _length / SIZE;
    int endChunk = newCapacity / SIZE;

    if (_bufferList.length <= endChunk) {
      byte [][]bufferList = new byte[endChunk + 32][];
      System.arraycopy(_bufferList, 0, bufferList, 0, _bufferList.length);
      _bufferList = bufferList;
    }

    for (; chunk <= endChunk; chunk++) {
      if (_bufferList[chunk] == null)
        _bufferList[chunk] = new byte[SIZE];
    }
  }

  /**
   * Returns the hash code.
   */
  @Override
  public int hashCode()
  {
    if (_hashCode != 0)
      return _hashCode;
    
    int hash = 37;

    int length = _length;

    byte [][]bufferList = _bufferList;
    for (int i = 0; i < length; i++) {
      hash = 65521 * hash + (bufferList[i / SIZE][i % SIZE] & 0xff);
    }

    _hashCode = hash;

    return hash;
  }

  @Override
  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    int length = length();

    sb.append("string(");
    sb.append(length);
    sb.append(") \"");

    int appendLength = length > 256 ? 256 : length;

    for (int i = 0; i < appendLength; i++)
      sb.append(charAt(i));

    if (length > 256)
      sb.append(" ...");

    sb.append('"');

    return sb.toString();
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    int length = length();

    if (length < 0)
        length = 0;
    
    out.print("string(");
    out.print(length);
    out.print(") \"");

    for (int i = 0; i < length; i++) {
      int ch = charAt(i);

      out.print((char) ch);
    }

    out.print("\"");
    
    /*
    int length = length();

    if (length < 0)
        length = 0;

    out.print("string");
    
    out.print("(");
    out.print(length);
    out.print(") \"");

    for (int i = 0; i < length; i++) {
      char ch = charAt(i);

      if (0x20 <= ch && ch <= 0x7f || ch == '\t' || ch == '\r' || ch == '\n')
        out.print(ch);
      else if (ch <= 0xff)
        out.print("\\x"
        + Integer.toHexString(ch / 16) + Integer.toHexString(ch % 16));
      else {
        out.print("\\u"
                  + Integer.toHexString((ch >> 12) & 0xf)
                  + Integer.toHexString((ch >> 8) & 0xf)
                  + Integer.toHexString((ch >> 4) & 0xf)
                  + Integer.toHexString((ch) & 0xf));
      }
    }

    out.print("\"");
    */
  }

  class BuilderInputStream extends InputStream {
    private int _index;
    
    /**
     * Reads the next byte.
     */
    @Override
    public int read()
    {
      if (_index < _length)
        return charAt(_index++);
      else
        return -1;
    }

    /**
     * Reads into a buffer.
     */
    @Override
    public int read(byte []buffer, int offset, int length)
    {
      int sublen = _length - _index;

      if (length < sublen)
        sublen = length;

      if (sublen <= 0)
        return -1;

      for (int i = 0; i < sublen; i++)
        buffer[offset + i] = (byte) charAt(_index + i);

      _index += sublen;

      return sublen;
    }
  }

  class BuilderOutputStream extends OutputStream {
    /**
     * Writes the next byte.
     */
    @Override
    public void write(int ch)
    {
      append(ch);
    }

    /**
     * Reads into a buffer.
     */
    @Override
    public void write(byte []buffer, int offset, int length)
    {
      append(buffer, offset, length);
    }
  }
}

