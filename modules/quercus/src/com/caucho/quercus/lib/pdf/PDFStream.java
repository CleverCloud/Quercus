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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.util.L10N;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * pdf object oriented API facade
 */
public class PDFStream {
  private static final Logger log
    = Logger.getLogger(PDFStream.class.getName());
  private static final L10N L = new L10N(PDFStream.class);

  private int _id;

  private TempStream _tempStream = new TempStream();
  private WriteStream _out = new WriteStream();

  private PDFProcSet _procSet;
  private PDFFont _font;
  private double _fontSize = 24;

  private boolean _inText;
  private boolean _hasFont;
  private boolean _hasTextPos = true;
  private double _textX = 0;
  private double _textY = 0;

  private double _x = 0;
  private double _y = 0;
  private boolean _hasGraphicsPos = true;

  PDFStream(int id)
  {
    _id = id;

    _tempStream = new TempStream();
    _tempStream.openWrite();
    _out.init(_tempStream);

    _procSet = new PDFProcSet();
    _procSet.add("/PDF");

    _font = null;
    _inText = false;
    _hasFont = false;
    _hasTextPos = true;
    _hasGraphicsPos = true;
  }

  public int getId()
  {
    return _id;
  }

  public void setFont(PDFFont font, double size)
  {
    _font = font;
    _fontSize = size;
    _hasFont = false;
  }

  public PDFFont getFont()
  {
    return _font;
  }

  public double getFontSize()
  {
    return _fontSize;
  }

  public void setTextPos(double x, double y)
  {
    _textX = x;
    _textY = y;
    _hasTextPos = false;
  }

  public void stroke()
  {
    flushToGraph();

    println("S");
  }

  public void closepath()
  {
    flushToGraph();

    println("h");
  }

  public void clip()
  {
    flushToGraph();

    println("W");
  }

  public void curveTo(double x1, double y1,
                      double x2, double y2,
                      double x3, double y3)
  {
    flushToGraph();

    if (x1 == x2 && y1 == y2) {
      println(x1 + " " + y1 + " " +
                   x3 + " " + y3 + " y");
    }
    else if (x2 == x3 && y2 == y3) {
      println(x1 + " " + y1 + " " +
                   x2 + " " + y2 + " v");
    }
    else {
      println(x1 + " " + y1 + " " +
                   x2 + " " + y2 + " " +
                   x3 + " " + y3 + " c");
    }

    _x = x3;
    _y = y3;
    _hasGraphicsPos = true;
  }

  public void endpath()
  {
    flushToGraph();

    println("n");
  }

  public void closepathStroke()
  {
    flushToGraph();

    println("s");
  }

  public void closepathFillStroke()
  {
    flushToGraph();

    println("b");
  }

  public void fill()
  {
    flushToGraph();

    println("f");
  }

  public void fillStroke()
  {
    flushToGraph();

    println("B");
  }

  public void lineTo(double x, double y)
  {
    flushToGraph();

    if (x != _x || y != _y || ! _hasGraphicsPos)
      println(x + " " + y + " l");

    _x = x;
    _y = y;
    _hasGraphicsPos = true;
  }

  public void rect(double x, double y, double w, double h)
  {
    flushToGraph();

    println(x + " " + y + " " + w + " " + h + " re");
  }

  public void moveTo(double x, double y)
  {
    if (_x != x || _y != y) {
      _x = x;
      _y = y;
      _hasGraphicsPos = false;
    }
  }

  public static int STROKE = 1;
  public static int FILL = 2;
  public static int BOTH = 3;

  public boolean setcolor(String fstype, String colorspace,
                          double c1, double c2, double c3, double c4)
  {
    flushToGraph();

    int type;
    if ("both".equals(fstype) || "fillstroke".equals(fstype))
      type = BOTH;
    else if ("fill".equals(fstype))
      type = FILL;
    else if ("stroke".equals(fstype))
      type = STROKE;
    else
      return false;

    if ("gray".equals(colorspace)) {
      if ((type & STROKE) != 0)
        println(c1 + " G");
      if ((type & FILL) != 0)
        println(c1 + " g");

      return true;
    }
    else if ("rgb".equals(colorspace)) {
      if ((type & STROKE) != 0)
        println(c1 + " " + c2 + " " + c3 + " RG");
      if ((type & FILL) != 0)
        println(c1 + " " + c2 + " " + c3 + " rg");

      return true;
    }
    else if ("cmyk".equals(colorspace)) {
      if ((type & STROKE) != 0)
        println(c1 + " " + c2 + " " + c3 + " " + c4 + " K");
      if ((type & FILL) != 0)
        println(c1 + " " + c2 + " " + c3 + " " + c4 + " k");

      return true;
    }
    else {
      // spot, pattern, iccbasedgray, iccbasedrgb, iccbasedcmyk, lab

      return false;
    }
  }

  public void setDash(double b, double w)
  {
    println("[" + b + " " + w + "] 0 d");
  }

  public boolean setlinewidth(double w)
  {
    println(w + " w");

    return true;
  }

  /**
   * Saves the graphics state
   */
  public boolean save()
  {
    println("q");

    return true;
  }

  /**
   * Restores the graphics state
   */
  public boolean restore()
  {
    println("Q");

    return true;
  }

  public boolean concat(double a, double b, double c,
                        double d, double e, double f)
  {
    println(String.format("%.4f %.4f %.4f %.4f %.4f %.4f cm",
                          a, b, c, d, e, f));

    return true;
  }

  public void show(String text)
  {
    _procSet.add("/Text");

    if (! _inText) {
      println("BT");
      _inText = true;
    }

    if (! _hasFont && _font != null) {
      println("/" + _font.getPDFName() + " " + _fontSize + " Tf");
      _hasFont = true;
    }

    if (! _hasTextPos) {
      println(_textX + " " + _textY + " Td");
      _hasTextPos = true;
    }

    println("(" + text + ") Tj");
  }

  public void continue_text(String text)
  {
    println("(" + text + ") T*");
  }

  public boolean fit_image(PDFImage img)
  {
    _procSet.add("/ImageB");
    _procSet.add("/ImageC");
    _procSet.add("/ImageI");

    println("/I" + img.getId() + " Do");

    return true;
  }

  public void flushToGraph()
  {
    try {
      flush();

      if (! _hasGraphicsPos) {
        _out.println(_x + " " + _y + " m");
        _hasGraphicsPos = true;
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  private void println(String s)
  {
    try {
      _out.println(s);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  public void flush()
  {
    try {
      if (_inText) {
        _out.println("ET");
        _inText = false;
      }

      _out.flush();
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  public PDFProcSet getProcSet()
  {
    return _procSet;
  }

  public int getLength()
  {
    try {
      _out.flush();

      return _tempStream.getLength();
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  public void write(PDFWriter out)
    throws IOException
  {
    out.writeStream(getId(), this);
  }

  public void writeToStream(WriteStream os)
    throws IOException
  {
    _tempStream.writeToStream(os);
    _tempStream.destroy();
  }
}
