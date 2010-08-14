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

package com.caucho.xsl;

import com.caucho.util.CharBuffer;
import com.caucho.util.IntArray;

import java.util.ArrayList;

/**
 * Formatting for the xsl:number action.
 */
public class XslNumberFormat {
  private String head;
  private String tail;

  private String []separators;
  private int []formats;
  private int []zeroSize;

  private String format;
  private String lang;
  private boolean isAlphabetic;
  private String groupSeparator;
  private int groupSize;

  /**
   * Create a new number formatting object.
   *
   * @param format format string as specified by the XSLT draft
   * @param lang language for alphanumeric numbering
   * @param isAlphabetic resolves ambiguity for alphanumeric numbering
   * @param groupSeparator separator between number grouping
   * @param groupSize digits between group separator
   */
  public XslNumberFormat(String format, String lang, boolean isAlphabetic,
                         String groupSeparator, int groupSize)
  {
    this.format = format;
    this.lang = lang;
    this.isAlphabetic = isAlphabetic;
    if (groupSize <= 0) {
      groupSize = 3;
      groupSeparator = "";
    }
    if (groupSeparator == null)
      groupSeparator = "";
    this.groupSeparator = groupSeparator;
    this.groupSize = groupSize;

    if (format == null)
      format = "1";

    int headIndex = format.length();

    ArrayList separators = new ArrayList();
    IntArray zeroSizes = new IntArray();
    IntArray formats = new IntArray();

    CharBuffer cb = new CharBuffer();
    int i = 0;
    while (i < format.length()) {
      char ch;

      // scan the separator
      cb.clear();
      for (; i < format.length(); i++) {
        ch = format.charAt(i);
        if (Character.isLetterOrDigit(ch))
          break;
        cb.append(ch);
      }

      // head and tail separators are just sticked on the ends
      if (head == null)
        head = cb.toString();
      else if (i >= format.length())
        tail = cb.toString();
      else
        separators.add(cb.toString());

      if (i >= format.length())
        break;

      // scan the format code
      int zeroSize = 1;
      int code = '0';
      for (; i < format.length(); i++) {
        ch = format.charAt(i);
        if (! Character.isLetterOrDigit(ch))
          break;

        if (! Character.isDigit(ch)) {
          if (code != '0' || zeroSize != 1)
            code = 0;
          else
            code = ch;
        }
        else if (Character.digit(ch, 10) == 0 && zeroSize >= 0)
          zeroSize++;
        else if (Character.digit(ch, 10) == 1)
          code = ch - 1;
        else
          code = 0;
      }
      if (code == 0)
        code = '0';

      zeroSizes.add(zeroSize);
      formats.add(code);
    }

    // default format is '1'
    if (formats.size() == 0) {
      tail = head;
      head = "";
      formats.add('0');
      zeroSizes.add(0);
    }

    // default separator is '.'
    if (separators.size() == 0)
      separators.add(".");
    if (separators.size() < formats.size())
      separators.add(separators.get(separators.size() - 1));

    this.separators = (String []) separators.toArray(new String[separators.size()]);
    this.zeroSize = zeroSizes.toArray();
    this.formats = formats.toArray();

    if (head == null)
      head = "";
    if (tail == null)
      tail = "";
  }
  
  public String getFormat()
  {
    return format;
  }
  
  public String getLang()
  {
    return lang;
  }
  
  public boolean isAlphabetic()
  {
    return isAlphabetic;
  }
  
  public String getGroupSeparator()
  {
    return groupSeparator;
  }
  
  public int getGroupSize()
  {
    return groupSize;
  }

