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

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.Env;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.IdentityHashMap;

public class DOMImplementation
{
  private static L10N L = new L10N(DOMImplementation.class);

  private final IdentityHashMap<Object, Object> _wrapperMap =
      new IdentityHashMap<Object, Object>();
  private final DOMFactory _factory;

  final org.w3c.dom.DOMImplementation _delegate;

  static DOMImplementation get(Env env)
  {
    DOMImplementation impl
      = (DOMImplementation) env.getSpecialValue("caucho.dom");

    if (impl == null) {
      impl = new DOMImplementation();
      env.setSpecialValue("caucho.dom", impl);
    }

    return impl;
  }

  public DOMImplementation()
  {
    _factory = new QDOMFactory();
    _delegate = _factory.getImplementation();
  }

  static public boolean hasFeature(Env env, String feature, String version)
  {
    return get(env)._delegate.hasFeature(feature, version);
  }

  static public DOMDocument createDocument(Env env,
                                           @Optional String namespaceURI,
                                           @Optional String name,
                                           @Optional DOMDocumentType docType)
  {
    DOMDocument doc;

    if (docType != null)
      doc = get(env).createDocument(docType);
    else
      doc = get(env).createDocument();

    if (name != null && name.length() > 0) {
      DOMElement elt;

      if (namespaceURI != null && namespaceURI.length() > 0)
        elt = doc.createElementNS(namespaceURI, name);
      else
        elt = doc.createElement(name);

      doc.appendChild(elt);
    }

    return doc;
  }

  @ReturnNullAsFalse
  static public DOMDocumentType createDocumentType(
      Env env,
      @NotNull String qualifiedName,
      @Optional String publicId,
      @Optional String systemId) {
    if (qualifiedName == null)
      return null;

    if ((publicId != null && publicId.length() > 0)
        && (publicId != null && publicId.length() > 0))
      return get(env).createDocumentType(qualifiedName, publicId, systemId);
    else
      return get(env).createDocumentType(qualifiedName);
  }


