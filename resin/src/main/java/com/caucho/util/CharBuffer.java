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

package com.caucho.util;

import java.io.InputStream;

/**
 * CharBuffer is an unsynchronized version of StringBuffer.
 */
public final class CharBuffer extends CharSegment {
  private static final int MIN_CAPACITY = 64;

  /**
   * Constructs a char buffer with no characters.
   */
  public CharBuffer()
  {
    _buffer = new char[MIN_CAPACITY];
    _length = 0;
  }

  /**
   * Constructs a char buffer with the given initial capacity
   *
   * @param capacity initial capacity
   */
  public CharBuffer(int capacity)
  {
    if (capacity < 0)
      throw new IllegalArgumentException();
    if (capacity < MIN_CAPACITY)
      capacity = MIN_CAPACITY;

    _buffer = new char[capacity];
    _length = 0;
  }

  /**
   * Constructs a char buffer with the given initial string
   *
   * @param string initial string
   */
  public CharBuffer(String string)
  {
    int length = string.length();
    int capacity = length + MIN_CAPACITY;

    _buffer = new char[capacity];
    _length = length;
    string.getChars(0, length, _buffer, 0);
  }

  /**
   * Constructs a char buffer with the given initial string
   *
   * @param string initial string
   */
  public CharBuffer(String string, int offset, int length)
  {
    int capacity = length;
    if (capacity < MIN_CAPACITY)
      capacity = MIN_CAPACITY;

    _buffer = new char[capacity];
    _length = length;
    string.getChars(offset, length, _buffer, 0);
  }

  public static CharBuffer allocate()
  {
    return new CharBuffer();
  }

  public void free()
  {
  }

  /**
   * Returns the capacity of the buffer, i.e. how many chars it
   * can hold.
   */
  public int capacity()
  {
    return _buffer.length;
  }
  
  public int getCapacity()
  {
    return _buffer.length;
  }

  /**
   * Ensure the buffer can hold at least 'minimumCapacity' chars.
   */
  public final void ensureCapacity(int minimumCapacity)
  {
    if (minimumCapacity <= _buffer.length) {
      return;
    }

    expandCapacity(minimumCapacity);
  }

  /**
   * Expands the capacity to a new value.
   */
  private final void expandCapacity(int minimumCapacity)
  {
    int oldCapacity = _buffer.length;
    int newCapacity = oldCapacity * 2;

    if (newCapacity < 0)
      newCapacity = Integer.MAX_VALUE;
    else if (newCapacity < minimumCapacity)
      newCapacity = minimumCapacity;

    char []chars = new char[newCapacity];
    
    System.arraycopy(_buffer, 0, chars, 0, oldCapacity); 

    _buffer = chars;
  }

  /**
   * Clears the buffer.  Equivalent to setLength(0)
   */
  public final void clear()
  {
    _length = 0;
  }

  /**
   * Set the length of the buffer.
   */
  public final void setLength(int newLength)
  {
    if (newLength < 0)
      throw new IndexOutOfBoundsException("illegal argument");
    else if (_buffer.length < newLength)
      expandCapacity(newLength);

    _length = newLength;
  }

  /**
   * Returns the char at the specified offset.
   */
  public char charAt(int i)
  {
    if (i < 0 || _length <= i)
      throw new IndexOutOfBoundsException();

    return _buffer[i];
  }

  /**
   * Returns the last character of the buffer
   *
   * @throws IndexOutOfBoundsException for an empty buffer
   */
  public char getLastChar()
  {
    if (_length == 0)
      throw new IndexOutOfBoundsException();

    return _buffer[_length - 1];
  }
  
  /**
   * Returns the buffer's char array.
   */
  public final char []getBuffer()
  {
    return _buffer;
  }

  /**
   * Copies characters to the destination buffer.
   */
  public void getChars(int srcBegin, int srcEnd, char []dst, int dstBegin)
  {
    char []buffer = _buffer;
    while (srcBegin < srcEnd)
      dst[dstBegin++] = buffer[srcBegin++];
  }

  /**
   * Sets the character at the given index.
   */
  public void setCharAt(int index, char ch)
  {
    if (index < 0 || _length <= index)
      throw new IndexOutOfBoundsException();

    _buffer[index] = ch;
  }

  /**
   * Appends the string representation of the object to the buffer.
   */
  public CharBuffer append(Object obj)
  {
    return append(String.valueOf(obj));
  }

  /**
   * Appends the string representation of the object to the buffer.
   */
  public CharBuffer append(CharBuffer cb)
  {
    return append(cb._buffer, 0, cb._length);
  }

  /**
   * Appends the string.
   */
  public CharBuffer append(String string)
  {
    if (string == null)
      string = "null";

    int len = string.length();
    int newLength = _length + len;
    int length = _length;
    if (_buffer.length <= newLength)
      expandCapacity(newLength);

    string.getChars(0, len, _buffer, length);

    _length = newLength;

    return this;
  }

