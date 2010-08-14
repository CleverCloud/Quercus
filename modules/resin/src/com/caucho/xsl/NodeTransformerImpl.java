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

package com.caucho.xsl;

import com.caucho.java.LineMap;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.xml.DOMBuilder;
import com.caucho.xml.Xml;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public class NodeTransformerImpl extends TransformerImpl {
  protected LineMap _lineMap;
  
  NodeTransformerImpl(StylesheetImpl stylesheet)
  {
    super(stylesheet);
  }

  public Object getProperty(String name)
  {
    if (name.equals(LINE_MAP))
      return _lineMap;
    else
      return super.getProperty(name);
  }

  /**
   * Transforms the input stream as an XML document into children
   * of the result node.
   *
   * @param source InputStream containing an XML document.
   * @param node parent of the new results.
   */
  public Node transform(InputStream source, Node node)
    throws SAXException, IOException
  {
    return transform(parseDocument(source, null), node);
  }
  
  public Node transform(String systemId, Node node)
    throws SAXException, IOException
  {
    return transform(parseDocument(systemId), node);
  }
  
  public Node transformString(String source, Node node)
    throws SAXException, IOException
  {
    return transform(parseStringDocument(source, null), node);
  }
  
  public Node transform(Node sourceNode, Node destNode)
    throws SAXException, IOException
  {
    _lineMap = null;
    OutputFormat output = _stylesheet.getOutputFormat();

    if (destNode == null)
      destNode = Xml.createDocument();

    DOMBuilder out = new DOMBuilder();
    out.init(destNode);

    try {
      out.startDocument();
      _stylesheet.transform(sourceNode, out, this);
      out.endDocument();
    } catch (Exception e) {
      throw new IOExceptionWrapper(e);
    }

    return destNode;
  }
}
