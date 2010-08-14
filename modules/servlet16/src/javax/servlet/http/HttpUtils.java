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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.servlet.http;

import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * @deprecated
 */
public class HttpUtils {
  /**
   * Converts a queryString to a hashtable.
   */
  public static Hashtable<String,String[]> parseQueryString(String query)
  {
    Hashtable<String,String[]> table = new Hashtable<String,String[]>();
    int length = query.length();
    int i = 0;
    char ch;
    char []buf = new char[length];
    int offset;

    while (i < length) {
      for (ch = query.charAt(i);
           i < length && (Character.isWhitespace((ch = query.charAt(i))) || ch == '&');
           i++) {
      }

      offset = 0;
      for (; i < length && (ch = query.charAt(i)) != '='; i++) {
        if (ch == '+')
          buf[offset++] = ' ';
        else if (ch == '%' && i + 2 < length) {
          int ch1 = query.charAt(++i);
          int ch2 = query.charAt(++i);

          buf[offset++] = (char) ((toHex(ch1) << 4) + toHex(ch2));
        }
        else
          buf[offset++] = (char) ch;
      }

      if (offset == 0)
        break;

      String key = new String(buf, 0, offset);
      offset = 0;
      for (i++; i < length && (ch = query.charAt(i)) != '&'; i++) {
        if (ch == '+')
          buf[offset++] = (char) ' ';
        else if (ch == ' ') { // XXX:
        }
        else if (ch == '%' && i + 2 < length) {
          int ch1 = query.charAt(++i);
          int ch2 = query.charAt(++i);

          buf[offset++] = (char) ((toHex(ch1) << 4) + toHex(ch2));
        }
        else
          buf[offset++] = (char) ch;
      }
      
      i++;

      String value = new String(buf, 0, offset);
      String []oldValue = (String []) table.get(key);
      if (oldValue == null)
        table.put(key, new String[] { value });
      else {
        String []newValue = new String[oldValue.length + 1];
        System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
        newValue[oldValue.length] = value;
        table.put(key, newValue);
      }
    }

    return table;
  }

  /**
   * Parses POST data using www-form-urlencoding
   */
  public static Hashtable<String,String[]> parsePostData(int length, ServletInputStream is)
  {
    try {
      if (length >= 0) {
        byte buf[] = new byte[length];
        int offset = 0;

        while (length > 0) {
          int sublen;
          
          sublen = is.read(buf, offset, length);

          if (sublen > 0) {
            offset += sublen;
            length -= sublen;
          }
          else
            throw new IOException("unexpected end of file");
        }
      
        return parseQueryString(new String(buf, 0, buf.length));
      }
      else
        return new Hashtable<String,String[]>();
    } catch (IOException e) {
      throw new IllegalArgumentException("illegal post data");
    }
  }

  /**
   * Converts the request back to an original request URL.
   */
  public static StringBuffer getRequestURL(HttpServletRequest req)
  {
    StringBuffer sb = new StringBuffer();

    sb.append(req.getScheme());
    sb.append("://");
    sb.append(req.getServerName());
    if (req.getServerPort() > 0 &&
        req.getServerPort() != 80 &&
        req.getServerPort() != 443) {
      sb.append(":");
      sb.append(req.getServerPort());
    }
    sb.append(req.getRequestURI());

    return sb;
  }

  /**
   * Convert a single digit to a hex digit.
   */
  private static int toHex(int ch)
  {
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    else if (ch >= 'a' && ch <= 'f')
      return ch - 'a' + 10;
    else if (ch >= 'A' && ch <= 'F')
      return ch - 'A' + 10;
    else
      return -1;
  }
}
