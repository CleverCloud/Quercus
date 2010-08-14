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

/**
 * A scanner for simple delimiter based parsing.
 */
public class CharScanner {
  private char []delimiters;

  /**
   * Creates a compiled character scanner.
   *
   * @param delimiters string containing the delimiters
   */
  public CharScanner(String delimiters)
  {
    this.delimiters = delimiters.toCharArray();
  }

  /**
   * Skips characters from the cursor until one matches a delimiter.
   *
   * @param cursor CharCursor to scan.
   *
   * @return the last character read
   */
  public char skip(CharCursor cursor)
  {
    char ch;
    char []delim = delimiters;
    int len = delim.length;

  loop:
    for (ch = cursor.current();
         ch != cursor.DONE;
         ch = cursor.next()) {
      for (int i = 0; i < len; i++) {
        if (delim[i] == ch)
          continue loop;
      }
      return ch;
    }

    return ch;
  }

  /**
   * Scans characters from a cursor, filling a char buffer.
   *
   * @param cursor CharCursor to scan.
   * @param buf CharBuffer to fill.
   *
   * @return the last character read
   */
  public char scan(CharCursor cursor, CharBuffer buf)
  {
    char ch;
    char []delim = delimiters;
    int len = delim.length;

    for (ch = cursor.current();
         ch != cursor.DONE;
         ch = cursor.next()) {
      for (int i = 0; i < len; i++) {
        if (delim[i] == ch)
          return ch;
      }

      buf.append(ch);
    }

    return ch;
  }

  /**
   * Scans characters from a cursor, until reaching the delimiter
   *
   * @param cursor CharCursor to scan.
   *
   * @return the last character read
   */
  public char scan(CharCursor cursor)
  {
    char ch;
    char []delim = delimiters;
    int len = delim.length;

    for (ch = cursor.current();
         ch != cursor.DONE;
         ch = cursor.next()) {
      for (int i = 0; i < len; i++) {
        if (delim[i] == ch)
          return ch;
      }
    }

    return ch;
  }
}
