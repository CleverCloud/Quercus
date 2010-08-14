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

package com.caucho.xml2;

import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

import org.w3c.dom.*;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Implements the top-level document for the XML tree.
 */
public class QDocument extends QDocumentFragment implements CauchoDocument {
  QDOMImplementation _implementation;
  QDocumentType _dtd;
  QElement _element; // top
  HashMap<String,String> _attributes;
  String _encoding = "UTF-8";
  String _version;

  private String _systemId;

  private HashMap<String,String> _namespaces;

  private transient HashMap<NameKey,QName> _nameCache = new HashMap<NameKey,QName>();
  private transient NameKey _nameKey = new NameKey();
  private transient ArrayList<Path> _depends;
  private transient ArrayList<Depend> _dependList;

  int _changeCount;

  // possibly different from the systemId if the DOCTYPE doesn't match
  // the actual file location
  String _rootFilename;
  private boolean _standalone;

  public QDocument()
  {
    _implementation = new QDOMImplementation();
    _owner = this;
  }

  public QDocument(DocumentType docType)
  {
    _owner = this;
    setDoctype(docType);
  }

  public QDocument(QDOMImplementation impl)
  {
    _implementation = impl;
    _owner = this;
  }

  void setAttribute(String name, String value)
  {
    if (name.equals("version"))
      _version = value;
    else if (name.equals("encoding"))
      _encoding = value;
    else {
      if (_attributes == null)
        _attributes = new HashMap<String,String>();
      _attributes.put(name, value);
    }
  }

  public String getRootFilename()
  {
    return _rootFilename;
  }

  public void setRootFilename(String filename)
  {
    _rootFilename = filename;
  }

  public void setSystemId(String systemId)
  {
    _systemId = systemId;
  }

  public String getSystemId()
  {
    return _systemId;
  }

  /**
   * Returns the base URI of the node.
   */
  public String getBaseURI()
  {
    return getSystemId();
  }

  public Document getOwnerDocument()
  {
    return null;
  }

  public DOMConfiguration getDomConfig()
  {
    return null;
  }

  public boolean isSupported(String feature, String version)
  {
    return _owner.getImplementation().hasFeature(feature, version);
  }

  /**
   * The node name for the document is #document.
   */
  public String getNodeName()
  {
    return "#document";
  }

  public short getNodeType()
  {
    return DOCUMENT_NODE;
  }

  protected Node copyNode(QDocument newNode, boolean deep)
  {
    newNode._dtd = _dtd;
    newNode._element = _element;

    return newNode;
  }

  /**
   * Returns a clone of the document.
   *
   * @param deep if true, recursively copy the document.
   */
  public Node cloneNode(boolean deep)
  {
    QDocument newDoc = new QDocument();

    newDoc._implementation = _implementation;
    newDoc._dtd = _dtd;
    if (_attributes != null)
      newDoc._attributes = (HashMap) _attributes.clone();
    newDoc._encoding = _encoding;
    newDoc._version = _version;

    if (_namespaces != null)
      newDoc._namespaces = (HashMap) _namespaces.clone();

    if (deep) {
      for (Node node = getFirstChild();
           node != null;
           node = node.getNextSibling()) {
        newDoc.appendChild(newDoc.importNode(node, true));
      }
    }

    return newDoc;
  }

  Node importNode(QDocument doc, boolean deep)
  {
    return null;
  }

  /**
   * Imports a copy of a node into the current document.
   *
   * @param node the node to import/copy
   * @param deep if true, recursively copy the children.
   *
   * @return the new imported node.
   */
  public Node importNode(Node node, boolean deep)
  {
    if (node == null)
      return null;

    QName name;

    switch (node.getNodeType()) {
    case ELEMENT_NODE:
      return importElement((Element) node, deep);

    case ATTRIBUTE_NODE:
      Attr attr = (Attr) node;
      name = createName(attr.getNamespaceURI(), attr.getNodeName());
      QAttr newAttr = new QAttr(name, attr.getNodeValue());
      newAttr._owner = this;
      return newAttr;

    case TEXT_NODE:
      QText newText = new QText(node.getNodeValue());
      newText._owner = this;
      return newText;

    case CDATA_SECTION_NODE:
      QCdata newCData = new QCdata(node.getNodeValue());
      newCData._owner = this;
      return newCData;

    case ENTITY_REFERENCE_NODE:
      QEntityReference newER = new QEntityReference(node.getNodeName());
      newER._owner = this;
      return newER;

    case ENTITY_NODE:
      Entity oldEntity = (Entity) node;
      QEntity newEntity = new QEntity(oldEntity.getNodeName(),
                                      oldEntity.getNodeValue(),
                                      oldEntity.getPublicId(),
                                      oldEntity.getSystemId());
      newEntity._owner = this;
      return newEntity;

    case PROCESSING_INSTRUCTION_NODE:
      QProcessingInstruction newPI;
      newPI = new QProcessingInstruction(node.getNodeName(),
                                         node.getNodeValue());

      newPI._owner = this;
      return newPI;

    case COMMENT_NODE:
      QComment newComment = new QComment(node.getNodeValue());
      newComment._owner = this;
      return newComment;

    case DOCUMENT_FRAGMENT_NODE:
      return importFragment((DocumentFragment) node, deep);

    default:
      throw new UnsupportedOperationException(String.valueOf(node));
    }
  }

