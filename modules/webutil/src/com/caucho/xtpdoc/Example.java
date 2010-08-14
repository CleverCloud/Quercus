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

public class Example extends VerboseFormattedTextWithAnchors {
  private String _title;
  private String _file;
  private String _language;

  public Example(Document document)
  {
    super(document);
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public void setLanguage(String language)
  {
    _language = language;
  }

  public void setFile(String file)
  {
    _file = file;
  }

  public String getCssClass()
  {
    return "example";
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeCharacters("\n");

    out.writeStartElement("div");
    out.writeAttribute("class", getCssClass());

    if (_title != null) {
      out.writeStartElement("div");
      out.writeAttribute("class", "caption");
      out.writeCharacters(_title);
      out.writeEndElement();
    }

    out.writeStartElement("div");
    out.writeAttribute("class", "example-body");

    out.writeStartElement("pre");

    super.writeHtml(out);

    out.writeEndElement(); // pre
    out.writeEndElement(); // div
    out.writeEndElement(); // div
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    if (_language != null) {
      out.println("\\lstset{fancyvrb,language=" + _language + ",");
      out.println("         showstringspaces=false,basicstyle=\\small,");
      out.println("         stringstyle=\\color[gray]{0.6}}");
    }

    out.println("\\begin{center}");
    out.println("\\begin{Verbatim}[frame=single,fontfamily=courier,");
    out.println("                  framerule=1pt,");
    out.println("                  fontsize=\\footnotesize,");
    out.println("                  commandchars=\\\u0001\\\u0002\\\u0003,");

    if (_title != null && ! "".equals(_title)) {
      out.print("                  labelposition=bottomline,label=\\fbox{");
      out.println(LaTeXUtil.escapeForLaTeX(_title) + "},");
    }

    out.println("                  samepage=true]");

    super.writeLaTeXVerbatim(out);

    // make room for the title box
    if (_title != null)
      out.println();

    out.println();
    out.println("\\end{Verbatim}");

    out.println("\\end{center}");

    if (_language != null)
      out.println("\\lstset{fancyvrb=false}");
  }

  @Override
  public void writeLaTeXVerbatim(PrintWriter out)
  {
    throw new ConfigException("<example> should not be in a verbatim context");
  }
}
