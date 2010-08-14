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

public class Def extends VerboseFormattedTextWithAnchors {
  private String _title;

  public Def(Document document)
  {
    super(document);
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public String getCssClass()
  {
    return "definition";
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("div");
    out.writeAttribute("class", getCssClass());

    if (_title != null) {
      out.writeStartElement("div");
      out.writeAttribute("class", "def-caption");
      out.writeCharacters(_title);
      out.writeEndElement();
    }

    out.writeStartElement("div");
    out.writeAttribute("class", "definition-body");

    out.writeStartElement("pre");

    super.writeHtml(out);

    out.writeEndElement(); // pre
    out.writeEndElement(); // div
    out.writeEndElement(); // div
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.println("\\begin{center}");
    out.println("\\begin{Verbatim}[fontfamily=courier,");
    out.println("                  fontsize=\\footnotesize,");

    if (_title != null && ! "".equals(_title)) {
      out.println("                  label=" + _title + ",");
      out.println("                  labelposition=bottomline,");
    }

    out.println("                  samepage=true]");

    super.writeLaTeX(out);

    out.println();
    out.println("\\end{Verbatim}");
    out.println("\\end{center}");
  }
}
