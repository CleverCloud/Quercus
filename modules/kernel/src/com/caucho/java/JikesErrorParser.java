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

package com.caucho.java;

import com.caucho.util.CharBuffer;
import com.caucho.vfs.ByteToChar;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads jikes error messages and parses them into a usable format.
 */
class JikesErrorParser extends ErrorParser {
  private ByteToChar token = ByteToChar.create();
  private ByteToChar buf = ByteToChar.create();
  private String filename = "filename";

  /**
   * Parse all the errors into an array.
   *
   * @return null if no errors found.
   */
  String parseErrors(InputStream is, LineMap lineMap)
    throws IOException
  {
    CharBuffer errors = new CharBuffer();

    scanError(is, lineMap, errors);

    return errors.toString();
  }

  /**
   * Scans a single error.
   *
   * <p>Javac errors look like "filename:line: message"
   */
  private void scanError(InputStream is, LineMap lineMap, CharBuffer errors)
    throws IOException
  {
    int ch = is.read();
    ByteToChar sourceLine = ByteToChar.create();
    sourceLine.setEncoding(System.getProperty("file.encoding"));
    filename = "filename";

    buf.clear();
    while (ch >= 0) {
      int colOffset = 0;
      for (; ch >= 0 && (ch == ' ' || ch == '\t'); ch = is.read())
        colOffset++;

      int line = 0;
      for (; ch >= 0 && ch >= '0' && ch <= '9'; ch = is.read()) {
        line = 10 * line + ch - '0';
        colOffset++;
      }

      if (ch < 0)
        return;
      else if (colOffset == 0 && ! Character.isWhitespace((char) ch)) {
        ch = scanUnknownLine(is, ch, errors);
      }
      else if (ch != '.') {
        ch = skipToNewline(is, ch);
        continue;
      }

      sourceLine.clear();
      for (ch = is.read(); ch >= 0 && ch != '\n'; ch = is.read())
        sourceLine.addByte(ch);
      sourceLine.addChar('\n');

      int column = 0;
      for (ch = is.read(); ch >= 0 && ch != '\n'; ch = is.read()) {
        if (ch == '^')
          break;
        else if (ch == ' ')
          column++;
        else if (ch == '\t')
          column = ((column + 8) / 8) * 8;
      }
      for (int i = colOffset + 1; i < column; i++)
        sourceLine.addChar(' ');
      sourceLine.addChar('^');
      sourceLine.addChar('\n');

      ch = skipToNewline(is, ch);
      if (ch != '*')
        continue;

      buf.clear();
      for (; ch >= 0 && ch != ':' && ch != '\n'; ch = is.read()) {
      }

      if (ch != ':') {
        ch = skipToNewline(is, ch);
        continue;
      }

      for (ch = is.read();
           ch >= 0 && (ch == ' ' || ch == ' ');
           ch = is.read()) {
      }

      for (; ch >= 0 && ch != '\n'; ch = is.read())
        buf.addByte(ch);

      String message = buf.getConvertedString();

      if (lineMap != null)
        errors.append(lineMap.convertError(filename, line, 0, message));
      else
        errors.append(filename + ":" + line + ": " + message);
      errors.append('\n');
      errors.append(sourceLine.getConvertedString());
    }
  }

  private int scanUnknownLine(InputStream is, int ch, CharBuffer errors)
    throws IOException
  {
    token.clear();
    token.addByte(ch);
    for (ch = is.read();
         ch > 0 && ! Character.isWhitespace((char) ch);
         ch = is.read()) {
    }

    if (token.equals("Found")) {
      return scanFilename(is, ch);
    }
    else if (token.equals("***")) {
      for (; ch == ' ' || ch == '\t'; ch = is.read()) {
      }
      token.clear();
      for (; ch > 0 && ch != '\n' && ch != ':'; ch = is.read()) {
        token.addByte(ch);
      }
      
      if (token.equals("Warning"))
        return skipToNewline(is, ch);

      token.clear();
      for (ch = is.read(); ch > 0 && ch != '\n'; ch = is.read())
        token.addByte(ch);
      token.addChar('\n');

      errors.append(token.getConvertedString());

      return is.read();
    }
    else
      return skipToNewline(is, ch);
  }

  private int scanFilename(InputStream is, int ch)
    throws IOException
  {
    for (; ch >= 0 && ch != '\n' && ch != '"'; ch = is.read()) {
    }

    if (ch != '"')
      return skipToNewline(is, ch);

    token.clear();
    for (ch = is.read(); 
         ch >= 0 && ch != '"' && ch != '\n';
         ch = is.read()) {
      token.addByte(ch);
    }

    if (ch != '"')
      return skipToNewline(is, ch);

    filename = token.getConvertedString();

    return skipToNewline(is, ch);
  }

  private int skipToNewline(InputStream is, int ch)
    throws IOException
  {
    for (; ch >= 0 && ch != '\n'; ch = is.read()) {
    }

    return is.read();
  }
}