  /**
   * Imports an element.
   */
  private Element importElement(Element elt, boolean deep)
  {
    QElement newElt = new QElement(createName(elt.getNamespaceURI(),
                                              elt.getNodeName()));
    QElement oldElt = null;

    if (elt instanceof QElement)
      oldElt = (QElement) elt;

    newElt._owner = this;

    if (oldElt != null) {
      newElt._filename = oldElt._filename;
      newElt._line = oldElt._line;
    }

    NamedNodeMap attrs = elt.getAttributes();

    int len = attrs.getLength();
    for (int i = 0; i < len; i++) {
      Attr attr = (Attr) attrs.item(i);

      newElt.setAttributeNode((Attr) importNode(attr, deep));
    }

    if (! deep)
      return newElt;

    for (Node node = elt.getFirstChild();
         node != null;
         node = node.getNextSibling()) {
      newElt.appendChild(importNode(node, true));
    }

    return newElt;
  }

  /**
   * Imports an element.
   */
  private DocumentFragment importFragment(DocumentFragment elt, boolean deep)
  {
    QDocumentFragment newFrag = new QDocumentFragment();

    newFrag._owner = this;

    if (! deep)
      return newFrag;

    for (Node node = elt.getFirstChild();
         node != null;
         node = node.getNextSibling()) {
      newFrag.appendChild(importNode(node, true));
    }

    return newFrag;
  }

  public DocumentType getDoctype() { return _dtd; }

  public void setDoctype(DocumentType dtd)
  {
    QDocumentType qdtd = (QDocumentType) dtd;

    _dtd = qdtd;
    if (qdtd != null)
      qdtd._owner = this;
  }

  public String getEncoding()
  {
    if (_encoding == null)
      return null;
    else
      return _encoding;
  }

  public DOMImplementation getImplementation()
  {
    return _implementation;
  }

  public Element getDocumentElement()
  {
    return _element;
  }

  public void setDocumentElement(Element elt)
  {
    _element = (QElement) elt;
  }

  /**
   * Creates a new element
   */
  public Element createElement(String tagName)
    throws DOMException
  {
    if (! isNameValid(tagName))
      throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
                              "illegal tag `" + tagName + "'");

    QElement elt = new QElement(createName(null, tagName));
    elt._owner = this;

