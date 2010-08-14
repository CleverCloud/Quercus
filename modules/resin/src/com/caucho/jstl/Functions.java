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

package com.caucho.jstl;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.lang.reflect.*;
import java.util.*;

/**
 * Tag representing the functions.
 */
public class Functions {
  private static L10N L = new L10N(Functions.class);

  /**
   * Returns the length of the object
   */
  public static int length(Object obj)
  {
    if (obj == null)
      return 0;

    else if (obj instanceof CharSequence)
      return ((CharSequence) obj).length();

    else if (obj instanceof Collection)
      return ((Collection) obj).size();
      
    else if (obj instanceof Map)
      return ((Map) obj).size();

    else if (obj.getClass().isArray())
      return Array.getLength(obj);

    else if (obj instanceof Iterator) {
      Iterator iter = (Iterator) obj;
      
      int count = 0;
      while (iter.hasNext()) {
        count++;

        iter.next();
      }
      
      return count;
    }

    else
      return 0;
  }

  /**
   * Returns true if the first string contains the second.
   */
  public static boolean contains(String whole, String part)
  {
    if (whole == null)
      whole = "";

    if (part == null)
      part = "";

    return whole.indexOf(part) >= 0;
  }

  /**
   * Returns true if the first string contains the second.
   */
  public static boolean containsIgnoreCase(String whole, String part)
  {
    if (whole == null)
      whole = "";

    if (part == null)
      part = "";

    whole = whole.toUpperCase();
    part = part.toUpperCase();
    
    return whole.indexOf(part) >= 0;
  }

  /**
   * Returns true if the first string contains the second.
   */
  public static boolean endsWith(String whole, String part)
  {
    if (whole == null)
      whole = "";

    if (part == null)
      part = "";

    return whole.endsWith(part);
  }

  /**
   * Escapes the XML string.
   */
  public static String escapeXml(String string)
  {
    if (string == null)
      return "";

    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < string.length(); i++) {
      int ch = string.charAt(i);

      switch (ch) {
      case '<':
        cb.append("&lt;");
        break;
      case '>':
        cb.append("&gt;");
        break;
      case '&':
        cb.append("&amp;");
        break;
      case '\'':
        cb.append("&#039;");
        break;
      case '"':
        cb.append("&#034;");
        break;
      default:
        cb.append((char) ch);
        break;
      }
    }

    return cb.close();
  }

  /**
   * Returns the index of the part in the whole.
   */
  public static int indexOf(String whole, String part)
  {
    if (whole == null)
      whole = "";

    if (part == null)
      part = "";

    return whole.indexOf(part);
  }

  /**
   * Joins the array.
   */
  public static String join(String []array, String sep)
  {
    if (array == null)
      return "";

    if (sep == null)
      sep = "";

    CharBuffer result = CharBuffer.allocate();

    for (int i = 0; i < array.length; i++) {
      if (i != 0)
        result.append(sep);

      result.append(array[i]);
    }

    return result.close();
  }

  /**
   * Replaces substrings.
   */
  public static String replace(String input, String before, String after)
  {
    if (input == null)
      return "";

    if (before == null || before.equals(""))
      return input;

    if (after == null)
      after = "";

    CharBuffer result = CharBuffer.allocate();

    int head = 0;
    int next;
    while ((next = input.indexOf(before, head)) >= 0) {
      result.append(input.substring(head, next));

      result.append(after);

      head = next + before.length();
    }

    result.append(input.substring(head));

    return result.close();
  }

  /**
   * Splits into
   */
  public static String []split(String input, String delimiters)
  {
    if (input == null || input.equals("")) {
      return new String[] {""};
    }

    if (delimiters == null || delimiters.equals("")) {
      return new String[] { input };
    }

    StringTokenizer tokenizer = new StringTokenizer(input, delimiters);
    ArrayList<String> values = new ArrayList<String>();

    while (tokenizer.hasMoreTokens())
      values.add(tokenizer.nextToken());

    String []array = new String[values.size()];

    return values.toArray(array);
  }

  /**
   * Returns true if the first string starts with the second.
   */
  public static boolean startsWith(String whole, String part)
  {
    if (whole == null)
      whole = "";

    if (part == null)
      part = "";

    return whole.startsWith(part);
  }

  /**
   * Returns true if the first string starts with the second.
   */
  public static String substring(String string, int begin, int end)
  {
    if (string == null || string.equals(""))
      return "";

    int length = string.length();
    
    if (begin < 0)
      begin = 0;

    if (length <= begin)
      return "";

    if (end < 0 || length < end)
      end = length;

    if (end <= begin)
      return "";

    return string.substring(begin, end);
  }

  /**
   * Returns the substring after the match.
   */
  public static String substringAfter(String whole, String part)
  {
    if (whole == null || whole.equals(""))
      return "";

    if (part == null || part.equals(""))
      return whole;

    int p = whole.indexOf(part);

    if (p < 0)
      return "";
    else
      return whole.substring(p + part.length());
  }

  /**
   * Returns the substring before the match.
   */
  public static String substringBefore(String whole, String part)
  {
    if (whole == null || whole.equals(""))
      return "";

    if (part == null || part.equals(""))
      return "";

    int p = whole.indexOf(part);

    if (p < 0)
      return "";
    else
      return whole.substring(0, p);
  }

  /**
   * Convert to lower case.
   */
  public static String toLowerCase(String string)
  {
    if (string == null || string.equals(""))
      return "";

    return string.toLowerCase();
  }

  /**
   * Convert to upper case.
   */
  public static String toUpperCase(String string)
  {
    if (string == null || string.equals(""))
      return "";

    return string.toUpperCase();
  }

  /**
   * Trim
   */
  public static String trim(String string)
  {
    if (string == null || string.equals(""))
      return "";

    return string.trim();
  }
}
