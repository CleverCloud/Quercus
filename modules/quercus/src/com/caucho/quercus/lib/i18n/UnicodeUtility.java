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

import com.caucho.quercus.env.*;

public class UnicodeUtility
{
  public static StringValue utf8Clean(Env env,
                                      StringValue str,
                                      String replacement,
                                      boolean isIgnore)
  {
    StringValue sb = str.createStringBuilder();
    
    int len = str.length();
    
    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);
      
      if (ch <= 0x7F)
        sb.append(ch);
      else if (0xC2 <= ch && ch <= 0xDF) {
        char ch2;
        if (i + 1 < len
            && 0x80 <= (ch2 = str.charAt(i + 1)) && ch2 <= 0xBF) {
          i++;
          sb.append(ch);
          sb.append(ch2);
        }
        else if (isIgnore) {
        }
        else if (replacement != null)
          sb.append(replacement);
        else
          return sb;
      }
      else if (0xE0 <= ch && ch <= 0xEF) {
        char ch2;
        char ch3;
        if (i + 2 < len
            && 0x80 <= (ch2 = str.charAt(i + 1)) && ch2 <= 0xBF
            && 0x80 <= (ch3 = str.charAt(i + 2)) && ch3 <= 0xBF) {
          i += 2;
          sb.append(ch);
          sb.append(ch2);
          sb.append(ch3);
        }
        else if (isIgnore) {
        }
        else if (replacement != null)
          sb.append(replacement);
        else
          return sb;
      }
      else if (0xF0 <= ch && ch <= 0xF4) {
        char ch2;
        char ch3;
        char ch4;
        
        if (i + 3 < len
            && 0x80 <= (ch2 = str.charAt(i + 1)) && ch2 <= 0xBF
            && 0x80 <= (ch3 = str.charAt(i + 2)) && ch3 <= 0xBF
            && 0x80 <= (ch4 = str.charAt(i + 3)) && ch4 <= 0xBF) {
          i += 3;
          sb.append(ch);
          sb.append(ch2);
          sb.append(ch3);
          sb.append(ch4);
        }
        else if (isIgnore) {
        }
        else if (replacement != null)
          sb.append(replacement);
        else
          return sb;
      }
      else if (isIgnore) {
      }
      else if (replacement != null)
        sb.append(replacement);
      else
        return sb;
    }
    
    return sb;
  }
  
  public static CharSequence decode(Env env,
                                    StringValue str,
                                    String charset)
  {
    return decode(env, str, charset, null, false);
  }
  
  public static CharSequence decode(Env env,
                                    StringValue str,
                                    String charset,
                                    String replacement,
                                    boolean isIgnoreErrors)
  {
    Decoder decoder = Decoder.create(charset);
    
    decoder.setReplacement(replacement);
    decoder.setIgnoreErrors(isIgnoreErrors);
    
    return decoder.decode(env, str);
  }
  
  public static StringValue encode(Env env,
                                   CharSequence str,
                                   String charset)
  {
    return encode(env, str, charset, null, false);
  }
  
  public static StringValue encode(Env env,
                                   CharSequence str,
                                   String charset,
                                   String replacement,
                                   boolean isIgnoreErrors)
  {
    Encoder encoder = Encoder.create(charset);
    
    encoder.setReplacement(replacement);
    encoder.setIgnoreErrors(isIgnoreErrors);
    
    return encoder.encode(env, str);
  }
  
  public static StringValue decodeEncode(Env env,
                                         StringValue str,
                                         String inCharset,
                                         String outCharset,
                                         String replacement,
                                         boolean isIgnoreErrors)
  {
    boolean isStartUtf8 = false;
    boolean isEndUtf8 = false;
    
    if (inCharset.equalsIgnoreCase("utf8")
        || inCharset.equalsIgnoreCase("utf-8"))
      isStartUtf8 = true;
    
    if (outCharset.equalsIgnoreCase("utf8")
        || outCharset.equalsIgnoreCase("utf-8"))
      isEndUtf8 = true;
    
    if (isStartUtf8 && isEndUtf8)
      return UnicodeUtility.utf8Clean(env, str, null, isIgnoreErrors);
    
    // decode phase
    
    CharSequence unicodeStr;
    
    Decoder decoder;
    if (isStartUtf8)
      decoder = new Utf8Decoder(inCharset);
    else
      decoder = new GenericDecoder(inCharset);
    
    decoder.setIgnoreErrors(isIgnoreErrors);

    unicodeStr = decoder.decode(env, str);
    
    // encode phase
    
    Encoder encoder;
    if (isEndUtf8)
      encoder = new Utf8Encoder();
    else
      encoder = Encoder.create(outCharset);
    
    encoder.setIgnoreErrors(isIgnoreErrors);
    
    return encoder.encode(env, unicodeStr);
  }
}