    return elt;
  }

  /**
   * Creates a new namespace-aware element
   */
  public Element createElementNS(String namespaceURI, String name)
    throws DOMException
  {
    QName qname = createName(namespaceURI, name);

    validateName(qname);
    addNamespace(qname);

    QElement elt = new QElement(qname);
    elt._owner = this;

    return elt;
  }

  public void validateName(QName qname)
    throws DOMException
  {
    String prefix = qname.getPrefix();
    String namespaceURI = qname.getNamespaceURI();

    if (qname.getPrefix() == "") {
    }
    else if (prefix == "xml" &&
             namespaceURI != "http://www.w3.org/XML/1998/namespace")
      throw new DOMException(DOMException.NAMESPACE_ERR,
                             L.l("`xml' prefix expects namespace uri 'http://www.w3.org/XML/1998/namespace'"));
    else if (prefix != "" && prefix != null && namespaceURI == null)
      throw new DOMException(DOMException.NAMESPACE_ERR,
                             L.l("`{0}' prefix expects a namespace uri",
                                 prefix));

  }

  /**
   * Creates a new namespace-aware element
   */
  public Element createElement(String prefix, String local, String url)
    throws DOMException
  {
    QName name = new QName(prefix, local, url);
    addNamespace(name);

    QElement elt = new QElement(name);
    elt._owner = this;

    return elt;
  }

  public Element createElementByName(QName name)
    throws DOMException
  {
    QElement elt = new QElement(name);
    elt._owner = this;

    return elt;
  }

  /**
   * Creates a new document fragment.
   */
  public DocumentFragment createDocumentFragment()
  {
    QDocumentFragment frag = new QDocumentFragment();
    frag._owner = this;

    return frag;
  }

  /**
   * Creates a new text node in this document.
   */
  public Text createTextNode(String data)
  {
    if (data == null)
      data = "";

    QText text = new QText(data);
    text._owner = this;

    return text;
  }

  public Text createUnescapedTextNode(String data)
  {
    if (data == null)
      data = "";

    QText text = new QUnescapedText(data);
    text._owner = this;

    return text;
  }

  public Comment createComment(String data)
  {
    if (data == null)
      data = "";

    QComment comment = new QComment(data);
    comment._owner = this;

    return comment;
  }

  public CDATASection createCDATASection(String data)
  {
    if (data == null)
      data = "";

    QCdata cdata = new QCdata(data);
    cdata._owner = this;

    return cdata;
  }

  public ProcessingInstruction createProcessingInstruction(String target,
                                                           String data)
    throws DOMException
  {
    if (target == null || target.length() == 0)
      throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
                              L.l("Empty processing instruction name.  The processing instruction syntax is: <?name ... ?>"));

    if (! isNameValid(target))
      throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
                              L.l("`{0}' is an invalid processing instruction name.  The processing instruction syntax is: <?name ... ?>", target));

    if (data == null)
      data = "";

    QProcessingInstruction pi = new QProcessingInstruction(target, data);
    pi._owner = this;

    return pi;
  }

  public Attr createAttribute(String name, String value)
    throws DOMException
  {
    if (! isNameValid(name))
      throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
                              "illegal attribute `" + name + "'");

    if (value == null)
      value = "";

    QAttr attr = new QAttr(new QName(null, name, null), value);
    attr._owner = this;

    return attr;
  }

  public Attr createAttribute(String name)
    throws DOMException
  {
    return createAttribute(name, null);
  }

  /**
   * Creates a new namespace-aware attribute
   */
  public Attr createAttribute(String prefix, String local, String url)
    throws DOMException
  {
    QName name = new QName(prefix, local, url);
    if (url != null && ! url.equals(""))
      addNamespace(prefix, url);

    QAttr attr = new QAttr(name, null);
    attr._owner = this;

    return attr;
  }

  /**
   * Creates a new namespace-aware attribute
   */
  public Attr createAttributeNS(String namespaceURI, String qualifiedName)
    throws DOMException
  {
    QName qname = createName(namespaceURI, qualifiedName);

    validateName(qname);
    addNamespace(qname);

    /* xml/0213
    else if (name.getNamespace() == "")
      throw new DOMException(DOMException.NAMESPACE_ERR,
                             L.l("`{0}' prefix expects a namespace uri",
                                 name.getPrefix()));
    */

    QAttr attr = new QAttr(qname, null);
    attr._owner = this;

    return attr;
  }

  public QName createName(String uri, String name)
  {
    _nameKey.init(name, uri);
    QName qName = _nameCache.get(_nameKey);

    if (qName != null)
      return qName;

    if (uri == null) {
      qName = new QName(null, name, null);
    }
    else {
      int p = name.indexOf(':');
      String prefix;
      String local;
      if (p < 0) {
        prefix = null;
        local = name;
      }
      else {
        prefix = name.substring(0, p);
        local = name.substring(p + 1);
      }

      qName = new QName(prefix, local, uri);
    }

    _nameCache.put(new NameKey(name, uri), qName);

    return qName;
  }

  /**
   * Creates a new namespace-aware attribute
   */
  public Attr createAttribute(QName name, String value)
    throws DOMException
  {
    String url = name.getNamespaceURI();

    if (url != null && url != "") {
      addNamespace(name.getPrefix(), url);
    }

    QAttr attr = new QAttr(name, value);
    attr._owner = this;

    return attr;
  }

  public EntityReference createEntityReference(String name)
    throws DOMException
  {
    if (! isNameValid(name))
      throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
                              "illegal entityReference `" + name + "'");

    QEntityReference er = new QEntityReference(name);
    er._owner = this;

    return er;
  }

  /**
   * Returns a list of elements, filtered by the tag name.
   */
  public NodeList getElementsByTagName(String name)
  {
    if (_element == null)
      return new QDeepNodeList(null, null, null);
    else
      return new QDeepNodeList(_element, _element, new QElement.TagPredicate(name));
  }

  public NodeList getElementsByTagNameNS(String uri, String name)
  {
    if (_element == null)
      return new QDeepNodeList(null, null, null);
    else
      return new QDeepNodeList(_element, _element, new QElement.NSTagPredicate(uri, name));
  }

  public Element getElementById(String name)
  {
    Node node = _element;

    for (; node != null; node = XmlUtil.getNext(node)) {
      if (node instanceof Element) {
        Element elt = (Element) node;

        String id = elt.getAttribute("id");

        if (name.equals(id))
          return elt;
      }
    }

    return null;
  }

  static public Document create()
  {
    QDocument doc = new QDocument();
    doc._masterDoc = doc;

    return doc;
  }

  void setAttributes(HashMap<String,String> attributes)
  {
    _attributes = attributes;
  }

  public Node appendChild(Node newChild) throws DOMException
  {
    if (newChild instanceof Element) {
      _element = (QElement) newChild;

      // xml/0201
      if (false && _namespaces != null) {
        Iterator<String> iter = _namespaces.keySet().iterator();

        while (iter.hasNext()) {
          String prefix = iter.next();
          String ns = _namespaces.get(prefix);

          String xmlns;

          if (prefix.equals(""))
            xmlns = "xmlns";
          else
            xmlns = "xmlns:" + prefix;

          if (_element.getAttribute(xmlns).equals("")) {
            QName qName = new QName(xmlns, XmlParser.XMLNS);
            _element.setAttributeNode(createAttribute(qName, ns));
          }
        }
      }
    }

    return super.appendChild(newChild);
  }

  public Node removeChild(Node oldChild) throws DOMException
  {
    Node value = super.removeChild(oldChild);
    if (oldChild == _element)
      _element = null;
    return value;
  }

  // non-DOM

  public void addNamespace(QName qname)
  {
    addNamespace(qname.getPrefix(), qname.getNamespaceURI());
  }

  /**
   * Add a namespace declaration to a document.  If the declaration
   * prefix already has a namespace, the old one wins.
   */
  public void addNamespace(String prefix, String url)
  {
    if (url == null
        || url.length() == 0
        || XmlParser.XMLNS.equals(url)
        || XmlParser.XML.equals(url))
    {
      return;
    }

    if (prefix == null)
      prefix = "";

    if (_namespaces == null)
      _namespaces = new HashMap<String,String>();

    String old = _namespaces.get(prefix);
    if (old == null)
      _namespaces.put(prefix, url.intern());
  }

  public HashMap<String,String> getNamespaces()
  {
    return _namespaces;
  }

  /**
   * Returns the namespace url for a given prefix.
   */
  public String getNamespace(String prefix)
  {
    if (_namespaces == null)
      return null;
    else
      return _namespaces.get(prefix);
  }

  /**
   * Returns an iterator of top-level namespace prefixes.
   */
  public Iterator<String> getNamespaceKeys()
  {
    if (_namespaces == null)
      return null;

    return _namespaces.keySet().iterator();
  }

  public Object getProperty(String name)
  {
    if (name.equals(DEPENDS))
      return _depends;
    else
      return null;
  }

  public ArrayList<Path> getDependList()
  {
    return _depends;
  }

  public ArrayList<Depend> getDependencyList()
  {
    return _dependList;
  }

  public void setProperty(String name, Object value)
  {
    if (name.equals(DEPENDS))
      _depends = (ArrayList) value;
  }

  // DOM LEVEL 3
  public String getActualEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public void setActualEncoding(String actualEncoding)
  {
    throw new UnsupportedOperationException();
  }
