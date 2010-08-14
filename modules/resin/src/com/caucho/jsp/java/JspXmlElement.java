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
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Represents a JSP implicit element
 */
public class JspXmlElement extends JspContainerNode {
  private ArrayList<QName> _attrNames = new ArrayList<QName>();
  private ArrayList<String> _attrValues = new ArrayList<String>();

  public JspXmlElement()
  {
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    _attrNames.add(name);
    _attrValues.add(value);
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, JspAttribute value)
    throws JspParseException
  {
    // jsp/10j0
    if (_gen.isPrototype())
      return;
    
    throw error(L.l("jsp:attribute '{0}' is not allowed as a child of XML element <{1}>.",
                    name.getName(), getTagName()));
  }

  /**
   * Returns true if the namespace decl has been printed.
   */
  public boolean hasNamespace(String prefix, String uri)
  {
    QName name = getQName();

    if (prefix == null || uri == null)
      return true;
    else if (prefix.equals(name.getPrefix()) &&
             uri.equals(name.getNamespaceURI()))
      return true;
    else
      return _parent.hasNamespace(prefix, uri);
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<" + getTagName());

    for (int i = 0; i < _attrNames.size(); i++) {
      QName name = _attrNames.get(i);
      String value = _attrValues.get(i);
      
      os.print(" " + name.getName() + "=\"");
      
      printXmlText(os, value);
      
      os.print("\"");
    }

    os.print(">");

    printXmlChildren(os);

    os.print("</" + getTagName() + ">");
  }

  /**
   * Generates the code for the element.
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    out.addText("<");
    out.addText(getTagName());

    QName qName = getQName();

    HashSet<String> prefixes = new HashSet<String>();
    
    if (qName.getNamespaceURI() != null &&
        ! _parent.hasNamespace(qName)) {
      prefixes.add(qName.getPrefix());
      
      out.addText(" ");
      if (qName.getPrefix() == null || qName.getPrefix().equals(""))
        out.addText("xmlns=\"");
      else
        out.addText("xmlns:" + qName.getPrefix() + "=\"");
      out.addText(qName.getNamespaceURI());
      out.addText("\"");
    }

    for (int i = 0; i < _attrNames.size(); i++) {
      QName name = _attrNames.get(i);
      String value = _attrValues.get(i);

      if (name.getNamespaceURI() != null &&
          ! prefixes.contains(name.getPrefix()) &&
          ! _parent.hasNamespace(name)) {
        prefixes.add(name.getPrefix());
        out.addText(" ");
        if (name.getPrefix() == null || name.getPrefix().equals(""))
          out.addText("xmlns=\"");
        else
          out.addText("xmlns:" + name.getPrefix() + "=\"");
        out.addText(name.getNamespaceURI());
        out.addText("\"");
      }
      
      out.addText(" ");
      out.addText(name.getName());

      if (value == null || value.equals("")) {
        // XXX: possibly differ for html/text

        out.addText("=\"\"");
      }
      else {
        out.addText("=\"");

        if (value.indexOf("${") < 0 &&
            ! value.startsWith("<%") &&
            ! value.startsWith("%")) {
          out.addText(value);
        }
        else {
          String javaValue = generateParameterValue(String.class, value,
                                                    true, null,
                                                    _parseState.isELIgnored());
          out.println("out.print(" + javaValue + ");");
        }
        out.addText("\"");
      }
    }

    if (getChildren() != null && getChildren().size() > 0) {
      out.addText(">");

      generateChildren(out);

      out.addText("</" + getTagName() + ">");
    }
    else
      out.addText(" />");
  }
}
