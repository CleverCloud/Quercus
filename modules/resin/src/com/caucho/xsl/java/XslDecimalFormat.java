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

package com.caucho.xsl.java;

import com.caucho.java.JavaWriter;
import com.caucho.xml.QName;
import com.caucho.xsl.XslParseException;

import java.text.DecimalFormatSymbols;

/**
 * xsl:decimal-format
 */
public class XslDecimalFormat extends XslNode implements XslTopNode {
  private String _name;
  private String _decimalSeparator;
  private String _groupingSeparator;
  private String _infinity;
  private String _minusSign;
  private String _nan;
  private String _percent;
  private String _perMille;
  private String _zeroDigit;
  private String _digit;
  private String _patternSeparator;
  
  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:decimal-format";
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("name"))
      _name = value;
    else if (name.getName().equals("decimal-separator"))
      _decimalSeparator = value;
    else if (name.getName().equals("grouping-separator"))
      _groupingSeparator = value;
    else if (name.getName().equals("infinity"))
      _infinity = value;
    else if (name.getName().equals("minus-sign"))
      _minusSign = value;
    else if (name.getName().equals("NaN"))
      _nan = value;
    else if (name.getName().equals("percent"))
      _percent = value;
    else if (name.getName().equals("per-mille"))
      _perMille = value;
    else if (name.getName().equals("zero-digit"))
      _zeroDigit = value;
    else if (name.getName().equals("digit"))
      _digit = value;
    else if (name.getName().equals("pattern-separator"))
      _patternSeparator = value;
    else
      super.addAttribute(name, value);
  }

  /**
   * Ends the attributes.
   */
  public void endAttributes()
    throws XslParseException
  {
  }

  /**
   * Called when the element ends.
   */
  public void endElement()
    throws Exception
  {
    String name = _name;
    
    if (name == null)
      name = "*";

    DecimalFormatSymbols format = new DecimalFormatSymbols();

    if (_decimalSeparator != null && _decimalSeparator.length() > 0) 
      format.setDecimalSeparator(_decimalSeparator.charAt(0));

    if (_groupingSeparator != null && _groupingSeparator.length() > 0) 
      format.setGroupingSeparator(_groupingSeparator.charAt(0));

    if (_infinity != null)
      format.setInfinity(_infinity);

    if (_minusSign != null && _minusSign.length() > 0) 
      format.setMinusSign(_minusSign.charAt(0));

    if (_nan != null)
      format.setNaN(_nan);

    if (_percent != null && _percent.length() > 0) 
      format.setPercent(_percent.charAt(0));

    if (_perMille != null && _perMille.length() > 0) 
      format.setPerMill(_perMille.charAt(0));

    if (_zeroDigit != null && _zeroDigit.length() > 0) 
      format.setZeroDigit(_zeroDigit.charAt(0));

    if (_digit != null && _digit.length() > 0) 
      format.setDigit(_digit.charAt(0));

    if (_patternSeparator != null && _patternSeparator.length() > 0) 
      format.setPatternSeparator(_patternSeparator.charAt(0));

    _gen.addLocale(name, format);
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
  }
}