/*  
  public String getEncoding()
  {
    throw new UnsupportedOperationException();
  }
*/

  public void setEncoding(String encoding)
  {
    throw new UnsupportedOperationException();
  }

  public boolean getStandalone()
  {
    return _standalone;
  }

  public void setStandalone(boolean standalone)
  {
    _standalone = true;
  }

  public String getXmlVersion()
  {
    return _version;
  }

  public void setXmlVersion(String version)
    throws DOMException
  {
    _version = version;
  }

  public void setXmlStandalone(boolean value)
    throws DOMException
  {
  }

  public TypeInfo getSchemaTypeInfo()
  {
    return null;
  }

  public String getXmlEncoding()
  {
    return null;
  }

  public String getInputEncoding()
  {
    return null;
  }

  public boolean getXmlStandalone()
    throws DOMException
  {
    return false;
  }

  public boolean getStrictErrorChecking()
  {
    throw new UnsupportedOperationException();
  }

  public void setStrictErrorChecking(boolean strictErrorChecking)
  {
    throw new UnsupportedOperationException();
  }

  public DOMErrorHandler getErrorHandler()
  {
    throw new UnsupportedOperationException();
  }

  public void setErrorHandler(DOMErrorHandler errorHandler)
  {
    throw new UnsupportedOperationException();
  }

  public String getDocumentURI()
  {
    throw new UnsupportedOperationException();
  }

  public void setDocumentURI(String documentURI)
  {
    throw new UnsupportedOperationException();
  }

  public Node adoptNode(Node source)
    throws DOMException
  {
    throw new UnsupportedOperationException();
  }

  public void normalizeDocument()
  {
    throw new UnsupportedOperationException();
  }

  public boolean canSetNormalizationFeature(String name,
                                            boolean state)
  {
    throw new UnsupportedOperationException();
  }

  public void setNormalizationFeature(String name,
                                      boolean state)
    throws DOMException
  {
    throw new UnsupportedOperationException();
  }

  public boolean getNormalizationFeature(String name)
    throws DOMException
  {
    throw new UnsupportedOperationException();
  }

  public Node renameNode(Node n,
                         String namespaceURI,
                         String name)
    throws DOMException
  {
      throw new UnsupportedOperationException();
  }

  // CAUCHO

  public void addDepend(Path path)
  {
    if (path == null)
      return;

    if (_depends == null)
      _depends = new ArrayList<Path>();

    if (! _depends.contains(path)) {
      _depends.add(path);

      if (_dependList == null)
        _dependList = new ArrayList<Depend>();

      _dependList.add(new Depend(path));
    }
  }

  public boolean isModified()
  {
    if (_dependList == null)
      return false;

    for (int i = 0; i < _dependList.size(); i++) {
      Depend depend = _dependList.get(i);

      if (depend.isModified())
        return true;
    }

    return false;
  }

  void print(XmlPrinter os) throws IOException
  {
    os.startDocument(this);

    if (_namespaces != null) {
      Iterator<String> iter = _namespaces.keySet().iterator();
      while (iter.hasNext()) {
        String prefix = iter.next();
        String url = _namespaces.get(prefix);

        if (prefix.equals(""))
          os.attribute(null, prefix, "xmlns", url);
        else
          os.attribute(null, prefix, "xmlns:" + prefix, url);
      }
    }

    if (getFirstChild() == null)
      os.printHeader(null);

    for (Node node = getFirstChild();
         node != null;
         node = node.getNextSibling()) {
      ((QAbstractNode) node).print(os);
      if (os.isPretty())
        os.println();
    }

    os.endDocument();
  }

  public String toString()
  {
    String topElt = _element == null ? "XXX:top" : _element.getNodeName();

    if (_dtd == null)
      return "Document[" + topElt + "]";

    if (_dtd.getPublicId() != null && _dtd.getSystemId() != null)
      return ("Document[" + topElt + " PUBLIC '" + _dtd.getPublicId() + "' '" +
              _dtd.getSystemId() + "']");
    else if (_dtd._publicId != null)
      return "Document[" + topElt + " PUBLIC '" + _dtd.getPublicId() + "']";
    else if (_dtd.getSystemId() != null)
      return "Document[" + topElt + " SYSTEM '" + _dtd.getSystemId() + "']";
    else
      return "Document[" + topElt + "]";
  }

  static class NameKey {
    String _qName;
    String _url;

    NameKey()
    {
    }

    NameKey(String qName, String url)
    {
      init(qName, url);
    }

    void init(String qName, String url)
    {
      if (qName == null)
        throw new NullPointerException();

      if (url == null)
        url = "";

      _qName = qName;
      _url = url;
    }

    public int hashCode()
    {
      return 65521 * _url.hashCode() + _qName.hashCode();
    }

    public boolean equals(Object b)
    {
      if (! (b instanceof NameKey))
        return false;

      NameKey key = (NameKey) b;

      return _qName.equals(key._qName) && _url.equals(key._url);
    }
  }

  private Object writeReplace()
  {
    return new SerializedXml(this);
  }
}
