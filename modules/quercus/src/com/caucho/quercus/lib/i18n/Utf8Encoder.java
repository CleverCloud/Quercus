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

import java.util.logging.Logger;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.L10N;

public class Utf8Encoder
  extends Encoder
{
  private static final Logger log
    = Logger.getLogger(Utf8Encoder.class.getName());

  private static final L10N L = new L10N(Utf8Encoder.class);
  
  public Utf8Encoder()
  {
    super("utf-8");
  }
  
  @Override
  public boolean isUtf8()
  {
    return true;
  }

  public boolean isEncodable(Env env, StringValue str)
  {
    int len = str.length();
    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);
      
      if (ch <= 0x7F)
        continue;
      
      if (0xD800 <= ch && ch <= 0xDBFF) {
        char ch2;
        
        if (i + 1 < len
            && 0xDC00 <= (ch2 = str.charAt(i + 1)) && ch2 <= 0xDFFF) {
          i++;
        }
        else
          return false;
      }
    }
    
    return true;
  }
  
  @Override
  public StringValue encode(Env env, CharSequence str)
  {
    StringValue sb = env.createBinaryBuilder();
    
    int len = str.length();
    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);
      
      if (ch <= 0x7F) {
        sb.appendByte(ch);
        continue;
      }
      
      int code = ch;
      
      
      if (0xD800 <= ch && ch <= 0xDBFF) {
        char ch2;
        
        if (i + 1 < len
            && 0xDC00 <= (ch2 = str.charAt(i + 1)) && ch2 <= 0xDFFF) {
          i++;
          
          code = 0x10000 + ((ch - 0xD800) << 10) + (ch2 - 0xDC00);
        }
        else {
          if (_isIgnore) {
          }
          else if (_replacement != null)
            sb.append(_replacement);
          else
            return sb;
          
          continue;
        }
      }
            
      if (0x80 <= code && code <= 0x7FF) {
        sb.appendByte(0xC0 | (code >> 6));
        sb.appendByte(0x80 | (code & 0x3F));
      }
      else if (0x800 <= code && code <= 0xFFFF) {
        sb.appendByte(0xE0 | (code >> 12));
        sb.appendByte(0x80 | ((code >> 6) & 0x3F));
        sb.appendByte(0x80 | (code & 0x3F));
      }
      else {
        sb.appendByte(0xF0 | (code >> 18));
        sb.appendByte(0x80 | ((code >> 12) & 0x3F));
        sb.appendByte(0x80 | ((code >> 6) & 0x3F));
        sb.appendByte(0x80 | (code & 0x3F));
      }
    }
    
    return sb;
  }
  
}