  DOMAttr createWrapper(Attr node)
  {
    DOMAttr wrapper = new DOMAttr(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMCDATASection createWrapper(CDATASection node)
  {
    DOMCDATASection wrapper = new DOMCDATASection(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMComment createWrapper(Comment node)
  {
    DOMComment wrapper = new DOMComment(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMDocument createWrapper(Document node)
  {
    DOMDocument wrapper = new DOMDocument(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMDocumentFragment createWrapper(DocumentFragment node)
  {
    DOMDocumentFragment wrapper = new DOMDocumentFragment(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMDocumentType createWrapper(DocumentType node)
  {
    DOMDocumentType wrapper = new DOMDocumentType(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMConfiguration createWrapper(org.w3c.dom.DOMConfiguration node)
  {
    DOMConfiguration wrapper = new DOMConfiguration(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMException createWrapper(org.w3c.dom.DOMException node)
  {
    DOMException wrapper = new DOMException(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMElement createWrapper(Element node)
  {
    DOMElement wrapper = new DOMElement(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMEntityReference createWrapper(EntityReference node)
  {
    DOMEntityReference wrapper = new DOMEntityReference(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMNamedNodeMap createWrapper(NamedNodeMap node)
  {
    DOMNamedNodeMap wrapper = new DOMNamedNodeMap(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMNodeList createWrapper(NodeList node)
  {
    DOMNodeList wrapper = new DOMNodeList(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMNotation createWrapper(Notation node)
  {
    DOMNotation wrapper = new DOMNotation(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMProcessingInstruction createWrapper(ProcessingInstruction node)
  {
    DOMProcessingInstruction wrapper = new DOMProcessingInstruction(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMStringList createWrapper(org.w3c.dom.DOMStringList node)
  {
    DOMStringList wrapper = new DOMStringList(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMText createWrapper(Text node)
  {
    DOMText wrapper = new DOMText(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  DOMTypeInfo createWrapper(TypeInfo node)
  {
    DOMTypeInfo wrapper = new DOMTypeInfo(this, node);
    _wrapperMap.put(node, wrapper);

    return wrapper;
  }

  Object getWrapper(Object obj)
  {
    if (obj == null)
      return null;
    
    Object wrapper;

    if (obj instanceof NodeList)
      wrapper = createWrapper((NodeList) obj);
    else {
      wrapper = _wrapperMap.get(obj);

      if (wrapper == null) {
        if (obj instanceof Attr)
          wrapper = createWrapper((Attr) obj);
        else if (obj instanceof CDATASection)
          wrapper = createWrapper((CDATASection) obj);
        else if (obj instanceof Comment)
          wrapper = createWrapper((Comment) obj);
        else if (obj instanceof Document)
          wrapper = createWrapper((Document) obj);
        else if (obj instanceof DocumentFragment)
          wrapper = createWrapper((DocumentFragment) obj);
        else if (obj instanceof DocumentType)
          wrapper = createWrapper((DocumentType) obj);
        else if (obj instanceof org.w3c.dom.DOMConfiguration)
          wrapper = createWrapper((org.w3c.dom.DOMConfiguration) obj);
        else if (obj instanceof org.w3c.dom.DOMException)
          wrapper = createWrapper((org.w3c.dom.DOMException) obj);
        else if (obj instanceof Element)
          wrapper = createWrapper((Element) obj);
        else if (obj instanceof EntityReference)
          wrapper = createWrapper((EntityReference) obj);
        else if (obj instanceof NamedNodeMap)
          wrapper = createWrapper((NamedNodeMap) obj);
        else if (obj instanceof Notation)
          wrapper = createWrapper((Notation) obj);
        else if (obj instanceof ProcessingInstruction)
          wrapper = createWrapper((ProcessingInstruction) obj);
        else if (obj instanceof org.w3c.dom.DOMStringList)
          wrapper = createWrapper((org.w3c.dom.DOMStringList) obj);
        else if (obj instanceof Text)
          wrapper = createWrapper((Text) obj);
        else if (obj instanceof TypeInfo)
          wrapper = createWrapper((TypeInfo) obj);
        else
          throw new UnimplementedException(
              L.l("cannot wrap element of type {0}", obj.getClass().getName()));

        _wrapperMap.put(obj, wrapper);
      }
    }

    return wrapper;
  }

  public String toString()
  {
    return getClass().getSimpleName();
  }

  DOMAttr createAttr(String name)
  {
    return createWrapper(_factory.createAttr(name));
  }

  public DOMComment createComment()
  {
    return createWrapper(_factory.createComment());
  }

  public DOMDocument createDocument()
  {
    return createWrapper(_factory.createDocument());
  }

  public DOMDocument createDocument(DOMDocumentType docType)
  {
    return createWrapper(_factory.createDocument(docType._delegate));
  }

  public DOMDocumentType createDocumentType(String qualifiedName)
  {
    return createWrapper(_factory.createDocumentType(qualifiedName));
  }

  public DOMDocumentType createDocumentType(String qualifiedName,
                                            String publicId,
                                            String systemId)
  {
    return createWrapper(_factory.createDocumentType(qualifiedName,
                                                     publicId,
                                                     systemId));
  }

  public DOMElement createElement(String name)
  {
    return createWrapper(_factory.createElement(name));
  }

  public DOMElement createElement(String name, String namespace)
  {
    return createWrapper(_factory.createElement(name, namespace));
  }

  public DOMEntityReference createEntityReference(String name)
  {
    return createWrapper(_factory.createEntityReference(name));
  }

  public DOMProcessingInstruction createProcessingInstruction(String name)
  {
    return createWrapper(_factory.createProcessingInstruction(name));
  }

  public DOMText createText()
  {
    return createWrapper(_factory.createText());
  }

  public void parseHTMLDocument(Document document, ReadStream is, String path)
    throws IOException, SAXException
  {
    _factory.parseHTMLDocument(document, is, path);
  }

  public void parseXMLDocument(Document document, ReadStream is, String path)
    throws IOException, SAXException
  {
    _factory.parseXMLDocument(document, is, path);
  }
}
