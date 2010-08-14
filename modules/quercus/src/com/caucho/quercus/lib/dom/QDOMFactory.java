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
 * @author Sam
 */

package com.caucho.quercus.lib.dom;

import com.caucho.xml.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public class QDOMFactory
  implements DOMFactory
{
  public Attr createAttr(String name)
  {
    return new QAttr(name);
  }

  public Comment createComment()
  {
    return new QComment();
  }

  public Document createDocument()
  {
    return new QDocument();
  }

  public Document createDocument(DocumentType docType)
  {
    return new QDocument(docType);
  }

  public DocumentType createDocumentType(String qualifiedName)
  {
    return new QDocumentType(qualifiedName);
  }

  public DocumentType createDocumentType(String qualifiedName,
                                         String publicId,
                                         String systemId)
  {
    return new QDocumentType(qualifiedName, publicId, systemId);
  }

  public Element createElement(String name)
  {
    return new QElement(name);
  }

  public Element createElement(String name, String namespace)
  {
    return new QElement(name, namespace);
  }

  public EntityReference createEntityReference(String name)
  {
    return new QEntityReference(name);
  }

  public ProcessingInstruction createProcessingInstruction(String name)
  {
    return new QProcessingInstruction(name);
  }

  public Text createText()
  {
    return new QText();
  }

  public org.w3c.dom.DOMImplementation getImplementation()
  {
    return new QDOMImplementation();
  }

  public void parseXMLDocument(Document document, InputStream is, String path)
    throws IOException, SAXException
  {
    Xml xml = new Xml();
    xml.parseDocument((QDocument) document, is, path);
  }

  public void parseHTMLDocument(Document document, InputStream is, String path)
    throws IOException, SAXException
  {
    Html html = new Html();
    html.parseDocument((QDocument) document, is, path);
  }
}
