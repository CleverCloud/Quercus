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

package com.caucho.filters;

import java.security.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.util.*;

/**
 * Calculates a HTTP Content-MD5 footer following RFC 1864
 *
 * @since Resin 3.1.5
 */
public class MD5Filter implements Filter
{
  public void init(FilterConfig config)
    throws ServletException
  {
  }
  
  /**
   * Creates a wrapper to compress the output.
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    DigestResponse digestResponse = new DigestResponse(response);

    nextFilter.doFilter(request, digestResponse);

    digestResponse.finish();
  }
  
  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }

  class DigestResponse extends CauchoResponseWrapper {
    private DigestStream _digestStream;
    
    DigestResponse(ServletResponse response)
    {
      super((HttpServletResponse) response);
    }

    /**
     * This needs to be bypassed because the file's content
     * length has nothing to do with the returned length.
     */
    public void setContentLength(int length)
    {
    }

    /**
     * Calculates and returns the proper stream.
     */
    protected OutputStream getStream()
      throws IOException
    {
      if (_digestStream == null)
        _digestStream = new DigestStream(_response.getOutputStream());

      return _digestStream;
    }

    /**
     * Complets the request.
     */
    public void finish()
      throws IOException, ServletException
    {
      if (_digestStream != null)
        _digestStream.flush();

      close();
      
      if (_digestStream != null) {
        _digestStream.flush();
        super.setFooter("Content-MD5", _digestStream.getDigest());
      }
    }
  }
  
  static class DigestStream extends OutputStream {
    private OutputStream _os;
    private MessageDigest _digest;
    
    DigestStream(OutputStream os)
    {
      _os = os;

      try {
        _digest = MessageDigest.getInstance("MD5");
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Writes to the underlying stream.
     *
     * @param ch the byte to write
     */
    public void write(int ch)
      throws IOException
    {
      _os.write(ch);
      _digest.update((byte) ch);
    }

    /**
     * Writes a buffer to the underlying stream.
     *
     * @param buffer the byte array to write.
     * @param offset the offset into the byte array.
     * @param length the number of bytes to write.
     */
    public void write(byte []buffer, int offset, int length)
      throws IOException
    {
      _os.write(buffer, offset, length);
      _digest.update(buffer, offset, length);
    }

    public void flush()
      throws IOException
    {
      _os.flush();
    }

    public void close()
      throws IOException
    {
      _os.close();
    }

    public String getDigest()
    {
      byte []bytes = _digest.digest();

      CharBuffer cb = new CharBuffer();
      Base64.encode(cb, bytes, 0, bytes.length);

      return cb.toString();
    }
  }
}
