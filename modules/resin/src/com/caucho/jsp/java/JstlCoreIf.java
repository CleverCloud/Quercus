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
 * Special generator for a JSTL c:if tag.
 */
public class JstlCoreIf extends JstlNode {
  private static final QName TEST = new QName("test");
  private static final QName VAR = new QName("var");
  private static final QName SCOPE = new QName("scope");
  
  private String _test;
  private String _var;
  private String _scope;
  
  private JspAttribute _testAttr;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (TEST.equals(name))
      _test = value;
    else if (VAR.equals(name))
      _var = value;
    else if (SCOPE.equals(name))
      _scope = value;
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
    if (TEST.equals(name)) {
      _testAttr = value;

      addAttributeChild(value);
    }
    else
      throw error(L.l("'{0}' is an unknown jsp:attribute for <{1}>.",
                      name.getName(), getTagName()));
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
    os.print("<c:if test=\"");
    if (_testAttr != null)
      _testAttr.printXml(os);
    else
      printXmlText(os, _test);
    os.print("\">");

    printXmlChildren(os);

    os.print("</c:if>");
  }
  
  /**
   * Generates the code for the c:if tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_test == null && _testAttr == null)
      throw error(L.l("required attribute `test' missing from <{0}>",
                      getTagName()));

    String ifExpr;

    if (_testAttr != null)
      ifExpr = _testAttr.generateValue(boolean.class);
    else
      ifExpr = generateJstlValue(boolean.class, _test);

    out.println("if (" + ifExpr + ") {");
    out.pushDepth();

    if (_var != null)
      generateSetNotNull(out, _var, _scope, "Boolean.TRUE");
    
    generateChildren(out);
    
    out.popDepth();
    out.println("}");

    if (_var != null) {
      out.println("else {");
      out.pushDepth();
      
      generateSetNotNull(out, _var, _scope, "Boolean.FALSE");
      
      out.popDepth();
      out.println("}");
    }
  }
}
