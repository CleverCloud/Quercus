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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xsl;

import com.caucho.vfs.Path;

import org.w3c.dom.Document;

import java.util.ArrayList;

/**
 * A document wrapper allowing caching of XSL-generated content.
 */
public class CacheableDocument {
  private ArrayList depends;
  private Document document;

  CacheableDocument(Document document, ArrayList depends)
  {
    this.document = document;
    this.depends = depends;
  }

  /**
   * Returns the document
   */
  public Document getDocument()
  {
    return document;
  }

  /**
   * True if the document is cacheable
   */
  public boolean isCacheable()
  {
    return depends != null;
  }

  /**
   * Returns the modification time of the document sources.
   */
  public long getLastModified()
  {
    if (depends == null)
      return 0;

    long lastModified = 0;
    for (int i = 0; i < depends.size(); i++) {
      Path path = (Path) depends.get(i);

      long time = path.getLastModified();
      if (time > lastModified)
        lastModified = time;
    }

    return lastModified;
  }

  public ArrayList getCacheDepends()
  {
    return depends;
  }

  void addDepend(Path path)
  {
    if (depends != null)
      depends.add(path);
  }
}


