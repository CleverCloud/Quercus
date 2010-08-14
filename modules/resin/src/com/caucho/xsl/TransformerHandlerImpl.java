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

import com.caucho.util.L10N;
import com.caucho.xml.DOMBuilder;
import com.caucho.xml.QDocument;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.TransformerHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransformerHandlerImpl extends DOMBuilder
  implements TransformerHandler {
  protected static final Logger log
    = Logger.getLogger(TransformerHandlerImpl.class.getName());
  protected static final L10N L = new L10N(TransformerHandlerImpl.class);
  
  private javax.xml.transform.Transformer _transformer;
  private Result _result;
  
  TransformerHandlerImpl(javax.xml.transform.Transformer transformer)
  {
    _transformer = transformer;

    init(new QDocument());
  }

  public String getSystemId()
  {
    return "asdf";
  }

  public javax.xml.transform.Transformer getTransformer()
  {
    return _transformer;
  }

  public void setResult(Result result)
  {
    _result = result;
  }

  public void endDocument()
    throws SAXException
  {
    super.endDocument();

    Node node = getNode();
    DOMSource source = new DOMSource(node);

    try {
      ((TransformerImpl) _transformer).transform(source, _result);
    } catch (TransformerException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public void notationDecl(String name, String publicId, String systemId)
  {
  }
  
  public void unparsedEntityDecl(String name, String publicId,
                                 String systemId, String notationName)
  {
  }
  
  public void startDTD(String name, String publicId, String systemId)
    throws SAXException
  {
  }
  
  public void endDTD()
    throws SAXException
  {
  }
  
  public void startEntity(String name)
    throws SAXException
  {
  }
  
  public void endEntity(String name)
    throws SAXException
  {
  }
  
  public void startCDATA()
    throws SAXException
  {
  }
  
  public void endCDATA()
    throws SAXException
  {
  }
}
