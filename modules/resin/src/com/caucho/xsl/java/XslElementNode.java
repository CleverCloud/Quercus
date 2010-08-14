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
import com.caucho.xsl.JavaGenerator;
import com.caucho.xsl.XslParseException;

/**
 * Represents any XSL node from the stylesheet.
 */
public class XslElementNode extends XslNode {
  private static final QName _useAttributeSets =
    new QName("xsl", "use-attribute-sets", JavaGenerator.XSLNS);
  
  private QName _name;

  public XslElementNode(QName name)
  {
    _name = name;
  }

  /**
   * Returns the name of the tag.
   */
  public String getTagName()
  {
    return _name.getName();
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (_useAttributeSets.equals(name)) {
      XslUseAttributeSets attr = new XslUseAttributeSets(value);
      
      attr.setGenerator(_gen);
    
      addChild(attr);
    }
    else if (name.getName().startsWith("xmlns")) {
      super.addAttribute(name, value);
      
      if (! JavaGenerator.XSLNS.equals(value) &&
          ! JavaGenerator.XTPNS.equals(value)) {
        XslNamespaceNode attr = new XslNamespaceNode(name, value);

        attr.setParent(this);
        attr.setGenerator(_gen);

        addChild(attr);
      }
    }
    else if (JavaGenerator.XSLNS.equals(name.getNamespaceURI())) {
    }
    else if (JavaGenerator.XTPNS.equals(name.getNamespaceURI())) {
    }
    else {
      XslAttributeNode attr = new XslAttributeNode(name, value);

      attr.setParent(this);
      attr.setGenerator(_gen);
    
      addChild(attr);
    }
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    String namespace = _name.getNamespaceURI();
    String prefix = _name.getPrefix();
    String local = _name.getLocalName();
    String name = _name.getName();

    _gen.printLocation(_systemId, _filename, _startLine);

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
    
    //if (_namespace != null) {
      out.print("out.pushElement(");
      out.print(namespace == null ? "\"\"" : ("\"" + namespace + "\""));
      out.print(prefix == null ? ", null" : (", \"" + prefix + "\""));
      out.print(local == null ? ", null" : (", \"" + local + "\""));
      out.print(name == null ? ", null" : (", \"" + name + "\""));
      out.println(");");
      /*
    }
    else {
      out.print("out.pushElement(");
      out.print(name == null ? "null" : ("\"" + name + "\""));
      out.println(");");
    }
      */

    generateChildren(out);

    out.println("out.popElement();");
  }
}
