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
 * @author Emil Ong
 */

package com.caucho.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An OutputStream that writes to nothing.
 */
public class NullOutputStream extends OutputStream {
  public void write(int b)
    throws IOException
  {
  }

  public void write(byte[] buffer)
    throws IOException
  {
  }

  public void write(byte[] buffer, int offset, int length)
    throws IOException
  {
  }

  public void flush()
    throws IOException
  {
  }

  public void close()
    throws IOException
  {
  }
}
