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
import com.caucho.jsp.cfg.TldVariable;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;
import java.util.ArrayList;

/**
 * jsp:invoke invokes a fragment
 */
public class JspInvoke extends JspNode {
  private static final QName FRAGMENT = new QName("fragment");
  private static final QName VAR = new QName("var");
  private static final QName SCOPE = new QName("scope");
  private static final QName VAR_READER = new QName("varReader");
  
  private String _name;
  private String _var;
  private String _varReader;
  private String _scope;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (FRAGMENT.equals(name))
      _name = value;
    else if (VAR.equals(name))
      _var = value;
    else if (VAR_READER.equals(name))
      _varReader = value;
    else if (SCOPE.equals(name))
      _scope = value;
    else
      throw error(L.l("`{0}' is an unknown attribute for jsp:invoke.",
                      name.getName()));
  }

  /**
   * Called when the attributes end.
   */
  public void endAttributes()
    throws JspParseException
  {
    if (! _gen.getParseState().isTag())
      throw error(L.l("`{0}' is only allowed in .tag files.  Attribute directives are not allowed in normal JSP files.",
                      getTagName()));
  }

  /**
   * Called when the body ends.
   */
  public void endElement()
    throws JspParseException
  {
    if (_name == null)
      throw error(L.l("'fragment' is a required attribute of <jsp:invoke>."));

    if (_scope != null && _var == null && _varReader == null)
      throw error(L.l("'scope' requires a 'var' or a 'varReader' attribute for <jsp:invoke>."));

    if (_var != null && _varReader != null)
      throw error(L.l("jsp:invoke may not have both 'var' and 'varReader'"));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:invoke");

    if (_name != null)
      os.print(" name=\"" + _name + "\"");
    
    if (_var != null)
      os.print(" var=\"" + _var + "\"");
    
    if (_varReader != null)
      os.print(" varReader=\"" + _varReader + "\"");
    
    if (_scope != null)
      os.print(" scope=\"" + _scope + "\"");

    os.print("/>");
  }

  /**
   * Generates the children.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String name = "_jsp_frag_" + _gen.uniqueId();
    
    out.println("javax.servlet.jsp.tagext.JspFragment " + name + " = (javax.servlet.jsp.tagext.JspFragment) pageContext.getAttribute(\"" + _name + "\");");

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
    
    String context = null;
    if (_scope == null || _scope.equals("page"))
      context = "pageContext";
    else if (_scope.equals("request"))
      context = "pageContext.getRequest()";
    else if (_scope.equals("session"))
      context = "pageContext.getSessionScope()";
    else if (_scope.equals("application"))
      context = "pageContext.getApplication()";
    else
      throw error(L.l("Unknown scope `{0}' in <jsp:invoke>.  Scope must be `page', `request', `session', or `application'.", _scope));


    if (_var != null) {
      out.print(context + ".setAttribute(\"" + _var + "\", ");
      out.println("pageContext.invoke(" + name + "));");
    }
    else if (_varReader != null) {
      out.print(context + ".setAttribute(\"" + _varReader + "\", ");
      out.println("pageContext.invokeReader(" + name + "));");
    }
    else {
      out.println(name + ".invoke(null);");
    }
    
    out.popDepth();
    out.println("}");
  }
}
