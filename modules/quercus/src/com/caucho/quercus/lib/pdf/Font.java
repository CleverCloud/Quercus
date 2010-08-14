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

/**
 * font
 */
public class Font {
  private String _fontName;
  private String _weight;

  private double _llxBBox;
  private double _llyBBox;
  private double _urxBBox;
  private double _uryBBox;

  private double _capHeight;
  private double _xHeight;
  private double _ascender;
  private double _descender;

  private double _underlinePosition;
  private double _underlineThickness;
  private double _italicAngle;

  private FontChar []_chars = new FontChar[256];

  public String getFontName()
  {
    return _fontName;
  }

  void setFontName(String name)
  {
    _fontName = name;
  }

  public String getWeight()
  {
    return _weight;
  }

  void setWeight(String weight)
  {
    _weight = weight;
  }

  void setBBox(double llx, double lly, double urx, double ury)
  {
    _llxBBox = llx;
    _llyBBox = lly;
    _urxBBox = urx;
    _uryBBox = ury;
  }

  public double getCapHeight()
  {
    return _capHeight;
  }

  void setCapHeight(double height)
  {
    _capHeight = height;
  }

  public double getXHeight()
  {
    return _xHeight;
  }

  void setXHeight(double height)
  {
    _xHeight = height;
  }

  public double getAscender()
  {
    return _ascender;
  }

  void setAscender(double ascender)
  {
    _ascender = ascender;
  }

  public double getDescender()
  {
    return _descender;
  }

  void setDescender(double descender)
  {
    _descender = descender;
  }

  public double getUnderlinePosition()
  {
    return _underlinePosition;
  }

  void setUnderlinePosition(double underlinePosition)
  {
    _underlinePosition = underlinePosition;
  }

  public double getUnderlineThickness()
  {
    return _underlineThickness;
  }

  void setUnderlineThickness(double underlineThickness)
  {
    _underlineThickness = underlineThickness;
  }

  public double getItalicAngle()
  {
    return _italicAngle;
  }

  void setItalicAngle(double italicAngle)
  {
    _italicAngle = italicAngle;
  }

  void addChar(FontChar fontChar)
  {
    int code = fontChar.getCode();

    if (code >= 0 && code < _chars.length)
      _chars[code] = fontChar;
  }

  public double stringWidth(String text)
  {
    double width = 0;

    int len = text.length();
    char prevChar = 0;

    for (int i = 0; i < len; i++) {
      char ch = text.charAt(i);

      if (ch >= 256)
        continue;

      FontChar fontChar = _chars[ch];

      if (fontChar == null)
        continue;

      width += fontChar.getWidth();

      // XXX: kerning

      prevChar = ch;
    }

    return width;
  }
}
