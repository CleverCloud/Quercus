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
import com.caucho.config.ConfigException;
import com.caucho.util.*;

public class Book {
  private static final L10N L = new L10N(Book.class);
  
  private String _title;
  private ArrayList<Chapter> _chapters = new ArrayList<Chapter>();
  private String _type = "book";

  public void setTitle(String title)
  {
    _title = title;
  }

  public void setType(String type)
  {
    _type = type;

    if (! "book".equals(type)
        && ! "article".equals(type)) {
      throw new ConfigException(L.l("'{0}' is an unknown document type", type));
    }
  }

  public boolean isBook()
  {
    return "book".equals(_type);
  }

  public boolean isArticle()
  {
    return "article".equals(_type);
  }

  public void addChapter(Chapter chapter)
  {
    _chapters.add(chapter);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    if (isBook())
      out.println("\\documentclass[twoside]{book}");
    else
      out.println("\\documentclass{article}");
    
    out.println();
    out.println("\\usepackage{url}");
    out.println("\\usepackage{hyperref}");
    out.println("\\usepackage{graphicx}");
    out.println("\\usepackage{color}");
    out.println("\\usepackage{colortbl}");
    out.println("\\usepackage{fancyvrb}");
    out.println("\\usepackage{listings}");
    out.println("\\usepackage{tabularx}");
    out.println("\\usepackage{filecontents}");
    out.println("\\usepackage{ltxtable}");
    out.println("\\usepackage{epsfig}");
    out.println("\\usepackage{boxedminipage}");
    out.println("\\usepackage{fancyhdr}");
    out.println();
    out.println("\\pagestyle{fancy}");
    out.println();

    if (isBook()) {
      out.println("\\lhead[\\leftmark]{}");
      out.println("\\rhead[]{\\rightmark}");
    }

    out.println();
    out.println("\\definecolor{example-gray}{gray}{0.8}");
    out.println("\\definecolor{results-gray}{gray}{0.6}");
    out.println();
    out.println("\\newcommand{\\verbbf}[1]{\\textbf{\\Verb #1}}");
    out.println();
    out.println("\\title{" + _title + "}");

    out.println("\\begin{document}");

    if (! isBook()) {
      out.println("\\maketitle");
    }
    out.println("\\tableofcontents");
    
    out.println("\\sloppy");

    for (Chapter chapter : _chapters) {
      if (isBook()) {
        chapter.writeLaTeX(out);
      }
      else {
        // technically, the article should only have one chapter
        chapter.writeLaTeXArticle(out);
      }
    }

    out.println("\\end{document}");
  }
}
