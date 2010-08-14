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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Logger;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;

public class GenericEncoder
  extends Encoder
{
  private static final Logger log
    = Logger.getLogger(GenericEncoder.class.getName());

  private static final L10N L = new L10N(GenericEncoder.class);
  
  private Charset _charset;
  protected CharsetEncoder _encoder;
  
  public GenericEncoder(String charsetName)
  {
    super(charsetName);
    
    _charset = Charset.forName(charsetName);
    
    _encoder = _charset.newEncoder();
  }
  
  public boolean isEncodable(Env env, StringValue str)
  {
    int len = str.length();
    
    for (int i = 0; i < len; i++) {
      if (! _encoder.canEncode(str.charAt(i))) {
        return false;
      }
    }
    
    return false;
  }
  
  @Override
  public StringValue encode(Env env, CharSequence str)
  {
    CharBuffer in = CharBuffer.wrap(str);
    
    TempBuffer tempBuf = TempBuffer.allocate();
    
    try {
      ByteBuffer out = ByteBuffer.wrap(tempBuf.getBuffer());
      
      StringValue sb = env.createBinaryBuilder();

      while (in.hasRemaining()) {
        CoderResult coder = _encoder.encode(in, out, false);

        if (! fill(sb, in, out, coder))
          return sb;
        
        out.clear();
      }
      
      CoderResult coder = _encoder.encode(in, out, true);
      if (! fill(sb, in, out, coder))
        return sb;
      
      out.clear();
      
      coder = _encoder.flush(out);
      fill(sb, in, out, coder);
      
      return sb;
    } finally {
      TempBuffer.free(tempBuf);
    }
  }
  
  protected boolean fill(StringValue sb, CharBuffer in,
                         ByteBuffer out, CoderResult coder)
  {
    int len = out.position();
    
    if (len > 0) {
      int offset = out.arrayOffset();
      
      sb.appendBytes(out.array(), offset, offset + len);
    }
    
    if (coder.isMalformed() || coder.isUnmappable()) {
      int errorIndex = in.position();
      
      in.position(errorIndex + 1);
      
      if (_isIgnore) {
      }
      else if (_replacement != null)
        sb.append(_replacement);
      else if (_isReplaceUnicode)
        sb.append("U+" + Integer.toHexString(in.get(errorIndex)));
      else
        return false;
    }
    
    return true;
  }
  
  public void reset()
  {
    _encoder.reset();
  }

}
