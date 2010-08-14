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

public class Localtoc implements ContentItem {
  Document _document;
  
  Localtoc(Document doc)
  {
    _document = doc;
  }
  
  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    ContainerNode container = _document.getBody();

    writeContainer(out, container);
  }
  
  private void writeContainer(XMLStreamWriter out, ContainerNode container)
    throws XMLStreamException
  {
    if (container == null)
      return;

    out.writeCharacters("\n");
    out.writeStartElement("div");
    out.writeAttribute("class", "toc");

    writeContainerRec(out, container);
    
    out.writeEndElement(); // div
  }
  
  private void writeContainerRec(XMLStreamWriter out, ContainerNode container)
    throws XMLStreamException
  {
    if (container == null)
      return;

    out.writeStartElement("ol");

    for (ContentItem item : container.getItems()) {
      if (item instanceof Section) {
        Section section = (Section) item;

        if (section.getTitle() != null
            && ! "".equals(section.getTitle())) {
          out.writeStartElement("li");
          out.writeStartElement("a");
          out.writeAttribute("href", "#" + section.getHref());
          out.writeCharacters(section.getTitle());
          out.writeEndElement();
          out.writeEndElement();

          writeContainerRec(out, section);
        }
      }
    }
    out.writeEndElement(); // </ul>
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
  }

  public void writeLaTeXVerbatim(PrintWriter out)
    throws IOException
  {
    throw new ConfigException("<localtoc> not allowed in a verbatim context");
  }
}
