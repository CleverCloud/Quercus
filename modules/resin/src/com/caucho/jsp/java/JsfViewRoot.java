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

import com.caucho.jsp.*;
import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.xml.QName;
import com.caucho.vfs.*;

import javax.servlet.jsp.tagext.*;
import javax.faces.component.*;
import javax.faces.event.*;
import javax.el.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;

/**
 * Represents f:view
 */
public class JsfViewRoot extends JsfNode
{
  private JspNode _next;

  private ArrayList<Attr> _attrList = new ArrayList<Attr>();

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName qName, String value)
    throws JspParseException
  {
    String name = qName.getName();

    if (name.equals("afterPhase"))
      name = "afterPhaseListener";
    else if (name.equals("beforePhase"))
      name = "beforePhaseListener";

    String setterName = ("set" + Character.toUpperCase(name.charAt(0))
                         + name.substring(1));

    Method method = findSetter(UIViewRoot.class, setterName);

    if (method != null) {
      _attrList.add(new Attr(name, method, value));
    }
    else {
      super.addAttribute(qName, value);
    }
  }

  /**
   * Adds a JspAttribute attribute.
   *
   * @param name the name of the attribute.
   * @param value the value of the attribute.
   */
  public void addAttribute(QName qName, JspAttribute value)
    throws JspParseException
  {
    String name = qName.getName();

    if (name.equals("afterPhase"))
      name = "afterPhaseListener";
    else if (name.equals("beforePhase"))
      name = "beforePhaseListener";
    
    if (value.isStatic()) {
      addAttribute(qName, value.getStaticText().trim());
    }
    else {
      String setterName = ("set" + Character.toUpperCase(name.charAt(0))
                           + name.substring(1));

      Method method = findSetter(UIViewRoot.class, setterName);

      if (method != null) {
        _attrList.add(new Attr(name, method, value));
      }
      else {
        super.addAttribute(qName, value);
      }
    }
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    if (_next != null)
      _next.printXml(os);
  }

  /**
   * Returns the variable containing the jsf parent
   */
  @Override
  public String getJsfVar()
  {
    return _var;
  }

  /**
   * Returns the variable containing the jsf body
   */
  @Override
  public String getJsfBodyVar()
  {
    return _bodyVar;
  }
  
  /**
   * generates prologue data.
   */
  @Override
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    super.generatePrologue(out);
    
    _var = "_jsp_comp" + _gen.uniqueId();

    _bodyVar = "_jsp_body" + _gen.uniqueId();

    out.println("com.caucho.jsp.BodyContentImpl " + _bodyVar
                + " = (com.caucho.jsp.BodyContentImpl) pageContext.pushBody();");
    out.println("out = " + _bodyVar + ";");


    out.println("javax.faces.component.UIViewRoot " + _var + " = null;");
  }
  
  /**
   * Generates the code for a custom tag.
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String className = "javax.faces.component.UIViewRoot";

    long digest = calculateDigest();
    // XXX: eventually use pre-allocated long
    
    out.print(_var + " = ");
    out.println("com.caucho.jsp.jsf.JsfTagUtil.findRoot(_jsp_faces_context, request, " + digest + "L);");

    if (isJsfParentRequired()) {
      out.println("request.setAttribute(\"caucho.jsf.parent\""
                  + ", new com.caucho.jsp.jsf.JsfComponentTag("
                  + _var + ", true, " + _bodyVar + "));");
    }

    for (int i = 0; i < _attrList.size(); i++) {
      Attr attr = (Attr) _attrList.get(i);

      Method method = attr.getMethod();
      Class type = null;

      if (method != null)
        type = method.getParameterTypes()[0];
      
      JspAttribute jspAttr = attr.getAttr();
      String value = attr.getValue();

      if (jspAttr != null) {
        generateSetParameter(out, _var, jspAttr, method,
                             true, null, false, false, null);
      }
      else if ((value.indexOf("#{") >= 0
                && ! ValueExpression.class.isAssignableFrom(type)
                && ! MethodExpression.class.isAssignableFrom(type))) {
        out.print(_var + ".setValueExpression(\"" + attr.getName() + "\", ");

        String exprVar = "_caucho_value_expr_" + _gen.addValueExpr(value, type.getName());

        out.println(exprVar + ");");
      }
      else if (attr.getName().equals("beforePhaseListener")
               || attr.getName().equals("afterPhaseListener")) {
        String exprVar = "_caucho_method_expr_" + _gen.addMethodExpr(value, "void foo(javax.faces.event.PhaseEvent)");

        out.println(_var + "." + method.getName() + "(" + exprVar + ");");
      } else if (attr.getName().equals("locale")){
        out.print(_var + "." + method.getName() + "(");

        String[] values = value.split("[-_]");

        if (values.length > 2)
          out.print("new java.util.Locale(\"" +values[0]
                    + "\",\"" + values[1]
                    + "\",\"" + values[2]
                    + "\")");
        else if (values.length > 1)
          out.print("new java.util.Locale(\"" + values[0]
                    + "\",\"" + values[1]
                    + "\")");

        else
          out.print("new java.util.Locale(\"" + value + "\")");

        out.println(");");
      }
      else {
        out.print(_var + "." + method.getName() + "(");

        out.print(generateParameterValue(type, value, true, null, false));

        out.println(");");
      }
    }
      
    out.println("response.setLocale(" + _var + ".getLocale());");

    generateChildren(out);

    out.println("com.caucho.jsp.jsf.JsfTagUtil.afterRoot(_jsp_faces_context, request, response);");

    if (_bodyVar != null) {
      out.println("out = pageContext.popBody();");
      out.println("pageContext.releaseBody(" + _bodyVar + ");");
    }
  }

  private long calculateDigest()
  {
    long digest = 0;

    for (PersistentDependency pDepend : _gen.getDependList()) {
      if (pDepend instanceof Depend) {
        Depend depend = (Depend) pDepend;

        digest = 65521 * digest + depend.getDigest();
      }
    }

    return digest;
  }
}
