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

public class JstlFmtParam extends JstlNode {
  private static final QName VALUE = new QName("value");
  
  private String _value;
  private JspAttribute _valueAttr;
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (VALUE.equals(name)) {
      _value = value;
    }
    else
      throw error(L.l("`{0}' is an unknown attribute for <{1}>.",
                      name.getName(), getTagName()));
  }

  /**
   * Returns true if the tag has scripting values.
   */
  public boolean hasScripting()
  {
    return (super.hasScripting()
            || hasScripting(_value) || hasScripting(_valueAttr));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<fmt:param>");

    printXmlText(os, _value);
    
    printXmlChildren(os);

    os.print("</fmt:param>");
  }
  
  /**
   * Generates the code for the fmt:param tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    throw error(L.l("fmt:param can't be called directly."));
  }
  
  /**
   * Generates the code to set the fmt:param tag.
   */
  public void generateSet(JspJavaWriter out, String lhs)
    throws Exception
  {
    if (_value != null) {
      if (! getRuntimeAttribute(_value).equals(_value)) {
        // jsp/1c3s

        out.println(lhs + " = String.valueOf(" + getRuntimeAttribute(_value) + ");");
      }
      else {
        int paramIndex = _gen.addExpr(_value);

        out.println(lhs + " = _caucho_expr_" + paramIndex + ".evalObject(_jsp_env);");
      }
    }
    else {
      out.println("out = pageContext.pushBody();");

      generateChildren(out);

      out.println(lhs + " = ((com.caucho.jsp.BodyContentImpl) out).getTrimString();");

      out.println("out = pageContext.popBody();");
    }
  }
}
