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

public class JstlXmlIf extends JstlNode {
  private static final QName SELECT = new QName("select");
  private static final QName VAR = new QName("var");
  private static final QName SCOPE = new QName("scope");
  
  private String _select;
  private String _var;
  private String _scope;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (SELECT.equals(name))
      _select = value;
    else if (VAR.equals(name))
      _var = value;
    else if (SCOPE.equals(name))
      _scope = value;
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
    if (false)
      throw error(L.l("'{0}' is an unsupported jsp:attribute for <{1}>.",
                      name.getName(), getTagName()));
  }
  
  /**
   * Called after all the attributes from the tag.
   */
  @Override
  public void endAttributes()
    throws JspParseException
  {
    if (_scope != null && ! "".equals(_scope) && _var == null)
      throw error(L.l("x:if requires var attribute to be set if scope is set"));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<x:if");

    if (_select != null) {
      os.print(" select=\"");
      printXmlText(os, _select);
      os.print("\"");
    }
    
    if (_var != null)
      os.print(" var=\"" + _var + "\"");
    
    if (_scope != null)
      os.print(" scope=\"" + _scope + "\"");

    os.print(">");

    printXmlChildren(os);

    os.print("</x:if>");
  }
  
  /**
   * Generates the code for the c:set tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_select == null)
      throw error(L.l("required attribute 'select' missing from <{0}>",
                      getTagName()));

    String select = ("com.caucho.jstl.el.XmlIfTag.evalBoolean(pageContext, " +
                     _gen.addXPathExpr(_select, getNamespaceContext()) + ")");


    out.println("if (" + select + ") {");
    out.pushDepth();

    if (_var != null) {
      generateSetOrRemove(out, _var, _scope, "Boolean.TRUE");
    }

    generateChildren(out);

    out.popDepth();
    out.println("}");

    if (_var != null) {
      out.println("else {");
      out.pushDepth();
      
      generateSetOrRemove(out, _var, _scope, "Boolean.FALSE");
      
      out.popDepth();
      out.println("}");
    }
  }
}
