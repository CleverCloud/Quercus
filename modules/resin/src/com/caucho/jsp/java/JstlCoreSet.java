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

public class JstlCoreSet extends JstlNode {
  private static final QName VALUE = new QName("value");
  private static final QName VAR = new QName("var");
  private static final QName SCOPE = new QName("scope");
  private static final QName TARGET = new QName("target");
  private static final QName PROPERTY = new QName("property");
  
  private String _value;
  private JspAttribute _valueAttr;
  
  private String _var;
  private String _scope;
  
  private String _target;
  private JspAttribute _targetAttr;
  
  private String _property;
  private JspAttribute _propertyAttr;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (VALUE.equals(name))
      _value = value;
    else if (VAR.equals(name))
      _var = value;
    else if (SCOPE.equals(name))
      _scope = value;
    else if (TARGET.equals(name))
      _target = value;
    else if (PROPERTY.equals(name))
      _property = value;
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
    if (VALUE.equals(name))
      _valueAttr = value;
    else if (TARGET.equals(name))
      _targetAttr = value;
    else if (PROPERTY.equals(name))
      _propertyAttr = value;
    else
      throw error(L.l("`{0}' is an unsupported jsp:attribute for <{1}>.",
                      name.getName(), getTagName()));
  }

  /**
   * Returns true if the tag has scripting values.
   */
  public boolean hasScripting()
  {
    return (super.hasScripting()
            || hasScripting(_value) || hasScripting(_valueAttr)
            || hasScripting(_target) || hasScripting(_targetAttr)
            || hasScripting(_property) || hasScripting(_propertyAttr));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<c:set");

    if (_value != null) {
      os.print(" value=\"");
      printXmlText(os, _value);
      os.print("\"");
    }
    
    if (_var != null)
      os.print(" var=\"" + _var + "\"");
    
    if (_scope != null)
      os.print(" scope=\"" + _scope + "\"");
    
    if (_target != null)
      os.print(" target=\"" + _target + "\"");
    
    if (_property != null)
      os.print(" property=\"" + _property + "\"");

    os.print(">");

    printXmlChildren(os);

    os.print("</c:set>");
  }
  
  /**
   * Generates the code for the c:set tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_value != null && _value.contains("#{") && _var != null) {
      String exprVar = "_caucho_value_expr_" + _gen.addValueExpr(_value, null);

      out.print("_jsp_env.getVariableMapper().setVariable(\"");
      out.print(escapeJavaString(_var));
      out.print("\", ");
      out.print(exprVar);
      out.println(");");
      
      // jsp/1c2m
      generateSet(out, exprVar);
    }
    else if (_value != null) {
      String value = generateValue(Object.class, _value);
      
      generateSet(out, value);
    }
    else if (_valueAttr != null) {
      generateSet(out, _valueAttr.generateValue());
    }
    else if (! hasChildren()) {
      generateSet(out, "\"\"");
    }
    else if (isChildrenStatic()) {
      generateSet(out, '"' + escapeJavaString(getStaticText().trim()) + '"');
    }
    else if (hasChildren()) {
      out.println("out = pageContext.pushBody();");

      generateChildren(out);

      String cauchoVar = "_caucho_var_" + _gen.uniqueId();

      out.println("Object " + cauchoVar + " = ((com.caucho.jsp.BodyContentImpl) out).getTrimString();");

      out.println("out = pageContext.popAndReleaseBody();");

      generateSet(out, cauchoVar);
    }
  }

  private void generateSet(JspJavaWriter out, String value)
    throws Exception
  {
    if (_var != null)
      generateSetOrRemove(out, _var, _scope, value);
    else {
      out.print("com.caucho.el.Expr.setProperty(");
      if (_targetAttr != null)
        out.print(_targetAttr.generateValue() + ", ");
      else
        out.print(generateValue(Object.class, _target) + ", ");

      if (_propertyAttr != null)
        out.print(_propertyAttr.generateValue() + ", ");
      else
        out.print(generateValue(String.class, _property) + ", ");
      
      out.println(value + ");");
    }
  }
}
