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
import com.caucho.xsl.Sort;
import com.caucho.xsl.XslParseException;

/**
 * Represents the xsl:sort element.
 */
public class XslSort extends XslNode {
  private String _select;
  private String _lang;
  private String _order;
  private String _caseOrder;
  private String _dataType;

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:sort";
  }
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("select")) {
      _select = value;
    }
    else if (name.getName().equals("case-order")) {
      if (value.equals("upper-first") ||
          value.equals("lower-first"))
        _caseOrder = value;
      else
        throw error(L.l("'{0}' is not a valid case-order for xsl:sort.",
                        value));
    }
    else if (name.getName().equals("order")) {
      _order = value;
    }
    else if (name.getName().equals("data-type")) {
      _dataType = value;
    }
    else if (name.getName().equals("xsl:lang") ||
             name.getName().equals("lang")) {
      _lang = value;
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
    if (_select == null)
      throw error(L.l("<xsl:sort> requires a 'select' attribute."));
  }

  /**
   * Generates the sort value.
   */
  public Sort generateSort()
    throws Exception
  {
    Expr expr = parseExpr(_select);

    Expr isAscending = constructBoolean(_order, "ascending");
    Expr caseOrder = constructBoolean(_caseOrder, "upper-first");

    if (_caseOrder == null)
      caseOrder = null;
      
    boolean isText = ! "number".equals(_dataType);

    Sort sort;
    
    if (_lang == null) {
      sort = Sort.create(expr, isAscending, isText);
    }
    else {
      String lang = _lang;
      
      if (lang.startsWith("{") && lang.endsWith("}"))
        lang = lang.substring(1, lang.length() - 1);
      else
        lang = "'" + lang + "'";

      sort = Sort.create(expr, isAscending, parseExpr(lang));
    }

    sort.setCaseOrder(caseOrder);

    return sort;
  }

  /**
   * Tests for a boolean EL value.
   */
  private Expr constructBoolean(String test, String match)
    throws XslParseException
  {
    if (test == null) {
      return parseExpr("true()");
    }
    else if (test.startsWith("{") && test.endsWith("}")) {
      test = test.substring(1, test.length() - 1);
        
      return parseExpr(test + " = '" + match + "'");
    }
    else if (test.equals(match))
      return parseExpr("true()");
    else
      return parseExpr("false()");
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    throw error(L.l("<xsl:sort> must be a child of <xsl:for-each> or <xsl:apply-templates>"));
  }
}
