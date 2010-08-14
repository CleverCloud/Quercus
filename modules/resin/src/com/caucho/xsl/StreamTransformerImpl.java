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

import com.caucho.vfs.WriteStream;

import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamTransformerImpl extends TransformerImpl {
  StreamTransformerImpl(StylesheetImpl stylesheet)
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
  
  public void transform(InputStream source, OutputStream os)
    throws SAXException, IOException, TransformerException
  {
    transform(parseDocument(source, null), os);
  }
  
  public void transform(String systemId, OutputStream os)
    throws SAXException, IOException, TransformerException
  {
    transform(parseDocument(systemId), os);
  }
  
  public void transformString(String source, OutputStream os)
    throws SAXException, IOException, TransformerException
  {
    transform(parseStringDocument(source, null), os);
  }
  
  private void printText(WriteStream out, Node top)
    throws IOException
  {
    for (; top != null; top = top.getNextSibling()) {
      if (top instanceof Text)
        out.print(top.getNodeValue());
      else
        printText(out, top.getFirstChild());
    }
  }
}
