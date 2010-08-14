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

import com.caucho.config.types.RawString;

public class FormattedText extends ContainerNode {
  public FormattedText(Document document)
  {
    super(document);
  }

  public void setOccur(String occur)
  {
  }

  public void addText(RawString text)
  {
    addItem(new Text(text.getValue()));
  }

  public void addG(GlossaryText text)
  {
    addItem(text);
  }

  public Object createObject()
  {
    Object object = new Object(getDocument());
    addItem(object);
    return object;
  }

  public LineBreak createBr()
  {
    LineBreak lineBreak = new LineBreak(getDocument());
    addItem(lineBreak);
    return lineBreak;
  }

  public ItalicizedText createI()
  {
    ItalicizedText text = new ItalicizedText(getDocument());
    addItem(text);
    return text;
  }

  public BoldText createB()
  {
    BoldText text = new BoldText(getDocument());
    addItem(text);
    return text;
  }

  public EmphasizedText createEm()
  {
    EmphasizedText text = new EmphasizedText(getDocument());
    addItem(text);
    return text;
  }

  public SuperText createSup()
  {
    SuperText text = new SuperText(getDocument());
    addItem(text);
    return text;
  }

  public SmallText createSmall()
  {
    SmallText text = new SmallText(getDocument());
    addItem(text);
    return text;
  }

  public PreFormattedText createPre()
  {
    PreFormattedText pretext = new PreFormattedText(getDocument());
    addItem(pretext);
    return pretext;
  }

  public Variable createVar()
  {
    Variable variable = new Variable(getDocument());
    addItem(variable);
    return variable;
  }

  public Code createCode()
  {
    Code code = new Code(getDocument());
    addItem(code);
    return code;
  }

  public Font createFont()
  {
    Font font = new Font(getDocument());
    addItem(font);
    return font;
  }

  public Url createUrl()
  {
    Url url = new Url(getDocument());
    addItem(url);
    return url;
  }

  public Example createExample()
  {
    Example example = new Example(getDocument());
    addItem(example);
    return example;
  }

  public WebOnly createWebOnly()
  {
    WebOnly webOnly = new WebOnly(getDocument());
    addItem(webOnly);
    return webOnly;
  }

  public Figure createFigure()
  {
    Figure figure = new Figure(getDocument());
    addItem(figure);
    return figure;
  }

  public Table createTable()
  {
    Table table = new Table(getDocument());
    addItem(table);
    return table;
  }
}
