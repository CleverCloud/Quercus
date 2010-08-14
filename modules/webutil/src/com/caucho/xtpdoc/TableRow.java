/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
import java.util.ArrayList;

public class TableRow {
  private Document _document;
  private ArrayList<TableCell> _cells = new ArrayList<TableCell>();

  public TableRow(Document document)
  {
    _document = document;
  }

  public int getNumberOfColumns()
  {
    return _cells.size();
  }

  public TableData createTd()
  {
    TableData data = new TableData(_document);
    _cells.add(data);
    return data;
  }

  public TableHeader createTh()
  {
    TableHeader header = new TableHeader(_document);
    _cells.add(header);
    return header;
  }

  public void setOccur(String occur)
  {
  }

  public void writeHtml(XMLStreamWriter out, int count)
    throws XMLStreamException
  {
    out.writeStartElement("tr");
    out.writeAttribute("class", count % 2 == 0 ? "a" : "b");

    for (TableCell cell : _cells)
      cell.writeHtml(out);

    out.writeEndElement(); // tr
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    for (int i = 0; i < _cells.size(); i++) {
      _cells.get(i).writeLaTeX(out);

      if (i < _cells.size() - 1)
        out.print("&");
      else
        out.println("\\\\");

      out.flush();
    }
  }
}
