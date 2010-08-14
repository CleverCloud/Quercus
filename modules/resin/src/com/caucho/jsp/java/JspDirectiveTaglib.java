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

public class JspDirectiveTaglib extends JspNode {
  final static L10N L = new L10N(JspDirectiveTaglib.class);

  private static final QName PREFIX = new QName("prefix");
  private static final QName URI = new QName("uri");
  private static final QName TAGDIR = new QName("tagdir");

  private String _prefix;
  private String _uri;
  private String _tagDir;
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (PREFIX.equals(name))
      _prefix = value;
    else if (URI.equals(name))
      _uri = value;
    else if (TAGDIR.equals(name))
      _tagDir = value;
    else {
      throw error(L.l("'{0}' is an unknown JSP taglib directive attributes.  See the JSP documentation for a complete list of page directive attributes.",
                      name.getName()));
    }
  }

  /**
   * When the element complets.
   */
  public void endElement()
    throws JspParseException
  {
    if (_prefix == null)
      throw error(L.l("<{0}> needs a 'prefix' attribute.",
                      getTagName()));
    
    if (_uri == null && _tagDir == null)
      throw error(L.l("<{0}> needs a 'uri' or 'tagdir' attribute.",
                      getTagName()));
    
    if (_uri != null && _tagDir != null)
      throw error(L.l("<{0}> can't have both a 'uri' an a 'tagdir' attribute.",
                      getTagName()));

    if (_uri != null) {
      _gen.addTaglib(_prefix, _uri);
      addNamespace(_prefix, _uri);
    }
    else {
      _gen.addTaglibDir(_prefix, _tagDir);
      addNamespace(_prefix, "urn:jsptld:" + _tagDir);
    }
  }
  
  /**
   * Return true if the node only has static text.
   */
  public boolean isStatic()
  {
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
    /*
    os.print("<jsp:directive.taglib prefix=\"" + _prefix + "\"");
    
    if (_uri != null)
      os.print(" uri=\"" + _uri + "\"/>");
    else
      os.print(" tagdir=\"" + _tagDir + "\"/>");
    */
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
  }
}
