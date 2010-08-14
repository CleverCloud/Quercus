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

import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

// This should really be a section, except the 3.1 docs allowed a section
// to have a description 
public class DefunDescription extends FormattedTextWithAnchors {
  private final Defun _defun;

  public DefunDescription(Defun defun) 
  {
    super(defun.getDocument());

    _defun = defun;
  }

  public FormattedTextWithAnchors createNote()
  {
    Note note = new Note(getDocument());
    addItem(note);
    return note;
  }

  public Paragraph createP()
  {
    Paragraph paragraph = new Paragraph(getDocument());
    addItem(paragraph);
    return paragraph;
  }

  public DefinitionTable createDeftable()
  {
    DefinitionTable definitionTable = new DefinitionTable(getDocument());
    addItem(definitionTable);
    return definitionTable;
  }

  public Def createDef()
  {
    Def def = new Def(getDocument());
    addItem(def);
    return def;
  }

  public UnorderedList createUl()
  {
    UnorderedList unorderedList = new UnorderedList(getDocument());
    addItem(unorderedList);
    return unorderedList;
  }

  public S2 createS2()
  {
    S2 s2 = new S2(getDocument(), _defun.getHref());
    addItem(s2);
    return s2;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeCharacters("\n");
    out.writeStartElement("div");
    out.writeAttribute("class", "reference-description");
    
    super.writeHtml(out);
    
    out.writeEndElement();
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }
}
