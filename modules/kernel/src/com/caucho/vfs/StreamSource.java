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

package com.caucho.vfs;

import java.io.*;
import com.caucho.util.L10N;

/**
 * Stream source for Hessian serialization of large data
 */
public class StreamSource
{
  private static final L10N L = new L10N(StreamSource.class);
  
  private TempOutputStream _out;
  private StreamSource _indirectSource;

  /**
   * Constructor for subclasses.
   */
  protected StreamSource()
  {
  }

  /**
   * Constructor for Hessian deserialization.
   */
  public StreamSource(TempOutputStream os)
  {
    _out = os;
  }

  /**
   * Constructor allowing for dynamic opening.
   */
  public StreamSource(StreamSource indirectSource)
  {
    _indirectSource = indirectSource;
  }

  /**
   * Returns an input stream, freeing the results
   */
  public InputStream getInputStream()
    throws IOException
  {
    if (_indirectSource != null)
      return _indirectSource.getInputStream();
    else if (_out != null)
      return _out.openInputStream();
    else
      throw new IllegalStateException(L.l("{0}: no input stream is available",
                                          this));
  }

  /**
   * Returns an input stream, without freeing the results
   */
  public InputStream openInputStream()
    throws IOException
  {
    if (_indirectSource != null)
      return _indirectSource.openInputStream();
    else if (_out != null)
      return _out.openInputStreamNoFree();
    else
      throw new IllegalStateException(L.l("{0}: no input stream is available",
                                          this));
  }

  /**
   * Close the stream.
   */
  public void close()
  {
    TempOutputStream out = _out;
    _out = null;
    
    StreamSource ss = _indirectSource;
    _indirectSource = null;

    if (out != null)
      out.destroy();
    
    if (ss != null)
      ss.close();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _out + "," + _indirectSource + "]";
  }
}
