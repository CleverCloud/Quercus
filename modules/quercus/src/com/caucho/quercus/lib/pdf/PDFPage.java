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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib.pdf;

import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * pdf object oriented API facade
 */
public class PDFPage {
  private static final Logger log
    = Logger.getLogger(PDFStream.class.getName());
  private static final L10N L = new L10N(PDFStream.class);

  private int _parent;
  private int _id;

  private PDFStream _stream;

  private double _width;
  private double _height;

  private ArrayList<String> _resources = new ArrayList<String>();

  PDFPage(PDFWriter out, int parent, double width, double height)
  {
    _parent = parent;
    _id = out.allocateId(1);
    _width = width;
    _height = height;
    _stream = new PDFStream(out.allocateId(1));
  }

  /**
   * Returns the id.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Returns the stream.
   */
  public PDFStream getStream()
  {
    return _stream;
  }

  void addResource(String resource)
  {
    if (! _resources.contains(resource))
      _resources.add(resource);
  }

  void write(PDFWriter out)
    throws IOException
  {
    out.beginObject(_id);
    out.println("  << /Type /Page");
    out.println("     /Parent " + _parent + " 0 R");
    out.println("     /MediaBox [0 0 " + _width + " " + _height + "]");
    out.println("     /Contents " + _stream.getId() + " 0 R");
    out.println("     /Resources <<");

    for (int i = 0; i < _resources.size(); i++) {
      out.println("      " + _resources.get(i));
    }

    out.println("     >>");
    out.println("  >>");
    out.endObject();

    _stream.write(out);
  }
}
