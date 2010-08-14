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

import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * pdf object oriented API facade
 */
public class PDF {
  private static final Logger log = Logger.getLogger(PDF.class.getName());
  private static final L10N L = new L10N(PDF.class);

  private static final double KAPPA = 0.5522847498;

  private static final int PAGE_GROUP = 8;

  private static HashMap<String,Font> _faceMap = new HashMap<String,Font>();

  private HashMap<PDFFont,PDFFont> _fontMap
    = new HashMap<PDFFont,PDFFont>();

  private HashMap<PDFProcSet,PDFProcSet> _procSetMap
    = new HashMap<PDFProcSet,PDFProcSet>();

  private TempStream _tempStream;
  private WriteStream _os;
  private PDFWriter _out;

  private ArrayList<PDFPage> _pageGroup = new ArrayList<PDFPage>();
  private ArrayList<Integer> _pagesGroupList = new ArrayList<Integer>();
  private int _pageCount;

  private int _catalogId;

  private int _pageParentId;

  private PDFPage _page;
  private PDFStream _stream;

  public PDF(Env env)
  {
    _out = new PDFWriter(env.getOut());
  }

  public boolean begin_document(@Optional String fileName,
                                @Optional String optList)
    throws IOException
  {
    _tempStream = new TempStream();
    _tempStream.openWrite();
    _os = new WriteStream(_tempStream);

    _out = new PDFWriter(_os);
    _out.beginDocument();

    _catalogId = _out.allocateId(1);
    _pageParentId = _out.allocateId(1);

    return true;
  }

  public boolean begin_page(double width, double height)
    throws IOException
  {
    if (PAGE_GROUP <= _pageGroup.size()) {
      _out.writePageGroup(_pageParentId, _pageGroup);
      _pageGroup.clear();

      _pagesGroupList.add(_pageParentId);
      _pageParentId = _out.allocateId(1);
    }

    _page = new PDFPage(_out, _pageParentId, width, height);
    _stream = _page.getStream();

    _pageCount++;

    _pageGroup.add(_page);

    return true;
  }

  public boolean begin_page_ext(double width, double height, String opt)
    throws IOException
  {
    return begin_page(width, height);
  }

  public boolean set_info(String key, String value)
  {
    if ("Author".equals(key)) {
      _out.setAuthor(key);
      return true;
    }
    else if ("Title".equals(key)) {
      _out.setTitle(key);
      return true;
    }
    else if ("Creator".equals(key)) {
      _out.setCreator(key);
      return true;
    }
    else
      return false;
  }

  public boolean set_parameter(String key, String value)
  {
    return false;
  }

  public boolean set_value(String key, double value)
  {
    return false;
  }

  /**
   * Returns the result as a string.
   */
  public Value get_buffer(Env env)
  {
    TempStream ts = _tempStream;
    _tempStream = null;

    if (ts == null)
      return BooleanValue.FALSE;

    StringValue result = env.createBinaryBuilder();
    for (TempBuffer ptr = ts.getHead();
         ptr != null;
         ptr = ptr.getNext()) {
      result.append(ptr.getBuffer(), 0, ptr.getLength());
    }

    ts.destroy();

    return result;
  }

  /**
   * Returns the error message.
   */
  public String get_errmsg()
  {
    return "";
  }

  /**
   * Returns the error number.
   */
  public int get_errnum()
  {
    return 0;
  }

  /**
   * Returns the value for a parameter.
   */
  public String get_parameter(String name, @Optional double modifier)
  {
    if ("fontname".equals(name)) {
      PDFFont font = _stream.getFont();

      if (font != null)
        return font.getFontName();
      else
        return null;
    }
    else
      return null;
  }

