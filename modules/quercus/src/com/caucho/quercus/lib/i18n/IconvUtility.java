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

package com.caucho.quercus.lib.i18n;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.env.*;
import com.caucho.vfs.*;

import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.*;

public class IconvUtility {
  private static final Logger log
    = Logger.getLogger(IconvUtility.class.getName());

  public static StringValue decodeEncode(Env env,
                                        StringValue str,
                                        String inCharset,
                                        String outCharset)
    throws UnsupportedEncodingException
  {
    return decodeEncode(env, str, inCharset, outCharset, 0, Integer.MAX_VALUE);
  }

  public static StringValue decodeEncode(Env env,
                                         StringValue str,
                                         String inCharset,
                                         String outCharset,
                                         int offset)
    throws UnsupportedEncodingException
  {
    return decodeEncode(env, str, inCharset, outCharset,
                        offset, Integer.MAX_VALUE);
  }

  /**
   * Decodes and encodes to specified charsets at the same time.
   */
  public static StringValue decodeEncode(Env env,
                                         StringValue str,
                                         String inCharset,
                                         String outCharset,
                                         int offset,
                                         int length)
    throws UnsupportedEncodingException
  {
    TempCharBuffer tb = TempCharBuffer.allocate();
    char[] charBuf = tb.getBuffer();

    try {
      Reader in;

      try {
        in = str.toReader(inCharset);
      } catch (IOException e) {
        log.log(Level.WARNING, e.toString(), e);
    
        in = str.toReader("utf-8");
      }

      TempStream ts = new TempStream();
      WriteStream out = new WriteStream(ts);

      try {
        out.setEncoding(outCharset);
      } catch (IOException e) {
        log.log(Level.WARNING, e.toString(), e);
    
        out.setEncoding("utf-8");
      }

      while (offset > 0) {
        if (in.read() < 0)
          break;
        offset--;
      }

      int sublen;

      while (length > 0
          && (sublen = in.read(charBuf, 0, charBuf.length)) >= 0) {

        sublen = Math.min(length, sublen);

        out.print(charBuf, 0, sublen);
        length -= sublen;
      }

      out.flush();

      StringValue sb = env.createBinaryBuilder();
      for (TempBuffer ptr = ts.getHead(); ptr != null; ptr = ptr.getNext()) {
        sb.append(ptr.getBuffer(), 0, ptr.getLength());
      }
      
      return sb;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }

    finally {
      TempCharBuffer.free(tb);
    }
  }

  /**
   * Returns decoded Mime header/field.
   */
  public static StringValue decodeMime(Env env,
                              CharSequence word,
                              String charset)
    throws UnsupportedEncodingException
  {
    StringValue str = env.createString(
            MimeUtility.unfold(MimeUtility.decodeText(word.toString())));

    return str.toBinaryValue(charset);
  }

  public static Value encodeMime(Env env,
                              StringValue name,
                              StringValue value,
                              String inCharset,
                              String outCharset,
                              String scheme)
    throws UnsupportedEncodingException
  {
    return encodeMime(env,
                      name,
                      value,
                      inCharset,
                      outCharset,
                      scheme,
                      "\r\n",
                      76);
  }

  /**
   * Encodes a MIME header.
   *
   * XXX: preferences
   *
   * @param field_name header field name
   * @param field_value header field value
   * @param preferences
   */
  /**
   * Returns an encoded Mime header.
   */
  public static StringValue encodeMime(Env env,
                              StringValue name,
                              StringValue value,
                              String inCharset,
                              String outCharset,
                              String scheme,
                              String lineBreakChars,
                              int lineLength)
    throws UnsupportedEncodingException
  {
    name = name.toUnicodeValue(env, inCharset);
    value = value.toUnicodeValue(env, inCharset);

    StringValue sb = env.createUnicodeBuilder();
    sb.append(name);
    sb.append(':');
    sb.append(' ');

    String word = encodeMimeWord(
            value.toString(), outCharset, scheme, lineBreakChars, lineLength);

    sb.append(MimeUtility.fold(sb.length(), word));

    return sb;
  }

  public static String encodeMimeWord(String value,
                              String charset,
                              String scheme,
                              String lineBreakChars,
                              int lineLength)
    throws UnsupportedEncodingException
  {
    if (lineLength != 76)
      throw new UnimplementedException("Mime line length option");

    if (! lineBreakChars.equals("\r\n"))
      throw new UnimplementedException("Mime line break option");

    return MimeUtility.encodeWord(value, charset, scheme);
  }
}
