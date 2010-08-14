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

package com.caucho.xml2.readers;

import com.caucho.util.CharBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.xml2.XmlParser;

import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;

/**
 * A fast reader to convert bytes to characters for parsing XML.
 */
public class Utf8Reader extends XmlReader {
  /**
   * Create a new reader.
   */
  public Utf8Reader()
  {
  }

  /**
   * Create a new reader with the given read stream.
   */
  public Utf8Reader(XmlParser parser, ReadStream is)
  {
    super(parser, is);
  }

  /**
   * Read the next character, returning -1 on end of file..
   */
  public int read()
    throws IOException
  {
    int ch1 = _is.read();

    if (ch1 == '\n') {
      _parser.setLine(++_line);
      return ch1;
    }
    else if (ch1 == '\r') {
      _parser.setLine(++_line);

      int ch2 = _is.read();
      if (ch2 == '\n')
        return '\n';

      if (ch2 < 0) {
      }
      else if (ch2 < 0x80)
        _parser.unread(ch2);
      else
        _parser.unread(readSecond(ch2));
      
      return '\n';
    }
    else if (ch1 < 0x80)
      return ch1;
    else
      return readSecond(ch1);
  }
    
  private int readSecond(int ch1)
    throws IOException
  {
    if ((ch1 & 0xe0) == 0xc0) {
      int ch2 = _is.read();
      if (ch2 < 0)
        throw new EOFException("unexpected end of file in utf8 character");
      else if ((ch2 & 0xc0) != 0x80)
        throw error(L.l("illegal utf8 encoding {0}", hex(ch1)));
      
      return ((ch1 & 0x1f) << 6) + (ch2 & 0x3f);
    }
    else if ((ch1 & 0xf0) == 0xe0) {
      int ch2 = _is.read();
      int ch3 = _is.read();
      
      if (ch2 < 0)
        throw new EOFException("unexpected end of file in utf8 character");
      else if ((ch2 & 0xc0) != 0x80)
        throw error(L.l("illegal utf8 encoding at {0} {1} {2}", hex(ch1), hex(ch2), hex(ch3)));
      
      if (ch3 < 0)
        throw new EOFException("unexpected end of file in utf8 character");
      else if ((ch3 & 0xc0) != 0x80)
        throw error(L.l("illegal utf8 encoding {0} {1} {2}",
                        hex(ch1), hex(ch2), hex(ch3)));

      int ch = ((ch1 & 0x1f) << 12) + ((ch2 & 0x3f) << 6) + (ch3 & 0x3f);

      if (ch == 0xfeff) // handle some writers, e.g. microsoft
        return read();
      else
        return ch;
    }
    else
      throw error(L.l("illegal utf8 encoding at {0}", hex(ch1)));
  }

  private String hex(int n)
  {
    n = n & 0xff;
    
    CharBuffer cb = CharBuffer.allocate();

    cb.append("0x");

    int d = n / 16;
    if (d >= 0 && d <= 9)
      cb.append((char) ('0' + d));
    else
      cb.append((char) ('a' + d - 10));
    
    d = n % 16;
    if (d >= 0 && d <= 9)
      cb.append((char) ('0' + d));
    else
      cb.append((char) ('a' + d - 10));

    return cb.close();
  }

  private CharConversionException error(String msg)
  {
    String filename = _parser.getFilename();
    int line = _parser.getLine();

    if (filename != null)
      return new CharConversionException(filename + ":" + line + ": " + msg);
    else
      return new CharConversionException(msg);
  }
}

