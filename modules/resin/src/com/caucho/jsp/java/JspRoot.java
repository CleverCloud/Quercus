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

import com.caucho.jsp.JspParseException;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the root node.
 */
public class JspRoot extends JspContainerNode {
  static final L10N L = new L10N(JspRoot.class);

  static final private QName VERSION = new QName("version");

  private String _version;

  private HashMap<String,String> _namespaceMap
    = new HashMap<String,String>();

  private HashMap<String,String> _revNamespaceMap
    = new HashMap<String,String>();

  /**
   * Sets the versino.
   */
  public void setVersion(String version)
  {
    _version = version;
  }
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (VERSION.equals(name)) {
      if (! value.equals("2.1")
          && ! value.equals("2.0")
          && ! value.equals("1.2"))
        throw error(L.l("'{0}' is an unsupported jsp:root version.",
                        value));

      _version = value;
    }
    else {
      throw error(L.l("'{0}' is an unknown jsp:root attribute.  'version' is the only allowed JSP root value.",
                      name.getName()));
    }
  }

  /**
   * Adds a text node.
   */
  public JspNode addText(String text)
    throws JspParseException
  {
    for (int i = 0; i < text.length(); i++) {
      if (! XmlChar.isWhitespace(text.charAt(i))) {
        JspNode node = new StaticText(_gen, text, this);

        addChild(node);

        return node;
      }
    }

    return null;
  }

  /**
   * Called after all the attributes from the tag.
   */
  public void endAttributes()
    throws JspParseException
  {
    _gen.setOmitXmlDeclaration(true);

    if (getParent() != null && ! (getParent() instanceof JspTop))
      throw error(L.l("jsp:root must be the root JSP element."));

    if (_version == null)
      throw error(L.l("'version' is a required attribute of jsp:root"));
  }
  
  /**
   * Adds a child node.
   */
  public void addChild(JspNode node)
    throws JspParseException
  {
    super.addChild(node);
  }

  /**
   * Adds a namespace, e.g. from a prefix declaration.
   */
  @Override
  public void addNamespaceRec(String prefix, String value)
  {
    _namespaceMap.put(prefix, value);
    _revNamespaceMap.put(value, prefix);
  }

  /**
   * Adds a namespace, e.g. from a prefix declaration.
   */
  @Override
  public String getNamespacePrefix(String uri)
  {
    return _revNamespaceMap.get(uri);
  }
  
  /**
   * Set true if the node only has static text.
   */
  public boolean isStatic()
  {
    for (int i = 0; i < _children.size(); i++) {
      JspNode node = _children.get(i);

      if (! node.isStatic())
        return false;
    }

    return true;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:root xmlns:jsp=\"http://java.sun.com/JSP/Page\"");

    for (Map.Entry entry : _namespaceMap.entrySet()) {
      if (! "jsp".equals(entry.getKey()))
        os.print(" xmlns:" + entry.getKey() + "=\"" + entry.getValue() + "\"");
    }

    printJspId(os);
    os.print(" version=\"2.0\"");
    
    os.print(">");
    printXmlChildren(os);
    os.print("</jsp:root>");
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (! _gen.isOmitXmlDeclaration()) {
      String encoding = _gen.getCharacterEncoding();

      if (encoding == null)
        encoding = "UTF-8";

      out.addText("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
    }

    if (_gen.getDoctypeSystem() != null) {
      out.addText("<!DOCTYPE ");
      out.addText(_gen.getDoctypeRootElement());

      if (_gen.getDoctypePublic() != null) {
        out.addText(" PUBLIC \"");
        out.addText(_gen.getDoctypePublic());
        out.addText("\" \"");
        out.addText(_gen.getDoctypeSystem());
        out.addText("\"");
      }
      else {
        out.addText(" SYSTEM \"");
        out.addText(_gen.getDoctypeSystem());
        out.addText("\"");
      }
      
      out.addText(">\n");
    }
    
    generateChildren(out);
  }
}
