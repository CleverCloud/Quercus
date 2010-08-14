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

package com.caucho.quercus.lib.pdf;

import com.caucho.util.L10N;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * parses afm
 */
public class AfmParser {
  private static final L10N L = new L10N(AfmParser.class);
  
  private static final String END_OF_FILE = "end of file";

  private ReadStream _is;

  /**
   * Parses the AFM
   */
  public Font parse(String name)
    throws IOException
  {
    MergePath mergePath = new MergePath();
    mergePath.addClassPath();

    Path path = mergePath.lookup("com/caucho/quercus/lib/pdf/font/" + name + ".afm");

    if (! path.canRead())
      throw new FileNotFoundException(L.l("Can't find font {0}", name));

    _is = path.openRead();

    try {
      return parseTop();
    } finally {
      _is.close();
    }
  }

  private Font parseTop()
    throws IOException
  {
    Font font = new Font();

    while (skipWhitespace()) {
      String id = parseIdentifier();

      if ("FontName".equals(id)) {
        font.setFontName(parseString());
      }
      else if ("Weight".equals(id)) {
        font.setWeight(parseString());
      }
      else if ("FontBBox".equals(id)) {
        font.setBBox(parseNumber(), parseNumber(),
                     parseNumber(), parseNumber());
      }
      else if ("CapHeight".equals(id)) {
        font.setCapHeight(parseNumber());
      }
      else if ("XHeight".equals(id)) {
        font.setXHeight(parseNumber());
      }
      else if ("Ascender".equals(id)) {
        font.setAscender(parseNumber());
      }
      else if ("Descender".equals(id)) {
        font.setDescender(parseNumber());
      }
      else if ("UnderlinePosition".equals(id)) {
        font.setUnderlinePosition(parseNumber());
      }
      else if ("UnderlineThickness".equals(id)) {
        font.setUnderlineThickness(parseNumber());
      }
      else if ("C".equals(id)) {
        font.addChar(parseCharacter());
      }

      skipToEndOfLine();
    }

    return font;
  }

  private FontChar parseCharacter()
    throws IOException
  {
    int code = parseInteger();

    skipWhitespace();

    int ch;

    if ((ch = _is.read()) != ';')
      throw new IOException("Expected ';'");

    String wx = parseString();

    if (! "WX".equals(wx))
      throw new IOException("Expected 'WX'");

    double width = parseNumber();

    return new FontChar(code, width);
  }

  private String parseString()
    throws IOException
  {
    skipWhitespace();

    StringBuilder sb = new StringBuilder();
    int ch;

    while ((ch = _is.read()) >= 0 && ! Character.isWhitespace(ch)) {
      sb.append((char) ch);
    }

    if (ch >= 0)
      _is.unread();

    return sb.toString();
  }

  private int parseInteger()
    throws IOException
  {
    skipWhitespace();

    int value = 0;
    int sign = 1;

    int ch = _is.read();

    if (ch == '-') {
      sign = -1;
      ch = _is.read();
    }

    for (; '0' <= ch && ch <= '9'; ch = _is.read()) {
      value = 10 * value + ch - '0';
    }

    if (ch >= 0)
      _is.unread();

    return sign * value;
  }

  private double parseNumber()
    throws IOException
  {
    skipWhitespace();

    StringBuilder sb = new StringBuilder();
    int ch;

    while ('0' <= (ch = _is.read()) && ch <= '9' ||
           ch == '.' || ch == '-' || ch == '+') {
      sb.append((char) ch);
    }

    if (ch >= 0)
      _is.unread();

    if (sb.length() == 0)
      return 0;

    return Double.parseDouble(sb.toString());
  }

  private String parseIdentifier()
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    int ch;

    while (Character.isLetterOrDigit((ch = _is.read()))) {
      sb.append((char) ch);
    }

    _is.unread();

    return sb.toString();
  }

  private void skipToEndOfLine()
    throws IOException
  {
    int ch;

    while ((ch = _is.read()) >= 0 && ch != '\n') {
    }
  }

  private boolean skipWhitespace()
    throws IOException
  {
    int ch;

    while ((ch = _is.read()) == ' ' || ch == '\t') {
    }

    if (ch >= 0)
      _is.unread();

    return ch >= 0;
  }
}
