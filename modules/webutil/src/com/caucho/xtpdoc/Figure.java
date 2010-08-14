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
 * @author Emil Ong
 */

package com.caucho.xtpdoc;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.caucho.config.ConfigException;

public class Figure implements ContentItem {
  private int _height = -1;
  private int _width = -1;
  private String _source;
  private String _title;
  private Document _document;

  public Figure(Document document)
  {
    _document = document;
  }

  public Document getDocument()
  {
    return _document;
  }

  public void setHeight(int height)
  {
    _height = height;
  }

  public void setWidth(int width)
  {
    _width = width;
  }

  public void setSrc(String source)
  {
    _source = source;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("center");

    out.writeEmptyElement("img");

    out.writeAttribute("border", "0");

    if (_height >= 0)
      out.writeAttribute("height", Integer.toString(_height));

    if (_title != null)
      out.writeAttribute("title", _title);

    if (_width >= 0)
      out.writeAttribute("width", Integer.toString(_width));

    out.writeAttribute("src", 
                       _document.getContextPath() + "/images/" + _source);

    out.writeEndElement(); // center
  }

  public void writeLaTeXVerbatim(PrintWriter out)
    throws IOException
  {
    throw new ConfigException("<figure> not allowed in a verbatim context");
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    int dot = _source.lastIndexOf('.');

    String basename = _source.substring(0, dot);

    int lastSlash = basename.lastIndexOf('/');

    if (lastSlash >= 0)
      basename = basename.substring(lastSlash + 1);

    out.println();
    out.println();
    out.println("\\noindent");
    out.println("\\epsfig{file=../images/" + basename + ",width=\\linewidth}");
    out.println();
    out.println();
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }
}
