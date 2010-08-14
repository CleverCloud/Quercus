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
import com.caucho.xml.QName;

abstract public class JstlNode extends JspContainerNode {
  /**
   * True if this is a jstl node.
   */
  public boolean isJstl()
  {
    return true;
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    throw error(L.l("`{0}' is an unknown attribute for <{1}>.",
                    name.getName(), getTagName()));
  }
  
  /**
   * Generates the code to set a non-null value.
   *
   * @param out the writer to the *.java file
   * @param var the EL name
   * @param scope the scope name
   * @param value the value
   */
  protected void generateSetNotNull(JspJavaWriter out, String var,
                                    String scope, String value)
    throws Exception
  {
    if (var == null) {
    }
    else if (scope == null || scope.equals("page")) {
      out.println("pageContext.setAttribute(\"" + var + "\", " + value + ");");
    }
    else if (scope.equals("request")) {
      out.println("pageContext.getRequest().setAttribute(\"" + var + "\", " + value + ");");
    }
    else if (scope.equals("session")) {
      out.println("pageContext.getSession().setAttribute(\"" + var + "\", " + value + ");");
    }
    else if (scope.equals("application")) {
      out.println("pageContext.getServletContext().setAttribute(\"" + var + "\", " + value + ");");
    }
    else
      throw error(L.l("invalid scope `{0}'", scope));
  }

  protected void generateSetOrRemove(JspJavaWriter out,
                                     String var, String scope,
                                     String value)
    throws Exception
  {
    if (var == null) {
    }
    else if (scope == null) {
      out.println("pageContext.defaultSetOrRemove(\"" + var + "\", " + value + ");");
    }
    else if (scope.equals("page")) {
      out.println("pageContext.pageSetOrRemove(\"" + var + "\", " + value + ");");
    }
    else if (scope.equals("request")) {
      out.println("pageContext.requestSetOrRemove(\"" + var + "\", " + value + ");");
    }
    else if (scope.equals("session")) {
      out.println("pageContext.sessionSetOrRemove(\"" + var + "\", " + value + ");");
    }
    else if (scope.equals("application")) {
      out.println("pageContext.applicationSetOrRemove(\"" + var + "\", " + value + ");");
    }
    else
      throw error(L.l("invalid scope '{0}'", scope));
  }
}
