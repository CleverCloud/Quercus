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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.vfs.*;

import java.io.*;

/**
 * Represents a PHP string value.
 */
public class UnicodeBuilderValue
  extends UnicodeValue
{
  public static final UnicodeBuilderValue EMPTY = new UnicodeBuilderValue("");

  private static final UnicodeBuilderValue []CHAR_STRINGS;

  private char []_buffer;
  private int _length;

  protected boolean _isCopy;
  private int _hashCode;

  public UnicodeBuilderValue()
  {
    _buffer = new char[MIN_LENGTH];
  }

  public UnicodeBuilderValue(int capacity)
  {
    if (capacity < MIN_LENGTH)
      capacity = MIN_LENGTH;

    _buffer = new char[capacity];
  }

  public UnicodeBuilderValue(String s)
  {
    int len = s.length();

    _buffer = new char[len];
    _length = len;

    s.getChars(0, len, _buffer, 0);
  }

  public UnicodeBuilderValue(char []buffer, int offset, int length)
  {
    _buffer = new char[length];
    _length = length;

    System.arraycopy(buffer, offset, _buffer, 0, length);
  }

  public UnicodeBuilderValue(char []buffer)
  {
    this(buffer, 0, buffer.length);
  }

  public UnicodeBuilderValue(char []buffer, int length)
  {
    this(buffer, 0, length);
  }

  public UnicodeBuilderValue(Character []buffer)
  {
    int length = buffer.length;

    _buffer =  new char[length];
    _length = length;

    for (int i = 0; i < length; i++) {
      _buffer[i] = buffer[i].charValue();
    }
  }

  public UnicodeBuilderValue(char ch)
  {
    _buffer = new char[1];
    _length = 1;

    _buffer[0] = ch;
  }

  public UnicodeBuilderValue(char []s, Value v1)
  {
    int len = s.length;

    int bufferLength = MIN_LENGTH;
    while (bufferLength < len)
      bufferLength *= 2;

    _buffer = new char[bufferLength];

    _length = len;

    System.arraycopy(s, 0, _buffer, 0, len);

    v1.appendTo(this);
  }

  public UnicodeBuilderValue(Value v1)
  {
    _buffer = new char[MIN_LENGTH];

    v1.appendTo(this);
  }

  public UnicodeBuilderValue(UnicodeBuilderValue v)
  {
    if (v._isCopy) {
      _buffer = new char[v._buffer.length];
      System.arraycopy(v._buffer, 0, _buffer, 0, v._length);
      _length = v._length;
    }
    else {
      _buffer = v._buffer;
      _length = v._length;
      v._isCopy = true;
    }
  }

  public UnicodeBuilderValue(StringBuilderValue v, boolean isCopy)
  {
    byte []vBuffer = v.getBuffer();
    int vOffset = v.getOffset();
    
    _buffer = new char[vBuffer.length];
    
    System.arraycopy(vBuffer, 0, _buffer, 0, vOffset);
    
    _length = vOffset;
  }

  /**
   * Creates the string.
   */
  public static StringValue create(char value)
  {
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new UnicodeBuilderValue(value);
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
      return UnicodeBuilderValue.EMPTY;
    else
      return new UnicodeBuilderValue(value);
  }

  /**
   * Decodes the Unicode str from charset.
   *
   * @param str should be a Unicode string
   * @param charset to decode string from
   */
  @Override
  public StringValue create(Env env, StringValue str, String charset)
  {
    return str;
  }

  /**
   * Decodes from charset and returns UnicodeValue.
   *
   * @param env
   * @param charset
   */
  public final StringValue convertToUnicode(Env env, String charset)
  {
    return this;
  }

  /**
   * Returns true for UnicodeValue
   */
  @Override
  public final boolean isUnicode()
  {
    return true;
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

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    return new UnicodeBuilderValue(this);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return new UnicodeBuilderValue(this);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env, Value value)
  {
    UnicodeBuilderValue v = new UnicodeBuilderValue(this);

    value.appendTo(v);

    return v;
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue copyStringBuilder()
  {
    return new UnicodeBuilderValue(this);
  }

  /**
   * Converts to a UnicodeValue.
   */
  @Override
  public final StringValue toUnicodeValue()
  {
    return this;
  }

  /**
   * Converts to a UnicodeValue.
   */
  @Override
  public final StringValue toUnicodeValue(Env env)
  {
    return this;
  }

  /**
   * Converts to a UnicodeValue in desired charset.
   */
  @Override
  public final StringValue toUnicodeValue(Env env, String charset)
  {
    return this;
  }

  /**
   * Append a buffer to the value.
   */
  public final StringValue append(byte []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    /*
    Env env = Env.getInstance();
    String charset = (env != null
                      ? env.getRuntimeEncoding().toString()
                      : null);
    */

    char []charBuffer = _buffer;
    int charLength = _length;

    for (int i = 0; i < length; i++) {
      // php/3jdf
      charBuffer[charLength + i] = (char) (buf[offset + i] & 0xff);
    }

    _length += length;

    return this;
  }

  //
  // append code
  //

  /**
   * Append a Java value to the value.
   */
  public StringValue append(Value v)
  {
    v.appendTo(this);

    return this;
  }

  /**
   * Append a Java string to the value.
   */
  public StringValue append(String s)
  {
    int len = s.length();

    if (_buffer.length < _length + len)
      ensureCapacity(_length + len);

    s.getChars(0, len, _buffer, _length);

    _length += len;

    return this;
  }

  /**
   * Append a Java string to the value.
   */
  public StringValue append(String s, int start, int end)
  {
    int len = Math.min(s.length(), end - start);

    if (_buffer.length < _length + len)
      ensureCapacity(_length + len);

    s.getChars(start, start + len, _buffer, _length);

    _length += len;

    return this;
  }

  /**
   * Append a Java char to the value.
   */
  public StringValue append(char v)
  {
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = v;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(char []buf, int offset, int length)
  {
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    System.arraycopy(buf, offset, _buffer, _length, length);

    _length += length;

    return this;
  }

  /**
   * Append a Java double to the value.
   */
  public StringValue append(char []buf)
  {
    return append(buf, 0, buf.length);
  }

  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(CharSequence buf, int head, int tail)
  {
    int len = tail - head;

    if (_buffer.length < _length + len)
      ensureAppendCapacity(len);

    char []buffer = _buffer;
    int bufferLength = _length;

    for (; head < tail; head++) {
      buffer[bufferLength++] = buf.charAt(head);
    }

    _length = bufferLength;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(UnicodeBuilderValue sb, int head, int tail)
  {
    int len = tail - head;

    if (_buffer.length < _length + len)
      ensureAppendCapacity(len);

    System.arraycopy(sb._buffer, head, _buffer, _length, len);

    _length += len;

    return this;
  }


  /*
   * Appends a Unicode string to the value.
   *
   * @param str should be a Unicode string
   * @param charset to decode string from
   */
  @Override
  public StringValue append(Env env, StringValue unicodeStr, String charset)
  {
    return append(unicodeStr);
  }

  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    sb.append(_buffer, 0, _length);

    return sb;
  }

  @Override
  public StringValue append(Reader reader, long length)
    throws IOException
  {
    // php/4407 - oracle clob callback passes very long length

    int sublen = (int) Math.min(_length, length);

    try {
      while (length > 0) {
        if (_buffer.length < _length + sublen)
          ensureCapacity(_length + sublen);

        int count = reader.read(_buffer, _length, sublen);

        if (count <= 0)
          break;

        length -= count;
        _length += count;
      }

    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }

    return this;
  }

  /**
   * Append a Java byte to the value without conversions.
   */
  public StringValue appendByte(int v)
  {
    if (_buffer.length < _length + 1)
      ensureCapacity(_length + 1);

    _buffer[_length++] = (char) v;

    return this;
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
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    char []buffer = _buffer;
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

    for (int i = 0; i < _length; i++) {
      bytes[i] = (byte) (_buffer[i] & 0xFF);
    }

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
   * Returns the buffer.
   */
  public final char []getBuffer()
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
  public int getLength()
  {
    return _buffer.length;
  }

  /**
   * Converts to a BinaryValue.
   */
  @Override
  public StringValue toBinaryValue()
  {
    return toBinaryValue(Env.getInstance());
  }

  /**
   * Converts to a BinaryValue.
   */
  @Override
  public StringValue toBinaryValue(Env env)
  {
    return toBinaryValue(env.getRuntimeEncoding());
  }

  /**
   * Converts to a BinaryValue in desired charset.
   *
   * @param env
   * @param charset
   */
  @Override
  public StringValue toBinaryValue(String charset)
  {
    try {
      BinaryBuilderValue result = new BinaryBuilderValue();
      BinaryBuilderStream stream = new BinaryBuilderStream(result);

      // XXX: can use EncodingWriter directly(?)
      WriteStream out = new WriteStream(stream);
      out.setEncoding(charset);

      out.print(_buffer, 0, _length);

      out.close();

      return result;
    } catch (IOException e) {
      throw new QuercusModuleException(e.getMessage());
    }
  }

  /**
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
  {
    int len = _length;

    if (index < 0 || len <= index)
      return UnsetUnicodeValue.UNSET;
    else {
      int ch = _buffer[(int) index];

      if (ch < CHAR_STRINGS.length)
        return CHAR_STRINGS[ch];
      else
        return new UnicodeBuilderValue((char) ch);
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
      UnicodeBuilderValue sb = new UnicodeBuilderValue(_buffer, 0, len);

      StringValue str = value.toStringValue();

      int index = (int) indexL;

      if (value.length() == 0)
        sb._buffer[index] = 0;
      else
        sb._buffer[index] = str.charAt(0);

      return sb;
    }
    else {
      int index = (int) indexL;

      UnicodeBuilderValue sb = (UnicodeBuilderValue) copyStringBuilder();

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
        sb._buffer[index] = str.charAt(0);

      return sb;
    }
  }

  //
  // CharSequence
  //

  /**
   * Returns the length of the string.
   */
  public int length()
  {
    return _length;
  }

  /**
   * Returns the character at a particular location
   */
  public char charAt(int index)
  {
    return _buffer[index];
  }

  /**
   * Returns a subsequence
   */
  @Override
  public CharSequence subSequence(int start, int end)
  {
    int len = end - start;

    if (len == 0)
      return EMPTY;

    UnicodeBuilderValue sb = new UnicodeBuilderValue(len);

    sb.append(_buffer, start, len);

    return sb;
  }

  //
  // java.lang.String
  //

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toLowerCase()
  {
    int length = _length;

    UnicodeBuilderValue string = new UnicodeBuilderValue(length);

    char []srcBuffer = _buffer;
    char []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      char ch = srcBuffer[i];

      if ('A' <= ch && ch <= 'Z')
        dstBuffer[i] = (char) (ch + 'a' - 'A');
      else if (ch < 0x80)
        dstBuffer[i] = ch;
      else if (Character.isUpperCase(ch))
        dstBuffer[i] = Character.toLowerCase(ch);
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

    UnicodeBuilderValue string = new UnicodeBuilderValue(_length);

    char []srcBuffer = _buffer;
    char []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      char ch = srcBuffer[i];

      if ('a' <= ch && ch <= 'z')
        dstBuffer[i] = (char) (ch + 'A' - 'a');
      else if (ch < 0x80)
        dstBuffer[i] = ch;
      else if (Character.isLowerCase(ch))
        dstBuffer[i] = Character.toUpperCase(ch);
      else
        dstBuffer[i] = ch;
    }

    string._length = length;

    return string;
  }

  /**
   * Returns a character array
   */
  @Override
  public char []toCharArray()
  {
    char[] dest = new char[_length];

    System.arraycopy(_buffer, 0, dest, 0, _length);

    return dest;
  }

  /**
   * Return the underlying buffer.
   */
  @Override
  public char []getRawCharArray()
  {
    return _buffer;
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    env.print(_buffer, 0, _length);
  }

  /**
   * Serializes the value.
   */
  public void serialize(Env env, StringBuilder sb)
  {
    sb.append("U:");
    sb.append(_length);
    sb.append(":\"");
    sb.append(_buffer, 0, _length);
    sb.append("\";");
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
    return new UnicodeBuilderValue();
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder(int length)
  {
    return new UnicodeBuilderValue(length);
  }

  //
  // static helper functions
  //

  public static int getNumericType(char []buffer, int offset, int len)
  {
    if (len == 0)
      return IS_STRING;

    int i = offset;
    int ch = 0;

    if (i < len && ((ch = buffer[i]) == '+' || ch == '-')) {
      i++;
    }

    if (len <= i)
      return IS_STRING;

    ch = buffer[i];

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
        return IS_DOUBLE;
      }

      return IS_STRING;
    }
    else if (! ('0' <= ch && ch <= '9'))
      return IS_STRING;

    for (; i < len && '0' <= (ch = buffer[i]) && ch <= '9'; i++) {
    }

    if (len <= i)
      return IS_LONG;
    else if (ch == '.' || ch == 'e' || ch == 'E') {
      for (i++;
           i < len && ('0' <= (ch = buffer[i]) && ch <= '9'
                       || ch == '+' || ch == '-' || ch == 'e' || ch == 'E');
           i++) {
      }

      if (i < len)
        return IS_STRING;
      else
        return IS_DOUBLE;
    }
    else
      return IS_STRING;
  }

  public static ValueType getValueType(char []buffer, int offset, int len)
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
           && ('0' <= (ch = buffer[i])
               && ch <= '9'
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
   * Converts to a long.
   */
  public static long toLong(char []buffer, int offset, int len)
  {
    return parseLong(buffer, offset, len);
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toDouble(_buffer, 0, _length);
  }

  public static double toDouble(char []buffer, int offset, int len)
  {
    int start = offset;
    int i = offset;
    int ch = 0;

    while (i < len && Character.isWhitespace(buffer[i])) {
      start++;
      i++;
    }

    int end = len + offset;

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

    char []buffer = new char[bufferLength];
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
    char []buffer = _buffer;

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

    if (rValue instanceof UnicodeBuilderValue) {
      UnicodeBuilderValue value = (UnicodeBuilderValue) rValue;

      int length = _length;

      if (length != value._length)
        return false;

      char []bufferA = _buffer;
      char []bufferB = value._buffer;

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

    if (o instanceof UnicodeBuilderValue) {
      UnicodeBuilderValue value = (UnicodeBuilderValue) o;

      int length = _length;

      if (length != value._length)
        return false;

      char []bufferA = _buffer;
      char []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
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

    if (o instanceof UnicodeBuilderValue) {
      UnicodeBuilderValue value = (UnicodeBuilderValue) o;

      int length = _length;

      if (length != value._length)
        return false;

      char []bufferA = _buffer;
      char []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    else
      return false;
  }

  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  {
    _length = in.readInt();
    _buffer = new char[_length];

    for (int i = 0; i < _length; i++) {
      _buffer[i] = (char) (in.read() & 0xFF);
    }
  }

  public String toString()
  {
    return String.valueOf(_buffer, 0, _length);
  }

  static {
    CHAR_STRINGS = new UnicodeBuilderValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new UnicodeBuilderValue((char) i);
  }
}

