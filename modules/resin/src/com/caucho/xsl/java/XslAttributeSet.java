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
 * Represents an attribute set.
 */
public class XslAttributeSet extends XslNode implements XslTopNode {
  private String _name;
  private ArrayList<XslAttribute> _attributes = new ArrayList<XslAttribute>();
  private ArrayList<String> _useAttributeSets = new ArrayList<String>();

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:attribute-set";
  }
  
  /**
   * Returns the attributes.
   */
  public ArrayList<XslAttribute> getAttributes()
  {
    ArrayList<XslAttribute> attributes = new ArrayList<XslAttribute>();

    attributes.addAll(_attributes);

    for (int i = 0; i < _useAttributeSets.size(); i++) {
      String useSet = _useAttributeSets.get(i);

      attributes.addAll(_gen.getAttributeSetList(useSet));
    }

    return attributes;
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("name"))
      _name = value;
    else if (name.getName().equals("use-attribute-sets")) {
      _useAttributeSets.add(value);
    }
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
      throw error(L.l("xsl:attribute-set needs a 'name' attribute."));
  }

  /**
   * Adds a child node.
   */
  public void addChild(XslNode node)
    throws XslParseException
  {
    if (node instanceof XslAttribute) {
      _attributes.add((XslAttribute) node);
    }
    else if (node instanceof TextNode) {
    }
    else
      throw error(L.l("<xsl:attribute-set> can only have <xsl:attribute> children."));
  }

  /**
   * Called when the tag closes.
   */
  public void endElement()
    throws Exception
  {
    _gen.addAttributeSet(_name, this);
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
  }
}
