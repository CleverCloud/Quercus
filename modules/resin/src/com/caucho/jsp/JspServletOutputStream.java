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

package com.caucho.jsp;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class JspServletOutputStream extends ServletOutputStream {
  private PageContextImpl _pageContext;
  private OutputStream _os;

  JspServletOutputStream(PageContextImpl pageContext)
  {
    _pageContext = pageContext;
  }

  public final void write(int b) throws IOException
  {
    getOutputStream().write(b);
  }

  public final void write(byte []buf, int offset, int len) throws IOException
  {
    getOutputStream().write(buf, offset, len);
  }

  public final void flush() throws IOException
  {
    if (_os != null)
      _os.flush();
  }

  public final void close() throws IOException
  {
  }

  /**
   * Returns the output stream.
   */
  private OutputStream getOutputStream()
  {
    if (_os == null) {
      _os = _pageContext.getOutputStream();
    }

    return _os;      
  }

  void release()
  {
    _os = null;
  }
}
