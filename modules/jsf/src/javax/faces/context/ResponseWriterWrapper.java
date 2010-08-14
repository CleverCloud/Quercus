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
 * @author Alex Rojkov
 */


package javax.faces.context;

import javax.faces.component.UIComponent;
import java.io.IOException;
import java.io.Writer;

public abstract class ResponseWriterWrapper
  extends ResponseWriter
{

  public ResponseWriterWrapper()
  {
  }

  protected abstract ResponseWriter getWrapped();

  public String getContentType()
  {
    return getWrapped().getContentType();
  }

  public String getCharacterEncoding()
  {
    return getWrapped().getCharacterEncoding();
  }

  public void flush()
    throws IOException
  {
    getWrapped().flush();
  }

  public void startDocument()
    throws IOException
  {
    getWrapped().startDocument();
  }

  public void endDocument()
    throws IOException
  {
    getWrapped().endDocument();
  }

  public void startElement(String name, UIComponent component)
    throws IOException
  {
    getWrapped().startElement(name, component);
  }

  public void endElement(String name)
    throws IOException
  {
    getWrapped().endElement(name);
  }

  public void writeAttribute(String name, Object value, String property)
    throws IOException
  {
    getWrapped().writeAttribute(name, value, property);
  }

  public void writeURIAttribute(String name, Object value, String property)
    throws IOException
  {
    getWrapped().writeURIAttribute(name, value, property);
  }

  public void writeComment(Object comment)
    throws IOException
  {
    getWrapped().writeComment(comment);
  }

  public void writeText(Object text, String property)
    throws IOException
  {
    getWrapped().writeText(text, property);
  }

  public void writeText(char[] text, int offset, int length)
    throws IOException
  {
    getWrapped().writeText(text, offset, length);
  }

  public ResponseWriter cloneWithWriter(Writer writer)
  {
    return getWrapped().cloneWithWriter(writer);
  }

  public void write(char cbuf[], int off, int len)
    throws IOException
  {
    getWrapped().write(cbuf, off, len);
  }

  public void close()
    throws IOException
  {
    getWrapped().close();
  }
}
