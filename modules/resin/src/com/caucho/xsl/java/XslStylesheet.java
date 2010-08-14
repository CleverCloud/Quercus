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
import java.util.regex.Pattern;

/**
 * Represents the top-level xsl:stylesheet node.
 */
public class XslStylesheet extends XslNode {
  private String _version;
  private String _id;
  private String _extensionElementPrefixes;
  private String _excludeResultPrefixes;
  private String _xpathDefaultNamespace;
  private String _defaultValidation;

  private boolean _isStyleScript;
  private boolean _isDisableOutputEscaping;

  private ArrayList<XslNode> _init = new ArrayList<XslNode>();

  private ArrayList<XslNode> _imports = new ArrayList<XslNode>();

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:stylesheet";
  }

  /**
   * Set true if the output escaping should be disabled.
   */
  public void setDisableOutputEscaping(boolean disable)
  {
    _isDisableOutputEscaping = disable;
  }

  /**
   * Adds an import directive.
   */
  public void addImport(XslNode node)
  {
    _imports.add(node);
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("version"))
      _version = value;
    else if (name.getName().equals("extension-element-prefixes"))
      _extensionElementPrefixes = value;
    else if (name.getName().equals("exclude-result-prefixes"))
      _excludeResultPrefixes = value;
    else if (name.getName().equals("xpath-default-namespace"))
      _xpathDefaultNamespace = value;
    else if (name.getName().equals("default-validation"))
      _defaultValidation = value;
    else if (name.getName().equals("resin:stylescript"))
      _gen.setStyleScript(true);
    else
      super.addAttribute(name, value);
  }

  /**
   * Ends the attributes.
   */
  public void endAttributes()
    throws XslParseException
  {
    if (_excludeResultPrefixes != null)
      addExcludeResultPrefixes(_excludeResultPrefixes);
    /*
    if (_version == null)
      throw error(L.l("xsl:stylesheet needs a 'version' attribute."));
    else if (! _version.equals("1.0"))
      throw error(L.l("'{0}' is an unknown xsl:stylesheet version.",
                      _version));
    */
  }
  
  /**
   * Adds a child node.
   */
  public void addChild(XslNode node)
    throws XslParseException
  {
    if (node instanceof XslVariable) {
      ((XslVariable) node).setGlobal(true);
      _gen.addInit(node);
    }
    else if (node instanceof XslParam) {
      ((XslParam) node).setGlobal(true);
      _gen.addInit(node);
    }
    else if (node instanceof TextNode) {
      TextNode text = (TextNode) node;

      if (! text.isWhitespace())
        throw error(L.l("text not allowed in the top level."));
    }
    else if (node instanceof XslElementNode) {
    }
    else if (! (node instanceof XslTopNode)) {
      throw error(L.l("<{0}> is not allowed in the top level.",
                      node.getTagName()));
    }
    else
      super.addChild(node);
  }    

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    for (int i = 0; i < _imports.size(); i++) {
      int oldMinImportance = _gen.getMinImportance();
      _gen.setMinImportance(_gen.getImportance());
      _imports.get(i).generate(out);
      _gen.setMinImportance(oldMinImportance);
      _gen.incrementImportance();
    }
    
    generateChildren(out);
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generateDeclaration(JavaWriter out)
    throws Exception
  {
    for (int i = 0; i < _imports.size(); i++)
      _imports.get(i).generateDeclaration(out);
    
    super.generateDeclaration(out);
  }

  private void addExcludeResultPrefixes(String prefixes)
    throws XslParseException
  {
    if (prefixes == null)
      return;
    
    Pattern regexp = Pattern.compile("[,\\s]+");
    String []strings = regexp.split(prefixes);
    for (int i = 0; i < strings.length; i++) {
      String prefix = strings[i];
      String ns = getNamespace(prefix);
      if (ns == null)
        throw error(L.l("`{0}' must be a namespace prefix", prefix));
      _gen.addExcludedNamespace(ns);
    }
  }

  protected void printPopScope(JavaWriter out)
    throws Exception
  {
  }
}
