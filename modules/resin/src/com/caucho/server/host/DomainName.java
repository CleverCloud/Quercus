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

package com.caucho.server.host;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Domain name normalization
 */
public class DomainName {
  static final Logger log 
    = Logger.getLogger(DomainName.class.getName());
  static final L10N L = new L10N(DomainName.class);

  private final static char ENCODE[];
  private final static int DECODE[];

  private final static int base = 36;
  private final static int tmin = 1;
  private final static int tmax = 26;
  private final static int skew = 38;
  private final static int damp = 700;
  private final static int initialBias = 72;
  private final static int initialN = 128;
  
  /**
   * Converts from the ascii "punicode" to a string.
   */
  public static String fromAscii(String source)
  {
    CharBuffer result = CharBuffer.allocate();
    CharBuffer cb = CharBuffer.allocate();

    int index = 0;
    int length = source.length();
    boolean isFirst = true;

    try {
      while (index < length) {
        char ch = source.charAt(index + 0);
      
        if (isFirst && index + 4 < length &&
            source.charAt(index + 0) == 'x' &&
            source.charAt(index + 1) == 'n' &&
            source.charAt(index + 2) == '-' &&
            source.charAt(index + 3) == '-') {
          int p = source.indexOf('.', index);
          String seq;

          if (p < 0)
            seq = source.substring(index + 4);
          else
            seq = source.substring(index + 4, p);

          decode(result, cb, seq);

          index += 4 + seq.length();
          continue;
        }

        index++;

        isFirst = false;

        if (ch == '.') {
          isFirst = true;

          result.append(ch);
        }
        else
          result.append(Character.toLowerCase(ch));
      }

      return result.close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      throw new RuntimeException(e);
    }
  }

  /**
   * Converts to the ascii "punicode".
   */
  public static String toAscii(String source)
  {
    CharBuffer result = CharBuffer.allocate();
    CharBuffer cb = CharBuffer.allocate();

    int head = 0;
    int length = source.length();

    while (head < length) {
      boolean isAscii = true;

      cb.clear();
      
      int i = head;
      for (; i < length; i++) {
        char ch = source.charAt(i);

        if (ch == '.') {
          cb.append(ch);
          break;
        }
        else if (ch <= 0x7f)
          cb.append(ch);
        else {
          isAscii = false;
          break;
        }
      }

      if (isAscii) {
        head = i + 1;
        result.append(cb);
        continue;
      }

      cb.clear();
      i = head;
      for (; i < length; i++) {
        char ch = source.charAt(i);

        if (ch == '.')
          break;

        //cb.append(Character.toLowerCase(ch));
        cb.append(ch);
      }
      head = i;

      String seq = cb.toString();

      cb.clear();

      toAscii(cb, seq);

      result.append(cb);
    }

    return result.close();
  }

  private static void decode(CharBuffer result, CharBuffer cb, String seq)
  {
    int length = seq.length();
    int b = 0;

    for (int i = 0; i < length; i++) {
      char ch = seq.charAt(i);

      if (ch == '-')
        b = i;
    }

    for (int i = 0; i < b; i++) {
      char ch = seq.charAt(i);
      
      cb.append(Character.toLowerCase(ch));
    }

    int in = b > 0 ? b + 1 : 0;
    int i = 0;
    int bias = initialBias;
    int out = cb.length();
    int n = initialN;

    while (in < length) {
      int oldi = i;
      int w = 1;

      for (int k = base; true; k += base) {
        char ch = seq.charAt(in++);
        int digit = DECODE[ch];

        i += digit * w;

        int t;
        if (k <= bias)
          t = tmin;
        else if (bias + tmax <= k)
          t = tmax;
        else
          t = k - bias;

        if (digit < t)
          break;

        w *= (base - t);
      }

      bias = adapt(i - oldi, out + 1, oldi == 0);

      n += i / (out + 1);
      i %= (out + 1);

      cb.append(' ');
      char []cBuf = cb.getBuffer();

      System.arraycopy(cBuf, i, cBuf, i + 1, out - i);
      cBuf[i++] = Character.toLowerCase((char) n);

      out++;
    }

    result.append(cb);
  }

  private static void toAscii(CharBuffer cb, String seq)
  {
    cb.append("xn--");

    int length = seq.length();

    int index = 0;
    int n = initialN;
    int delta = 0;
    int bias = initialBias;
    int b = 0; // # of basic code points

    for (int i = 0; i < length; i++) {
      char ch = seq.charAt(i);
      if (ch < 0x80) {
        cb.append(ch);
        b++;
      }
    }

    if (b > 0)
      cb.append('-');

    int h = b;

    while (h < length) {
      int m = 0xffff;
      
      for (int i = 0; i < length; i++) {
        char ch = seq.charAt(i);

        if (n <= ch && ch < m)
          m = ch;
      }

      // XXX: overflow
      delta = delta + (m - n) * (h + 1);
      n = m;

      for (int i = 0; i < length; i++) {
        int ch = seq.charAt(i);

        if (ch < n) {
          delta++;
        }

        if (ch == n) {
          int q = delta;

          for (int k = base; true; k += base) {
            int t;

            if (k <= bias)
              t = tmin;
            else if (bias + tmax <= k)
              t = tmax;
            else
              t = k - bias;

            if (q < t)
              break;

            cb.append(ENCODE[t + (q - t) % (base - t)]);
            q = (q - t) / (base - t);
          }

          cb.append(ENCODE[q]);
          bias = adapt(delta, h + 1, h == b);
          delta = 0;
          h++;
        }
      }

      delta++;
      n++;
    }
  }

  private static int adapt(int delta, int nPoints, boolean isFirst)
  {
    int k;

    delta = isFirst ? delta / damp : delta / 2;
    delta += delta / nPoints;

    for (k = 0; ((base - tmin) * tmax) / 2 < delta; k += base) {
      delta /= base - tmin;
    }

    return k + (base - tmin + 1) * delta / (delta + skew);
  }

  static {
    ENCODE = new char[36];

    for (int i = 0; i < 26; i++) {
      ENCODE[i] = (char) ('a' + i);
    }

    for (int i = 0; i < 10; i++) {
      ENCODE[i + 26] = (char) ('0' + i);
    }
    
    DECODE = new int[0x80];

    for (int i = 0; i < 26; i++) {
      DECODE[(char) ('a' + i)] = i;
      DECODE[(char) ('A' + i)] = i;
    }

    for (int i = 0; i < 10; i++) {
      DECODE[(char) ('0' + i)] = 26 + i;
    }
  }
}
