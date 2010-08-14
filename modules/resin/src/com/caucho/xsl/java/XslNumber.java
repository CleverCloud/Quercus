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
import com.caucho.xpath.Expr;
import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xsl.XslNumberFormat;
import com.caucho.xsl.XslParseException;

/**
 * Returns the value of an expression.
 */
public class XslNumber extends XslNode {
  private String _value;
  private String _count;
  private String _from;
  private String _level = "single";
  private String _format;
  private String _letter;
  private String _lang;
  private String _groupingSeparator;
  private String _groupingSize = "";

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:number";
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if ("value".equals(name.getName())) {
      _value = value;
    }
    else if ("count".equals(name.getName())) {
      _count = value;
    }
    else if ("from".equals(name.getName())) {
      _from = value;
    }
    else if ("level".equals(name.getName())) {
      _level = value;
    }
    else if ("format".equals(name.getName())) {
      _format = value;
    }
    else if ("letter-value".equals(name.getName())) {
      _letter = value;
    }
    else if ("lang".equals(name.getName())) {
      _lang = value;
    }
    else if ("grouping-separator".equals(name.getName())) {
      _groupingSeparator = value;
    }
    else if ("grouping-size".equals(name.getName())) {
      _groupingSize = value;
    }
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
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    int size = 0;

    for (int i = 0; i < _groupingSize.length(); i++) {
        char ch = _groupingSize.charAt(i);
        if (ch >= '0' && ch <= '9')
            size = 10 * size + ch - '0';
    }

    boolean isAlphabetic = true;
    if (! "alphabetic".equals(_letter))
      isAlphabetic = false;

    AbstractPattern countPattern = null;
    if (_count != null)
      countPattern = parseMatch(_count);

    AbstractPattern fromPattern = null;
    if (_from != null)
      fromPattern = parseMatch(_from);

    if (_level == null || _level.equals("single")) {
      _level = "single";
    }
    else if (_level.equals("multiple")) {
    }
    else if (_level.equals("any")) {
    }
    else
      throw error(L.l("xsl:number can't understand level=`{0}'",
                      _level));

    XslNumberFormat xslFormat;
    xslFormat = new XslNumberFormat(_format, _lang, isAlphabetic,
                                    _groupingSeparator, size);

    if (_value != null)
      printNumber(out, parseExpr(_value), xslFormat);
    else
      printNumber(out, _level, countPattern, fromPattern, xslFormat);
  }

  void printNumber(JavaWriter out, Expr expr, XslNumberFormat format)
    throws Exception
  {
    int index = _gen.addExpr(expr);
    
    out.print("exprNumber(out, node, env, _exprs[" + index + "]");
    out.print(", _xsl_formats[" + _gen.addFormat(format) + "]");
    out.println(");");
  }

  void printNumber(JavaWriter out, String level,
                   AbstractPattern countPattern,
                   AbstractPattern fromPattern,
                   XslNumberFormat format)
    throws Exception
  {
    if (level.equals("single"))
      out.print("singleNumber(out, ");
    else if (level.equals("multiple"))
      out.print("multiNumber(out, ");
    else if (level.equals("any"))
      out.print("anyNumber(out, ");
    else
      throw error(L.l("xsl:number cannot understand level='{0}'",
                      level));

    out.print("node, env, ");
    printPattern(out, countPattern);
    out.print(", ");
    printPattern(out, fromPattern);
    out.print(", _xsl_formats[" + _gen.addFormat(format) + "]");
    out.println(");");
  }

  void printPattern(JavaWriter out, AbstractPattern pattern)
    throws Exception
  {
    if (pattern == null)
      out.print("null");
    else {
      out.print("_match_patterns[" + _gen.addMatch(pattern) + "]");
    }
  }
}
