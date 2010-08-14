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

public class Faq extends Section {
  private FormattedTextWithAnchors _description;

  public Faq(Document document)
  {
    super(document);
  }

  public S1 createS1()
  {
    S1 s1 = new S1(getDocument());
    addItem(s1);
    return s1;
  }

  public FormattedTextWithAnchors createDescription()
  {
    _description = new FormattedTextWithAnchors(getDocument());
    return _description;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("b");
    out.writeCharacters(_title);
    out.writeEndElement();

    if (_description != null) {
      out.writeStartElement("p");
      out.writeStartElement("i");

      _description.writeHtml(out);

      out.writeEndElement();
      out.writeEndElement();
    }

    super.writeHtml(out);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.println("\\textbf{" + _title + "}\\\\");
    out.println();
    out.println();

    if (_description != null) {
      out.print("\\textit{");
      _description.writeLaTeX(out);
      out.println("}");
    }

    super.writeLaTeX(out);
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    if (_type != null && _type.equals("defun"))
      out.println("\\newpage");

    if (_title != null)
      out.println("\\subsubsection{" + LaTeXUtil.escapeForLaTeX(_title) + "}");

    super.writeLaTeX(out);
  }
}