  /**
   * Returns the value for a parameter.
   */
  public double get_value(String name, @Optional double modifier)
  {
    if ("ascender".equals(name)) {
      PDFFont font = _stream.getFont();

      if (font != null)
        return font.getAscender();
      else
        return 0;
    }
    else if ("capheight".equals(name)) {
      PDFFont font = _stream.getFont();

      if (font != null)
        return font.getCapHeight();
      else
        return 0;
    }
    else if ("descender".equals(name)) {
      PDFFont font = _stream.getFont();

      if (font != null)
        return font.getDescender();
      else
        return 0;
    }
    else if ("fontsize".equals(name)) {
      return _stream.getFontSize();
    }
    else
      return 0;
  }

  public boolean initgraphics(Env env)
  {
    env.stub("initgraphics");

    return false;
  }

  /**
   * Loads a font for later use.
   *
   * @param name the font name, e.g. Helvetica
   * @param encoding the font encoding, e.g. winansi
   * @param opt any options
   */
  public PDFFont load_font(String name, String encoding, String opt)
    throws IOException
  {
    Font face = loadFont(name);

    PDFFont font = new PDFFont(face, encoding, opt);

    PDFFont oldFont = _fontMap.get(font);

    if (oldFont != null)
      return oldFont;

    font.setId(_out.allocateId(1));

    _fontMap.put(font, font);

    _out.addPendingObject(font);

    return font;
  }

  private Font loadFont(String name)
    throws IOException
  {
    synchronized (_faceMap) {
      Font face = _faceMap.get(name);

      if (face == null) {
        face = new AfmParser().parse(name);

        _faceMap.put(name, face);
      }

      return face;
    }
  }

  /**
   * Sets the dashing
   *
   * @param b black length
   * @param w which length
   */
  public boolean setdash(double b, double w)
  {
    _stream.setDash(b, w);

    return true;
  }

  /**
   * Sets the dashing
   */
  public boolean setdashpattern(Env env, @Optional String optlist)
  {
    env.stub("setdashpattern");

    return false;
  }

  /**
   * Sets the flatness
   */
  public boolean setflat(Env env, double flatness)
  {
    env.stub("setflat");

    return false;
  }

  /**
   * Sets the linecap style
   */
  public boolean setlinecap(Env env,
                            int cap)
  {
    env.stub("setlinecap");

    return false;
  }

  /**
   * Sets the linejoin style
   */
  public boolean setlinejoin(Env env,
                             int linejoin)
  {
    env.stub("setlinejoin");

    return false;
  }

  /**
   * Sets the current font
   *
   * @param name the font name, e.g. Helvetica
   * @param encoding the font encoding, e.g. winansi
   * @param opt any options
   */
  public boolean setfont(@NotNull PDFFont font, double size)
    throws IOException
  {
    if (font == null)
      return false;

    _stream.setFont(font, size);

    _page.addResource(font.getResource());

    return true;
  }

  /**
   * Sets the matrix style
   */
  public boolean setmatrix(Env env,
                           double a,
                           double b,
                           double c,
                           double d,
                           double e,
                           double f)
  {
    env.stub("setmatrix");

    return false;
  }

  /**
   * Sets the miter limit
   */
  public boolean setmiterlimit(Env env, double v)
  {
    env.stub("setmiterlimit");

    return false;
  }

  /**
   * Sets the shading pattern
   */
  public boolean shading_pattern(Env env,
                                 int shading,
                                 @Optional String optlist)
  {
    env.stub("shading_pattern");

    return false;
  }

  /**
   * Define a blend
   */
  public int shading(Env env,
                     String type,
                     double x1,
                     double y1,
                     double x2,
                     double y2,
                     double c1,
                     double c2,
                     double c3,
                     double c4,
                     @Optional String optlist)
  {
    env.stub("shading");

    return 0;
  }

  /**
   * Fill with a shading object.
   */
  public boolean shfill(Env env,
                        int shading)
  {
    env.stub("shfill");

    return false;
  }

  /**
   * Returns the length of a string for a font.
   */
  public double stringwidth(String string, @NotNull PDFFont font, double size)
  {
    if (font == null)
      return 0;

    return size * font.stringWidth(string) / 1000.0;
  }


  /**
   * Sets the text position.
   */
  public boolean set_text_pos(double x, double y)
  {
    _stream.setTextPos(x, y);

    return true;
  }

