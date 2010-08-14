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
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;
import java.util.ArrayList;

public class JspElement extends JspContainerNode {
  static final L10N L = new L10N(JspElement.class);

  static final private QName NAME = new QName("name");

  private String _name;
  private JspAttribute _attrName;

  private ArrayList<QName> _attrNames = new ArrayList<QName>();
  private ArrayList<JspAttribute> _attrValues = new ArrayList<JspAttribute>();
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (NAME.equals(name)) {
      _name = value;
    }
    else {
      throw error(L.l("`{0}' is an unknown jsp:element attribute.  See the JSP documentation for a complete list of jsp:element directive attributes.",
                      name.getName()));
    }
  }
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, JspAttribute value)
    throws JspParseException
  {
    if (_name == null && NAME.equals(name)) {
      _attrName = value;
    }
    else {
      _attrNames.add(name);
      _attrValues.add(value);
    }
  }

  /**
   * When the element complets.
   */
  public void endElement()
    throws JspParseException
  {
    if (_name == null && _attrName == null) {
      throw error(L.l("jsp:element requires a `name' attribute."));
    }
  }
  
  /**
   * Return true if the node only has static text.
   */
  public boolean isStatic()
  {
    return false;
  }

  @Override
  public boolean hasCustomTag()
  {
    if (super.hasCustomTag())
      return true;

    // jsp/0433
    for (JspAttribute attrValue : _attrValues) {
      if (attrValue.hasCustomTag())
        return true;
    }

    return false;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:element name=\"" + _name + "\">");
    printXmlChildren(os);
    os.print("</jsp:element>");
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String var = null;

    if (_attrName != null) {
      var = "_caucho_var" + _gen.uniqueId();

      out.println("String " + var + " = " + _attrName.generateValue() + ";");
    }
    else if (hasELAttribute(_name)) {
      var = "_caucho_var" + _gen.uniqueId();
      int index = _gen.addExpr(_name);
      
      out.println("String " + var + " = _caucho_expr_" + index + ".evalString(_jsp_env);");
    }
    else if (hasRuntimeAttribute(_name)) {
      // jsp/0408
      
      var = "_caucho_var" + _gen.uniqueId();
      
      out.println("String " + var + " = " + getRuntimeAttribute(_name) + ";");
    }

    if (var != null) {
      out.addText("<");
      out.println("out.print(" + var + ");");
    }
    else
      out.addText("<" + _name);

    for (int i = 0; i < _attrNames.size(); i++) {
      QName name = _attrNames.get(i);
      JspAttribute value = _attrValues.get(i);

      out.addText(" " + name.getName() + "=\"");

      if (value.isStatic())
        out.addText(value.getStaticText());
      else
        out.print("out.print(" + value.generateValue() + ");");

      out.addText("\"");
    }

    
    out.addText(">");

    generateChildren(out);

    if (var != null) {
      out.println("out.print(\"</\");");
      out.println("out.print(" + var + ");");
      out.println("out.print(\">\");");
    }
    else
      out.addText("</" + _name + ">");
  }
}
