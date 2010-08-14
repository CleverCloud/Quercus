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

import java.util.HashSet;

public class Header {
  private Document _document;
  private String _product;
  private String _version;
  private String _title;
  private String _browserTitle;
  private String _author;
  private String _date;
  private Section _description;
  private Keywords _keywords;
  private String _tutorial;

  public Header(Document document)
  {
    _document = document;
  }

  public void setProduct(String product)
  {
    _product = product;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public void setBrowserTitle(String title)
  {
    _browserTitle = title;
  }

  public String getTitle()
  {
    return _title;
  }

  public void setDate(String date)
  {
    _date = date;
  }

  public void setAuthor(String author)
  {
    _author = author;
  }

  public void setResin2_0(String resin2_0)
  {
  }

  public void setType(String type)
  {
  }

  public void setTutorialStartpage(String startPage)
  {
    _tutorial = startPage;
  }

  public String getTutorialStartPage()
  {
    return _tutorial;
  }

  public void setLevel(String level)
  {
  }

  public void setKeywords(Keywords keywords)
  {
    _keywords = keywords;
  }

  public ContentItem getDescription()
  {
    return _description;
  }

  public Section createDescription()
  {
    _description = new S1(_document);
    return _description;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("head");

    out.writeEmptyElement("meta");
    out.writeAttribute("http-equiv", "Content-Type");
    out.writeAttribute("content", "text/html; charset=utf-8");

    if (_product != null) {
      out.writeEmptyElement("meta");
      out.writeAttribute("name", "product");
      out.writeAttribute("content", _product);
    }

    if (_version != null) {
      out.writeEmptyElement("meta");
      out.writeAttribute("name", "version");
      out.writeAttribute("content", _version);
    }

    if (_keywords != null) {
      out.writeEmptyElement("meta");
      out.writeAttribute("name", "keywords");
      out.writeAttribute("content", _keywords.toString());
    }

    out.writeEmptyElement("link");
    out.writeAttribute("rel", "STYLESHEET");
    out.writeAttribute("type", "text/css");
    out.writeAttribute("href", _document.getContextPath() + "/css/default.css");

    out.writeEmptyElement("link");
    out.writeAttribute("rel", "shortcut icon");
    out.writeAttribute("href", _document.getContextPath() + "/images/favicon.ico");

    // this must be a start/end - browsers don't like empty <script/> tags
    out.writeStartElement("script");
    out.writeAttribute("type", "text/javascript");
    out.writeAttribute("src", _document.getContextPath() + "/js/default.js");
    out.writeEndElement(); // script

    out.writeStartElement("script");
    out.writeAttribute("type", "text/javascript");

    // this init() function is in default.js
    out.writeCharacters("  window.onload = function() {\n");
    out.writeCharacters("    init();\n");
    out.writeCharacters("  };\n");
    out.writeEndElement(); // script

    out.writeStartElement("title");

    NavigationItem nav = _document.getNavigation();

    if (nav != null
        && nav.getNavigation() != null
        && nav.getNavigation().getSection() != null)
      out.writeCharacters(nav.getNavigation().getSection());
    
    out.writeCharacters(_title);
    out.writeEndElement(); // title

    out.writeEndElement(); // head
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    out.println("\\usepackage[margin=1in]{geometry}");
    out.println("\\usepackage{url}");
    out.println("\\usepackage{hyperref}");
    out.println("\\usepackage{graphicx}");
    out.println("\\usepackage{color}");
    out.println("\\usepackage{colortbl}");
    out.println("\\usepackage{fancyvrb}");
    out.println("\\usepackage{listings}");
    out.println();
    out.println("\\definecolor{example-gray}{gray}{0.8}");
    out.println("\\definecolor{results-gray}{gray}{0.6}");
    out.println();
    out.println("\\title{" + _title + "}");
    //XXX: product & version
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    String label = _document.getDocumentPath().getUserPath();
    
    out.println("\\label{" + label + "}");
    out.println("\\hypertarget{" + label + "}{}");
    
    out.println("\\section{" + LaTeXUtil.escapeForLaTeX(_title) + "}");
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    String label = _document.getDocumentPath().getUserPath();

    out.println("\\label{" + label + "}");
    out.println("\\hypertarget{" + label + "}{}");
    
    out.println("\\subsection{" + LaTeXUtil.escapeForLaTeX(_title) + "}");
  }

  public void writeLaTeXArticle(PrintWriter out)
    throws IOException
  {
    /*
    String label = _document.getDocumentPath().getUserPath();
    
    out.println("\\label{" + label + "}");
    out.println("\\hypertarget{" + label + "}{}");
    
    out.println("\\section{" + LaTeXUtil.escapeForLaTeX(_title) + "}");
    */
  }
}