  /**
   * Converts the array of numbers into a formatted number
   *
   * @param numbers array of numbers
   */
  void format(XslWriter out, IntArray numbers) 
  {
    CharBuffer buf = new CharBuffer();
    
    buf.append(head);

    int i;
    for (i = numbers.size() - 1; i >= 0; i--) {
      int index = numbers.size() - i - 1;
      if (index >= formats.length)
        index = formats.length - 1;

      char code = (char) formats[index];
      int zeroCount = zeroSize[index];

      int count = numbers.get(i);

      switch (code) {
      case 'i':
        romanize(buf, "mdclxvi", count);
        break;

      case 'I':
        romanize(buf, "MDCLXVI", count);
        break;
      
      case 'a':
        formatAlpha(buf, 'a', count);
        break;
      
      case 'A':
        formatAlpha(buf, 'A', count);
        break;
      
      default:
        formatDecimal(buf, code, zeroCount, count);
        break;
      }

      if (i > 0)
        buf.append(separators[index]);
    }

    buf.append(tail);

    out.print(buf.toString());
  }

  /**
   * Formats a roman numeral.  Numbers bigger than 5000 are formatted as
   * a decimal.
   *
   * @param cb buffer to accumulate the result
   * @param xvi roman characters, e.g. "mdclxvi" and "MDCLXVI" 
   * @param count the number to convert,
   */
  private void romanize(CharBuffer cb, String xvi, int count)
  {
    if (count <= 0)
      throw new RuntimeException();

    if (count > 5000) {
      cb.append(count);
      return;
    }

    for (; count > 1000; count -= 1000)
      cb.append(xvi.charAt(0));

    romanize(cb, xvi.charAt(0), xvi.charAt(1), xvi.charAt(2), count / 100);
    count %= 100;
    romanize(cb, xvi.charAt(2), xvi.charAt(3), xvi.charAt(4), count / 10);
    count %= 10;
    romanize(cb, xvi.charAt(4), xvi.charAt(5), xvi.charAt(6), count);
  }

  /**
   * Convert a single decimal digit to a roman number
   *
   * @param cb buffer to accumulate the result.
   * @param x character for the tens number
   * @param v character for the fives number
   * @param i character for the ones number
   * @param count digit to convert
   */
  private void romanize(CharBuffer cb, char x, char v, char i, int count)
  {
    switch (count) {
    case 0:
      break;
    case 1:
      cb.append(i);
      break;
    case 2:
      cb.append(i);
      cb.append(i);
      break;
    case 3:
      cb.append(i);
      cb.append(i);
      cb.append(i);
      break;
    case 4:
      cb.append(i);
      cb.append(v);
      break;
    case 5:
      cb.append(v);
      break;
    case 6:
      cb.append(v);
      cb.append(i);
      break;
    case 7:
      cb.append(v);
      cb.append(i);
      cb.append(i);
      break;
    case 8:
      cb.append(v);
      cb.append(i);
      cb.append(i);
      cb.append(i);
      break;
    case 9:
      cb.append(i);
      cb.append(x);
      break;

    default:
      throw new RuntimeException();
    }
  }

  /**
   * Format an alphabetic number.  Only English encodings are supported. 
   *
   * @param cb buffer to accumulate results
   * @param a starting character
   * @param count number to convert
   */
  private void formatAlpha(CharBuffer cb, char a, int count)
  {
    if (count <= 0)
      throw new RuntimeException();
    
    int index = cb.length();
    while (count > 0) {
      count--;
      cb.insert(index, (char) (a + count % 26));

      count /= 26;
    }
  }

  /**
   * Format a decimal number.
   *
   * @param cb buffer to accumulate results
   * @param code code for the zero digit
   * @param zeroCount minimum digits
   * @param count number to convert
   */
  private void formatDecimal(CharBuffer cb, int code,
                             int zeroCount, int count)
  {
    int digits = 0;
    int index = cb.length();
    while (count > 0) {
      if (digits > 0 && digits % groupSize == 0)
        cb.insert(index, groupSeparator);
      cb.insert(index, (char) (code + count % 10));
      count /= 10;
      digits++;
    }
    while (cb.length() - index < zeroCount) {
      cb.insert(index, (char) code);
    }
  }
}
