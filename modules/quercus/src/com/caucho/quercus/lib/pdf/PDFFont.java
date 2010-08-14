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
import java.util.logging.Logger;

/**
 * pdf object oriented API facade
 */
public class PDFFont extends PDFObject {
  private static final Logger log
    = Logger.getLogger(PDFFont.class.getName());
  private static final L10N L = new L10N(PDFFont.class);

  private int _id;

  private final Font _face;

  private final String _encoding;
  private final String _opt;

  PDFFont(Font face, String encoding, String opt)
  {
    _face = face;
    _encoding = encoding;
    _opt = opt;
  }

  void setId(int id)
  {
    _id = id;
  }

  public int getId()
  {
    return _id;
  }

  public String getFontName()
  {
    return _face.getFontName();
  }

  public String getFontStyle()
  {
    return _face.getWeight();
  }

  public double getAscender()
  {
    return _face.getAscender();
  }

  public double getCapHeight()
  {
    return _face.getCapHeight();
  }

  public double getDescender()
  {
    return _face.getDescender();
  }

  public double stringWidth(String text)
  {
    return _face.stringWidth(text);
  }

  public String getPDFName()
  {
    return "F" + _id;
  }

  String getResource()
  {
    return("/Font << /F" + _id + " " + _id + " 0 R>>");
  }

  public void writeObject(PDFWriter out)
    throws IOException
  {
    out.println("<< /Type /Font");
    out.println("   /Subtype /Type1");
    out.println("   /Name /" + _face.getFontName());
    out.println("   /BaseFont /Helvetica");
    out.println("   /Encoding /MacRomanEncoding");
    out.println(">>");
  }

  public int hashCode()
  {
    int hash = 37;

    hash = 65521 * hash + _face.hashCode();
    hash = 65521 * hash + _encoding.hashCode();
    hash = 65521 * hash + _opt.hashCode();

    return hash;
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof PDFFont))
      return false;

    PDFFont font = (PDFFont) o;

    return (_face == font._face &&
            _encoding.equals(font._encoding) &&
            _opt.equals(font._opt));
  }

  public String toString()
  {
    return "PDFFont[" + _face.getFontName() + "," + _encoding + "," + _opt + "]";
  }
}
