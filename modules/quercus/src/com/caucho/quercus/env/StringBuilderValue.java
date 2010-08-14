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

import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;
import com.caucho.quercus.QuercusModuleException;

import java.io.*;
import java.util.IdentityHashMap;
import java.util.zip.CRC32;

/**
 * Represents a PHP 5 style string builder (unicode.semantics = off)
 */
public class StringBuilderValue
  extends BinaryValue
{
  public static final StringBuilderValue EMPTY = new ConstStringValue("");

  private static final StringBuilderValue []CHAR_STRINGS;
  private static final int LARGE_BUILDER_THRESHOLD
    = LargeStringBuilderValue.SIZE;

  private byte []_buffer;
  private int _length;
  private boolean _isCopy;

  private int _hashCode;

  public StringBuilderValue()
  {
    _buffer = new byte[MIN_LENGTH];
  }

  public StringBuilderValue(int capacity)
  {
    if (capacity < MIN_LENGTH)
      capacity = MIN_LENGTH;

    _buffer = new byte[capacity];
  }

  public StringBuilderValue(byte []buffer, int offset, int length)
  {
    _buffer = new byte[length];
    _length = length;

    System.arraycopy(buffer, offset, _buffer, 0, length);
  }

  public StringBuilderValue(char []buffer, int offset, int length)
  {
    _buffer = new byte[length];
    _length = length;

    for (int i = 0; i < length; i++) {
      _buffer[i] = (byte) buffer[offset + i];
    }
  }

  /**
   * Creates a new StringBuilderValue with the buffer without copying.
   */
  public StringBuilderValue(char []buffer, int length)
  {
    this(buffer, 0, length);
  }

  public StringBuilderValue(byte []buffer)
  {
    this(buffer, 0, buffer.length);
  }

  public StringBuilderValue(char ch)
  {
    _buffer = new byte[1];
    _length = 1;

    _buffer[0] = (byte) ch;
  }

  public StringBuilderValue(byte ch)
  {
    _buffer = new byte[1];
    _length = 1;

    _buffer[0] = ch;
  }

  public StringBuilderValue(String s)
  {
    int len = s.length();

    _buffer = new byte[len];
    _length = len;

    for (int i = 0; i < len; i++) {
      _buffer[i] = (byte) s.charAt(i);
    }
  }

  public StringBuilderValue(char []s)
  {
    this(s, 0, s.length);
  }

  public StringBuilderValue(char []s, Value v1)
  {
    int len = s.length;

    int bufferLength = MIN_LENGTH;
    while (bufferLength < len)
      bufferLength *= 2;

    _buffer = new byte[bufferLength];
    _length = len;

    for (int i = 0; i < len; i++) {
      _buffer[i] = (byte) s[i];
    }

    v1.appendTo(this);
  }

  public StringBuilderValue(byte []s, Value v1)
  {
    int len = s.length;

    int bufferLength = MIN_LENGTH;
    while (bufferLength < len)
      bufferLength *= 2;

    _buffer = new byte[bufferLength];
    _length = len;

    System.arraycopy(s, 0, _buffer, 0, len);

    v1.appendTo(this);
  }

  public StringBuilderValue(Value v1)
  {
    if (v1 instanceof StringBuilderValue)
      init((StringBuilderValue) v1);
    else {
      _buffer = new byte[MIN_LENGTH];

      v1.appendTo(this);
    }
  }

  public StringBuilderValue(StringBuilderValue v)
  {
    init(v);
  }

  private void init(StringBuilderValue v)
  {
    if (v._isCopy || v instanceof ConstStringValue) {
      _buffer = new byte[v._buffer.length];
      System.arraycopy(v._buffer, 0, _buffer, 0, v._length);
      _length = v._length;
    }
    else {
      _buffer = v._buffer;
      _length = v._length;
      v._isCopy = true;
    }
  }

  public StringBuilderValue(Value v1, Value v2)
  {
    _buffer = new byte[MIN_LENGTH];

    v1.appendTo(this);
    v2.appendTo(this);
  }

  public StringBuilderValue(Value v1, Value v2, Value v3)
  {
    _buffer = new byte[MIN_LENGTH];

    v1.appendTo(this);
    v2.appendTo(this);
    v3.appendTo(this);
  }

  /**
   * Creates the string.
   */
  public static StringValue create(byte value)
  {
    return CHAR_STRINGS[value & 0xFF];
  }

  /**
   * Creates the string.
   */
  public static StringValue create(char value)
  {
    return CHAR_STRINGS[value & 0xFF];
  }

  /**
   * Creates a PHP string from a Java String.
   * If the value is null then NullValue is returned.
   */
  public static Value create(String value)
  {
    if (value == null)
      return NullValue.NULL;
    else if (value.length() == 0)
      return StringBuilderValue.EMPTY;
    else
      return new StringBuilderValue(value);
  }

  /**
   * Returns the value.
   */
  public final String getValue()
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
    return getValueType(_buffer, 0, _length);
  }

  public static final ValueType getValueType(byte []buffer,
                                             int offset,
                                             int len)
  {
    if (len == 0) {
      // php/0307
      return ValueType.LONG_ADD;
    }

    int i = offset;
    int ch = 0;

    while (i < len && Character.isWhitespace(buffer[i])) {
      i++;
    }

    if (i + 1 < len && buffer[i] == '0' && buffer[i + 1] == 'x')
      return ValueType.LONG_EQ;

    if (i < len && ((ch = buffer[i]) == '+' || ch == '-')) {
      i++;
    }

    if (len <= i)
      return ValueType.STRING;

    ch = buffer[i];

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
        return ValueType.DOUBLE_CMP;
      }

      return ValueType.STRING;
    }
    else if (! ('0' <= ch && ch <= '9'))
      return ValueType.STRING;

    for (; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
    }

    while (i < len && Character.isWhitespace(buffer[i])) {
      i++;
    }

    if (len <= i)
      return ValueType.LONG_EQ;
    else if (ch == '.' || ch == 'e' || ch == 'E') {
      for (i++;
           i < len
           && ('0' <= (ch = buffer[i]) && ch <= '9'
               || ch == '+'
               || ch == '-'
               || ch == 'e'
               || ch == 'E');
           i++) {
      }

      while (i < len && Character.isWhitespace(buffer[i])) {
        i++;
      }

      if (i < len)
        return ValueType.STRING;
      else
        return ValueType.DOUBLE_CMP;
    }
    else
      return ValueType.STRING;
  }

  /**
   * Returns true for a scalar
   */
  @Override
  public final boolean isScalar()
  {
    return true;
  }

  /*
   * Returns true if this is a PHP5 string.
   */
  @Override
  public boolean isPHP5String()
  {
    return true;
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public final boolean toBoolean()
  {
    if (_length == 0)
      return false;
    else if (_length == 1 && _buffer[0] == '0')
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
    return parseLong(_buffer, 0, _length);
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toDouble(_buffer, 0, _length);
  }

  public static final double toDouble(byte []buffer, int offset, int len)
  {
    int start = offset;
    int i = offset;
    int ch = 0;

    while (i < len && Character.isWhitespace(buffer[i])) {
      start++;
      i++;
    }

    int end = offset + len;

    if (offset + 1 < end && buffer[offset] == '0'
        && ((ch = buffer[offset + 1]) == 'x' || ch == 'X')) {

      double value = 0;

      for (offset += 2; offset < end; offset++) {
        ch = buffer[offset] & 0xFF;

        if ('0' <= ch && ch <= '9')
          value = value * 16 + ch - '0';
        else if ('a' <= ch && ch <= 'z')
          value = value * 16 + ch - 'a' + 10;
        else if ('A' <= ch && ch <= 'Z')
          value = value * 16 + ch - 'A' + 10;
        else
          return value;
      }

      return value;
    }

    if (i < len && ((ch = buffer[i]) == '+' || ch == '-')) {
      i++;
    }

    for (; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
    }

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
      }

      if (i == 1)
        return 0;
    }

    if (ch == 'e' || ch == 'E') {
      int e = i++;

      if (i < len && (ch = buffer[i]) == '+' || ch == '-') {
        i++;
      }

      for (; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
      }

      if (i == e + 1)
        i = e;
    }

    if (i == 0)
      return 0;

    try {
      return Double.parseDouble(new String(buffer, start, i - start));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * Convert to an input stream.
   */
  @Override
  public final InputStream toInputStream()
  {
    return new BuilderInputStream();
  }

  /**
   * Converts to a string.
   */
  @Override
  public String toString()
  {
    if (_length == 1)
      return String.valueOf((char) (_buffer[0] & 0xFF));
    else {
      CharBuffer buf = CharBuffer.allocate();
      buf.append(_buffer, 0, _length);

      String str = buf.toString();
      buf.free();

      return str;
    }
  }

  /**
   * Converts to a BinaryValue.
   */
  @Override
  public final StringValue toBinaryValue(Env env)
  {
    return this;
  }

  /**
   * Converts to a BinaryValue in desired charset.
   */
  @Override
  public final StringValue toBinaryValue(String charset)
  {
    return this;
  }

  /**
   * Converts to a UnicodeValue.
   */
  @Override
  public StringValue toUnicodeValue()
  {
    // php/0c94
    return new UnicodeBuilderValue().append(getBuffer(), 0, length());
  }

  /**
   * Converts to a UnicodeValue.
   */
  @Override
  public StringValue toUnicodeValue(Env env)
  {
    return toUnicodeValue();
  }

  /**
   * Converts to a UnicodeValue in desired charset.
   */
  @Override
  public StringValue toUnicodeValue(Env env, String charset)
  {
    return toUnicodeValue();
  }

  /**
   * Converts to an object.
   */
  @Override
  public final Object toJavaObject()
  {
    return toString();
  }

  /**
   * Returns true if the value is empty.
   */
  @Override
  public final boolean isEmpty()
  {
    return _length == 0 || _length == 1 && _buffer[0] == '0';
  }

  /**
   * Writes to a stream
   */
  @Override
  public final void writeTo(OutputStream os)
  {
    try {
      os.write(_buffer, 0, _length);

    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(StringBuilderValue bb)
  {
    bb.append(_buffer, 0, _length);

    return bb;
  }

  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue bb)
  {
    bb.append(_buffer, 0, _length);

    return bb;
  }

  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(LargeStringBuilderValue bb)
  {
    bb.append(_buffer, 0, _length);

    return bb;
  }


  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(BinaryBuilderValue bb)
  {
    bb.append(_buffer, 0, _length);

    return bb;
  }

  /**
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    byte []buffer = _buffer;
    int len = _length;

    if (len == 0)
      return this;

    int sign = 1;
    long value = 0;

    int i = 0;
    int ch = buffer[i];
    if (ch == '-') {
      sign = -1;
      i++;
    }

    for (; i < len; i++) {
      ch = buffer[i];

      if ('0' <= ch && ch <= '9')
        value = 10 * value + ch - '0';
      else
        return this;
    }

    return LongValue.create(sign * value);
  }

  /**
   * Converts to a byte array, with no consideration of character encoding.
   * Each character becomes one byte, characters with values above 255 are
   * not correctly preserved.
   */
  public final byte[] toBytes()
  {
    byte[] bytes = new byte[_length];

    System.arraycopy(_buffer, 0, bytes, 0, _length);

    return bytes;
  }

  //
  // Operations
  //

  /**
   * Returns the character at an index
   */
  public final Value get(Value key)
  {
    return charValueAt(key.toLong());
  }

  /**
   * Sets the array ref.
   */
  @Override
  public Value put(Value index, Value value)
  {
    setCharValueAt(index.toLong(), value);

    return value;
  }

  /**
   * Sets the array ref.
   */
  @Override
  public Value append(Value index, Value value)
  {
    if (_length > 0)
      return setCharValueAt(index.toLong(), value);
    else
      return new ArrayValueImpl().append(index, value);
  }

  /**
   * sets the character at an index
   */
  /*
  public Value setCharAt(long index, String value)
  {
    int len = _length;

    if (index < 0 || len <= index)
      return this;
    else {
      StringBuilderValue sb = new StringBuilderValue(_buffer, 0, (int) index);
      sb.append(value);
      sb.append(_buffer, (int) (index + 1), (int) (len - index - 1));

      return sb;
    }
  }
  */

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
  public final char charAt(int index)
  {
    if (index < 0 || _length <= index)
      return 0;
    else
      return (char) (_buffer[index] & 0xff);
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
      byte ch = _buffer[(int) index];

      return CHAR_STRINGS[ch & 0xff];
    }
  }

  /**
   * sets the character at an index
   */
  @Override
  public Value setCharValueAt(long indexL, Value value)
  {
    int len = _length;

    if (indexL < 0)
      return this;
    else if (indexL < len) {
      StringBuilderValue sb = createStringBuilder(_buffer, 0, len);

      StringValue str = value.toStringValue();

      int index = (int) indexL;

      if (value.length() == 0)
        sb._buffer[index] = 0;
      else
        sb._buffer[index] = (byte) str.charAt(0);

      return sb;
    }
    else {
      // php/03mg, #2940

      int index = (int) indexL;

      StringBuilderValue sb = (StringBuilderValue) copyStringBuilder();

      if (sb._buffer.length < index + 1)
        sb.ensureCapacity(index + 1);

      int padLen = index - len;

      for (int i = 0; i <= padLen; i++) {
         sb._buffer[sb._length++] = ' ';
      }

      StringValue str = value.toStringValue();

      if (value.length() == 0)
        sb._buffer[index] = 0;
      else
        sb._buffer[index] = (byte) str.charAt(0);

      return sb;
    }
  }

  /**
   * Returns the first index of the match string, starting from the head.
   */
  public int indexOf(char match)
  {
    final int length = _length;
    final byte []buffer = _buffer;

    for (int head = 0; head < length; head++) {
      if (buffer[head] == match)
        return head;
    }

    return -1;
  }

  /**
   * Returns the last index of the match string, starting from the head.
   */
  @Override
  public int indexOf(char match, int head)
  {
    int length = _length;
    byte []buffer = _buffer;

    for (; head < length; head++) {
      if (buffer[head] == match)
        return head;
    }

    return -1;
  }

  /**
   * Returns a subsequence
   */
  @Override
  public CharSequence subSequence(int start, int end)
  {
    if (end <= start)
      return StringBuilderValue.EMPTY;
    else if (end - start == 1)
      return CHAR_STRINGS[_buffer[start] & 0xff];

    return createStringBuilder(_buffer, start, end - start);
  }

  /**
   * Returns a subsequence
   */
  @Override
  public String stringSubstring(int start, int end)
  {
    if (end <= start)
      return "";

    CharBuffer buf = CharBuffer.allocate();
    buf.append(_buffer, start, end - start);

    String str = buf.toString();
    buf.free();

    return str;
  }

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toLowerCase()
  {
    int length = _length;

    StringBuilderValue string = createStringBuilder(length);

    byte []srcBuffer = _buffer;
    byte []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      byte ch = srcBuffer[i];

      if ('A' <= ch && ch <= 'Z')
        dstBuffer[i] = (byte) (ch + 'a' - 'A');
      else
        dstBuffer[i] = ch;
    }

    string._length = length;

    return string;
  }

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toUpperCase()
  {
    int length = _length;

    StringBuilderValue string = createStringBuilder(_length);

    byte []srcBuffer = _buffer;
    byte []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      byte ch = srcBuffer[i];

      if ('a' <= ch && ch <= 'z')
        dstBuffer[i] = (byte) (ch + 'A' - 'a');
      else
        dstBuffer[i] = ch;
    }

    string._length = length;

    return string;
  }

  /**
   * Returns true if the region matches
   */
  public boolean regionMatches(int offset,
                               char []mBuffer,
                               int mOffset,
                               int mLength)
  {
    int length = _length;

    if (length < offset + mLength) {
      return false;
    }

    byte []buffer = _buffer;

    for (int i = 0; i < mLength; i++) {
      if ((buffer[offset + i] & 0xFF) != mBuffer[mOffset + i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns true if the region matches
   */
  public boolean regionMatchesIgnoreCase(int offset,
                                                     char []mBuffer,
                                                     int mOffset,
                                                     int mLength)
  {
    int length = _length;

    if (length < offset + mLength)
      return false;

    byte []buffer = _buffer;

    for (int i = 0; i < mLength; i++) {
      int a = buffer[offset + i] & 0xFF;
      char b = mBuffer[mOffset + i];

      if ('A' <= a && a <= 'Z')
        a += 'a' - 'A';

      if ('A' <= b && b <= 'Z')
        b += 'a' - 'A';

      if (a != b)
        return false;
    }

    return true;
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringBuilderValue createStringBuilder()
  {
    return new StringBuilderValue();
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringBuilderValue createStringBuilder(int length)
  {
    return new StringBuilderValue(length);
  }

  /**
   * Creates a string builder of the same type.
   */
  public StringBuilderValue
    createStringBuilder(byte []buffer, int offset, int length)
  {
    return new StringBuilderValue(buffer, offset, length);
  }

  /**
   * Converts to a string builder
   */
  public StringValue copyStringBuilder()
  {
    return new StringBuilderValue(this);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    return new StringBuilderValue(this);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    if (_length >= LARGE_BUILDER_THRESHOLD)
      return new LargeStringBuilderValue(this);
    else
      return new StringBuilderValue(this);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env, Value value)
  {
    if (_length + value.length() >= LARGE_BUILDER_THRESHOLD) {
      LargeStringBuilderValue v = new LargeStringBuilderValue(this);

      value.appendTo(v);

      return v;
    }
    else {
      StringBuilderValue v = new StringBuilderValue(this);

      value.appendTo(v);

      return v;
    }
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env, StringValue value)
  {
    if (_length + value.length() >= LARGE_BUILDER_THRESHOLD) {
      LargeStringBuilderValue v = new LargeStringBuilderValue(this);

      value.appendTo(v);

      return v;
    }
    else {
      StringBuilderValue v = new StringBuilderValue(this);

      value.appendTo(v);

      return v;
    }
  }

  //
  // append code
  //

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue append(String s)
  {
    int sublen = s.length();

    if (_buffer.length < _length + sublen)
      ensureCapacity(_length + sublen);

    byte []buffer = _buffer;
    int length = _length;

    for (int i = 0; i < sublen; i++) {
      buffer[length + i] = (byte) s.charAt(i);
    }

    _length = length + sublen;

    return this;
  }

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue append(String s, int start, int end)
  {
    int sublen = end - start;

    if (_buffer.length < _length + sublen)
      ensureCapacity(_length + sublen);

    byte []buffer = _buffer;
    int length = _length;

    for (; start < end; start++)
      buffer[length++] = (byte) s.charAt(start);

    _length = length;

    return this;
  }

  /**
   * Append a Java char to the value.
   */
  @Override
  public final StringValue append(char ch)
  {
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = (byte) ch;

    return this;
  }

  @Override
  public final void write(int ch)
  {
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = (byte) ch;
  }


  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(char []buf, int offset, int length)
  {
    int end = _length + length;

    if (_buffer.length < end)
      ensureCapacity(end);

    byte []buffer = _buffer;
    int bufferLength = _length;

    for (int i = 0; i < length; i++) {
      buffer[bufferLength + i] = (byte) buf[offset + i];
    }

    _length = end;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(char []buf)
  {
    int length = buf.length;

    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    byte []buffer = _buffer;
    int bufferLength = _length;

    for (int i = 0; i < length; i++)
      buffer[bufferLength++] = (byte) buf[i];

    _buffer = buffer;
    _length = bufferLength;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public StringValue appendUnicode(char []buf)
  {
    int length = buf.length;

    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    byte []buffer = _buffer;
    int bufferLength = _length;

    for (int i = 0; i < length; i++)
      buffer[bufferLength++] = (byte) buf[i];

    _buffer = buffer;
    _length = bufferLength;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public StringValue appendUnicode(char []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    byte []buffer = _buffer;
    int bufferLength = _length;

    for (; length > 0; length--)
      buffer[bufferLength++] = (byte) buf[offset++];

    _buffer = buffer;
    _length = bufferLength;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue append(CharSequence buf, int head, int tail)
  {
    int length = tail - head;

    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    if (buf instanceof StringBuilderValue) {
      StringBuilderValue sb = (StringBuilderValue) buf;

      System.arraycopy(sb._buffer, head, _buffer, _length, length);

      _length += length;

      return this;
    }
    else {
      byte []buffer = _buffer;
      int bufferLength = _length;

      for (; head < tail; head++) {
        buffer[bufferLength++] = (byte) buf.charAt(head);
      }

      _length = bufferLength;

      return this;
    }
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public StringValue append(StringBuilderValue sb, int head, int tail)
  {
    int length = tail - head;

    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    System.arraycopy(sb._buffer, head, _buffer, _length, length);

    _length += length;

    return this;
  }

  /**
   * Append a Java value to the value.
   */
  @Override
  public final StringValue append(Value v)
  {
    /*
    if (v.length() == 0)
      return this;
    else {
      // php/033a
      v.appendTo(this);

      return this;
    }
    */

    v.appendTo(this);

    return this;
  }

  /**
   * Returns the first index of the match string, starting from the head.
   */
  @Override
  public final int indexOf(CharSequence match, int head)
  {
    final int matchLength = match.length();

    if (matchLength <= 0)
      return -1;
    else if (head < 0)
      return -1;

    final int length = _length;
    final int end = length - matchLength;
    final char first = match.charAt(0);
    final byte []buffer = _buffer;

    loop:
    for (; head <= end; head++) {
      if (buffer[head] != first)
        continue;

      for (int i = 1; i < matchLength; i++) {
        if (buffer[head + i] != match.charAt(i))
          continue loop;
      }

      return head;
    }

    return -1;
  }

  /**
   * Append a Java value to the value.
   */
  @Override
  public StringValue appendUnicode(Value v)
  {
    v.appendTo(this);

    return this;
  }

  /**
   * Append a Java value to the value.
   */
  @Override
  public StringValue appendUnicode(Value v1, Value v2)
  {
    v1.appendTo(this);
    v2.appendTo(this);

    return this;
  }

  /**
   * Append a buffer to the value.
   */
  public final StringValue append(byte []buf, int offset, int length)
  {
    int end = _length + length;

    if (_buffer.length < end)
      ensureCapacity(end);

    System.arraycopy(buf, offset, _buffer, _length, length);

    _length = end;

    return this;
  }

  @Override
  public final void write(byte []buf, int offset, int length)
  {
    append(buf, offset, length);
  }

  /**
   * Append a double to the value.
   */
  public final StringValue append(byte []buf)
  {
    return append(buf, 0, buf.length);
  }

  /**
   * Append a buffer to the value.
   */
  @Override
  public final StringValue appendUtf8(byte []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    byte []charBuffer = _buffer;
    int charLength = _length;

    int end = offset + length;

    while (offset < end) {
      int ch = buf[offset++] & 0xff;

      if (ch < 0x80)
        charBuffer[charLength++] = (byte) ch;
      else if (ch < 0xe0) {
        int ch2 = buf[offset++] & 0xff;

        int v = (char) (((ch & 0x1f) << 6) + (ch2 & 0x3f));

        charBuffer[charLength++] = (byte) (v & 0xff);
      }
      else {
        int ch2 = buf[offset++] & 0xff;
        int ch3 = buf[offset++] & 0xff;

        byte v = (byte) (((ch & 0xf) << 12)
                         + ((ch2 & 0x3f) << 6)
                         + ((ch3) << 6));

        charBuffer[charLength++] = v;
      }
    }

    _length = charLength;

    return this;
  }

  /**
   * Append a Java byte to the value without conversions.
   */
  @Override
  public final StringValue appendByte(int v)
  {
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = (byte) v;

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
   * Append a bytes to the value.
   */
  @Override
  public StringValue appendBytes(String s)
  {
    int sublen = s.length();

    if (_buffer.length < _length + sublen)
      ensureCapacity(_length + sublen);

    for (int i = 0; i < sublen; i++) {
      _buffer[_length++] = (byte) s.charAt(i);
    }

    return this;
  }

  /**
   * Append Java bytes to the value without conversions.
   */
  @Override
  public final StringValue appendBytes(byte []bytes, int offset, int end)
  {
    int len = end - offset;

    if (_buffer.length < _length + len)
      ensureCapacity(_length + len);

    System.arraycopy(bytes, offset, _buffer, _length, len);

    _length += len;

    return this;
  }

  @Override
  public StringValue append(Reader reader, long length)
    throws IOException
  {
    // php/4407 - oracle clob callback passes very long length

    TempCharBuffer tempBuf = TempCharBuffer.allocate();

    char []buffer = tempBuf.getBuffer();

    int sublen = (int) Math.min(buffer.length, length);

    try {
      while (length > 0) {
        if (_buffer.length < _length + sublen)
          ensureCapacity(_length + sublen);

        int count = reader.read(buffer, 0, sublen);

        if (count <= 0)
          break;

        append(buffer, 0, count);

        length -= count;
      }

    } catch (IOException e) {
      throw new QuercusModuleException(e);
    } finally {
      TempCharBuffer.free(tempBuf);
    }

    return this;
  }

  /**
   * Returns the buffer.
   */
  public final byte []getBuffer()
  {
    return _buffer;
  }

  /**
   * Returns the offset.
   */
  public int getOffset()
  {
    return _length;
  }

  /**
   * Sets the offset.
   */
  public void setOffset(int offset)
  {
    _length = offset;
  }

  /**
   * Returns the current capacity.
   */
  public int getBufferLength()
  {
    return _buffer.length;
  }

  /**
   * Return true if the array value is set
   */
  public boolean isset(Value indexV)
  {
    int index = indexV.toInt();

    return 0 <= index && index < _length;
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
    env.write(_buffer, 0, _length);
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env, WriteStream out)
  {
    try {
      out.write(_buffer, 0, _length);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
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

    for (int i = 0; i < _length; i++) {
      sb.append((char) (_buffer[i] & 0xFF));
    }

    sb.append("\";");
  }

  @Override
  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    int length = length();

    sb.append("binary(");
    sb.append(length);
    sb.append(") \"");

    int appendLength = length < 256 ? length : 256;

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

    out.write(_buffer, 0, _length);

    out.print("\"");
  }

  /**
   * Returns an OutputStream.
   */
  public OutputStream getOutputStream()
  {
    return new BuilderOutputStream();
  }

  /**
   * Calculates CRC32 value.
   */
  @Override
  public long getCrc32Value()
  {
    CRC32 crc = new CRC32();

    crc.update(_buffer, 0, _length);

    return crc.getValue() & 0xffffffff;
  }

  public void ensureAppendCapacity(int newCapacity)
  {
    ensureCapacity(_length + newCapacity);
  }

  protected void ensureCapacity(int newCapacity)
  {
    int bufferLength = _buffer.length;

    if (newCapacity <= bufferLength)
      return;

    if (bufferLength < MIN_LENGTH)
      bufferLength = MIN_LENGTH;

    while (bufferLength <= newCapacity)
      bufferLength = 2 * bufferLength;

    byte []buffer = new byte[bufferLength];
    System.arraycopy(_buffer, 0, buffer, 0, _length);
    _buffer = buffer;
    _isCopy = false;
  }

  /**
   * Returns the hash code.
   */
  @Override
  public int hashCode()
  {
    int hash = _hashCode;

    if (hash != 0)
      return hash;

    hash = 37;

    int length = _length;
    byte []buffer = _buffer;

    if (length > 256) {
      for (int i = 127; i >= 0; i--) {
        hash = 65521 * hash + buffer[i];
      }

      for (int i = length - 128; i < length; i++) {
        hash = 65521 * hash + buffer[i];
      }

      _hashCode = hash;

      return hash;
    }

    for (int i = length - 1; i >= 0; i--) {
      hash = 65521 * hash + buffer[i];
    }

    _hashCode = hash;

    return hash;
  }

  /**
   * Returns the hash code.
   */
  @Override
  public int hashCodeCaseInsensitive()
  {
    int hash = 0;

    if (hash != 0)
      return hash;

    hash = 37;

    int length = _length;
    byte []buffer = _buffer;

    if (length > 256) {
      for (int i = 127; i >= 0; i--) {
        hash = 65521 * hash + toLower(buffer[i]);
      }

      for (int i = length - 128; i < length; i++) {
        hash = 65521 * hash + toLower(buffer[i]);
      }

      _hashCode = hash;

      return hash;
    }

    for (int i = length - 1; i >= 0; i--) {
      int ch = toLower(buffer[i]);
      
      hash = 65521 * hash + ch;
    }

    return hash;
  }
  
  private int toLower(int ch)
  {
    if ('A' <= ch && ch <= 'Z')
      return ch + 'a' - 'A';
    else
      return ch;
  }

  @Override
  public int getHashCode()
  {
    return hashCode();
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    rValue = rValue.toValue();

    ValueType typeB = rValue.getValueType();

    if (typeB.isNumber()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }
    else if (typeB.isBoolean()) {
      return toBoolean() == rValue.toBoolean();
    }

    ValueType typeA = getValueType();
    if (typeA.isNumberCmp() && typeB.isNumberCmp()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }

    if (rValue instanceof StringBuilderValue) {
      StringBuilderValue value = (StringBuilderValue) rValue;

      int length = _length;

      if (length != value._length)
        return false;

      byte []bufferA = _buffer;
      byte []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    else {
      String rString = rValue.toString();

      int len = rString.length();

      if (_length != len)
        return false;

      for (int i = len - 1; i >= 0; i--) {
        if (_buffer[i] != rString.charAt(i))
          return false;
      }

      return true;
    }
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this)
      return true;

    if (o instanceof StringBuilderValue) {
      StringBuilderValue value = (StringBuilderValue) o;

      int length = _length;

      if (length != value._length)
        return false;

      byte []bufferA = _buffer;
      byte []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    else if (o instanceof LargeStringBuilderValue) {
      StringValue value = (StringValue) o;

      int length = _length;
      int lengthB = value.length();

      if (length != lengthB)
        return false;

      byte []bufferA = _buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != value.charAt(i))
          return false;
      }

      return true;
    }
    /*
    else if (o instanceof UnicodeValue) {
      UnicodeValue value = (UnicodeValue)o;

      return value.equals(this);
    }
    */
    else
      return false;
  }

  @Override
  public boolean eql(Value o)
  {
    o = o.toValue();

    if (o == this)
      return true;

    if (o instanceof StringBuilderValue) {
      StringBuilderValue value = (StringBuilderValue) o;

      int length = _length;

      if (length != value._length)
        return false;

      byte []bufferA = _buffer;
      byte []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    else
      return false;
  }

  //
  // Java serialization code
  //

  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.writeInt(_length);

    out.write(_buffer, 0, _length);
  }

  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  {
    _length = in.readInt();
    _buffer = new byte[_length];

    in.read(_buffer, 0, _length);
  }

  class BinaryInputStream extends InputStream {
    private int _offset;

    /**
     * Reads the next byte.
     */
    @Override
    public int read()
    {
      if (_offset < _length)
        return _buffer[_offset++] & 0xFF;
      else
        return -1;
    }

    /**
     * Reads into a buffer.
     */
    @Override
    public int read(byte []buffer, int offset, int length)
    {
      int sublen = Math.min(_length - _offset, length);

      if (sublen <= 0)
        return -1;

      System.arraycopy(_buffer, _offset, buffer, offset, sublen);

      _offset += sublen;

      return sublen;
    }
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
        return _buffer[_index++] & 0xFF;
      else
        return -1;
    }

    /**
     * Reads into a buffer.
     */
    @Override
    public int read(byte []buffer, int offset, int length)
    {
      int sublen = Math.min(_length - _index, length);

      if (sublen <= 0)
        return -1;

      System.arraycopy(_buffer, _index, buffer, offset, sublen);

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
      appendByte(ch);
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

  static {
    CHAR_STRINGS = new ConstStringValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++) {
      CHAR_STRINGS[i] = new ConstStringValue((char) i);
    }
  }
}

