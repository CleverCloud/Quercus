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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.logging.Logger;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.L10N;
import com.caucho.vfs.TempCharBuffer;

public class GenericDecoder
  extends Decoder
{
  private static final Logger log
    = Logger.getLogger(GenericDecoder.class.getName());

  private static final L10N L = new L10N(GenericDecoder.class);
  
  private Charset _charset;
  protected CharsetDecoder _decoder;
  
  public GenericDecoder(String charsetName)
  {
    super(charsetName);
    
    _charset = Charset.forName(charsetName);
    
    _decoder = _charset.newDecoder();
  }
  
  @Override
  public void reset()
  {
    _decoder.reset();
    
    super.reset();
  }
  
  @Override
  public boolean isDecodable(Env env, StringValue str)
  {
    if (str.isUnicode())
      return true;
    
    ByteBuffer in = ByteBuffer.wrap(str.toBytes());
    CharBuffer out = CharBuffer.allocate(512);
    
    while (in.hasRemaining()) {
      CoderResult coder = _decoder.decode(in, out, false);
      if (coder.isMalformed())
        return false;
      
      out.clear();
    }
    
    CoderResult coder = _decoder.decode(in, out, true);
    if (coder.isMalformed())
      return false;
    
    out.clear();
    
    coder = _decoder.flush(out);
    if (coder.isMalformed())
      return false;
    
    return true;
  }
  
  @Override
  protected StringBuilder decodeImpl(Env env, StringValue str)
  {
    ByteBuffer in = ByteBuffer.wrap(str.toBytes());
    
    TempCharBuffer tempBuf = TempCharBuffer.allocate();
    
    try  {
      CharBuffer out = CharBuffer.wrap(tempBuf.getBuffer());
      
      StringBuilder sb = new StringBuilder();
      
      while (in.hasRemaining()) {
        CoderResult coder = _decoder.decode(in, out, false);
        if (! fill(sb, in, out, coder))
          return sb;
        
        out.clear();
      }
      
      CoderResult coder = _decoder.decode(in, out, true);
      if (! fill(sb, in, out, coder))
        return sb;
      
      out.clear();
      
      coder = _decoder.flush(out);
      fill(sb, in, out, coder);
      
      return sb;
    } finally {
      TempCharBuffer.free(tempBuf);
    }
  }
  
  protected boolean fill(StringBuilder sb, ByteBuffer in,
                         CharBuffer out, CoderResult coder)
  {
    int len = out.position();
    
    if (len > 0) {
      int offset = out.arrayOffset();
      sb.append(out.array(), offset, len);
    }
    
    if (coder.isMalformed() || coder.isUnmappable()) {
      _hasError = true;
      
      int errorPosition = in.position();
      
      in.position(errorPosition + 1);
      
      if (_isIgnoreErrors) {
      }
      else if (_replacement != null)
        sb.append(_replacement);
      else if (_isAllowMalformedOut)
        sb.append((char) in.get(errorPosition));
      else
        return false;
    }
    
    return true;
  }

}
