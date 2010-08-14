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

package com.caucho.xml.readers;

import com.caucho.util.CharBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.xml.XmlParser;

import java.io.IOException;

/**
 * A fast reader to convert bytes to characters for parsing XML.
 */
public class MacroReader extends XmlReader {
  char []_buffer = new char[256];
  int _offset;
  int _length;
  
  /**
   * Create a new reader.
   */
  public MacroReader()
  {
  }

  public void init(XmlParser parser, XmlReader next)
  {
    _parser = parser;
    _next = next;
    _offset = 0;
    _length = 0;
  }

  public ReadStream getReadStream()
  {
    return _next.getReadStream();
  }

  public String getSystemId()
  {
    if (_next != null)
      return _next.getSystemId();
    else
      return super.getSystemId();
  }

  public String getPublicId()
  {
    if (_next != null)
      return _next.getPublicId();
    else
      return super.getPublicId();
  }

  public String getFilename()
  {
    if (_next != null)
      return _next.getFilename();
    else
      return super.getFilename();
  }

  public int getLine()
  {
    if (_next != null)
      return _next.getLine();
    else
      return super.getLine();
  }

  /**
   * Adds a string to the macro.
   */
  public void add(String s)
  {
    int len = s.length();

    for (int i = 0; i < len; i++)
      add(s.charAt(i));
  }

  /**
   * Adds a char buffer to the macro.
   */
  public void add(CharBuffer cb)
  {
    int len = cb.length();

    for (int i = 0; i < len; i++)
      add(cb.charAt(i));
  }

  /**
   * Adds a new character to the buffer.
   */
  public void add(char ch)
  {
    if (_offset == _length) {
      _offset = 0;
      _length = 0;
    }

    if (_buffer.length == _length) {
      char []newBuffer = new char[2 * _buffer.length];
      System.arraycopy(_buffer, 0, newBuffer, 0, _length);
      _buffer = newBuffer;
    }

    _buffer[_length++] = ch;
  }

  public void prepend(char ch)
  {
    if (_offset == _length) {
      _offset = 0;
      _length = 0;
    }

    if (_buffer.length == _length) {
      char []newBuffer = new char[2 * _buffer.length];
      System.arraycopy(_buffer, 0, newBuffer, 0, _length);
      _buffer = newBuffer;
    }

    for (int i = _length; i >= 0; i--)
      _buffer[i + 1] = _buffer[i];

    _length++;
    _buffer[0] = ch;
  }
  
  /**
   * Read the next character, returning -1 on end of file..
   */
  public int read()
    throws IOException
  {
    if (_offset < _length)
      return _buffer[_offset++];
    else
      return _next.read();
  }
}

