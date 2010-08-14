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

package com.caucho.quercus.lib.file;

import com.caucho.quercus.annotation.ResourceType;
import com.caucho.quercus.env.Value;

/**
 * Interface for a Quercus stream
 */
@ResourceType("stream")
public interface BinaryStream {
  public static final int SEEK_SET = 0;
  public static final int SEEK_CUR = 1;
  public static final int SEEK_END = 2;

  /**
   * All streams can be closed.
   */
  public void close();

  /**
   * Tells the position in the stream.
   * The valid range for a stream position is 0 to Long.MAX_VALUE,
   * so a negative number can't be a valid stream position.
   */
  public long getPosition();

  /**
   * Sets the current position in the stream.
   * Returns true on success, false otherwise.
   */
  public boolean setPosition(long offset);

  /**
   * Seek according to offset and whence.
   * For fseek() compatibility in wrapped streams.
   */
  public long seek(long offset, int whence);

  /**
   * Returns true if end-of-file has been reached
   */
  public boolean isEOF();
 
  /**
   * Returns an array filled with stat information.  Mainly for wrapped
   * stream functionality.
   */
  public Value stat();
}