  /**
   * Fills
   */
  public boolean fill()
  {
    _stream.fill();

    return true;
  }

  /**
   * Closes the path
   */
  public boolean closepath()
  {
    _stream.closepath();

    return true;
  }

  /**
   * Appends the current path to the clipping path.
   */
  public boolean clip()
  {
    _stream.clip();

    return true;
  }

  /**
   * Closes the path strokes
   */
  public boolean closepath_stroke()
  {
    _stream.closepathStroke();

    return true;
  }

  /**
   * Closes the path strokes
   */
  public boolean closepath_fill_stroke()
  {
    _stream.closepathFillStroke();

    return true;
  }

  /**
   * Fills
   */
  public boolean fill_stroke()
  {
    _stream.fillStroke();

    return true;
  }

  /**
   * Ends the path
   */
  public boolean endpath()
  {
    _stream.endpath();

    return true;
  }

  /**
   * Draws a bezier curve
   */
  public boolean curveto(double x1, double y1,
                         double x2, double y2,
                         double x3, double y3)
  {
    _stream.curveTo(x1, y1, x2, y2, x3, y3);

    return true;
  }

  /**
   * Draws a bezier curve
   */
  public boolean curveto_b(double x1, double y1,
                           double x2, double y2)
  {
    _stream.curveTo(x1, y1, x1, y1, x2, y2);

    return true;
  }

  /**
   * Draws a bezier curve
   */
  public boolean curveto_e(double x1, double y1,
                           double x2, double y2)
  {
    _stream.curveTo(x1, y1, x2, y2, x2, y2);

    return true;
  }

  /**
   * Creates a counterclockwise arg
   */
  public boolean arc(double x1, double y1, double r, double a, double b)
  {
    a = a % 360;
    if (a < 0)
      a += 360;

    b = b % 360;
    if (b < 0)
      b += 360;

    if (b < a)
      b += 360;

    int aQuarter = (int) (a / 90);
    int bQuarter = (int) (b / 90);

    if (aQuarter == bQuarter) {
      clockwiseArc(x1, y1, r, a, b);
    }
    else {
      clockwiseArc(x1, y1, r, a, (aQuarter + 1) * 90);

      for (int q = aQuarter + 1; q < bQuarter; q++)
        clockwiseArc(x1, y1, r, q * 90, (q + 1) * 90);

      clockwiseArc(x1, y1, r, bQuarter * 90, b);
    }

    return true;
  }

  /**
   * Creates a clockwise arc
   */
  public boolean arcn(double x1, double y1, double r, double a, double b)
  {
    a = a % 360;
    if (a < 0)
      a += 360;

    b = b % 360;
    if (b < 0)
      b += 360;

    if (a < b)
      a += 360;

    int aQuarter = (int) (a / 90);
    int bQuarter = (int) (b / 90);

    if (aQuarter == bQuarter) {
      counterClockwiseArc(x1, y1, r, a, b);
    }
    else {
      counterClockwiseArc(x1, y1, r, a, aQuarter * 90);

      for (int q = aQuarter - 1; bQuarter < q; q--)
        counterClockwiseArc(x1, y1, r, (q + 1) * 90, q * 90);

      counterClockwiseArc(x1, y1, r, (bQuarter + 1) * 90, b);
    }

    return true;
  }

  /**
   * Creates an arc from 0 to pi/2
   */
  private boolean clockwiseArc(double x, double y, double r,
                               double aDeg, double bDeg)
  {
    double a = aDeg * Math.PI / 180.0;
    double b = bDeg * Math.PI / 180.0;

    double cos_a = Math.cos(a);
    double sin_a = Math.sin(a);

    double x1 = x + r * cos_a;
    double y1 = y + r * sin_a;

    double cos_b = Math.cos(b);
    double sin_b = Math.sin(b);

    double x2 = x + r * cos_b;
    double y2 = y + r * sin_b;

    double l = KAPPA * r * 2 * (b - a) / Math.PI;

    lineto(x1, y1);
    curveto(x1 - l * sin_a, y1 + l * cos_a,
            x2 + l * sin_b, y2 - l * cos_b,
            x2, y2);

    return true;
  }

