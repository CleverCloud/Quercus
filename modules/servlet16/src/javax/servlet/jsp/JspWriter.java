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

package javax.servlet.jsp;

import java.io.IOException;
import java.io.Writer;

abstract public class JspWriter extends Writer {
  public static final int DEFAULT_BUFFER = -1;
  public static final int NO_BUFFER = 0;
  public static final int UNBOUNDED_BUFFER = -2;
  
  protected int bufferSize;
  protected boolean autoFlush;

  protected JspWriter(int bufferSize, boolean autoFlush)
  {
    this.bufferSize = bufferSize;
    this.autoFlush = autoFlush;
  }

  public abstract void print(boolean b) throws IOException;
  public abstract void print(char c) throws IOException;
  public abstract void print(int i) throws IOException;
  public abstract void print(long l) throws IOException;
  public abstract void print(float f) throws IOException;
  public abstract void print(double d) throws IOException;
  public abstract void print(char []s) throws IOException;
  public abstract void print(String s) throws IOException;
  public abstract void print(Object o) throws IOException;

  public abstract void newLine() throws IOException;

  public abstract void println() throws IOException;
  public abstract void println(boolean b) throws IOException;
  public abstract void println(char c) throws IOException;
  public abstract void println(int i) throws IOException;
  public abstract void println(long l) throws IOException;
  public abstract void println(float f) throws IOException;
  public abstract void println(double d) throws IOException;
  public abstract void println(char []s) throws IOException;
  public abstract void println(String s) throws IOException;
  public abstract void println(Object o) throws IOException;

  public abstract void clear() throws IOException;
  public abstract void clearBuffer() throws IOException;
  public abstract void flush() throws IOException;

  public int getBufferSize()
  {
    return bufferSize;
  }
  
  public abstract int getRemaining();
  public boolean isAutoFlush()
  {
    return autoFlush;
  }
}
