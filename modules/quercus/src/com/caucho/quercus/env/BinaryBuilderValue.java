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
import com.caucho.quercus.marshal.Marshal;
import com.caucho.util.*;

/**
 * Represents a 8-bit PHP 6 style binary builder (unicode.semantics = on)
 */
public class BinaryBuilderValue
  extends StringBuilderValue
{
  public static final BinaryBuilderValue EMPTY = new BinaryBuilderValue("");

  private final static BinaryBuilderValue []CHAR_STRINGS;

  public BinaryBuilderValue()
  {
    super(128);
  }

  public BinaryBuilderValue(BinaryBuilderValue v)
  {
    super(v);
  }

  public BinaryBuilderValue(int capacity)
  {
    super(capacity);
  }

  public BinaryBuilderValue(byte []buffer, int offset, int length)
  {
    super(buffer, offset, length);
  }

  public BinaryBuilderValue(byte []buffer)
  {
    super(buffer);
  }

  public BinaryBuilderValue(String s)
  {
    super(s);
  }

  public BinaryBuilderValue(char []buffer)
  {
    super(buffer);
  }

  public BinaryBuilderValue(char []s, Value v1)
  {
    super(s, v1);
  }

  public BinaryBuilderValue(TempBuffer head)
  {
    this();

    // php/0c4l
    append(head);
  }

  public BinaryBuilderValue(byte ch)
  {
    super(ch);
  }

  /**
   * Creates the string.
   */
  public static StringValue create(int value)
  {
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new BinaryBuilderValue(value);
  }

  /**
   * Creates the string.
   */
  public static StringValue create(char value)
  {
    // php/3jb1
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new BinaryBuilderValue(value);
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
   * Returns true for a BinaryValue.
   */
  public boolean isBinary()
  {
    return true;
  }

  //
  // marshal costs
  //

  /**
   * Cost to convert to a byte
   */
  @Override
  public int toByteMarshalCost()
  {
    if (isLongConvertible())
      return Marshal.COST_NUMERIC_LOSSLESS;
    else if (isDoubleConvertible())
      return Marshal.COST_NUMERIC_LOSSY;
    else
      return Marshal.COST_BINARY_TO_BYTE;
  }

  /**
   * Cost to convert to a String
   */
  @Override
  public int toStringMarshalCost()
  {
    return Marshal.COST_BINARY_TO_STRING;
  }

  /**
   * Cost to convert to a char[]
   */
  @Override
  public int toCharArrayMarshalCost()
  {
    return Marshal.COST_BINARY_TO_STRING + 5;
  }

  /**
   * Cost to convert to a byte[]
   */
  @Override
  public int toByteArrayMarshalCost()
  {
    return Marshal.COST_EQUAL;
  }

  /**
   * Cost to convert to a binary value
   */
  @Override
  public int toBinaryValueMarshalCost()
  {
    return Marshal.COST_IDENTICAL;
  }

  /**
   * Cost to convert to a string value
   */
  @Override
  public int toStringValueMarshalCost()
  {
    return Marshal.COST_IDENTICAL + 1;
  }

  /**
   * Converts to a Unicode, 16-bit string.
   */
  @Override
  public StringValue toUnicode(Env env)
  {
    return new UnicodeBuilderValue(this);
  }

  /**
   * Converts to a UnicodeValue.
   */
  @Override
  public StringValue toUnicodeValue()
  {
    return new UnicodeBuilderValue(this);
  }

  /**
   * Converts to a UnicodeValue.
   */
  @Override
  public StringValue toUnicodeValue(Env env)
  {
    return new UnicodeBuilderValue(this);
  }

  /**
   * Converts to a UnicodeValue in desired charset.
   */
  @Override
  public StringValue toUnicodeValue(Env env, String charset)
  {
    return toUnicodeValue(env);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    // XXX: can this just return this, or does it need to return a copy?
    return new BinaryBuilderValue(this);
  }

  /**
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
  {
    int len = getOffset();

    if (index < 0 || len <= index)
      return UnsetBinaryValue.UNSET;
    else
      return BinaryBuilderValue.create(getBuffer()[(int) index] & 0xff);
  }

  /**
   * Returns a subsequence
   */
  @Override
  public CharSequence subSequence(int start, int end)
  {
    if (end <= start)
      return EMPTY;

    return new BinaryBuilderValue(getBuffer(), start, end - start);
  }

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toLowerCase()
  {
    int length = getOffset();

    BinaryBuilderValue string = new BinaryBuilderValue(length);

    byte []srcBuffer = getBuffer();
    byte []dstBuffer = string.getBuffer();

    for (int i = 0; i < length; i++) {
      byte ch = srcBuffer[i];

      if ('A' <= ch && ch <= 'Z')
        dstBuffer[i] = (byte) (ch + 'a' - 'A');
      else
        dstBuffer[i] = ch;
    }

    string.setOffset(length);

    return string;
  }

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toUpperCase()
  {
    int length = getOffset();

    BinaryBuilderValue string = new BinaryBuilderValue(getOffset());

    byte []srcBuffer = getBuffer();
    byte []dstBuffer = string.getBuffer();

    for (int i = 0; i < length; i++) {
      byte ch = srcBuffer[i];

      if ('a' <= ch && ch <= 'z')
        dstBuffer[i] = (byte) (ch + 'A' - 'a');
      else
        dstBuffer[i] = ch;
    }

    string.setOffset(length);

    return string;
  }

  //
  // append code
  //

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public BinaryBuilderValue createStringBuilder()
  {
    return new BinaryBuilderValue();
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public BinaryBuilderValue createStringBuilder(int length)
  {
    return new BinaryBuilderValue(length);
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public BinaryBuilderValue
    createStringBuilder(byte []buffer, int offset, int length)
  {
    return new BinaryBuilderValue(length);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return new BinaryBuilderValue(getBuffer(), 0, getOffset());
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env, Value value)
  {
    if (value.isUnicode()) {
      UnicodeBuilderValue sb = new UnicodeBuilderValue(this);

      value.appendTo(sb);

      return sb;
    }
    else {
      BinaryBuilderValue v = new BinaryBuilderValue(this);

      value.appendTo(v);

      return v;
    }
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env, StringValue value)
  {
    if (value.isUnicode()) {
      UnicodeBuilderValue sb = new UnicodeBuilderValue(this);

      value.appendTo(sb);

      return sb;
    }
    else {
      BinaryBuilderValue v = new BinaryBuilderValue(this);

      value.appendTo(v);

      return v;
    }
  }

  /**
   * Append a Java buffer to the value.
   */
  // @Override
  public final StringValue append(BinaryBuilderValue sb, int head, int tail)
  {
    int length = tail - head;
    
    byte []buffer = getBuffer();
    byte []sbBuffer = sb.getBuffer();
    
    int offset = getOffset();

    if (buffer.length < offset + length)
      ensureCapacity(offset + length);

    System.arraycopy(sbBuffer, head, buffer, offset, tail - head);

    setOffset(offset + tail - head);

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue appendUnicode(char []buf, int offset, int length)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(buf, offset, length);

    return sb;
  }

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue appendUnicode(String s)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(s);

    return sb;
  }

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue appendUnicode(String s, int start, int end)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(s, start, end);

    return sb;
  }

  /**
   * Append a value to the value.
   */
  @Override
  public final StringValue appendUnicode(Value value)
  {
    value = value.toValue();

    if (value instanceof BinaryBuilderValue) {
      append((BinaryBuilderValue) value);

      return this;
    }
    else if (value.isString()) {
      UnicodeBuilderValue sb = new UnicodeBuilderValue();

      appendTo(sb);
      sb.append(value);

      return sb;
    }
    else
      return value.appendTo(this);
  }

  /**
   * Append a Java char to the value.
   */
  @Override
  public final StringValue appendUnicode(char ch)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(ch);

    return sb;
  }

  /**
   * Append a Java boolean to the value.
   */
  @Override
  public final StringValue appendUnicode(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  @Override
  public StringValue appendUnicode(long v)
  {
    // XXX: this probably is frequent enough to special-case

    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  @Override
  public StringValue appendUnicode(double v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java object to the value.
   */
  @Override
  public StringValue appendUnicode(Object v)
  {
    if (v instanceof String)
      return appendUnicode(v.toString());
    else
      return append(v.toString());
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

  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    ValueType typeA = getValueType();
    ValueType typeB = rValue.getValueType();

    if (typeB.isNumber()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }
    else if (typeB.isBoolean()) {
      return toBoolean() == rValue.toBoolean();
    }
    else if (typeA.isNumberCmp() && typeB.isNumberCmp()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }

    rValue = rValue.toValue();

    if (rValue instanceof BinaryBuilderValue) {
      BinaryBuilderValue value = (BinaryBuilderValue) rValue;

      int length = getOffset();

      if (length != value.getOffset())
        return false;

      byte []bufferA = getBuffer();
      byte []bufferB = value.getBuffer();

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    else {
      return toString().equals(rValue.toString());
    }
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this)
      return true;

    if (o instanceof BinaryBuilderValue) {
      BinaryBuilderValue value = (BinaryBuilderValue) o;

      int length = getOffset();

      if (length != value.getOffset())
        return false;

      byte []bufferA = getBuffer();
      byte []bufferB = value.getBuffer();

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

    if (o instanceof BinaryBuilderValue) {
      BinaryBuilderValue value = (BinaryBuilderValue) o;

      int length = getOffset();

      if (length != value.getOffset())
        return false;

      byte []bufferA = getBuffer();
      byte []bufferB = value.getBuffer();

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    else
      return false;
  }

  @Override
  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    int length = length();

    sb.append("binary(");
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

    // QA needs to distinguish php5 string from php6 binary
    if (Alarm.isTest())
      out.print("binary");
    else
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
                  + Integer.toHexString(ch / 16)
                  + Integer.toHexString(ch % 16));
      else {
        out.print("\\u"
                  + Integer.toHexString((ch >> 12) & 0xf)
                  + Integer.toHexString((ch >> 8) & 0xf)
                  + Integer.toHexString((ch >> 4) & 0xf)
                  + Integer.toHexString((ch) & 0xf));
      }
    }

    out.print("\"");
  }


  static {
    CHAR_STRINGS = new BinaryBuilderValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new BinaryBuilderValue((byte) i);
  }
}

