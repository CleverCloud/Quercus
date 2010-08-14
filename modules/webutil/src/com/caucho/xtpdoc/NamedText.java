/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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

public class NamedText implements ContentItem {
  private String _name;
  private ContentItem _text;

  public NamedText(String name, ContentItem text)
  {
    _name = name;
    _text = text;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeCharacters("\n");
    out.writeStartElement("b");
    out.writeCharacters(_name);
    out.writeEndElement(); // b

    out.writeCharacters(" ");
    _text.writeHtml(out);
    out.writeEmptyElement("br");
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.print("\\textbf{" + LaTeXUtil.escapeForLaTeX(_name) + ":} ");

    _text.writeLaTeX(out);
  }

  public void writeLaTeXVerbatim(PrintWriter out)
    throws IOException
  {
    out.print("\u0001textbf\u0002" + _name + ":\u0003 ");

    _text.writeLaTeX(out);
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
