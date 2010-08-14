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
import com.caucho.config.types.*;

public class TableHeader extends FormattedTextWithAnchors implements TableCell {
  private String _rowspan;
  private String _colspan;
  private String _width;

  public TableHeader(Document document)
  {
    super(document);
  }

  public void setRowspan(String rowspan)
  {
    _rowspan = rowspan;
  }

  public void setColspan(String colspan)
  {
    _colspan = colspan;
  }

  public void setWidth(String width)
  {
    _width = width;
  }

  @Override
  public void addText(RawString text)
  {
    addItem(new Text(text.getValue().toUpperCase()));
  }

  @Override
  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("th");

    if (_width != null)
      out.writeAttribute("width", _width);

    if (_colspan != null)
      out.writeAttribute("colspan", _colspan);

    if (_rowspan != null)
      out.writeAttribute("rowspan", _rowspan);

    super.writeHtml(out);

    out.writeEndElement(); // th
  }

  @Override
  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.print("\\textbf{");

    super.writeLaTeX(out);

    out.print("}");
  }
}
