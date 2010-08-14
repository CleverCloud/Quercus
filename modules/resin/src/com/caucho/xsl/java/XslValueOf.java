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

/**
 * Returns the value of an expression.
 */
public class XslValueOf extends XslNode {
  private String _select;
  private String _separator;
  private String _disableOutputEscaping;

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:value-of";
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("select"))
      _select = value;
    else if (name.getName().equals("disable-output-escaping"))
      _disableOutputEscaping = value;
    else if (name.getName().equals("separator"))
      _separator = value;
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
      throw error(L.l("'select' is a required attribute of <xsl:value-of>"));
  }
  
  /**
   * Adds a child.
   */
  public void addChild(XslNode node)
    throws XslParseException
  {
    throw error(L.l("element <{0}> is not allowed in <{1}>.",
                    node.getTagName(), getTagName()));
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    String var = null;
    
    if ("yes".equals(_disableOutputEscaping)) {
      var = "_xsl_out_" + _gen.generateId();
      out.println("boolean " + var + " = out.disableEscaping(true);");
    }
    else if ("no".equals(_disableOutputEscaping)) {
      var = "_xsl_out_" + _gen.generateId();
      out.println("boolean " + var + " = out.disableEscaping(false);");
    }
    else if (_disableOutputEscaping != null)
      throw error(L.l("'{0}' is an unknown value for disable-output-escaping.  'yes' and 'no' are the valid values.",
                      _disableOutputEscaping));
      
    printStringExpr(out, _select);

    if (var != null)
      out.println("out.disableEscaping(" + var + ");");
  }
}
