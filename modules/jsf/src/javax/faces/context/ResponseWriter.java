/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package javax.faces.context;

import java.io.*;

import javax.faces.component.*;

public abstract class ResponseWriter extends Writer {
  public abstract String getContentType();

  public abstract String getCharacterEncoding();

  public abstract void flush()
    throws IOException;

  public abstract void startDocument()
    throws IOException;

  public abstract void endDocument()
    throws IOException;

  public abstract void startElement(String name, UIComponent component)
    throws IOException;

  public abstract void endElement(String name)
    throws IOException;

  public abstract void writeAttribute(String name,
                                      Object value,
                                      String property)
    throws IOException;

  public abstract void writeURIAttribute(String name,
                                         Object value,
                                         String property)
    throws IOException;

  public abstract void writeComment(Object comment)
    throws IOException;

  public abstract void writeText(Object text,
                                 String property)
    throws IOException;

  public void writeText(Object text,
                        UIComponent component,
                        String property)
    throws IOException
  {
    writeText(text, property);
  }

  public abstract void writeText(char []text, int offset, int length)
    throws IOException;

  public abstract ResponseWriter cloneWithWriter(Writer writer);
}
