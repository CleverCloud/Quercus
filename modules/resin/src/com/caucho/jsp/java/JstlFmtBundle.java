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
 * Generates code for the fmt:bundle bundle.
 *
 * <p>Set bundle looks up the correct localization context based on
 * a <code>basename</code> and the current <code>pageContext</code>.
 */
public class JstlFmtBundle extends JstlNode {
  private static final QName BASENAME = new QName("basename");
  private static final QName VAR = new QName("var");
  private static final QName SCOPE = new QName("scope");
  private static final QName PREFIX = new QName("prefix");
  
  private String _basename;
  private JspAttribute _basenameAttr;
  
  private String _var;
  private String _scope;

  private String _prefix;
  private JspAttribute _prefixAttr;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (BASENAME.equals(name))
      _basename = value;
    else if (VAR.equals(name))
      _var = value;
    else if (SCOPE.equals(name))
      _scope = value;
    else if (PREFIX.equals(name))
      _prefix = value;
    else
      throw error(L.l("`{0}' is an unknown attribute for <{1}>.",
                      name.getName(), getTagName()));
  }

  /**
   * Returns true if the tag has scripting values.
   */
  public boolean hasScripting()
  {
    return (super.hasScripting() ||
            hasScripting(_prefix) || hasScripting(_prefixAttr));
  }

  /**
   * Returns the prefix.
   */
  public String getPrefixCode()
  {
    if (_prefix != null)
      return "\"" +  _prefix + "\" + ";
    else
      return "";
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<fmt:bundle");

    if (_basename != null)
      os.print(" basename=\"" + xmlText(_basename) + "\"");
    
    if (_var != null)
      os.print(" var=\"" + xmlText(_var) + "\"");
    
    if (_scope != null)
      os.print(" scope=\"" + xmlText(_scope) + "\"");
    
    if (_prefix != null)
      os.print(" prefix=\"" + xmlText(_prefix) + "\"");

    os.print(">");

    printXmlChildren(os);

    os.print("</fmt:bundle>");
  }

  /**
   * Generates the code for the fmt:bundle tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_basename == null && _basenameAttr == null)
      throw error(L.l("required attribute `basename' missing from `{0}'",
                      getTagName()));

    String basenameExpr;

    if (_basenameAttr != null)
      basenameExpr = _basenameAttr.generateValue();
    else
      basenameExpr = generateValue(String.class, _basename);

    String oldVar = "_caucho_bundle_" + _gen.uniqueId();

    String locCtxVar = "_caucho_loc_ctx_" + _gen.uniqueId();

    out.println("javax.servlet.jsp.jstl.fmt.LocalizationContext " +
                locCtxVar +
                " = pageContext.getBundle(" +
                basenameExpr +
                ");");

    String localeVar = "_caucho_locale_" + _gen.uniqueId();

    out.println("java.util.Locale " + localeVar + ";");
    out.println("if ((" + localeVar + " = " + locCtxVar + ".getLocale()) != null)");
    out.pushDepth();
    out.println("com.caucho.jstl.rt.I18NSupport.setResponseLocale(pageContext, " + localeVar + ");");
    out.popDepth();

    out.print("Object " + oldVar + " = pageContext.putAttribute(\"caucho.bundle\", ");
    out.println(locCtxVar + ");");

    /*
    String var = _var;
    if (var == null)
      var = Config.FMT_LOCALIZATION_CONTEXT;


    if (var != null) {
      out.print("pageContext.putAttribute(\"");
      out.printJavaString(var);
      out.println("\", pageContext.getAttribute(\"caucho.bundle\"));");
    }
    */

    String oldPrefixVar = "_caucho_bundle_prefix_" + _gen.uniqueId();

    if (_prefix != null) {
      out.print("Object " + oldPrefixVar + " = pageContext.putAttribute(\"caucho.bundle.prefix\", ");
      out.print(generateValue(String.class, _prefix));
      out.println(");");
    }
    else
      out.println("Object " + oldPrefixVar + " = pageContext.putAttribute(\"caucho.bundle.prefix\", null);");

    out.println("try {");
    out.pushDepth();

    generateChildren(out);

    out.popDepth();
    out.println("} finally {");
    out.println("  pageContext.setAttribute(\"caucho.bundle\", " + oldVar + ");");
    out.println("  pageContext.setAttribute(\"caucho.bundle.prefix\", " + oldPrefixVar + ");");
    /*
    if (hasPrefix) {
      out.println("  _caucho_bundle_prefix = " + oldPrefix + ";");
    }
    */
    
    out.println("}");
  }
}
