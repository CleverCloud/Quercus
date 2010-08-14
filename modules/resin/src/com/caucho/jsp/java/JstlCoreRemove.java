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

public class JstlCoreRemove extends JstlNode {
  private static final QName VAR = new QName("var"); 
  private static final QName SCOPE = new QName("scope");
  
  private String _var;
  private String _scope;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (VAR.equals(name))
      _var = value;
    else if (SCOPE.equals(name))
      _scope = value;
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
    os.print("<c:remove var=\"" + xmlText(_var) + "\"");

    if (_scope != null)
      os.print(" scope=\"" + xmlText(_scope) + "\"");

    os.print("/>");
  }

  /**
   * Generates the code for the c:remove tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_var == null)
      throw error(L.l("required attribute `var' missing from `{0}'",
                      getTagName()));

    if (_scope == null || _scope.equals("page")) {
      out.println("pageContext.removeAttribute(\"" + _var + "\");");
    }
    else if (_scope.equals("request")) {
      out.println("pageContext.getRequest().removeAttribute(\"" + _var + "\");");
    }
    else if (_scope.equals("session")) {
      out.println("pageContext.getSession().removeAttribute(\"" + _var + "\");");
    }
    else if (_scope.equals("application")) {
      out.println("pageContext.getServletContext().removeAttribute(\"" + _var + "\");");
    }
    else
      throw error(L.l("invalid scope `{0}'", _scope));
  }
}
