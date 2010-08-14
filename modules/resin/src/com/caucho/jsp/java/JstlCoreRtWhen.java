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

/**
 * Special generator for a JSTL c:when tag.
 */
public class JstlCoreRtWhen extends JstlNode {
  private static final QName TEST = new QName("test");
  private Object _test;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (TEST.equals(name))
      _test = value;
    else
      throw error(L.l("`{0}' is an unknown attribute for <{1}>.",
                      name.getName(), getTagName()));
  }
  
  /**
   * Adds a fragment attribute.
   */
  public void addAttribute(String name, JspAttribute value)
    throws JspParseException
  {
    if (name.equals("test"))
      _test = value;
    else
      throw error(L.l("`{0}' can is an illegal jsp:attribute for <{1}>.",
                      name, getTagName()));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<c-rt:when");

    if (_test instanceof String) {
      os.print(" test=\"");
      printXmlText(os, (String) _test);
      os.print("\">");
    }
    else {
      os.print(">");
    }

    printXmlChildren(os);

    os.print("</c-rt:when>");
  }
  
  /**
   * Generates the code for the c:if tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_test == null)
      throw error(L.l("required attribute `test' missing from <{0}>",
                      getTagName()));

    String ifExpr = generateRTValue(boolean.class, _test);

    out.println("if (" + ifExpr + ") {");
    out.pushDepth();
    generateChildren(out);

    out.popDepth();
    out.println("}");
  }
}
