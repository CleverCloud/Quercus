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

import java.io.IOException;
import java.io.OutputStream;

import com.caucho.server.http.ToByteResponseStream;

public class FilterWrapperResponseStream extends ToByteResponseStream {
  private CauchoResponseWrapper _response;
  
  private OutputStream _os;
  
  public FilterWrapperResponseStream()
  {
  }

  public void init(CauchoResponseWrapper response)
  {
    _response = response;
    _os = null;
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  protected void writeNext(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    OutputStream os = getStream();
    
    if (os != null)
      os.write(buf, offset, length);
  }

  /**
   * flushing
   */
  public void flush()
    throws IOException
  {
    flushBuffer();

    OutputStream os = getStream();
    if (os != null)
      os.flush();
  }

  /**
   * Gets the stream.
   */
  private OutputStream getStream()
    throws IOException
  {
    if (_os != null)
      return _os;
    else if (_response != null)
      _os = _response.getStream();

    return _os;
  }

  /**
   * Close.
   */
  public void closeImpl()
    throws IOException
  {
    super.closeImpl();

    _response = null;
    
    OutputStream os = _os;
    _os = null;
    // server/1839
    
    if (os != null)
      os.close();
  }
}
