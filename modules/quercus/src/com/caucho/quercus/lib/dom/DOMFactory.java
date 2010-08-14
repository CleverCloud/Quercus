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

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public interface DOMFactory {
  Attr createAttr(String name);

  Comment createComment();

  Document createDocument();

  Document createDocument(DocumentType docType);

  DocumentType createDocumentType(String qualifiedName);

  DocumentType createDocumentType(String qualifiedName,
                                  String publicId,
                                  String systemId);

  Element createElement(String name);

  Element createElement(String name, String namespace);

  EntityReference createEntityReference(String name);

  ProcessingInstruction createProcessingInstruction(String name);

  Text createText();

  org.w3c.dom.DOMImplementation getImplementation();

  void parseXMLDocument(Document document, InputStream is, String path)
    throws IOException, SAXException;

  void parseHTMLDocument(Document document, InputStream is, String path)
    throws IOException, SAXException;
}
