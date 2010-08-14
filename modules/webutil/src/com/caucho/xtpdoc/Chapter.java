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
import java.util.ArrayList;

public class Chapter {
  private String _title;
  private ArrayList<ChapterSection> _sections = new ArrayList<ChapterSection>();

  public void setTitle(String title)
  {
    _title = title;
  }

  public void addLinkedSection(LinkedChapterSection section)
  {
    _sections.add(section);
  }

  public void addEnclosingSection(EnclosingChapterSection section)
  {
    _sections.add(section);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.println("\\chapter{" + LaTeXUtil.escapeForLaTeX(_title) + "}");

    writeLaTeXBody(out);
  }
  
  public void writeLaTeXBody(PrintWriter out)
    throws IOException
  {
    for (ChapterSection section : _sections)
      section.writeLaTeX(out);
  }
  
  public void writeLaTeXArticle(PrintWriter out)
    throws IOException
  {
    for (ChapterSection section : _sections) {
      section.writeLaTeXArticle(out);
    }
  }
}
