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

import com.caucho.vfs.ReadStream;

import org.w3c.dom.Document;

import javax.xml.transform.TransformerConfigurationException;

/**
 * Public facade for creating StyleScript stylesheets.
 *
 * <code><pre>
 * import java.io.*;
 * import javax.xml.transform.*;
 * import javax.xml.transform.stream.*;
 * import org.xml.sax.*;
 *
 * import com.caucho.xsl.*;
 *
 * ...
 *
 * TransformerFactory factory = new Xsl();
 * StreamSource xslSource = new StreamSource("mystyle.xsl");
 * Transformer transformer = factory.newTransformer(xslSource);
 *
 * StreamSource xmlSource = new StreamSource("test.xml");
 * StreamResult htmlResult = new StreamResult("test.html");
 *
 * transformer.transform(xmlSource, htmlResult);
 * </pre></code>
 */
public class StyleScript extends AbstractStylesheetFactory {
  /**
   * Parses the XSL into a DOM document.
   *
   * @param rs the input stream.
   */
  protected Document parseXSL(ReadStream rs)
    throws TransformerConfigurationException
  {
    try {
      XslParser parser = new XslParser();
      
      return parser.parse(rs);
    } catch (TransformerConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new XslParseException(e);
    }
  }
}
