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
import com.caucho.vfs.WriteStream;
import com.caucho.xml.CauchoNode;
import com.caucho.xml.XmlPrinter;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Transforms a document to a result string.
 */
public class StringTransformerImpl extends TransformerImpl {
  protected LineMap _lineMap;
  
  StringTransformerImpl(StylesheetImpl stylesheet)
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
  
  public String transform(InputStream source)
    throws SAXException, IOException
  {
    return transform(parseDocument(source, null));
  }
  
  public String transform(String systemId)
    throws SAXException, IOException
  {
    return transform(parseDocument(systemId));
  }
  
  public String transformString(String source)
    throws SAXException, IOException
  {
    return transform(parseStringDocument(source, null));
  }

  /**
   * Transform a node, producing an output string.
   *
   * @param node the input node to transform
   *
   * @return the resulting string.
   */
  public String transform(Node node)
    throws SAXException, IOException
  {
    _lineMap = null;
    Properties output = _stylesheet.getOutputProperties();

    com.caucho.vfs.StringWriter sw = new com.caucho.vfs.StringWriter();
    WriteStream ws = sw.openWrite();
    
    XmlPrinter out = new XmlPrinter(ws);

    out.setMethod((String) output.get(OutputKeys.METHOD));
    out.setEncoding("UTF-8");
    out.setMimeType((String) output.get(OutputKeys.MEDIA_TYPE));
    String omit = (String) output.get(OutputKeys.OMIT_XML_DECLARATION);

    if (omit == null || omit.equals("false") || omit.equals("no"))
      out.setPrintDeclaration(true);
    out.setStandalone((String) output.get(OutputKeys.STANDALONE));
    out.setSystemId((String) output.get(OutputKeys.DOCTYPE_SYSTEM));
    out.setPublicId((String) output.get(OutputKeys.DOCTYPE_PUBLIC));
    
    String indent = (String) output.get(OutputKeys.INDENT);
    if (indent != null)
      out.setPretty(indent.equals("true"));
    out.setVersion((String) output.get(OutputKeys.VERSION));
    if (node instanceof CauchoNode) {
      String filename = ((CauchoNode) node).getFilename();
      out.setLineMap(filename != null ? filename : "anonymous.xsl");
    }
    else
      out.setLineMap("anonymous.xsl");
    
      String includeContentType = (String) output.get("include-content-type");
      if (includeContentType != null)
        out.setIncludeContentType(includeContentType.equals("true") ||
                                  includeContentType.equals("yes"));

    try {
      out.startDocument();
      _stylesheet.transform(node, out, this);
      out.endDocument();
      _lineMap = out.getLineMap();
      ws.close();

      return sw.getString();
    } catch (Exception e) {
      throw new IOExceptionWrapper(e);
    }
  }
}
