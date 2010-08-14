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

package com.caucho.es;

import com.caucho.util.CharBuffer;

class Printf {
  private final static int ALT = 0x01;
  private final static int ZERO_FILL = 0x02;
  private final static int POS_PLUS = 0x04;
  private final static int POS_SPACE = 0x08;
  private final static int LALIGN = 0x10;
  private final static int BIG = 0x20;
  private final static int NO_TRAIL_ZERO = 0x40;

  private static char []digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                          'a', 'b', 'c', 'd', 'e', 'f'};
  private static char []bigDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                             'A', 'B', 'C', 'D', 'E', 'F'};

  private Printf()
  {
  }

  public static String sprintf(Call eval, int length)
    throws Throwable
  {
    if (length == 0)
      return "";

    CharBuffer buf = new CharBuffer();

    printf(buf, eval.getArg(0).toStr(), eval, length);

    return buf.toString();
  }

  public static CharBuffer printf(CharBuffer result, ESString format, 
                                  Call eval, int length)
                                  throws Throwable
  {
    int arg = 1;
    int len = format.length();

    for (int i = 0; i < len; i++) {
      int ch;
      int start = i;

      if ((ch = format.charAt(i)) != '%') {
        result.append((char) ch);
        continue;
      }

      int flags = 0;
    loop:
      while (++i < len) {
        switch ((ch = format.charAt(i))) {
        case '0': flags |= ZERO_FILL; break;
        case '+': flags |= POS_PLUS; break;
        case ' ': flags |= POS_SPACE; break;
        case '#': flags |= ALT; break;
        case '-': flags |= LALIGN; break;

        default: break loop;
        }
      }

      int width = 0;
      for (; i < len && (ch = format.charAt(i)) >= '0' && ch <= '9'; i++) {
        width = 10 * width + ch - '0';
      }

      if (i >= len) {
        fixBits(result, format, start, i);
        break;
      }

      int prec = 0;
      if (ch == '.') {
        while (++i < len && (ch = format.charAt(i)) >= '0' && ch <= '9') {
          prec = 10 * prec + ch - '0';
        }
      } else
        prec = -1;

      if (i >= len) {
        fixBits(result, format, start, i);
        break;
      }

      switch (ch) {
      case '%':
        result.append((char) '%');
        break;

      case 'd':
        if (arg >= length)
          throw new ESException("missing printf argument");
        formatInteger(result, eval.getArg(arg++).toNum(),
                      width, prec, flags, 10);
        break;

      case 'o':
        if (arg >= length)
          throw new ESException("missing printf argument");
        formatInteger(result, eval.getArg(arg++).toNum(),
                      width, prec, flags, 8);
        break;

      case 'X':
        flags |= BIG;
      case 'x':
        if (arg >= length)
          throw new ESException("missing printf argument");
        formatInteger(result, eval.getArg(arg++).toNum(),
                      width, prec, flags, 16);
        break;

      case 'E':
      case 'G':
        flags |= BIG;
      case 'f':
      case 'e':
      case 'g':
        if (arg >= length)
          throw new ESException("missing printf argument");
        formatDouble(result, eval.getArg(arg++).toNum(),
                      width, prec, flags, ch);
        break;

      case 'c':
        if (arg >= length)
          throw new ESException("missing printf argument");
        formatChar(result, (int) eval.getArg(arg++).toNum(), width, flags);
        break;

      case 's':
        if (arg >= length)
          throw new ESException("missing printf argument");
        formatString(result, eval.getArg(arg++).toStr(),
                     prec, width, flags);
        break;

      default:
        fixBits(result, format, start, i + 1);
        break;
      }
    }

    return result;
  }

  private static void formatDouble(CharBuffer cb, double value,
                                  int prec, int flags, int type)
  {
    String raw = Double.toString(value);
    int expt = 0;
    int i = 0;
    CharBuffer digits = new CharBuffer();
    int ch = raw.charAt(i);
    boolean seenDigit = false;

    // XXX: locale screws us?
    for (; i < raw.length(); i++) {
      if ((ch = raw.charAt(i)) == '.' || ch == 'e' || ch == 'E')
        break;
      else if (! seenDigit && ch == '0') {
      }
      else {
        seenDigit = true;
        digits.append((char) ch);
        expt++;
      }
    }

    if (ch == '.')
      i++;

    for (; i < raw.length(); i++) {
      ch = raw.charAt(i);

      if (! seenDigit && ch == '0') {
        expt--;
      } else if (ch >= '0' && ch <= '9') {
        digits.append((char) ch);
        seenDigit = true;
      }
      else {
        int sign = 1;
        i++;
        if ((ch = raw.charAt(i)) == '+') {
          i++;
        }
        else if (ch == '-') {
          i++;
          sign = -1;
        }

        int e = 0;
        for (; i < raw.length() && (ch = raw.charAt(i)) >= '0' && ch <= '9';
             i++) {
          e = 10 * e + ch - '0';
        }

        expt += sign * e;
        break;
      }
    }

    if (! seenDigit)
      expt = 1;

    while (digits.length() > 0 && digits.charAt(digits.length() - 1) == '0')
      digits.setLength(digits.length() - 1);

    if (type == 'f') {
      if (roundDigits(digits, expt + prec)) {
        expt++;
      }

      formatFixed(cb, digits, expt, prec, flags);
    }
    else if (type == 'e' || type == 'E') {
      if (roundDigits(digits, prec + 1))
        expt++;

      formatExpt(cb, digits, expt, prec, flags);
    }
    else {
      if (roundDigits(digits, prec))
        expt++;

      if (expt < -3 || expt > prec)
        formatExpt(cb, digits, expt, prec - 1, flags|NO_TRAIL_ZERO);
      else
        formatFixed(cb, digits, expt, prec - expt, flags|NO_TRAIL_ZERO);
    }
  }

  private static void formatDouble(CharBuffer cb, double value,
                                  int width, int prec, int flags,
                                  int type)
  {
    if (prec < 0)
      prec = 6;

    int offset = cb.length();

    if ((flags & ZERO_FILL) != 0 &&
        (value < 0 || (flags & (POS_PLUS|POS_SPACE)) != 0)) {
        offset++;
        width--;
    }

    if (value < 0) {
      cb.append((char) '-');
      value = -value;
    } else if ((flags & POS_PLUS) != 0) {
      cb.append((char) '+');
    } else if ((flags & POS_SPACE) != 0) {
      cb.append((char) ' ');
    }

    formatDouble(cb, value, prec, flags, type);

    width -= cb.length() - offset;

    for (int i = 0; i < width; i++) {
      if ((flags & LALIGN) != 0)
        cb.append(' ');
      else
        cb.insert(offset, (flags & ZERO_FILL) == 0 ? ' ' : '0');
    }
  }

  private static boolean roundDigits(CharBuffer digits, int len)
  {
    if (len < 0 || digits.length() <= len)
      return false;

    int value = digits.charAt(len);
    if (value < '5')
      return false;

    for (int i = len - 1; i >= 0; i--) {
      int ch = digits.charAt(i);

      if (ch != '9') {
        digits.setCharAt(i, (char) (ch + 1));
        return false;
      }
      digits.setCharAt(i, '0');
    }

    digits.insert(0, '1');

    return true;
  }

  private static void formatFixed(CharBuffer cb, CharBuffer digits,
                                 int expt, int prec, int flags)
  {
    int i = 0;
    int origExpt = expt;

    for (; expt > 0; expt--) {
      if (i < digits.length())
        cb.append((char) digits.charAt(i++));
      else
        cb.append('0');
    }
    
    if (origExpt <= 0) // || digits.length() == 0)
      cb.append('0');

    if (prec > 0 || (flags & ALT) != 0)
      cb.append('.');

    for (; expt < 0 && prec > 0; expt++) {
      cb.append('0');
      prec--;
    }

    for (; prec > 0 && i < digits.length(); i++) {
      cb.append(digits.charAt(i));
      prec--;
    }

    for (; prec > 0 && (flags & (NO_TRAIL_ZERO|ALT)) != NO_TRAIL_ZERO; prec--)
      cb.append('0');
  }

  private static void formatExpt(CharBuffer cb, CharBuffer digits,
                                 int expt, int prec, int flags)
  {
    if (digits.length() == 0)
      cb.append('0');
    else
      cb.append((char) digits.charAt(0));

    if (prec > 0 || (flags & ALT) != 0)
      cb.append('.');

    for (int i = 1; i < digits.length(); i++) {
      if (prec > 0)
        cb.append((char) digits.charAt(i));
      prec--;
    }

    for (; prec > 0 && (flags & (NO_TRAIL_ZERO|ALT)) != NO_TRAIL_ZERO; prec--)
      cb.append('0');

    if ((flags & BIG) != 0)
      cb.append('E');
    else
      cb.append('e');

    formatInteger(cb, expt - 1, 0, 2, POS_PLUS, 10);
  }

  private static void formatInteger(CharBuffer cb, double dvalue,
                                   int width, int prec, int flags, int radix)
  {
    boolean isBig = (flags & BIG) != 0;
    int begin = cb.length();

    long value;
    if (dvalue > 0)
      value = (long) (dvalue + 0.5);
    else
      value = (long) (dvalue - 0.5);

    if (value < 0 && radix == 10) {
      cb.append((char) '-');
      value = -value;
    } else if (value >= 0 && radix == 10 && (flags & POS_PLUS) != 0)
      cb.append((char) '+');
    else if (value >= 0 && radix == 10 && (flags & POS_SPACE) != 0)
      cb.append((char) ' ');
    else if (value < 0)
      value &= 0xffffffffL;
    else if (radix == 8 && (flags & ALT) != 0 && value != 0)
      cb.append('0');
    else if (radix == 16 && (flags & ALT) != 0)
      cb.append((flags & BIG) == 0 ? "0x" : "0X");

    if ((flags & ZERO_FILL) != 0) {
      width -= cb.length() - begin;
      begin = cb.length();
    }

    int offset = cb.length();
    int len = 0;
    while (value != 0) {
      len++;
      cb.insert(offset, (isBig ? bigDigits : digits)[(int) (value % radix)]);
      value /= radix;
    }

    for (int i = 0; i < prec - len; i++)
      cb.insert(offset, '0');
    if (len == 0 && prec == 0)
      cb.insert(offset, '0');

    width -= cb.length() - begin;
    for (; width > 0; width--) {
      if ((flags & LALIGN) != 0)
        cb.append(' ');
      else if ((flags & ZERO_FILL) != 0 && prec < 0)
        cb.insert(begin, '0');
      else
        cb.insert(begin, ' ');
    }

    if (cb.length() == begin)
      cb.append('0');
  }

  private static void formatChar(CharBuffer cb, int ch, int width, int flags)
  {
    int offset = cb.length();

    cb.append((char) ch);

    if ((flags & LALIGN) == 0) {
      for (int i = 0; i < width - 1; i++)
        cb.insert(offset, (char) ' ');
    } else {
      for (int i = 0; i < width - 1; i++)
        cb.append((char) ' ');
    }
  }

  private static void formatString(CharBuffer cb, ESString string,
                                  int prec, int width, int flags)
  {
    int offset = cb.length();

    if (prec < 0)
      prec = Integer.MAX_VALUE;

    for (int i = 0; i < string.length() && i < prec; i++) {
      width--;
      cb.append(string.charAt(i));
    }

    if ((flags & LALIGN) == 0) {
      for (int i = 0; i < width; i++)
        cb.insert(offset, (char) ' ');
    } else {
      for (int i = 0; i < width; i++)
        cb.append((char) ' ');
    }
  }

  private static void fixBits(CharBuffer cb, ESString format, int s, int i)
  {
    for (; s < i; s++)
      cb.append((char) format.charAt(s));
  }
}