  public CharBuffer append(String string, int offset, int len)
  {
    if (_buffer.length <= len + _length)
      expandCapacity(len + _length);

    string.getChars(offset, offset + len, _buffer, _length);

    _length += len;

    return this;
  }
  
  /**
   * Appends the characters to the buffer.
   */
  public CharBuffer append(char []buffer)
  {
    return append(buffer, 0, buffer.length);
  }

  /**
   * Appends the characters to the buffer.
   */
  public CharBuffer append(char []buffer, int offset, int length)
  {
    if (_buffer.length < _length + length)
      expandCapacity(_length + length);

    System.arraycopy(buffer, offset, _buffer, _length, length);

    _length += length;

    return this;
  }

  /**
   * Appends the boolean representation to the buffer
   */
  public final CharBuffer append(boolean b)
  {
    return append(String.valueOf(b));
  }
  
  /**
   * Appends the character to the buffer
   */
  public final CharBuffer append(char ch)
  {
    if (_buffer.length <= _length)
      expandCapacity(_length + 1);

    _buffer[_length++] = ch;

    return this;
  }
  
  /**
   * Add an int to the buffer.
   */
  public CharBuffer append(int i)
  {
    if (i == 0x80000000) {
      return append("-2147483648");
    }
    
    int length = _length;
    
    if (_buffer.length <= length + 16)
      expandCapacity(length + 16);

    char []buffer = _buffer;

    if (i < 0) {
      buffer[length++] = '-';
      i = -i;
    }
    else if (i == 0) {
      buffer[_length++] = '0';
      return this;
    }

    int start = length;
    while (i > 0) {
      buffer[length++] = (char) ((i % 10) + '0');
      i /= 10;
    }

    for (int j = (length - start) / 2; j > 0; j--) {
      char temp = buffer[length - j];
      buffer[length - j] = buffer[start + j - 1];
      buffer[start + j - 1] = temp;
    }

    _length = length;

    return this;
  }
  
  /**
   * Add a long to the buffer.
   */
  public CharBuffer append(long i)
  {
    if (i == 0x8000000000000000L) {
      return append("-9223372036854775808");
    }
    
    int length = _length;
    
    if (_buffer.length < length + 32)
      expandCapacity(length + 32);

    char []buffer = _buffer;

    if (i < 0) {
      buffer[length++] = '-';
      i = -i;
    }
    else if (i == 0) {
      buffer[_length++] = '0';
      return this;
    }

    int start = length;
    while (i > 0) {
      buffer[length++] = (char) ((i % 10) + '0');
      i /= 10;
    }

    for (int j = (length - start) / 2; j > 0; j--) {
      char temp = buffer[length - j];
      buffer[length - j] = buffer[start + j - 1];
      buffer[start + j - 1] = temp;
    }

    _length = length;

    return this;
  }

  /**
   * Add a float to the buffer.
   */
  public CharBuffer append(float f)
  {
    return append(String.valueOf(f));
  }

  /**
   * Add a double to the buffer.
   */
  public CharBuffer append(double d)
  {
    return append(String.valueOf(d));
  }
  
  /**
   * Appends iso-8859-1 bytes to the buffer
   */
  public final CharBuffer append(byte []buf, int offset, int len)
  {
    int length = _length;
    if (_buffer.length < _length + len)
      expandCapacity(_length + len);

    char []buffer = _buffer;
    for (; len > 0; len--)
      buffer[length++] = (char) (buf[offset++] & 0xff);

    _length = length;

    return this;
  }

  /**
   * Deletes characters from the buffer.
   */
  public CharBuffer delete(int start, int end)
  {
    if (start < 0 || end < start || _length < start)
      throw new StringIndexOutOfBoundsException();

    if (_length < end)
      end = _length;
    
    int tail = _length - end;
    char []buffer = _buffer;
    
    for (int i = 0; i < tail; i++)
      buffer[start + i] = buffer[end + i];

    _length -= end - start;

    return this;
  }

  /**
   * Deletes a character from the buffer.
   */
  public CharBuffer deleteCharAt(int index)
  {
    if (index < 0 || _length < index)
      throw new StringIndexOutOfBoundsException();

    if (index == _length)
      return this;
    
    int tail = _length - index + 1;
    char []buffer = _buffer;

    for (int i = 0; i < tail; i++)
      buffer[index + i] = buffer[index + i + 1];

    _length--;

    return this;
  }

