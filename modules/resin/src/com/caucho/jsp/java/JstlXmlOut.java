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

package com.caucho.jsp.java;

import com.caucho.jsp.JspParseException;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;

public class JstlXmlOut extends JstlNode {
  private static final QName SELECT = new QName("select");
  private static final QName ESCAPE_XML = new QName("escapeXml");
  
  private String _select;
  
  private String _escapeXml = "true";
  private JspAttribute _escapeXmlAttr;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (SELECT.equals(name))
      _select = value;
    else if (ESCAPE_XML.equals(name))
      _escapeXml = value;
    else
      throw error(L.l("`{0}' is an unknown attribute for <{1}>.",
                      name.getName(), getTagName()));
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, JspAttribute value)
    throws JspParseException
  {
    if (ESCAPE_XML.equals(name))
      _escapeXmlAttr = value;
    else
      throw error(L.l("`{0}' is an unknown attribute for <{1}>.",
                      name.getName(), getTagName()));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<x:out");

    if (_select != null) {
      os.print(" select=\"");
      printXmlText(os, _select);
      os.print("\"");
    }
    os.print("/>");
  }
  
  /**
   * Generates the code for the c:out tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_select == null)
      throw error(L.l("required attribute `select' missing from <{0}>",
                      getTagName()));

    String select = _gen.addXPathExpr(_select, getNamespaceContext());
      
    String escapeXml = "true";

    if (_escapeXml != null)
      escapeXml = generateValue(boolean.class, _escapeXml);
    else if (_escapeXmlAttr != null)
      escapeXml = _escapeXmlAttr.generateValue(String.class);

    out.println("com.caucho.jstl.el.XmlOutTag.toStream(out, pageContext, " + select + ", " + escapeXml + ");");
  }
}
