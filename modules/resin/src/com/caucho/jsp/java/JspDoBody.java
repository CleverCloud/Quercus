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
import com.caucho.jsp.cfg.TldVariable;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a custom tag.
 */
public class JspDoBody extends JspNode {
  private static final QName VAR = new QName("var");
  private static final QName VAR_READER = new QName("varReader");
  private static final QName SCOPE = new QName("scope");
  
  private String _var;
  private String _varReader;
  private String _scope;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (VAR.equals(name))
      _var = value;
    else if (VAR_READER.equals(name))
      _varReader = value;
    else if (SCOPE.equals(name))
      _scope = value;
    else
      throw error(L.l("'{0}' is an unknown attribute for jsp:doBody.",
                      name.getName()));
  }

  /**
   * Called when the attributes end.
   */
  public void endAttributes()
    throws JspParseException
  {
    if (! _gen.getParseState().isTag())
      throw error(L.l("'{0}' is only allowed in .tag files.  Attribute directives are not allowed in normal JSP files.",
                      getTagName()));

    if (_var != null && _varReader != null)
      throw error(L.l("'var' and 'varReader' cannot both be set in jsp:doBody."));

    if (_scope != null && _var == null && _varReader == null)
      throw error(L.l("jsp:doBody requires a 'var' or 'varReader' if 'scope' is set."));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:do-body");
    os.print(" jsp:id=\"" + _gen.generateJspId() + "\"");
    if (_var != null)
      os.print(" var=\"" + _var + "\"");
    os.print(">");
    
    os.print("</jsp:do-body>");
  }

  /**
   * Generates the children.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String name = "_jsp_frag_" + _gen.uniqueId();

    if (_gen.hasScripting())
      out.println("javax.servlet.jsp.tagext.JspFragment " + name + " = getJspBody();");
    else
      out.println("javax.servlet.jsp.tagext.JspFragment " + name + " = _jspBody;");

    out.println("if (" + name + " != null) {");
    out.pushDepth();
    
    JavaTagGenerator gen = (JavaTagGenerator) _gen;
    ArrayList<TldVariable> vars = gen.getVariables();

    for (int i = 0; i < vars.size(); i++) {
      TldVariable var = vars.get(i);

      if (var.getScope().equals("AT_END"))
        continue;

      String srcName = var.getNameGiven();
      String dstName = srcName;
      
      if (srcName == null) {
        srcName = var.getAlias();
        dstName = var.getNameFromAttribute();
        dstName = "_jsp_var_from_attribute_" + i;
      }
      else
        dstName = "\"" + dstName + "\"";

      out.print("_jsp_parentContext.setAttribute(" + dstName + ", ");
      out.println("pageContext.getAttribute(\"" + srcName + "\"));");
    }

    /*
    if (vars.size() > 0) {
      out.println("try {");
      out.pushDepth();
    }
    */

    if (_var != null) {
      out.print(getScope() + ".setAttribute(\"" + _var + "\", ");
      out.println("pageContext.invoke(" + name + "));");
    }
    else if (_varReader != null) {
      out.print(getScope() + ".setAttribute(\"" + _varReader + "\", ");
      out.println("pageContext.invokeReader(" + name + "));");
    }
    else {
      out.println(name + ".invoke(null);");
    }
    
    out.popDepth();
    out.println("}");
  }

  private String getScope()
    throws JspParseException
  {
    String context = null;
    if (_scope == null || _scope.equals("page"))
      return "pageContext";
    else if (_scope.equals("request")) {
      return "pageContext.getRequest()";
    }
    else if (_scope.equals("session")) {
      return "pageContext.getSession()";
    }
    else if (_scope.equals("application")) {
      return "pageContext.getApplication()";
    }
    else
      throw error(L.l("Unknown scope '{0}' in <jsp:doBody>.  Scope must be 'page', 'request', 'session', or 'application'.", _scope));
  }
}
  
