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
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.transform.TransformerException;
import java.io.IOException;

public class SAXFilterImpl implements XMLFilter {
  protected static L10N L = new L10N(SAXFilterImpl.class);

  protected TransformerImpl transformer;

  private XMLReader parent;
  private ContentHandler contentHandler;
  private LexicalHandler lexicalHandler;
  private ErrorHandler errorHandler;

  protected SAXFilterImpl(TransformerImpl transformer)
  {
    this.transformer = transformer;
  }
  
  public void setParent(XMLReader parent)
  {
    this.parent = parent;
  }
  
  public XMLReader getParent()
  {
    return parent;
  }
  
  public boolean getFeature(String name)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    return false;
  }
  
  public void setFeature(String name, boolean value)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
  }
  
  public Object getProperty(String name)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    return null;
  }
  
  public void setProperty(String name, Object value)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
  }
  
  public void setEntityResolver(EntityResolver resolver)
  {
  }
  
  public EntityResolver getEntityResolver()
  {
    return null;
  }
  
  public void setDTDHandler(DTDHandler handler)
  {
  }
  
  public DTDHandler getDTDHandler()
  {
    return null;
  }
  
  public void setContentHandler(ContentHandler handler)
  {
    this.contentHandler = handler;
  }
  
  public ContentHandler getContentHandler()
  {
    return contentHandler;
  }
  
  public void setErrorHandler(ErrorHandler handler)
  {
    this.errorHandler = handler;
  }
  
  public ErrorHandler getErrorHandler()
  {
    return errorHandler;
  }
  
  public void parse(InputSource input)
    throws IOException, SAXException
  {
    DOMBuilder builder = new DOMBuilder();
    Node sourceNode = new QDocument();
    builder.init(sourceNode);

    parent.setContentHandler(builder);
    parent.parse(input);
    
    try {
      transformer.transform(sourceNode, contentHandler, lexicalHandler);
    } catch (TransformerException e) {
      throw new SAXException(String.valueOf(e));
    }
  }
  
  public void parse(String systemId)
    throws IOException, SAXException
  {
    DOMBuilder builder = new DOMBuilder();
    Node sourceNode = new QDocument();
    builder.init(sourceNode);

    parent.setContentHandler(builder);
    
    parent.parse(systemId);

    try {
      transformer.transform(sourceNode, contentHandler, lexicalHandler);
    } catch (TransformerException e) {
      throw new SAXException(String.valueOf(e));
    }
  }
}
