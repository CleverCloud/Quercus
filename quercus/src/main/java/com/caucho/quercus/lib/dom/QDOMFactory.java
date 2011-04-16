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
        implements DOMFactory {

   @Override
   public Attr createAttr(String name) {
      return new QAttr(name);
   }

   @Override
   public Comment createComment() {
      return new QComment();
   }

   @Override
   public Document createDocument() {
      return new QDocument();
   }

   @Override
   public Document createDocument(DocumentType docType) {
      return new QDocument(docType);
   }

   @Override
   public DocumentType createDocumentType(String qualifiedName) {
      return new QDocumentType(qualifiedName);
   }

   @Override
   public DocumentType createDocumentType(String qualifiedName,
           String publicId,
           String systemId) {
      return new QDocumentType(qualifiedName, publicId, systemId);
   }

   @Override
   public Element createElement(String name) {
      return new QElement(name);
   }

   @Override
   public Element createElement(String name, String namespace) {
      return new QElement(name, namespace);
   }

   @Override
   public EntityReference createEntityReference(String name) {
      return new QEntityReference(name);
   }

   @Override
   public ProcessingInstruction createProcessingInstruction(String name) {
      return new QProcessingInstruction(name);
   }

   @Override
   public Text createText() {
      return new QText();
   }

   @Override
   public org.w3c.dom.DOMImplementation getImplementation() {
      return new QDOMImplementation();
   }

   @Override
   public void parseXMLDocument(Document document, InputStream is, String path)
           throws IOException, SAXException {
      Xml xml = new Xml();
      xml.parseDocument((QDocument) document, is, path);
   }

   @Override
   public void parseHTMLDocument(Document document, InputStream is, String path)
           throws IOException, SAXException {
      Html html = new Html();
      html.parseDocument((QDocument) document, is, path);
   }
}
