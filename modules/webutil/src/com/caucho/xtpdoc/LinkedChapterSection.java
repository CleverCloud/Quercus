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

import com.caucho.config.Config;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class LinkedChapterSection extends ChapterSection {
  private static final Logger log = Logger.getLogger(Anchor.class.getName());
  private String _link;

  public void setLink(String link)
  {
    _link = link;
  }

  private Document configureDocument(Path xtpFile)
  {
    Document document = new Document(xtpFile, null);

    try {
      //org.w3c.dom.Node node = LooseToStrictHtml.looseToStrictHtml(xtpFile);

      Config config = new Config();
      config.setEL(false);

      config.configure(document, xtpFile);

      return document;
    } catch (Exception e) {
      System.err.println("Error configuring document (" + xtpFile + "): " + e);

      if (e.getCause() != null)
        e.getCause().printStackTrace();
      else 
        e.printStackTrace();

      return null;
    }
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    Path xtpFile = Vfs.lookup(_link);
    Document document = configureDocument(xtpFile);

    try {
      if (document != null)
        document.writeLaTeX(out);
    } catch (Exception e) {
      System.err.println("Error configuring document (" + xtpFile + "): " + e);

      if (e.getCause() != null)
        e.getCause().printStackTrace();
      else 
        e.printStackTrace();

      return;
    }
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    Path xtpFile = Vfs.lookup(_link);
    Document document = configureDocument(xtpFile);

    try {
      if (document != null)
        document.writeLaTeXEnclosed(out);
    } catch (Exception e) {
      System.err.println("Error configuring document (" + xtpFile + "): " + e);

      if (e.getCause() != null)
        e.getCause().printStackTrace();
      else 
        e.printStackTrace();

      return;
    }
  }

  public void writeLaTeXArticle(PrintWriter out)
    throws IOException
  {
    Path xtpFile = Vfs.lookup(_link);
    Document document = configureDocument(xtpFile);

    try {
      if (document != null) {
        document.writeLaTeXArticle(out);
      }
    } catch (Exception e) {
      System.err.println("Error configuring document (" + xtpFile + "): " + e);

      if (e.getCause() != null)
        e.getCause().printStackTrace();
      else 
        e.printStackTrace();

      return;
    }
  }
}
