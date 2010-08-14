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
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.xml2.XmlChar;
import com.caucho.xml2.XmlParser;

import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * A fast reader to convert bytes to characters for parsing XML.
 */
public class XmlReader {
  static final L10N L = new L10N(XmlReader.class);
  
  protected static boolean []isAsciiNameChar;
  
  protected XmlParser _parser;
  protected XmlReader _next;

  protected Path _searchPath;
  protected ReadStream _is;
  protected String _filename;
  protected int _line;

  protected String _systemId;
  protected String _publicId;

  /**
   * Create a new reader.
   */
  public XmlReader()
  {
  }

  /**
   * Create a new reader with the given read stream.
   */
  public XmlReader(XmlParser parser, ReadStream is)
  {
    init(parser, is);
  }

  /**
   * Initialize a reader at the start of parsing.
   */
  public void init(XmlParser parser, ReadStream is)
  {
    _parser = parser;
    _is = is;
    _filename = is.getUserPath();
    _line = 1;
  }

  /**
   * Sets the filename.
   */
  public void setFilename(String filename)
  {
    _filename = filename;
  }

  /**
   * Gets the filename.
   */
  public String getFilename()
  {
    return _filename;
  }

  /**
   * Sets the current line number.
   */
  public void setLine(int line)
  {
    _line = line;
  }

  /**
   * Gets the current line number.
   */
  public int getLine()
  {
    return _line;
  }

  /**
   * Sets the systemId.
   */
  public void setSystemId(String systemId)
  {
    _systemId = systemId;
  }

  /**
   * Gets the systemId.
   */
  public String getSystemId()
  {
    return _systemId;
  }

  /**
   * Sets the publicId.
   */
  public void setPublicId(String publicId)
  {
    _publicId = publicId;
  }

  /**
   * Gets the publicId.
   */
  public String getPublicId()
  {
    return _publicId;
  }

  /**
   * Sets the current search path.
   */
  public void setSearchPath(Path searchPath)
  {
    _searchPath = searchPath;
  }

  /**
   * Gets the current search path.
   */
  public Path getSearchPath()
  {
    return _searchPath;
  }

  /**
   * Sets the next reader.
   */
  public void setNext(XmlReader next)
  {
    _next = next;
  }

  /**
   * Sets the next reader.
   */
  public XmlReader getNext()
  {
    return _next;
  }

  /**
   * Returns the read stream.
   */
  public ReadStream getReadStream()
  {
    return _is;
  }
  
  /**
   * Read the next character, returning -1 on end of file..
   */
  public int read()
    throws IOException
  {
    int ch = _is.readChar();

    if (ch == '\n')
      _parser.setLine(++_line);
    
    return ch;
  }

  /**
   * Parses a name.
   */
  public int parseName(CharBuffer name, int ch)
    throws IOException, SAXException
  {
    char []buffer = name.getBuffer();
    int capacity = buffer.length;
    int offset = 0;

    buffer[offset++] = (char) ch;

    for (ch = read();
         ch > 0 && ch < 128 && isAsciiNameChar[ch] || XmlChar.isNameChar(ch);
         ch = read()) {
      if (offset >= capacity) {
        name.setLength(offset);
        name.append((char) ch);
        offset++;
        buffer = name.getBuffer();
        capacity = buffer.length;
      }
      else
        buffer[offset++] = (char) ch;
    }

    name.setLength(offset);

    return ch;
  }

  /**
   * Finish reading.
   */
  public void finish()
  {
    _is = null;
  }

  static {
    isAsciiNameChar = XmlChar.getAsciiNameCharArray();
  }
}