  /**
   * Replaces a range with a string
   */
  public CharBuffer replace(int start, int end, String string)
  {
    if (start < 0 || end < start || _length < start)
      throw new StringIndexOutOfBoundsException();

    int len = string.length();
    int length = _length;

    if (_buffer.length < len + length - (end - start))
      expandCapacity(len + length - (end - start));

    char []buffer = _buffer;

    if (len < end - start) {
      int tail = length - end;
      for (int i = 0; i < tail; i++)
        buffer[start + len + i] = buffer[end + i];
    }
    else {
      int tail = length - end;
      for (int i = tail - 1; i >= 0; i--)
        buffer[end + i] = buffer[start + len + i];
    }

    string.getChars(0, len, buffer, start);

    _length = length + len - (end - start);

    return this;
  }

  /**
   * Replaces a range with a character array
   */
  public CharBuffer replace(int start, int end,
                            char []buffer, int offset, int len)
  {
    if (start < 0 || end < start || _length < start)
      throw new StringIndexOutOfBoundsException();

    if (_buffer.length < len + _length - (end - start))
      expandCapacity(len + _length - (end - start));

    char []thisBuffer = _buffer;

    if (len < end - start) {
      int tail = _length - end;
      for (int i = 0; i < tail; i++)
        thisBuffer[start + len + i] = thisBuffer[end + i];
    }
    else {
      int tail = _length - end;
      for (int i = tail - 1; i >= 0; i--)
        thisBuffer[end + i] = thisBuffer[start + len + i];
    }

    System.arraycopy(buffer, offset, thisBuffer, start, len);

    _length += len - (end - start);

    return this;
  }

  /**
   * Returns a substring
   */
  public String substring(int start)
  {
    if (_length < start || start < 0)
      throw new StringIndexOutOfBoundsException();

    return new String(_buffer, start, _length - start);
  }

  /**
   * Returns a substring
   */
  public String substring(int start, int end)
  {
    if (_length < start || start < 0 || end < start)
      throw new StringIndexOutOfBoundsException();

    return new String(_buffer, start, end - start);
  }
  /**
   * Inserts a string.
   */
  public CharBuffer insert(int index, String string)
  {
    if (string == null)
      string = "null";

    if (index < 0 || _length < index)
      throw new StringIndexOutOfBoundsException();

    int len = string.length();

    if (_buffer.length < _length + len) 
      expandCapacity(len + _length);

    int tail = _length - index;
    char []buffer = _buffer;
    
    for (int i = tail - 1; i >= 0; i--)
      buffer[index + len + i] = buffer[index + i];

    string.getChars(0, len, buffer, index);
    _length += len;

    return this;
  }

  /**
   * Inserts a character buffer.
   */
  public CharBuffer insert(int index, char []buffer, int offset, int len)
  {
    if (index < 0 || _length < index)
      throw new StringIndexOutOfBoundsException();

    if (_buffer.length < len + _length)
      expandCapacity(len + _length);

    int tail = _length - index;
    char []thisBuffer = _buffer;
    for (int i = tail - 1; i >= 0; i--)
      buffer[index + len + i] = thisBuffer[index + i];

    System.arraycopy(buffer, offset, thisBuffer, index, len);
    _length += len;

    return this;
  }

  /**
   * Inserts an object at a given offset.
   */
  public CharBuffer insert(int offset, Object o)
  {
    return insert(offset, String.valueOf(o));
  }

  /**
   * Inserts a character at a given offset.
   */
  public CharBuffer insert(int offset, char ch)
  {
    return insert(offset, String.valueOf(ch));
  }

  /**
   * Inserts an integer at a given offset.
   */
  public CharBuffer insert(int offset, int i)
  {
    return insert(offset, String.valueOf(i));
  }

  /**
   * Inserts a long at a given offset.
   */
  public CharBuffer insert(int offset, long l)
  {
    return insert(offset, String.valueOf(l));
  }

  /**
   * Inserts a float at a given offset.
   */
  public CharBuffer insert(int offset, float f)
  {
    return insert(offset, String.valueOf(f));
  }

  /**
   * Inserts a double at a given offset.
   */
  public CharBuffer insert(int offset, double d)
  {
    return insert(offset, String.valueOf(d));
  }

  public int indexOf(char ch)
  {
    return indexOf(ch, 0);
  }

  /**
   * Clones the buffer
   */
  public Object clone()
  {
    CharBuffer newBuffer = new CharBuffer();

    newBuffer.setLength(_length);

    System.arraycopy(_buffer, 0, newBuffer._buffer, 0, _length);

    return newBuffer;
  }

  /**
   * String representation of the buffer.
   */
  public String toString()
  {
    return new String(_buffer, 0, _length);
  }

  public String close()
  {
    String string = new String(_buffer, 0, _length);
    free();
    return string;
  }

  class CBInputStream extends InputStream {
    int _index = 0;

    public int read()
    {
      if (_length <= _index)
        return -1;

      return _buffer[_index++];
    }
  }

  public InputStream getInputStream()
  {
    return new CBInputStream();
  }
}
