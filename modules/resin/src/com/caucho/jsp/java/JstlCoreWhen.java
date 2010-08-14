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

package com.caucho.jsp.java;

import com.caucho.jsp.JspParseException;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;

/**
 * Special generator for a JSTL c:when tag.
 */
public class JstlCoreWhen extends JstlNode {
  private static final QName TEST = new QName("test");
  
  private String _test;
  private JspAttribute _testAttr;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (TEST.equals(name))
      _test = value;
    else
      throw error(L.l("'{0}' is an unknown attribute for <{1}>.",
                      name.getName(), getTagName()));
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, JspAttribute value)
    throws JspParseException
  {
    if (TEST.equals(name))
      _testAttr = value;
    else
      throw error(L.l("'{0}' is an unknown jsp:attribute for <{1}>.",
                      name.getName(), getTagName()));
  }
  
  /**
   * Called after all the attributes from the tag.
   */
  @Override
  public void endAttributes()
    throws JspParseException
  {
  }
  
  /**
   * Called after the element from the tag.
   */
  @Override
  public void endElement()
    throws JspParseException
  {
    if (! (getParent() instanceof JstlCoreChoose)) {
      throw error(L.l("c:when must be contained in a c:choose tag."));
    }
    
    if (_test == null && _testAttr == null)
      throw error(L.l("'test' attribute missing from c:when."));
  }

  /**
   * Returns true if the tag has scripting values.
   */
  public boolean hasScripting()
  {
    return (super.hasScripting()
            || hasScripting(_test) || hasScripting(_testAttr));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<c:when test=\"");
    printXmlText(os, _test);
    os.print("\">");

    printXmlChildren(os);

    os.print("</c:when>");
  }
  
  /**
   * Generates the code for the c:if tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_test == null && _testAttr == null)
      throw error(L.l("required attribute 'test' missing from <{0}>",
                      getTagName()));

    String ifExpr;

    if (_testAttr != null)
      ifExpr = _testAttr.generateValue(boolean.class);
    else
      ifExpr = generateJstlValue(boolean.class, _test);

    out.println("if (" + ifExpr + ") {");
    out.pushDepth();
    
    generateChildren(out);

    out.popDepth();
    out.println("}");
  }
}
