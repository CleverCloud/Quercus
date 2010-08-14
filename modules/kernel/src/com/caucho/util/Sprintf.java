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
 * @author Sam
 */

package com.caucho.util;

public class Sprintf {
  private static final L10N L = new L10N(Sprintf.class);

  /** copied then modified from com.caucho.es.Printf */

  private final static int ALT = 0x01;
  private final static int ZERO_FILL = 0x02;
  private final static int POS_PLUS = 0x04;
  private final static int POS_SPACE = 0x08;
  private final static int LALIGN = 0x10;
  private final static int BIG = 0x20;
  private final static int NO_TRAIL_ZERO = 0x40;
  private final static int JAVAESCAPE = 0x80;
  private final static int CSVESCAPE = 0x100;
  private final static int XMLESCAPE = 0x200;

  private static char []digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                  'a', 'b', 'c', 'd', 'e', 'f'};
  private static char []bigDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                     'A', 'B', 'C', 'D', 'E', 'F'};

  /**
   * An implementation of the classic sprintf.<p>
   *
   * `sprintf' accepts a series of arguments, applies to each a format 
   * specifier  from `format', and returns the formatted data as a string.
   * `format' is a string containing two types of objects:  ordinary
   * characters (other than `%'), which are copied unchanged to the output, and
   * conversion specifications, each of which is introduced by `%'. (To include
   * `%' in the output, use `%%' in the format string). <p>
   *
   * A conversion specification has the following form:
   *
   * <pre>%[FLAGS][WIDTH][.PREC][TYPE]</pre>
   *
   * TYPE is required, the rest are optional.
   *
   * The following TYPE's are supported:
   *
   * <table>
   * <tr><td>%%<td>a percent sign
   * <tr><td>%c<td>a character with the given number
   * <tr><td>%s<td>a string, a null string becomes "#null"
   * <tr><td>%z<td>a string, a null string becomes the empty string ""
   * <tr><td>%d<td>a signed integer, in decimal
   * <tr><td>%o<td>an integer, in octal
   * <tr><td>%u<td>an integer, in decimal
   * <tr><td>%x<td>an integer, in hexadecimal
   * <tr><td>%X<td>an integer, in hexadecimal using upper-case letters
   * <tr><td>%e<td>a floating-point number, in scientific notation
   * <tr><td>%E<td>a floating-point number, like %e with an upper-case "E"
   * <tr><td>%f<td>a floating-point number, in fixed decimal notation
   * <tr><td>%g<td>a floating-point number, in %e or %f notation
   * <tr><td>%G<td>a floating-point number, like %g with an upper-case "E"
   * <tr><td>%p<td>a pointer (outputs a value like the default of toString())
   * </table>
   *
   * Intepret the word `integer' to mean the java type long.  
   * Since java does not support unsigned integers, all integers are treated 
   * the same.<p>
   * 
   * The following optional FLAGS are supported:
   * <table>
   * <tr><td>0<td>If the TYPE character is an integer leading zeroes are used 
   *              to pad the field width instead of spaces (following any 
   *              indication of sign or base).
   *
   * <tr><td>+<td>Include a `+' with positive numbers.
   *
   * <tr><td>(a space)<td>use a space placeholder for the `+' that would result
   *                      from a positive number
   * <tr><td>-<td>The result of is left justified, and the right is padded with
   *              blanks until the result is `WIDTH' in length.  If you do not 
   *              use this flag, the result is right justified, and padded on 
   *              the left.
   * <tr><td>#<td>an alternate display is used, for `x' and `X' a
   *              non-zero result will have an "0x" prefix; for floating 
   *              point numbers the result will always contain a decimal point.
   *
   * <tr><td>j<td>escape a string suitable for a Java string, or a CSV file. 
   *              The following escapes are applied: " becomes \", 
   *              newline becomes \n, return becomes \r, \ becomes \\.
   *
   * <tr><td>v<td>escape a string suitable for CSV files, the same as `j'
   *              with an additional <code>"</code> placed at the beginning 
   *              and ending of the string
   *
   * <tr><td>m<td>escape a string suitable for a XML file.  The following
   *              escapes are applied: &lt; becomes &amp;lt;, 
   *              &gt; becomes &amp;gt; &amp; becomes &amp;amp;
   *              ' becomes &amp;#039, " becomes &amp;034;
   * </table>
   *
   * The optional WIDTH argument specifies a minium width for the field.
   * Spaces are used unless the `0' FLAG was used to indicate 0 padding.<p>
   *
   * The optional PREC argument is introduced with a `.', and gives the 
   * maximum number of characters to print; or the minimum
   * number of digits to print for integer and hex values; or the maximum 
   * number of significant digits for `g' and `G'; or the number of digits 
   * to print after the decimal point for floating points.<p>
   */

  public static String sprintf(String format, Object[] args)
  {
    String ret;
    CharBuffer cb = CharBuffer.allocate();
    try {
      sprintf(cb,format,args);
    }
    finally {
      ret = cb.close();
    }
    return ret;
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9)
  {
    return sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8)
  {
    return sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7)
  {
    return sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6)
  {
    return sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5, arg6 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5)
  {
    return sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4, arg5 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4)
  {
    return sprintf(format, new Object[] { arg0, arg1, arg2, arg3, arg4 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2, Object arg3)
  {
    return sprintf(format, new Object[] { arg0, arg1, arg2, arg3 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1, Object arg2)
  {
    return sprintf(format, new Object[] { arg0, arg1, arg2 } );
  }

  public static String sprintf(String format, Object arg0, Object arg1)
  {
    return sprintf(format, new Object[] { arg0, arg1 } );
  }

  public static String sprintf(String format, Object arg0)
  {
    return sprintf(format, new Object[] { arg0 } );
  }

  public static CharBuffer sprintf(CharBuffer result,
                                   String format,
                                   Object ... args)
  {
    int length = args.length;
    int arg = 0;
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
        case 'j': flags |= JAVAESCAPE; break;
        case 'v': flags |= JAVAESCAPE; flags |= CSVESCAPE; break;
        case 'm': flags |= XMLESCAPE; break;

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
      case 'D':
      case 'i':
      case 'u':
      case 'U':
        if (arg >= length)
          missing(arg);
        formatInteger(result, toLong(args[arg++]), width, prec, flags, 10);
        break;

      case 'o':
      case 'O':
        if (arg >= length)
          missing(arg);
        formatInteger(result, toLong(args[arg++]), width, prec, flags, 8);
        break;

      case 'X':
        flags |= BIG;
      case 'x':
        if (arg >= length)
          missing(arg);
        formatInteger(result, toLong(args[arg++]), width, prec, flags, 16);
        break;

      case 'E':
      case 'G':
        flags |= BIG;
      case 'f':
      case 'e':
      case 'g':
        if (arg >= length)
          missing(arg);
        formatDouble(result, toDouble(args[arg++]), width, prec, flags, ch);
        break;

      case 'c':
        if (arg >= length)
          missing(arg);
        formatChar(result, toLong(args[arg++]), width, flags);
        break;

      case 's':
        if (arg >= length)
          missing(arg);
        formatString(result, toString(args[arg++],"#null"), prec, width, flags);
        break;

      case 'z':
        if (arg >= length)
          missing(arg);
        formatString(result, toString(args[arg++],""), prec, width, flags);
        break;

      case 'p':
        if (arg >= length)
          missing(arg);

        // like the default for toString()
        Object o  = args[arg++];
        result.append(o.getClass().getName());
        result.append('@');
        result.append(Integer.toHexString(o.hashCode()));

      default:
        fixBits(result, format, start, i + 1);
        break;
      }
    }

    return result;
  }

  private static void missing(int arg)
  {
    throw new IllegalArgumentException(L.l("missing sprintf argument {0}",arg));
  }

  public static void formatDouble(CharBuffer cb, double value,
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

  public static void formatDouble(CharBuffer cb, double value,
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

  public static void formatInteger(CharBuffer cb, long dvalue,
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

  public static void formatInteger(CharBuffer cb, double dvalue,
                                   int width, int prec, int flags, int radix)
  {
    long value;
    if (dvalue > 0)
      value = (long) (dvalue + 0.5);
    else
      value = (long) (dvalue - 0.5);

    formatInteger(cb,value,width,prec,flags,radix);
  }

  public static void formatChar(CharBuffer cb, long ch, int width, int flags)
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

  public static void formatString(CharBuffer cb, String string,
                                  int prec, int width, int flags)
  {
    int offset = cb.length();

    if (prec < 0)
      prec = Integer.MAX_VALUE;

    if ((flags & CSVESCAPE) != 0) { cb.append('\"'); offset+=1; width -= 1; }

    for (int i = 0; i < string.length() && i < prec; i++) {
      width--;
      char ch = string.charAt(i);
      if ((flags & JAVAESCAPE) != 0) {
        switch (ch) {
          case '\\':
            cb.append("\\\\"); offset+=1; width-=1; continue;
          case '\n':
            cb.append("\\n"); offset+=1; width-=1; continue;
          case '\r':
            cb.append("\\r"); offset+=1; width-=1; continue;
          case '\"':
            cb.append("\\\""); offset+=1; width-=1; continue;
        }
      }
      if ((flags & XMLESCAPE) != 0) {
        switch (ch) {
          case '<':
            cb.append("&lt;"); offset+=3; width-=3; continue;
          case '>':
            cb.append("&gt;"); offset+=3; width-=3; continue;
          case '&':
            cb.append("&amp;"); offset+=4; width-=4; continue;
          case '\'':
            cb.append("&#039;"); offset+=5; width-=5; continue;
          case '"':
            cb.append("&#034;"); offset+=5; width-=5; continue;
        }
      }
      cb.append(ch);
    }

    if ((flags & CSVESCAPE) != 0) { cb.append('\"'); offset+=1; width -= 1; }

    if ((flags & LALIGN) == 0) {
      for (int i = 0; i < width; i++)
        cb.insert(offset, (char) ' ');
    } else {
      for (int i = 0; i < width; i++)
        cb.append((char) ' ');
    }
  }

  private static void fixBits(CharBuffer cb, String format, int s, int i)
  {
    for (; s < i; s++)
      cb.append((char) format.charAt(s));
  }

  /**
   * Converts some unknown value to a double.
   *
   * @param value the value to be converted.
   *
   * @return the double-converted value.
   */
  public static double toDouble(Object value)
  {
    /** copied from com.caucho.el.Expr */

    if (value == null)
      return 0;
    else if (value instanceof Number) {
      double dValue = ((Number) value).doubleValue();

      if (Double.isNaN(dValue))
        return 0;
      else
        return dValue;
    }
    else if (value.equals(""))
      return 0;
    else if (value instanceof String) {
      double dValue = Double.parseDouble((String) value);

      if (Double.isNaN(dValue))
        return 0;
      else
        return dValue;
    }
    else {
      throw new IllegalArgumentException(L.l("can't convert {0} to double.",
                                             value.getClass().getName()));
    }
  }

  /**
   * Converts some unknown value to a long.
   *
   * @param value the value to be converted.
   *
   * @return the long-converted value.
   */
  public static long toLong(Object value)
  {
    /** copied from com.caucho.el.Expr */

    if (value == null)
      return 0;
    else if (value instanceof Number)
      return ((Number) value).longValue();
    else if (value.equals(""))
      return 0;
    else if (value instanceof String) {
      int sign = 1;
      String string = (String) value;
      int length = string.length();
      long intValue = 0;

      int i = 0;
      for (; i < length && Character.isWhitespace(string.charAt(i)); i++) {
      }

      if (length <= i)
        return 0;

      int ch = string.charAt(i);
      if (ch == '-') {
        sign = -1;
        i++;
      }
      else if (ch == '+')
        i++;

      for (; i < length; i++) {
        ch = string.charAt(i);

        if (ch >= '0' && ch <= '9') {
          intValue = 10 * intValue + ch - '0';
        }
        else if (ch == '.') {
          // truncate the decimal
          i = length;
          break;
        } else
          break;
      }

      for (; i < length && Character.isWhitespace(string.charAt(i)); i++) {
      }

      if (i < length)
        throw new IllegalArgumentException(L.l("can't convert '{0}' to long.", string));

      return sign * intValue;

    }
    else {
      throw new IllegalArgumentException(L.l("can't convert {0} to long.",
                                             value.getClass().getName()));
    }
  }

  /**
   * Converts some unknown value to a String.
   *
   * @param value the value to be converted.
   * @param nullValue the value to return if value == null
   *
   * @return the String converted value
   */
  public static String toString(Object value, String nullValue)
  {
    return (value == null) ? nullValue : value.toString();
  }

}
