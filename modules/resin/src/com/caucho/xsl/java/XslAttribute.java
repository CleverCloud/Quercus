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

package com.caucho.xsl.java;

import com.caucho.java.JavaWriter;
import com.caucho.xml.QName;
import com.caucho.xsl.XslParseException;

import java.util.ArrayList;

/**
 * Represents any XSL node from the stylesheet.
 */
public class XslAttribute extends XslNode {
  private String _name;
  private String _namespace;

  /**
   * Returns the attribute name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the attribute value.
   */
  public String getValue()
  {
    ArrayList<XslNode> children = getChildren();

    if (children == null || children.size() == 0)
      return "";

    XslNode node = children.get(0);

    if (node instanceof TextNode)
      return ((TextNode) node).getText();
    else
      return "";
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("name"))
      _name = value;
    else if (name.getName().equals("namespace"))
      _namespace = value;
    else
      super.addAttribute(name, value);
  }

  /**
   * Ends the attributes.
   */
  public void endAttributes()
    throws XslParseException
  {
    if (_name == null)
      throw error(L.l("xsl:attribute needs a 'name' attribute."));
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    if (hasChildren() || _namespace != null) {
      String var = "_xsl_attr_" + generateId();

      out.print("XMLWriter " + var + " = ");
      
      if (_namespace != null) {
        out.print("out.pushAttributeNs(");
    
        generateString(out, _name, '+');
      
        out.print(", ");
        generateString(out, _namespace, '+');

      
        out.println(");");
      }
      else {
        out.print("out.pushAttribute(");
    
        generateString(out, _name, '+');
      
        if (getOutputNamespace() != null) {
          out.print(", ");
          printNamespace(out, getOutputNamespace());
        }
    
        out.println(");");
      }
      
      generateChildren(out);
    
      out.println("out.popAttribute(" + var + ");");
    }
    else {
      out.print("out.setAttribute(");
    
      generateString(out, _name, '+');

      if (getOutputNamespace() != null) {
        out.print(", ");
        printNamespace(out, getOutputNamespace());
      }

      out.println(", \"\");");
    }
  }
}