  /**
   * Creates an arc from 0 to pi/2
   */
  private boolean counterClockwiseArc(double x, double y, double r,
                                      double aDeg, double bDeg)
  {
    double a = aDeg * Math.PI / 180.0;
    double b = bDeg * Math.PI / 180.0;

    double cos_a = Math.cos(a);
    double sin_a = Math.sin(a);

    double x1 = x + r * cos_a;
    double y1 = y + r * sin_a;

    double cos_b = Math.cos(b);
    double sin_b = Math.sin(b);

    double x2 = x + r * cos_b;
    double y2 = y + r * sin_b;

    double l = KAPPA * r * 2 * (a - b) / Math.PI;

    lineto(x1, y1);
    curveto(x1 + l * sin_a, y1 - l * cos_a,
            x2 - l * sin_b, y2 + l * cos_b,
            x2, y2);

    return true;
  }

  /**
   * Creates a circle
   */
  public boolean circle(double x1, double y1, double r)
  {
    double l = r * KAPPA;

    moveto(x1, y1 + r);

    curveto(x1 - l, y1 + r, x1 - r, y1 + l, x1 - r, y1);

    curveto(x1 - r, y1 - l, x1 - l, y1 - r, x1, y1 - r);

    curveto(x1 + l, y1 - r, x1 + r, y1 - l, x1 + r, y1);

    curveto(x1 + r, y1 + l, x1 + l, y1 + r, x1, y1 + r);

    return true;
  }

  /**
   * Sets the graphics position.
   */
  public boolean lineto(double x, double y)
  {
    _stream.lineTo(x, y);

    return true;
  }

  /**
   * Sets the graphics position.
   */
  public boolean moveto(double x, double y)
  {
    _stream.moveTo(x, y);

    return true;
  }

  /**
   * Creates a rectangle
   */
  public boolean rect(double x, double y, double width, double height)
  {
    _stream.rect(x, y, width, height);

    return true;
  }

  /**
   * Sets the color to a grayscale
   */
  public boolean setgray_stroke(double g)
  {
    return _stream.setcolor("stroke", "gray", g, 0, 0, 0);
  }

  /**
   * Sets the color to a grayscale
   */
  public boolean setgray_fill(double g)
  {
    return _stream.setcolor("fill", "gray", g, 0, 0, 0);
  }

  /**
   * Sets the color to a grayscale
   */
  public boolean setgray(double g)
  {
    return _stream.setcolor("both", "gray", g, 0, 0, 0);
  }

  /**
   * Sets the color to a rgb
   */
  public boolean setrgbcolor_stroke(double r, double g, double b)
  {
    return _stream.setcolor("stroke", "rgb", r, g, b, 0);
  }

  /**
   * Sets the fill color to a rgb
   */
  public boolean setrgbcolor_fill(double r, double g, double b)
  {
    return _stream.setcolor("fill", "rgb", r, g, b, 0);
  }

  /**
   * Sets the color to a rgb
   */
  public boolean setrgbcolor(double r, double g, double b)
  {
    return _stream.setcolor("both", "rgb", r, g, b, 0);
  }

  /**
   * Sets the color
   */
  public boolean setcolor(String fstype, String colorspace,
                          double c1,
                          @Optional double c2,
                          @Optional double c3,
                          @Optional double c4)
  {
    return _stream.setcolor(fstype, colorspace, c1, c2, c3, c4);
  }

  /**
   * Sets the line width
   */
  public boolean setlinewidth(double w)
  {
    return _stream.setlinewidth(w);
  }

  /**
   * Concatenates the matrix
   */
  public boolean concat(double a, double b, double c,
                        double d, double e, double f)
  {
    return _stream.concat(a, b, c, d, e, f);
  }

