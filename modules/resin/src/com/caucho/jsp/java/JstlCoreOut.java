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

import com.caucho.el.Expr;
import com.caucho.jsp.JspParseException;
import com.caucho.jsp.el.JspELParser;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;

public class JstlCoreOut extends JstlNode {
  private static final QName VALUE = new QName("value");
  private static final QName ESCAPE_XML = new QName("escapeXml");
  private static final QName DEFAULT = new QName("default");
  
  private String _value;
  private String _escapeXml = "true";
  private String _default;
  
  private JspAttribute _valueAttr;
  private JspAttribute _escapeXmlAttr;
  private JspAttribute _defaultAttr;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (VALUE.equals(name))
      _value = value;
    else if (ESCAPE_XML.equals(name))
      _escapeXml = value;
    else if (DEFAULT.equals(name))
      _default = value;
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
    if (VALUE.equals(name))
      _valueAttr = value;
    else if (ESCAPE_XML.equals(name))
      _escapeXmlAttr = value;
    else if (DEFAULT.equals(name))
      _defaultAttr = value;
    else
      throw error(L.l("`{0}' is an unknown attribute for <{1}>.",
                      name.getName(), getTagName()));
  }

  /**
   * Returns true if the tag has scripting values.
   */
  @Override
  public boolean hasScripting()
  {
    return (super.hasScripting()
            || hasScripting(_value) || hasScripting(_valueAttr)
            || hasScripting(_escapeXml) || hasScripting(_escapeXmlAttr)
            || hasScripting(_default) || hasScripting(_defaultAttr));
  }

  @Override
  public boolean hasCustomTag()
  {
    return (super.hasCustomTag()
            || _valueAttr != null && _valueAttr.hasCustomTag()
            || _escapeXmlAttr != null && _escapeXmlAttr.hasCustomTag()
            || _defaultAttr != null && _defaultAttr.hasCustomTag());
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    String prefix = getNamespacePrefix("http://java.sun.com/jsp/jstl/core");

    if (prefix == null) {
      prefix = "c";
      os.print("<c:out xmlns:c='http://java.sun.com/jsp/jstl/core'");
    }
    else
      os.print("<" + prefix + ":out");

    if (_value != null) {
      os.print(" value=\"");
      printXmlText(os, _value);
      os.print("\"");
    }
    os.print(">");

    printXmlChildren(os);

    os.print("</" + prefix + ":out>");
  }

  /**
   * Generates the code for the c:out tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_value == null && _valueAttr == null)
      throw error(L.l("required attribute `value' missing from <{0}>",
                      getTagName()));

    String escapeXml = "true";

    if (_escapeXml != null)
      escapeXml = generateValue(boolean.class, _escapeXml);
    else if (_escapeXmlAttr != null)
      escapeXml = _escapeXmlAttr.generateValue(String.class);

    if (_escapeXml != null && (_default != null || _defaultAttr != null)) {
      String var = "_jsp_var_" + _gen.uniqueId();
      
      out.println("boolean " + var + " = " + escapeXml + ";");
      escapeXml = var;
    }

    if (_default != null || _defaultAttr != null || hasChildren())
      out.print("if (");

    if (_valueAttr != null) {
      out.print("com.caucho.el.Expr.toStream(out, ");
      out.print(_valueAttr.generateValue(String.class));
      out.print(", " + escapeXml + ")");
    }
    else if (hasRuntimeAttribute(_value)) {
      out.print("com.caucho.el.Expr.toStream(out, ");
      out.print(generateRTValue(String.class, _value));
      out.print(", " + escapeXml + ")");
    }
    else {
      int index = _gen.addExpr(_value);
    
      out.print("_caucho_expr_" + index +
                ".print(out, _jsp_env, " + escapeXml + ")");
    }
    
    if (_default != null || _defaultAttr != null) {
      out.println(") {");
      out.pushDepth();

      if (_defaultAttr != null) {
        out.print("com.caucho.el.Expr.toStream(out, ");
        out.print(_defaultAttr.generateValue(String.class));
        out.println(", " + escapeXml + ");");
      }
      else if (hasRuntimeAttribute(_default)) {
        out.print("com.caucho.el.Expr.toStream(out, ");
        out.print(generateRTValue(String.class, _default));
        out.println(", " + escapeXml + ");");
      }
      else {
        Expr defaultExpr = new JspELParser(_gen.getELContext(),
                                           _default).parse();

        if (defaultExpr.isConstant() && escapeXml.equals("true")) {
          String string = defaultExpr.evalString(null);

          if (string != null && ! string.equals("")) {
            out.print("com.caucho.el.Expr.toStreamEscaped(out, \"");
            out.printJavaString(string);
            out.println("\");");
          }
        }
        else if (defaultExpr.isConstant() && escapeXml.equals("false")) {
          String string = defaultExpr.evalString(null);

          if (string != null && ! string.equals("")) {
            out.addText(string);
          }
        }
        else {
          int defaultIndex = _gen.addExpr(_default);
          out.println("_caucho_expr_" + defaultIndex +
                      ".print(out, _jsp_env, " + escapeXml + ");");
        }
      }
      
      out.popDepth();
      out.println("}");
    }
    else if (! hasChildren()) {
      out.println(";");
    }
    else if (isChildrenStatic()) {
      out.println(") {");
      out.pushDepth();
      
      out.print("com.caucho.el.Expr.toStream(out, ");
      out.print("\"" + escapeJavaString(getStaticText().trim()) + "\"");
      out.println(", " + escapeXml + ");");
      
      out.popDepth();
      out.println("}");
    }
    else {
      out.println(") {");
      out.pushDepth();
      
      out.println("out = pageContext.pushBody();");

      generateChildren(out);

      out.println("pageContext.printBody((com.caucho.jsp.BodyContentImpl) out, " + escapeXml + ");");

      out.println("out = pageContext.popAndReleaseBody();");

      out.popDepth();
      out.println("}");
    }
  }
}
