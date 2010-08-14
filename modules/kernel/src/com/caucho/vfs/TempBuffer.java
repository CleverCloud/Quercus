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

import com.caucho.util.FreeList;

import java.io.IOException;
import java.util.logging.*;

/**
 * Pooled temporary byte buffer.
 */
public class TempBuffer implements java.io.Serializable {
  private static Logger _log;
  
  private static final FreeList<TempBuffer> _freeList
    = new FreeList<TempBuffer>(32);
  
  private static final FreeList<TempBuffer> _smallFreeList
    = new FreeList<TempBuffer>(32);
  
  private static final FreeList<TempBuffer> _largeFreeList
    = new FreeList<TempBuffer>(32);

  private static final boolean _isSmallmem;
  
  public static final int SMALL_SIZE;
  public static final int LARGE_SIZE;
  public static final int SIZE;
  
  private static boolean _isFreeException;
  
  TempBuffer _next;
  final byte []_buf;
  int _offset;
  int _length;
  int _bufferCount;

  // validation of allocate/free
  private transient boolean _isFree;
  private transient RuntimeException _freeException;

  /**
   * Create a new TempBuffer.
   */
  public TempBuffer(int size)
  {
    _buf = new byte[size];
  }

  /**
   * Returns true for a smallmem configuration
   */
  public static boolean isSmallmem()
  {
    return _isSmallmem;
  }

  /**
   * Allocate a TempBuffer, reusing one if available.
   */
  public static TempBuffer allocate()
  {
    TempBuffer next = _freeList.allocate();

    if (next == null)
      return new TempBuffer(SIZE);
    else if (! next._isFree) // XXX:
      throw new IllegalStateException();

    next._isFree = false;
    next._next = null;

    next._offset = 0;
    next._length = 0;
    next._bufferCount = 0;

    return next;
  }

  /**
   * Allocate a TempBuffer, reusing one if available.
   */
  public static TempBuffer allocateSmall()
  {
    TempBuffer next = _smallFreeList.allocate();

    if (next == null)
      return new TempBuffer(SMALL_SIZE);

    next._isFree = false;
    next._next = null;

    next._offset = 0;
    next._length = 0;
    next._bufferCount = 0;

    return next;
  }

  /**
   * Allocate a TempBuffer, reusing one if available.
   */
  public static TempBuffer allocateLarge()
  {
    TempBuffer next = _smallFreeList.allocate();

    if (next == null)
      return new TempBuffer(LARGE_SIZE);

    next._isFree = false;
    next._next = null;

    next._offset = 0;
    next._length = 0;
    next._bufferCount = 0;

    return next;
  }

  /**
   * Clears the buffer.
   */
  public void clear()
  {
    _next = null;

    _offset = 0;
    _length = 0;
    _bufferCount = 0;
  }

  /**
   * Returns the buffer's underlying byte array.
   */
  public final byte []getBuffer()
  {
    return _buf;
  }

  /**
   * Returns the number of bytes in the buffer.
   */
  public final int getLength()
  {
    return _length;
  }

  /**
   * Sets the number of bytes used in the buffer.
   */
  public final void setLength(int length)
  {
    _length = length;
  }

  public final int getCapacity()
  {
    return _buf.length;
  }

  public int getAvailable()
  {
    return _buf.length - _length;
  }

  public final TempBuffer getNext()
  {
    return _next;
  }

  public final void setNext(TempBuffer next)
  {
    _next = next;
  }

  public int write(byte []buf, int offset, int length)
  {
    byte []thisBuf = _buf;
    int thisLength = _length;

    if (thisBuf.length - thisLength < length)
      length = thisBuf.length - thisLength;

    System.arraycopy(buf, offset, thisBuf, thisLength, length);

    _length = thisLength + length;

    return length;
  }

  /**
   * Frees a single buffer.
   */
  public static void free(TempBuffer buf)
  {
    buf._next = null;
    
    if (buf._isFree) {
      _isFreeException = true;
      RuntimeException freeException = buf._freeException;
      RuntimeException secondException = new IllegalStateException("duplicate free");
      secondException.fillInStackTrace();
      
      log().log(Level.WARNING, "initial free location", freeException);
      log().log(Level.WARNING, "secondary free location", secondException);
      
      throw new IllegalStateException();
    }
    
    buf._isFree = true;

    if (buf._buf.length == SIZE) {
      if (_isFreeException) {
        buf._freeException = new IllegalStateException("initial free");
        buf._freeException.fillInStackTrace();
      }
      _freeList.free(buf);
    }
  }

  public static void freeAll(TempBuffer buf)
  {
    while (buf != null) {
      TempBuffer next = buf._next;
      buf._next = null;
      
      free(buf);
      
      buf = next;
    }
  }

  /**
   * Frees a single buffer.
   */
  public static void freeSmall(TempBuffer buf)
  {
    buf._next = null;

    if (buf._buf.length == SMALL_SIZE) {
      if (buf._isFree) {
        RuntimeException e
          = new IllegalStateException("illegal TempBuffer.free.  Please report at http://bugs.caucho.com");
        log().log(Level.SEVERE, e.toString(), e);
        throw e;
      }

      buf._isFree = true;
      
      _smallFreeList.free(buf);
    }
  }

  public static void freeAllSmall(TempBuffer buf)
  {
    while (buf != null) {
      TempBuffer next = buf._next;
      buf._next = null;
      
      if (buf._buf.length == SMALL_SIZE) {
        if (buf._isFree) {
          RuntimeException e
            = new IllegalStateException("illegal TempBuffer.free.  Please report at http://bugs.caucho.com");

          log().log(Level.SEVERE, e.toString(), e);
          throw e;
        }

        buf._isFree = true;
      
        _smallFreeList.free(buf);
      }
      
      buf = next;
    }
  }

  /**
   * Frees a single buffer.
   */
  public static void freeLarge(TempBuffer buf)
  {
    buf._next = null;

    if (buf._buf.length == LARGE_SIZE) {
      if (buf._isFree) {
        RuntimeException e
          = new IllegalStateException("illegal TempBuffer.free.  Please report at http://bugs.caucho.com");
        log().log(Level.SEVERE, e.toString(), e);
        throw e;
      }

      buf._isFree = true;
      
      _largeFreeList.free(buf);
    }
  }

  public static void freeAllLarge(TempBuffer buf)
  {
    while (buf != null) {
      TempBuffer next = buf._next;
      buf._next = null;
      
      if (buf._buf.length == LARGE_SIZE) {
        if (buf._isFree) {
          RuntimeException e
            = new IllegalStateException("illegal TempBuffer.free.  Please report at http://bugs.caucho.com");

          log().log(Level.SEVERE, e.toString(), e);
          throw e;
        }

        buf._isFree = true;
      
        _largeFreeList.free(buf);
      }
      
      buf = next;
    }
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(TempBuffer.class.getName());

    return _log;
  }

  static {
    // the max size needs to be less than JNI code, currently max 16k
    // the min size is 8k because of the JSP spec
    int size = 8 * 1024;
    boolean isSmallmem = false;

    String smallmem = System.getProperty("caucho.smallmem");
    
    if (smallmem != null && ! "false".equals(smallmem)) {
      isSmallmem = true;
      size = 512;
    }

    _isSmallmem = isSmallmem;
    SIZE = size;
    LARGE_SIZE = 8 * 1024;
    SMALL_SIZE = 512;
  }
}
