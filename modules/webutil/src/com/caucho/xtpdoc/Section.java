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

import java.io.IOException;
import java.io.PrintWriter;

public abstract class Section extends ContainerNode {
  protected FormattedTextWithAnchors _description;
  protected String _name;
  protected String _title;
  protected String _version;
  protected String _type;

  private String _parentHref;

  public Section(Document document)
  {
    this(document, "");
  }

  public Section(Document document, String parentHref)
  {
    super(document);
    _parentHref = parentHref;
  }


  // 
  // XXX: Stubbed
  //
  
  public void setOccur(String occur)
  {
  }

  public void setLocalTOCIndent(String localTOCIndent)
  {
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public void setProduct(String product)
  {
  }

  public void setIndex(String index)
  {
  }

  //
  // XXX: End stubbed
  //
  
  public void setType(String type)
  {
    _type = type;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public String getName()
  {
    return _name;
  }

  public String getTitle()
  {
    return _title;
  }

  public String getHref()
  {
    if (_name != null)
      return cleanHref(_name);
    else
      return cleanHref(_title);
  }

  public static String cleanHref(String href)
  {
    if (href == null)
      return href;
    
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < href.length(); i++) {
      char ch = href.charAt(i);

      switch (ch) {
        case '<': case '>': case '(': case ')': case '?':
          break;

        default:
          sb.append(ch);
          break;
      }
    }

    return sb.toString();
  }

  public Defun createDefun()
  {
    Defun defun = new Defun(getDocument());
    addItem(defun);
    return defun;
  }

  public DefinitionList createDl()
  {
    DefinitionList list = new DefinitionList(getDocument());
    addItem(list);
    return list;
  }

  public FormattedTextWithAnchors createDescription()
  {
    _description = new FormattedTextWithAnchors(getDocument());
    return _description;
  }

  public BlockQuote createBlockquote()
  {
    BlockQuote blockQuote = new BlockQuote(getDocument());
    addItem(blockQuote);
    return blockQuote;
  }

  public Center createCenter()
  {
    Center center = new Center(getDocument());
    addItem(center);
    return center;
  }

  public Paragraph createP()
  {
    Paragraph paragraph = new Paragraph(getDocument());
    addItem(paragraph);
    return paragraph;
  }

  public PreFormattedText createPre()
  {
    PreFormattedText pretext = new PreFormattedText(getDocument());
    addItem(pretext);
    return pretext;
  }

  public OrderedList createOl()
  {
    OrderedList orderedList = new OrderedList(getDocument());
    addItem(orderedList);
    return orderedList;
  }

  public UnorderedList createUl()
  {
    UnorderedList unorderedList = new UnorderedList(getDocument());
    addItem(unorderedList);
    return unorderedList;
  }

  public Figure createFigure()
  {
    Figure figure = new Figure(getDocument());
    addItem(figure);
    return figure;
  }

  public Example createExample()
  {
    Example example = new Example(getDocument());
    addItem(example);
    return example;
  }

  public Table createTable()
  {
    Table table = new Table(getDocument());
    addItem(table);
    return table;
  }

  public DefinitionTable createDeftable()
  {
    DefinitionTable definitionTable = new DefinitionTable(getDocument());
    addItem(definitionTable);
    return definitionTable;
  }

  public DefinitionTable createDeftableChildtags()
  {
    DefinitionTable definitionTable = new DefinitionTable(getDocument());
    addItem(definitionTable);
    return definitionTable;
  }

  public DefinitionTable createDeftableParameters()
  {
    DefinitionTable definitionTable = new DefinitionTable(getDocument());
    addItem(definitionTable);
    return definitionTable;
  }

  public Example createResults()
  {
    Example results = new Example(getDocument());
    addItem(results);
    return results;
  }

  public Def createDef()
  {
    Def def = new Def(getDocument());
    addItem(def);
    return def;
  }

  public FormattedTextWithAnchors createNote()
  {
    Note note = new Note(getDocument());
    addItem(note);
    return note;
  }

  public FormattedTextWithAnchors createWarn()
  {
    FormattedTextWithAnchors warning = 
      new FormattedTextWithAnchors(getDocument());
    addItem(new NamedText("Warning", warning));
    return warning;
  }

  public FormattedText createParents()
  {
    FormattedText parents = new FormattedText(getDocument());
    addItem(new NamedText("child of", parents));
    return parents;
  }

  public FormattedText createDefault()
  {
    FormattedText def = new FormattedText(getDocument());
    addItem(new NamedText("default", def));
    return def;
  }

  public Glossary createGlossary()
  {
    Glossary glossary = new Glossary(getDocument());
    addItem(glossary);
    return glossary;
  }
 
  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }

  protected void writeLaTeXLabel(PrintWriter out)
  {
    String fileName = getDocument().getDocumentPath().getUserPath();
    String label = fileName + ":" + _title;

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < label.length(); i++) {
      char ch = label.charAt(i);

      if (ch == ' ')
        sb.append('-');
      else if (ch == '<' || ch == '>') {
      }
      else
        sb.append(ch);
    }
    
    label = sb.toString();

    out.println("\\label{" + label + "}");
    out.println("\\hypertarget{" + label + "}{}");

  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    if (isWebOnly())
      return;
    
    writeLaTeXLabel(out);
    
    super.writeLaTeX(out);

    if (_type != null && _type.equals("defun"))
      out.println("\\newpage");
  }

  abstract public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException;
}
