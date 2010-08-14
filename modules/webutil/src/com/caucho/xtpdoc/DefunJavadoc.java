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

import com.caucho.config.types.RawString;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;

public class DefunJavadoc implements ContentItem {
  private static final Logger log 
    = Logger.getLogger(DefunJavadoc.class.getName());

  private final Defun _defun;
  private String _class;

  public DefunJavadoc(Defun defun)
  {
    _defun = defun;
  }

  public void setClass(String cl)
  {
    _class = cl;
  }

  private String getHref()
  {
    String path = _class.replace('.', '/') + ".html";

    return "http://www.caucho.com/resin-javadoc/" + path;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("div");
    out.writeAttribute("class", "javadoc");

    out.writeStartElement("span");
    out.writeAttribute("class", "javadoc");
    out.writeCharacters("javadoc ");
    out.writeEndElement(); // span

    out.writeStartElement("a");

    out.writeAttribute("href", getHref());

    out.writeCharacters(_defun.getTitle());

    out.writeEndElement(); // a
    out.writeEndElement(); // div
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.print("{\\bf javadoc}");
    out.print("\\href{");
    out.print(getHref());
    out.print("}{");
    out.print(LaTeXUtil.escapeForLaTeX(_defun.getTitle()));
    out.print("}\\\\");
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
  }
  
  public void writeLaTeXVerbatim(PrintWriter out)
    throws IOException
  {
    throw new ConfigException("<javadoc> not allowed in a verbatim context");
  }
}
