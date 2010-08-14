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
import com.caucho.xml.XmlChar;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the root node.
 */
public class JspTop extends JspContainerNode implements JspSegmentNode {
  private static final L10N L = new L10N(JspTop.class);

  private boolean _hasRoot;
  private int _maxFragmentIndex = -1;
  private int _maxStaticFragmentIndex = -1;

  private HashMap<String,String> _namespaceMap = new HashMap<String,String>();
  private HashMap<String,String> _revNamespaceMap
    = new HashMap<String,String>();

  /**
   * Adds a text node.
   */
  public JspNode addText(String text)
    throws JspParseException
  {
    // jsp/0705
    if (true || ! _hasRoot) {
      JspNode node = new StaticText(_gen, text, this);

      addChild(node);

      return node;
    }
    else {
      for (int i = 0; i < text.length(); i++)
        if (! XmlChar.isWhitespace(text.charAt(i)))
          throw error(L.l("JSP pages with <jsp:root> must not have text outside the <jsp:root>."));

      return null;
    }
  }

  /**
   * Adds a child node.
   */
  public void addChild(JspNode child)
    throws JspParseException
  {
    if (child instanceof JspRoot) {
      _hasRoot = true;
      _gen.setOmitXmlDeclaration(true);
    }
    
    super.addChild(child);
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
   * Returns the containing segment.
   */
  public JspSegmentNode getSegment()
  {
    return this;
  }
  
  /**
   * Returns the largest fragment index.
   */
  public int getMaxFragmentIndex()
  {
    return _maxFragmentIndex;
  }
  
  /**
   * Sets the largest fragment index.
   */
  public void setMaxFragmentIndex(int index)
  {
    if (_maxFragmentIndex < index)
      _maxFragmentIndex = index;
  }
  
  /**
   * Returns the largest static fragment index.
   */
  public int getMaxStaticFragmentIndex()
  {
    return _maxStaticFragmentIndex;
  }
  
  /**
   * Sets the largest static fragment index.
   */
  public void setMaxStaticFragmentIndex(int index)
  {
    if (_maxStaticFragmentIndex < index)
      _maxStaticFragmentIndex = index;
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
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    if (_hasRoot) {
      printXmlChildren(os);
      return;
    }
    
    /*
    os.print("<jsp:root xmlns:jsp=\"http://java.sun.com/JSP/Page\">");
    printXmlChildren(os);
    os.print("</jsp:root>");
    */
    os.print("<jsp:root");
    printJspId(os);
    os.print(" version=\"2.0\"");
    os.print(" xmlns:jsp=\"http://java.sun.com/JSP/Page\"");

    for (Map.Entry entry : _namespaceMap.entrySet()) {
      os.print(" xmlns:" + entry.getKey() + "=\"" + entry.getValue() + "\"");
    }
    os.print(">");
    printXmlChildren(os);
    os.print("</jsp:root>");
  }

  /**
   * Returns true if the namespace decl has been printed.
   */
  public boolean hasNamespace(String prefix, String uri)
  {
    if ("".equals(uri) && ("".equals(prefix) || prefix == null))
      return true;
    else
      return false;
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (! _hasRoot) {
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
    }
    
    generateChildren(out);
  }
}
