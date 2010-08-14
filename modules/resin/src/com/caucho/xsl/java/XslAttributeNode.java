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

/**
 * Represents any XSL node from the stylesheet.
 */
public class XslAttributeNode extends XslNode {
  private QName _name;
  private String _value;

  public XslAttributeNode(QName name, String value)
  {
    _name = name;
    _value = value;
  }

  /**
   * Generates the code for the attribute
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    // XXX: filename:line
    
    String namespace = _name.getNamespaceURI();
    String prefix = _name.getPrefix();
    String local = _name.getLocalName();
    String name = _name.getName();
    
    String []postPrefix = _gen.getNamespaceAlias(namespace);
    
    if (postPrefix != null) {
      prefix = postPrefix[0];
      namespace = postPrefix[1];
      if (prefix == null || prefix.equals(""))
        name = local;
      else
        name = prefix + ":" + local;
    }
    /*
    if (_excludedNamespaces.get(namespace) != null)
      namespace = null;
    */

    if (name.equals("xmlns")) {
      out.print("out.bindNamespace(null, \"");
      out.printJavaString(_value);
      out.println("\");");
    }
    else if (name.startsWith("xmlns:")) {
      out.print("out.bindNamespace(\"" + name.substring(6) + "\", \"");
      out.printJavaString(_value);
      out.println("\");");
    }
    else if (namespace != null && _value.indexOf('{') < 0) {
      out.print("out.attribute(");
      out.print(namespace == null ? "null" : ("\"" + namespace + "\""));
      out.print(prefix == null ? ", null" : (", \"" + prefix + "\""));
      out.print(local == null ? ", null" : (", \"" + local + "\""));
      out.print(name == null ? ", null" : (", \"" + name + "\""));
      out.print(", ");
      if (_value == null)
        out.print("null");
      else {
        out.print("\"");
        out.printJavaString(_value);
        out.print("\"");
      }
      out.println(");");
    }
    else if (namespace != null) {
      out.print("out.attribute(");
      out.print(namespace == null ? "null" : ("\"" + namespace + "\""));
      out.print(prefix == null ? ", null" : (", \"" + prefix + "\""));
      out.print(local == null ? ", null" : (", \"" + local + "\""));
      out.print(name == null ? ", null" : (", \"" + name + "\""));
      out.print(",");
      generateString(out, _value, '+');
      out.println(");");
    }
    else if (_value.indexOf('{') < 0) {
      out.print("out.attribute(");
      out.print(name == null ? "null" : ("\"" + name + "\""));
      out.print(", ");
      if (_value == null)
        out.print("null");
      else {
        out.print("\"");
        out.printJavaString(_value);
        out.print("\"");
      }
      out.println(");");
    }
    else {
      out.print("out.attribute(");
      out.print(name == null ? "null" : ("\"" + name + "\""));
      out.print(", ");
      generateString(out, _value, '+');
      out.println(");");
    }
  }

  public String toString()
  {
    return "XslAttributeNode[" + _name + "," + _value + "]";
  }
}