  /**
   * open image
   */
  public PDFImage open_image_file(String type, Path file,
                                  @Optional String stringParam,
                                  @Optional int intParam)
    throws IOException
  {
    PDFImage img = new PDFImage(file);

    img.setId(_out.allocateId(1));

    _out.addPendingObject(img);

    return img;
  }

  /**
   * open image
   */
  public PDFImage load_image(String type,
                             Path file,
                             @Optional String optlist)
    throws IOException
  {
    PDFImage img = new PDFImage(file);

    img.setId(_out.allocateId(1));

    _out.addPendingObject(img);

    return img;
  }

  public boolean fit_image(PDFImage img, double x, double y,
                           @Optional String opt)
  {
    _page.addResource(img.getResource());

    _stream.save();

    concat(img.get_width(), 0, 0, img.get_height(), x, y);

    _stream.fit_image(img);

    _stream.restore();

    return true;
  }

  /**
   * Skews the coordinates
   *
   * @param a degrees to skew the x axis
   * @param b degrees to skew the y axis
   */
  public boolean skew(double aDeg, double bDeg)
  {
    double a = aDeg * Math.PI / 180;
    double b = bDeg * Math.PI / 180;

    return _stream.concat(1, Math.tan(a), Math.tan(b), 1, 0, 0);
  }

  /**
   * scales the coordinates
   *
   * @param sx amount to scale the x axis
   * @param sy amount to scale the y axis
   */
  public boolean scale(double sx, double sy)
  {
    return _stream.concat(sx, 0, 0, sy, 0, 0);
  }

  /**
   * translates the coordinates
   *
   * @param tx amount to translate the x axis
   * @param ty amount to translate the y axis
   */
  public boolean translate(double tx, double ty)
  {
    return _stream.concat(1, 0, 0, 1, tx, ty);
  }

  /**
   * rotates the coordinates
   *
   * @param p amount to rotate
   */
  public boolean rotate(double pDeg)
  {
    double p = pDeg * Math.PI / 180;

    return _stream.concat(Math.cos(p), Math.sin(p),
                          -Math.sin(p), Math.cos(p),
                          0, 0);
  }

  /**
   * Saves the graphics state.
   */
  public boolean save()
  {
    return _stream.save();
  }

  /**
   * Restores the graphics state.
   */
  public boolean restore()
  {
    return _stream.restore();
  }

  /**
   * Displays text
   */
  public boolean show(String text)
  {
    _stream.show(text);

    return true;
  }

  /**
   * Displays text
   */
  public boolean show_boxed(String text, double x, double y,
                            double width, double height,
                            String mode, @Optional String feature)
  {
    set_text_pos(x, y);
    _stream.show(text);

    return true;
  }

  /**
   * Displays text
   */
  public boolean show_xy(String text, double x, double y)
  {
    set_text_pos(x, y);
    _stream.show(text);

    return true;
  }

  /**
   * Draws the graph
   */
  public boolean stroke()
  {
    _stream.stroke();

    return true;
  }

  /**
   * Displays text
   */
  public boolean continue_text(String text)
  {
    _stream.continue_text(text);

    return true;
  }

  public boolean end_page()
  {
    _stream.flush();

    PDFProcSet procSet = _stream.getProcSet();

    _page.addResource(procSet.getResource());

    _page = null;
    _stream = null;

    return true;
  }

  public boolean end_page_ext(String optlist)
  {
    return end_page();
  }

  public boolean end_document(@Optional String optList)
    throws IOException
  {
    if (_pageGroup.size() > 0) {
      _out.writePageGroup(_pageParentId, _pageGroup);
      _pageGroup.clear();

      if (_pagesGroupList.size() > 0)
        _pagesGroupList.add(_pageParentId);
    }

    _out.writeCatalog(_catalogId, _pageParentId, _pagesGroupList, _pageCount);

    _out.endDocument();

    _os.close();
    _out = null;

    return true;
  }

  public boolean close()
    throws IOException
  {
    return end_document("");
  }

  public boolean delete()
    throws IOException
  {
    return true;
  }

  public String toString()
  {
    return "PDF[]";
  }
}
