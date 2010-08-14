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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.i18n;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeUtility;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

public class QuercusMimeUtility
{
  private static final Logger log = Logger.getLogger(
      QuercusMimeUtility.class.getName());
  
  /*
   * Returns an array of decoded Mime headers/fields.
   */
  public static Value decodeMimeHeaders(Env env,
                                        StringValue encodedHeaders,
                                        String charset)
    throws UnsupportedEncodingException
  {
    ArrayValue headers = new ArrayValueImpl();

    try {
      Enumeration<Header> enumeration
        = new InternetHeaders(encodedHeaders.toInputStream()).getAllHeaders();

      while (enumeration.hasMoreElements()) {
        Header header = enumeration.nextElement();

        StringValue name
        = QuercusMimeUtility.decodeMime(env, header.getName(), charset);
        StringValue val
        = QuercusMimeUtility.decodeMime(env, header.getValue(), charset);

        Value headerName;
        if ((headerName = headers.containsKey(name)) == null) {
          headers.put(name, val);
          continue;
        }

        ArrayValue inner;
        if (headerName.isArray()) {
          inner = headerName.toArrayValue(env);
        }
        else {
          inner = new ArrayValueImpl();
          inner.put(headerName);
        }

        inner.put(val);
        headers.put(name, inner);
      }

      return headers;
    
    } catch (MessagingException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e.getMessage());
      
      return BooleanValue.FALSE;
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
    String decodedStr = MimeUtility.decodeText(word.toString());
    
    StringValue str
      = env.createString(MimeUtility.unfold(decodedStr));

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
   * @return encoded mime header
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
    Decoder decoder = Decoder.create(inCharset);
    
    CharSequence nameUnicode = decoder.decode(env, name);

    decoder.reset();
    String valueUnicode = decoder.decode(env, value).toString();

    StringValue sb = env.createUnicodeBuilder();
    sb.append(UnicodeUtility.encode(env, nameUnicode, outCharset));
    sb.append(':');
    sb.append(' ');

    String word = encodeMimeWord(valueUnicode.toString(),
                                 outCharset,
                                 scheme,
                                 lineBreakChars,
                                 lineLength);

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
