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

public class Utf8Decoder
  extends Decoder
{
  private static final Logger log
    = Logger.getLogger(Utf8Decoder.class.getName());

  private static final L10N L = new L10N(Utf8Decoder.class);
  
  private static final int ERROR_CHARACTER = 0xFFFE;
  private static final int EOF = -1;
  
  public Utf8Decoder(String charset)
  {
    super(charset);
  }
  
  public boolean isUtf8()
  {
    return true;
  }
  
  @Override
  public boolean isDecodable(Env env, StringValue str)
  {
    if (str.isUnicode())
      return true;

    Utf8Reader reader = new Utf8Reader(str);
    
    int ch;
    
    while ((ch = reader.read()) >= 0) {
      if (ch == ERROR_CHARACTER)
        return false;
    }

    return true;
  }
  
  @Override
  protected StringBuilder decodeImpl(Env env, StringValue str)
  {
    StringBuilder sb = new StringBuilder();
    
    int len = str.length();
    for (int i = 0; i < len; i++) {
      int ch = str.charAt(i);
      
      if (ch <= 0x7F)
        sb.append((char) ch);
      else if (0xC2 <= ch && ch <= 0xDF) {
        int ch2;
        if (i + 1 < len
            && 0x80 <= (ch2 = str.charAt(i + 1)) && ch2 <= 0xBF) {
          i++;
          
          int code = ((ch - 0xC0) << 6) + (ch2 - 0x80);
          
          sb.append((char) code);
        }
        else if (_isIgnoreErrors) {
        }
        else if (_replacement != null)
          sb.append(_replacement);
        else if (_isAllowMalformedOut)
          sb.append((char) ch);
        else
          return sb;
      }
      else if (0xE0 <= ch && ch <= 0xEF) {
        int ch2;
        int ch3;
        if (i + 2 < len
            && 0x80 <= (ch2 = str.charAt(i + 1)) && ch2 <= 0xBF
            && 0x80 <= (ch3 = str.charAt(i + 2)) && ch3 <= 0xBF) {
          i += 2;

          int code = ((ch - 0xE0) << 12)
                     + ((ch2 - 0x80) << 6)
                     + (ch3 - 0x80);
          
          if (0xD800 <= code && code <= 0xDBFF) {
            code &= 0xFFFFF;
            
            int high = 0xD800 + (code >> 10);
            int low = 0xDC00 + (code & 0x3FF);
            
            sb.append((char) high);
            sb.append((char) low);
          }
          else
            sb.append((char) code);
        }
        else if (_isIgnoreErrors) {
        }
        else if (_replacement != null)
          sb.append(_replacement);
        else if (_isAllowMalformedOut)
          sb.append((char) ch);
        else
          return sb;
      }
      else if (0xF0 <= ch && ch <= 0xF4) {
        int ch2;
        int ch3;
        int ch4;
        
        if (i + 3 < len
            && 0x80 <= (ch2 = str.charAt(i + 1)) && ch2 <= 0xBF
            && 0x80 <= (ch3 = str.charAt(i + 2)) && ch3 <= 0xBF
            && 0x80 <= (ch4 = str.charAt(i + 3)) && ch4 <= 0xBF) {
          i += 3;
          
          int code = ((ch - 0xF0) << 18)
                     + ((ch2 - 0x80) << 12)
                     + ((ch3 - 0x80) << 6)
                     + (ch4 - 0x80);
          
          if (code > 0xFFFF || 0xD800 <= code && code <= 0xDBFF) {
            code &= 0xFFFFF;
            
            int high = 0xD800 + code >> 10;
            int low = 0xDC00 + code & 0x3FF;
            
            sb.append((char) high);
            sb.append((char) low);
          }
          else
            sb.append((char) code);
        }
        else if (_isIgnoreErrors) {
        }
        else if (_replacement != null)
          sb.append(_replacement);
        else if (_isAllowMalformedOut)
          sb.append((char) ch);
        else
          return sb;
      }
      else if (_isIgnoreErrors) {
      }
      else if (_replacement != null)
        sb.append(_replacement);
      else if (_isAllowMalformedOut)
        sb.append((char) ch);
      else
        return sb;
    }
    
    /*
    Utf8Reader reader = new Utf8Reader(str);

    int ch;
    
    while ((ch = reader.read()) >= 0) {
      if (ch == ERROR_CHARACTER) {
        _hasError = true;
        
        if (_isIgnoreErrors) {
        }
        else if (_replacement != null)
          sb.append(_replacement);
        else
          return sb;
      }
      else
        sb.append((char) ch);
    }
    */

    return sb;
  }
  
  private static void decodeCodePoint(StringBuilder sb, int code)
  {
    code &= 0xFFFFF;
    
    int high = 0xD800 + code >> 10;
    int low = 0xDC00 + code & 0x3FF;
    
    sb.append((char) high);
    sb.append((char) low);
  }
  
  static class Utf8Reader
  {
    int _peek = -1;
    
    int _index;
    final int _len;
    StringValue _str;
    
    public Utf8Reader(StringValue str)
    {
      _str = str;
      _len = str.length();
    }
    
    public int read()
    {
      int ch1;
      if (_peek >= 0) {
        ch1 = _peek;
        _peek = -1;
      }
      else
        ch1 = readByte();

      if (ch1 < 0x80) {
        return ch1;
      }
      if ((ch1 & 0xe0) == 0xc0) {
        int ch2 = readByte();

        if (ch2 < 0)
          return ERROR_CHARACTER;
        else if ((ch2 & 0xc0) != 0x80) {
          unread();
          return ERROR_CHARACTER;
        }
        
        return ((ch1 & 0x1f) << 6) + (ch2 & 0x3f);
      }
      else if ((ch1 & 0xf0) == 0xe0) {
        int ch2 = readByte();
        
        if (ch2 < 0)
          return ERROR_CHARACTER;
        else if ((ch2 & 0xc0) != 0x80) {
          unread();
          return ERROR_CHARACTER;
        }
        
        int ch3 = readByte();
        
        if (ch3 < 0) {
          unread();
          return ERROR_CHARACTER;
        }
        else if ((ch3 & 0xc0) != 0x80) {
          unread();
          unread();
          return ERROR_CHARACTER;
        }

        int ch = ((ch1 & 0x1f) << 12) + ((ch2 & 0x3f) << 6) + (ch3 & 0x3f);

        if (ch == 0xfeff) // handle some writers, e.g. microsoft
          return readByte();
        else
          return ch;
      }
      else if ((ch1 & 0xf0) == 0xf0) {
        int ch2 = readByte();
        
        if (ch2 < 0)
          return ERROR_CHARACTER;
        else if ((ch2 & 0xc0) != 0x80) {
          unread();
          return ERROR_CHARACTER;
        }
        
        int ch3 = readByte();
        
        if (ch3 < 0) {
          unread();
          return ERROR_CHARACTER;
        }
        else if ((ch3 & 0xc0) != 0x80) {
          unread();
          unread();
          return ERROR_CHARACTER;
        }
        
        int ch4 = readByte();
        
        if (ch4 < 0) {
          unread();
          unread();
          
          return ERROR_CHARACTER;
        }
        else if ((ch4 & 0xc0) != 0x80) {
          unread();
          unread();
          unread();
          return ERROR_CHARACTER;
        }
        
        int ch = (((ch1 & 0xf) << 18)
            + ((ch2 & 0x3f) << 12)
            + ((ch3 & 0x3f) << 6)
            + ((ch4 & 0x3f)));

        _peek = 0xdc00 + (ch & 0x3ff);
        
        return 0xd800 + ((ch - 0x10000) / 0x400);
      }
      else
        return ERROR_CHARACTER;
    }
    
    private int readByte()
    {
      if (_index < _len)
        return _str.charAt(_index++);
      else
        return EOF;
    }
    
    private void unread()
    {
      _index--;
    }
  }
}
