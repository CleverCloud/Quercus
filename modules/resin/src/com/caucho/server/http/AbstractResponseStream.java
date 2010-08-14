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

package com.caucho.server.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Locale;

import com.caucho.vfs.OutputStreamWithBuffer;
import com.caucho.vfs.Path;

/**
 * API for handling the PrintWriter/ServletOutputStream
 */
public abstract class AbstractResponseStream extends OutputStreamWithBuffer {
  private State _state = State.START;
  
  /**
   * Starts the response stream.
   */
  public void start()
  {
    _state = _state.toStart();
  }
  
  //
  // state predicates
  //

  /**
   * Set true for HEAD requests.
   */
  public void setHead()
  {
    _state = _state.toHead();
  }

  /**
   * Set true for HEAD requests.
   */
  public final boolean isHead()
  {
    return _state.isHead();
  }

  /**
   * Test if data has been flushed to the client.
   */
  public boolean isCommitted()
  {
    return _state.isCommitted();
  }

  /**
   * Sets the committed state
   */
  public void setCommitted()
  {
    _state = _state.toCommitted();
  }
  
  /**
   * Test if the request is closing.
   */
  public boolean isClosing()
  {
    return _state.isClosing();
  }

  /**
   * Returns true for a Caucho response stream.
   */
  abstract public boolean isCauchoResponseStream();

  public String getEncoding()
  {
    return null;
  }

  /**
   * Sets the encoding.
   */
  public void setEncoding(String encoding)
    throws UnsupportedEncodingException
  {
  }

  /**
   * Set true for output stream only request.
   */
  public void setOutputStreamOnly(boolean isOutputStreamOnly)
  {
  }

  /**
   * Sets the locale.
   */
  public void setLocale(Locale locale)
    throws UnsupportedEncodingException
  {
  }

  /**
   * Sets the buffer size.
   */
  abstract public void setBufferSize(int size);

  /**
   * Gets the buffer size.
   */
  abstract public int getBufferSize();

  /**
   * Sets the auto-flush
   */
  public void setAutoFlush(boolean isAutoFlush)
  {
  }

  /**
   * Return the auto-flush.
   */
  public boolean isAutoFlush()
  {
    return true;
  }

  /**
   * Returns the remaining buffer entries.
   */
  abstract public int getRemaining();

  /**
   * Returns the char buffer.
   */
  abstract public char []getCharBuffer()
    throws IOException;

  /**
   * Returns the char buffer offset.
   */
  abstract public int getCharOffset()
    throws IOException;

  /**
   * Sets the char buffer offset.
   */
  abstract public void setCharOffset(int offset)
    throws IOException;

  /**
   * Returns the next char buffer.
   */
  abstract public char []nextCharBuffer(int offset)
    throws IOException;

  /**
   * Sets a byte cache stream.
   */
  public void setByteCacheStream(OutputStream cacheStream)
  {
    if (cacheStream != null)
      throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets a byte cache stream.
   */
  protected OutputStream getByteCacheStream()
  {
    return null;
  }

  /**
   * Sets a char cache stream.
   */
  public void setCharCacheStream(Writer cacheStream)
  {
    if (cacheStream != null)
      throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets a char cache stream.
   */
  protected Writer getCharCacheStream()
  {
    return null;
  }

  /**
   * Returns the written content length
   */
  public int getContentLength()
  {
    return 0;
  }
  
  /**
   * Writes a byte to the output.
   */
  abstract public void write(int v)
    throws IOException;

  /**
   * Writes a byte array to the output.
   */
  abstract public void write(byte []buffer, int offset, int length)
    throws IOException;

  /**
   * Writes a character to the output.
   */
  abstract public void print(int ch)
    throws IOException;

  /**
   * Writes a char array to the output.
   */
  abstract public void print(char []buffer, int offset, int length)
    throws IOException;

  /**
   * Clears the output buffer, including headers if possible.
   */
  public void clear()
    throws IOException
  {
    clearBuffer();
  }

  /**
   * Clears the output buffer.
   */
  abstract public void clearBuffer();

  /**
   * Flushes the output buffer.
   */
  abstract public void flushBuffer()
    throws IOException;
  
  /**
   * Flushes the next buffer, leaving the current buffer alone
   */
  // server/1s04
  public void flushNext()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Flushes the output.
   */
  public void flushByte()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Flushes the output.
   */
  public void flushChar()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Sends a file.
   *
   * @param path the path to the file
   * @param length the length of the file (-1 if unknown)
   */
  public void sendFile(Path path, long length)
    throws IOException
  {
    path.writeToStream(this);
  }

  /**
   * Flushes the output.
   */
  @Override
  public void flush()
    throws IOException
  {
    flushByte();
  }

  protected void killCaching()
  {
  }

  public final boolean isClosed()
  {
    return _state.isClosed();
  }
  
  /**
   * Closes the response stream
   */
  @Override
  public final void close()
    throws IOException
  {
    State state = _state;
    
    if (state.isClosed())
      return;
    
    _state = state.toClosing();
    
    try {
      closeImpl();
    } finally {
      _state = _state.toClose();
    }
  }
  
  protected void closeImpl()
    throws IOException
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _state + "]";
  }
  
  enum State {
    START {
      State toHead() { return HEAD; }
      State toCommitted() { return COMMITTED; }
      State toClosing() { return CLOSING; }
    },
    HEAD {
      boolean isHead() { return true; }
      
      State toHead() { return this; }
      State toCommitted() { return COMMITTED_HEAD; }
      State toClosing() { return CLOSING_HEAD; }
    },
    COMMITTED {
      boolean isCommitted() { return true; }
      
      State toHead() { return COMMITTED_HEAD; }
      State toCommitted() { return this; }
      State toClosing() { return CLOSING; }
    },
    COMMITTED_HEAD {
      boolean isCommitted() { return true; }
      boolean isHead() { return true; }
      
      State toHead() { return this; }
      State toCommitted() { return this; }
      State toClosing() { return CLOSING_HEAD; }
    },
    CLOSING {
      boolean isClosing() { return true; }
      
      State toHead() { return CLOSING_HEAD; }
      State toCommitted() { return CLOSING_COMMITTED; }
      State toClose() { return CLOSED; }
    },
    CLOSING_HEAD {
      boolean isHead() { return true; }
      boolean isClosing() { return true; }
      
      State toHead() { return this; }
      State toCommitted() { return CLOSING_HEAD_COMMITTED; }
      State toClose() { return CLOSED; }
    },
    CLOSING_COMMITTED {
      boolean isCommitted() { return true; }
      boolean isClosing() { return true; }
      
      State toHead() { return CLOSING_HEAD_COMMITTED; }
      State toCommitted() { return this; }
      State toClose() { return CLOSED; }
    },
    CLOSING_HEAD_COMMITTED {
      boolean isHead() { return true; }
      boolean isCommitted() { return true; }
      boolean isClosing() { return true; }
      
      State toHead() { return this; }
      State toCommitted() { return this; }
      State toClose() { return CLOSED; }
    },
    CLOSED {
      boolean isCommitted() { return true; }
      boolean isClosed() { return true; }
    };
    
    boolean isHead() { return false; }
    boolean isCommitted() { return false; }
    boolean isClosing() { return false; }
    boolean isClosed() { return false; }
   
    State toStart() { return START; }
    State toHead()
    { 
      throw new IllegalStateException(toString());
    }
    State toCommitted()
    {
      throw new IllegalStateException(toString());
    }
    State toClosing()
    {
      throw new IllegalStateException(toString());
    }
    State toClose()
    { 
      throw new IllegalStateException(toString());
    }
  }
}
