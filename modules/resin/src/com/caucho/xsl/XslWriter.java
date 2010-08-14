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

import com.caucho.util.CharBuffer;
import com.caucho.util.IntArray;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.xml.*;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.NamespaceContext;
import com.caucho.xpath.XPathException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writer stream for generating stylesheet output.
 *
 * <p>Because XSL produces an XML tree, XslWriter contains extra
 * methods for constructing the tree.
 *
 * <p>The writer methods, e.g. println, add to the current text node.
 *
 * <p>In addition, stylesheets can access variables through getPwd and
 * getPage.
 */
public class XslWriter extends Writer implements ExtendedLocator {
  private static final Logger log
   = Logger.getLogger(XslWriter.class.getName());
  static final L10N L = new L10N(XslWriter.class);

  // This is the value Axis wants
  private final static String XMLNS = "http://www.w3.org/2000/xmlns/";

  private final static XMLWriter ATTR_WRITER = new DOMBuilder();

  private XMLWriter _xmlWriter;
  
  String _systemId;
  String _filename;
  int _line;
  int _tailLine;

  private IntArray flags = new IntArray();
  
  private CharBuffer _text = new CharBuffer();
  private String elementName;

  private String _attributeURL;
  private String _attributePrefix;
  private String _attributeLocalName;
  private String _attributeName;

  private ArrayList depends = new ArrayList();
  private boolean _isCacheable = true;
  private boolean _disableEscaping;

  private boolean generateLocation;

  private Document _document;

  private StylesheetImpl _stylesheet;
  private TransformerImpl _transformer;

  private HashMap<String,String> _cdataElements;
  private boolean isCdata;

  private HashMap<String,String> _namespaces;
  private ArrayList<String> _topNamespaces;
  private ArrayList<StackItem> _elementStack;
  private int _depth;

  private ExtendedLocator _locator = null;

  XslWriter(HashMap env,
            StylesheetImpl stylesheet,
            TransformerImpl transformer)
  {
    _stylesheet = stylesheet;
    _transformer = transformer;

    ArrayList<String> cdata = stylesheet.getOutputFormat().getCdataSectionElements();
    if (cdata != null) {
      _cdataElements = new HashMap<String,String>();
      
      for (int i = 0; i < cdata.size(); i++) {
        String element = cdata.get(i);

        _cdataElements.put(element, element);
      }
    }
  }

  void init(XMLWriter xmlWriter)
  {
    _xmlWriter = xmlWriter;
    _namespaces = new HashMap<String,String>();
    _topNamespaces = new ArrayList<String>();
    _elementStack = new ArrayList<StackItem>();

    _document = null;

    _locator = this;
    xmlWriter.setDocumentLocator(_locator);
  }

  public TransformerImpl getTransformer()
  {
    return _transformer;
  }

  /**
   * Returns true if the generated stylesheet is currently cacheable.
   */
  boolean isCacheable()
  {
    return _isCacheable;
  }

  /**
   * Returns the Path dependency list of the generated stylesheet.
   */
  ArrayList getDepends()
  {
    return depends;
  }

  /**
   * Indicate that the result document is not cacheable.
   */
  public void setNotCacheable()
  {
    _isCacheable = false;
  }

  /**
   * Add a dependency to the result document.  When the result is checked
   * for modification, this path will also be checked.
   */
  public void addCacheDepend(Path path)
  {
    _transformer.addCacheDepend(path);
  }

  /**
   * Implementation function so jsp:decl tags aren't repeated.
   */
  public boolean isFlagFirst(int id)
  {
    while (flags.size() <= id)
      flags.add(0);

    int value = flags.get(id);
    flags.set(id, 1);

    return value == 0;
  }

  /**
   * Adds a byte to the current text node.
   */
  public void write(int ch)
  {
    _text.append((char) ch);
  }
  /**
   * Adds a byte buffer to the current text node.
   */
  public void write(byte []buf, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      write(buf[offset + i]);
  }

  /**
   * Adds a char buffer to the current text node.
   */
  public void write(char []buf, int offset, int length)
  {
    _text.append(buf, offset, length);
  }

  /**
   * Adds a string to the current text node.
   */
  public void print(String string)
  {
    if (string == null) {
      _text.append("null");
      return;
    }

    _text.append(string);
  }

  /**
   * Adds a boolean to the current text node.
   */
  public void print(boolean b)
  {
    _text.append(b);
  }

  /**
   * Adds a character to the current text node.
   */
  public void print(char ch)
  {
    _text.append(ch);
  }

  /**
   * Adds an integer to the current text node.
   */
  public void print(int i)
  {
    _text.append(i);
  }

  /**
   * Adds an integer to the current text node.
   */
  public void print(long l)
  {
    _text.append(l);
  }

  /**
   * Adds a float to the current text node.
   */
  public void print(float f)
  {
    _text.append(f);
  }

  /**
   * Adds a double to the current text node.
   */
  public void print(double d)
  {
    _text.append(d);
  }

  /**
   * Adds an object to the current text node, converted by
   * String.valueOf.
   */
  public void print(Object o)
  {
    _text.append(o);
  }
  /**
   * Adds a newline to the current text node.
   */
  public void println()
  {
    _text.append('\n');
    _tailLine++;
  }

  /**
   * Adds a boolean to the current text node.
   */
  public void println(boolean b)
  {
    _text.append(b);
    println();
  }

  /**
   * Adds a string to the current text node.
   */
  public void println(String s)
  {
    print(s);
    println();
  }

  /**
   * Adds a character to the current text node.
   */
  public void println(char ch)
  {
    print(ch);
    println();
  }

  /**
   * Adds an integer to the current text node.
   */
  public void println(int i)
  {
    _text.append(i);
    println();
  }

  /**
   * Adds a long to the current text node.
   */
  public void println(long l)
  {
    _text.append(l);
    println();
  }

  /**
   * Adds a double to the current text node.
   */
  public void println(double d)
  {
    _text.append(d);
    println();
  }

  /**
   * Adds a float to the current text node.
   */
  public void println(float f)
  {
    _text.append(f);
    println();
  }

  /**
   * Adds an object to the current text node, converted by String.valueOf.
   */
  public void println(Object o)
  {
    _text.append(o);
    println();
  }

  /**
   * flush is meaningless for XslWriter.  It's only added to conform to Writer.
   */
  public void flush()
  {
  }

  public void close()
    throws IOException
  {
    try {
      for (int i = 0; i < _topNamespaces.size(); i++) {
        String topPrefix = _topNamespaces.get(i);
        String topUrl = _namespaces.get(topPrefix);

        if (topPrefix.equals(""))
          _xmlWriter.endPrefixMapping(null);
        else
          _xmlWriter.endPrefixMapping(topPrefix);
      }

      popText();
      _xmlWriter.endDocument();
    } catch (SAXException e) {
      throw new IOException(e.toString());
    }
  }

  public boolean getDisableEscaping()
  {
    return _disableEscaping;
  }
  
  public boolean disableEscaping(boolean disable)
    throws IOException, SAXException
  {
    if (disable != _disableEscaping) {
      popText();
      _xmlWriter.setEscapeText(! disable);
    }

    boolean old = _disableEscaping;
    _disableEscaping = disable;

    return old;
  }

  public void setLocation(String systemId, String filename, int line)
    throws IOException, SAXException
  {
    // Don't need to pop the text if the line # matches
    if (filename == null || ! filename.equals(_filename) ||
        line != _tailLine)
      popText();

    _systemId = systemId;
    _filename = filename;
    _line = line;
    _tailLine = line;
  }

  /**
   * Adds a new element to the current node, making the new element the
   * current node.
   *
   * <p>Each pushElement should be matched by a popElement.
   *
   * @param name name of the element
   */
  public void pushElement(String name)
   throws IOException, SAXException
  {
    popText();

    String local;
    int p = name.lastIndexOf(':');
    if (p > 0)
      local = name.substring(p + 1);
    else
      local = name;
    
    startElement(null, null, local, name);
  }

  /**
   * Adds a new element to the current node, making the new element the
   * current node.
   *
   * <p>Each pushElement should be matched by a popElement.
   *
   * @param name name of the element
   * @param namespace namespace context
   */
  public void pushElement(String name, NamespaceContext namespace)
   throws IOException, SAXException
  {
    popText();

    // Look up the proper namespace for the element.
    int p = name.indexOf(':');
    if (p <= 0) {
      startElement(null, null, name, name);
      return;
    }

    String prefix = name.substring(0, p);
    String url = namespace.find(namespace, prefix);
    
    if (url != null)
      startElement(url, prefix, name.substring(p + 1), name);
    else
      startElement(null, null, name, name);
  }

  /**
   * Adds a new element to the current node, making the new element the
   * current node.
   *
   * <p>Each pushElement should be matched by a popElement.
   *
   * @param name name of the element
   * @param url namespace url
   */
  public void pushElementNs(String name, String url)
   throws IOException, SAXException
  {
    popText();
    
    // Look up the proper namespace for the element.
    int p = name.indexOf(':');
    if (p <= 0) {
      startElement(url, "", name, name);
      return;
    }

    String prefix = name.substring(0, p);
    String local = name.substring(p + 1);

    startElement(url, prefix, local, name);
  }

  /**
   * Adds a namespace-aware element to the current node, making the
   * new element the current node.
   *
   * <p>Each pushElement should be matched by a popElement.
   *
   * @param prefix the prefix of the element name, e.g. xsl
   * @param local the local part of the element name, e.g. template
   * @param url the namespace url, e.g. http://www.xml.org/...
   */
  public void pushElement(String url, String prefix, String local, String name)
   throws IOException, SAXException
  {
    popText();

    /*
    if (url != null && url.startsWith("quote:"))
      url = url.substring(6);
    */
    
    startElement(url, prefix, local, name);
  }

  /**
   * Adds a new attribute with the given name to the current node, making
   * the attribute the current node.
   */
  public XMLWriter pushAttribute(String name)
    throws IOException, SAXException
  {
    popText();

    XMLWriter oldWriter = _xmlWriter;
    _xmlWriter = ATTR_WRITER;
    
    _attributeURL = null;
    _attributePrefix = null;
    _attributeLocalName = null;
    _attributeName = name;

    return oldWriter;
  }

  /**
   * Adds a new attribute with the given name to the current node, making
   * the attribute the current node.
   */
  public XMLWriter pushAttribute(String name, NamespaceContext namespace)
   throws IOException, SAXException
  {
    popText();

    XMLWriter oldWriter = _xmlWriter;
    _xmlWriter = ATTR_WRITER;
    
    // Look up the proper namespace for the element.
    int p = name.indexOf(':');
    String prefix = null;
    if (p > 0)
      prefix = name.substring(0, p);
    String url = namespace.find(namespace, prefix);
    Attr attr;
    
    if (url != null) {
      _attributeURL = url;
      _attributePrefix = prefix;
      _attributeLocalName = name.substring(p + 1);
      _attributeName = name;
    }
    else {
      _attributeURL = null;
      _attributePrefix = null;
      _attributeLocalName = null;
      _attributeName = name;
    }

    return oldWriter;
  }

  /**
   * Adds a new attribute to the current node, making the new attribute the
   * current node.
   *
   * <p>Each pushAttributeNs should be matched by a popAttribute.
   *
   * @param name name of the element
   * @param url namespace url
   */
  public XMLWriter pushAttributeNs(String name, String url)
   throws IOException, SAXException
  {
    popText();

    XMLWriter oldWriter = _xmlWriter;
    _xmlWriter = ATTR_WRITER;
    
    Attr attr;

    // Look up the proper namespace for the element.
    int p = name.indexOf(':');
    String prefix = null;
    String local = name;
    if (p > 0) {
      prefix = name.substring(0, p);
      local = name.substring(p + 1);
    }

    _attributeURL = url;
    _attributePrefix = prefix;
    _attributeLocalName = local;
    _attributeName = name;

    return oldWriter;
  }

  /**
   * Adds a namespace-aware attribute to the current node, making the
   * new attribute the current node.
   *
   * <p>Each pushAttribute should be matched by a popAttribute.
   *
   * @param prefix the prefix of the element name, e.g. xsl
   * @param local the local part of the element name, e.g. template
   * @param url the namespace url, e.g. http://www.xml.org/...
   */
  public XMLWriter pushAttribute(String prefix, String local, String url)
    throws IOException, SAXException
  {
    popText();

    XMLWriter oldWriter = _xmlWriter;
    _xmlWriter = ATTR_WRITER;
    
    /*
    if (url != null && url.startsWith("quote:"))
      url = url.substring(6);
    */
    
    _attributeURL = url;
    _attributePrefix = prefix;
    _attributeLocalName = local;
    
    if (prefix != null && ! prefix.equals(""))
      _attributeName = prefix + ":" + local;
    else
      _attributeName = local;

    return oldWriter;
  }

  /**
   * Adds a namespace-aware attribute to the current node, making the
   * new attribute the current node.
   *
   * <p>Each pushAttribute should be matched by a popAttribute.
   *
   * @param prefix the prefix of the element name, e.g. xsl
   * @param local the local part of the element name, e.g. template
   * @param url the namespace url, e.g. http://www.xml.org/...
   */
  public void setAttribute(String prefix, String local, String url,
                           String value)
    throws IOException, SAXException
  {
    popText();

    /*
    if (url != null && url.startsWith("quote:"))
      url = url.substring(6);
    */

    String attributeName;
    if (prefix != null && ! prefix.equals(""))
      attributeName = prefix + ":" + local;
    else
      attributeName = local;
    
    attribute(url, prefix, local, attributeName, value);
  }

  /**
   * Adds a new attribute with the given name to the current node, making
   * the attribute the current node.
   */
  public void setAttribute(String name, NamespaceContext namespace, String value)
   throws IOException, SAXException
  {
    popText();
    
    // Look up the proper namespace for the element.
    int p = name.indexOf(':');
    String prefix = null;
    if (p > 0)
      prefix = name.substring(0, p);
    String url = namespace.find(namespace, prefix);
    Attr attr;
    
    if (url != null) {
      attribute(url, prefix, name.substring(p + 1), name, value);
    }
    else {
      attribute(null, null, null, name, value);
    }
  }

  /**
   * Sets the attribute value to the current text, and sets the current node
   * to the parent.
   */
  public void popAttribute(XMLWriter writer)
   throws IOException, SAXException
  {
    _xmlWriter = writer;
    
    attribute(_attributeURL, _attributePrefix,
              _attributeLocalName, _attributeName,
              _text.toString());
    
    _text.clear();
    _attributeName = null;
  }

  /**
   * Directly sets an attribute with a value.
   */
  public void setAttribute(String name, String value)
    throws IOException, SAXException
  {
    attribute(null, null, name, name, value);
  }

  /**
   * Copies the node without attributes or children.
   */
  public void pushCopy(Node copyNode)
    throws IOException, SAXException
  {
    popText();

    switch (copyNode.getNodeType()) {
    case Node.ATTRIBUTE_NODE:
      Node oldNode = copyNode;
      attribute(oldNode.getNamespaceURI(),
                oldNode.getPrefix(),
                oldNode.getLocalName(),
                oldNode.getNodeName(),
                oldNode.getNodeValue());
      break;

    case Node.DOCUMENT_NODE:
      return;

    case Node.ELEMENT_NODE:
      Element oldElt = (Element) copyNode;

      /*
      String oldSystemId = _systemId;
      String oldFilename = _filename;
      int oldLine = _line;

      _systemId = oldElt.getBaseURI();
      _filename = oldElt.getFilename();
      _line = oldElt.getLine();

      if (generateLocation)
        _xmlWriter.setLocation(.getFilename(), oldElt.getLine(), 0);
      */

      startElement(oldElt.getNamespaceURI(),
                   oldElt.getPrefix(),
                   oldElt.getLocalName(),
                   oldElt.getNodeName());

      /*
      _systemId = oldSystemId;
      _filename = oldFilename;
      _line = oldLine;
      */
      break;

    case Node.COMMENT_NODE:
      _xmlWriter.comment(((Comment) copyNode).getData());
      break;

    case Node.TEXT_NODE:
      /*
      if (generateLocation)
        _xmlWriter.setLocation(((QAbstractNode) copyNode).getFilename(),
                              ((QAbstractNode) copyNode).getLine(),
                              0);
      */
      _text.append(((Text) copyNode).getData());
      break;

    case Node.PROCESSING_INSTRUCTION_NODE:
      ProcessingInstruction oldPi = (ProcessingInstruction) copyNode;

      _xmlWriter.processingInstruction(oldPi.getNodeName(), oldPi.getNodeValue());
      break;
    }
  }

  /**
   * Pops the copy.
   */
  public void popCopy(Node copyNode)
   throws IOException, SAXException
  {
    if (copyNode.getNodeType() == Node.ELEMENT_NODE) {
      popText();
      popElement();
    }
  }

  public void pushPi()
    throws IOException, SAXException
  {
    popText();
  }
  /**
   * Sets the PI data to the current text, and sets the current node
   * to the parent.
   */
  public void popPi(String name)
   throws IOException, SAXException
  {
    _xmlWriter.processingInstruction(name, _text.toString());
    _text.clear();
  }

  /**
   * Adds an empty comment to the current node, making
   * the attribute the current node.
   */
  public void pushComment()
   throws IOException, SAXException
  {
    popText();
  }

  /**
   * Sets the comment data to the current text, and sets the current
   * to the the parent.
   */
  public void popComment()
   throws IOException, SAXException
  {
    _xmlWriter.comment(_text.toString());
    
    _text.clear();
  }

  /**
   * Starts a fragment.  The fragment becomes the current node.
   */
  public XMLWriter pushFragment()
   throws IOException, SAXException
  {
    popText();

    DOMBuilder domBuilder = new DOMBuilder();

    if (_document == null)
      _document = Xml.createDocument();
    domBuilder.init(_document.createDocumentFragment());
    domBuilder.setDocumentLocator(_locator);

    XMLWriter oldWriter = _xmlWriter;
    _xmlWriter = domBuilder;

    return oldWriter;
  }

  /**
   * Returns the generated fragment. The current node does not contain
   * the new fragment.
   *
   * @return the generated fragment.
   */
  public Node popFragment(XMLWriter oldWriter)
   throws IOException, SAXException
  {
    popText();

    DOMBuilder domBuilder = (DOMBuilder) _xmlWriter;

    _xmlWriter = oldWriter;

    domBuilder.endDocument();
    Node node = domBuilder.getNode();

    return node;
  }

  /**
   * Adds a the contents of the node to the current node.
   *
   * @param node node to print
   */
  public void valueOf(Object node)
   throws IOException, SAXException
  {
    if (node == null)
      return;
    else if (node instanceof Element || node instanceof DocumentFragment) {
      Node elt = (Node) node;
      for (Node child = elt.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        elementValueOf(child);
      }
    }
    else if (node instanceof Text) {
      String data = ((Text) node).getNodeValue();
      for (int i = 0; i < data.length(); i++) {
        if (! XmlChar.isWhitespace(data.charAt(i))) {
          print(data);
          return;
        }
      }
      /*
      if (! _stylesheet.stripSpaces(((Node) node).getParentNode()))
      */
      print(data);
    }
    else if (node instanceof Node) {
      print(((QAbstractNode) node).getNodeValue());
    }
    else if (node instanceof NodeList) {
      NodeList list = (NodeList) node;
      Node value = list.item(0);

      if (value != null)
        valueOf(value);
    }
    else if (node instanceof ArrayList) {
      ArrayList list = (ArrayList) node;
      if (list.size() > 0)
        valueOf(list.get(0));
    }
    else if (node instanceof Iterator) {
      Iterator list = (Iterator) node;
      valueOf(list.next());
    }
    else if (node instanceof Double) {
      Double d = (Double) node;
      double dValue = d.doubleValue();

      if ((int) dValue == dValue)
        print((int) dValue);
      else
        print(dValue);
    }
    else
      print(node);
  }

  /**
   * Adds a the contents of the node to the current node.
   *
   * @param node node to print
   */
  private void elementValueOf(Node node)
   throws IOException, SAXException
  {
    if (node == null)
      return;
    else if (node instanceof Element) {
      Element elt = (Element) node;
      for (Node child = elt.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        elementValueOf(child);
      }
    }
    else if (node instanceof Text) {
      String data = ((Text) node).getNodeValue();
      for (int i = 0; i < data.length(); i++) {
        if (! XmlChar.isWhitespace(data.charAt(i))) {
          print(data);
          return;
        }
      }
      /*
      if (! _stylesheet.stripSpaces(node.getParentNode()))
      */
      print(data);
    }
  }

  /**
   * Adds a deep copy of the node to the current node.
   *
   * @param XPath node to be copied to the destination.
   */
  public void copyOf(Object value)
    throws IOException, SAXException, XPathException
  {
    popText();

    if (value instanceof NodeList) {
      NodeList list = (NodeList) value;

      int length = list.getLength();
      for (int i = 0; i < length; i++) {
        Node child = list.item(i);

        copyOf(child);
      }
    }
    else if (value instanceof ArrayList) {
      ArrayList list = (ArrayList) value;

      for (int i = 0; i < list.size(); i++) {
        Node child = (Node) list.get(i);

        copyOf(child);
      }
    }
    else if (value instanceof Iterator) {
      Iterator iter = (Iterator) value;

      while (iter.hasNext()) {
        Node child = (Node) iter.next();

        copyOf(child);
      }
    }
    else if (value instanceof Attr) {
      Attr child = (Attr) value;

      attribute(child.getNamespaceURI(),
                child.getPrefix(),
                child.getLocalName(),
                child.getNodeName(),
                child.getNodeValue());
    }
    else if (value instanceof QElement) {
      QElement child = (QElement) value;

      String oldSystemId = _systemId;
      String oldFilename = _filename;
      int oldLine = _line;

      _systemId = child.getBaseURI();
      _filename = child.getFilename();
      _line = child.getLine();

      startElement(child.getNamespaceURI(),
                   child.getPrefix(),
                   child.getLocalName(),
                   child.getNodeName());
      Node subNode = child.getFirstAttribute();
      for (; subNode != null; subNode = subNode.getNextSibling()) {
        QAttr attr = (QAttr) subNode;
        
        attribute(attr.getNamespaceURI(),
                  attr.getPrefix(),
                  attr.getLocalName(),
                  attr.getNodeName(),
                  attr.getNodeValue());
      }
      
      for (subNode = child.getFirstChild();
           subNode != null;
           subNode = subNode.getNextSibling()) {
        copyOf(subNode);
      }

      popElement();

      _systemId = oldSystemId;
      _filename = oldFilename;
      _line = oldLine;
    }
    else if (value instanceof DocumentFragment) {
      for (Node subNode = ((Node) value).getFirstChild();
           subNode != null;
           subNode = subNode.getNextSibling()) {
        copyOf(subNode);
      }
    }
    else if (value instanceof Text) {
      Text child = (Text) value;

      _text.append(child.getNodeValue());
    }
    else if (value instanceof Comment) {
      Comment child = (Comment) value;

      _xmlWriter.comment(child.getNodeValue());
    }
    else if (value instanceof ProcessingInstruction) {
      ProcessingInstruction pi = (ProcessingInstruction) value;
      
      _xmlWriter.processingInstruction(pi.getNodeName(),
                                       pi.getNodeValue());
    }
    else if (value instanceof EntityReference) {
      EntityReference child = (EntityReference) value;

      _text.append("&" + child.getNodeName() + ";");
    }
    else if (value instanceof Node) {
      Node child = (Node) value;

      _text.append(child.getNodeValue());
    }
    else {
      print(Expr.toString(value));
    }
  }

  public void addNamespace(String prefix, String url)
  {
    /*
    if (url.startsWith("quote:"))
      url = url.substring(6);
    */
    if (! url.equals("")) {
      _namespaces.put(prefix, url);

      _topNamespaces.add(prefix);
    }
  }

  void startElement(String url, String prefix, String local, String qName)
    throws IOException, SAXException
  {
    if (_attributeName != null)
      throw error(L.l("element `{0}' is not allowed inside attribute `{1}'.  xsl:attribute must contain text only.", qName, _attributeName));
    popText();

    StackItem item = null;
    if (_elementStack.size() <= _depth) {
      item = new StackItem();
      _elementStack.add(item);
    }
    else
      item = _elementStack.get(_depth);

    item.init(url, prefix, local, qName, isCdata);

    if (_cdataElements != null && _cdataElements.get(qName) != null)
      isCdata = true;
    
    _depth++;

    _xmlWriter.startElement(url, local, qName);

    // Initialize top-level namespaces
    if (_depth == 1) {
      for (int i = 0; i < _topNamespaces.size(); i++) {
        String topPrefix = _topNamespaces.get(i);
        String topUrl = _namespaces.get(topPrefix);

        if (topPrefix.equals("")) {
          _xmlWriter.startPrefixMapping(null, topUrl);
          _xmlWriter.attribute(XMLNS, null, "xmlns", topUrl);
        }
        else {
          _xmlWriter.startPrefixMapping(topPrefix, topUrl);
          _xmlWriter.attribute(XMLNS, topPrefix, "xmlns:" + topPrefix, topUrl);
        }
      }
    }

    if (url == null)
      return;

    bindNamespace(prefix, url);
  }

  public void popElement()
    throws IOException, SAXException
  {
    popText();
    _depth--;

    StackItem item = _elementStack.get(_depth);
    
    try{
      _xmlWriter.endElement(item.getNamespace(),
                           item.getLocalName(),
                           item.getName());

      // If this element bound namespaces, pop the old values
      for (int i = 0; i < item.nsSize(); i++) {
        String oldPrefix = item.getNSPrefix(i);
        String oldUrl = item.getNSUrl(i);

        if (oldUrl == null)
          _namespaces.remove(oldPrefix);
        else
          _namespaces.put(oldPrefix, oldUrl);

        _xmlWriter.endPrefixMapping(oldPrefix);
      }

      isCdata = item.getCdata();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Sends the attribute to the output
   *
   * @param url the namespace for the attribute name
   * @param prefix the prefix for the attribute name
   * @param local the local attribute name
   * @param qName the full qualified name
   * @param value the attribute's value
   */
  public void attribute(String url, String prefix, String local,
                        String qName, String value)
    throws IOException, SAXException
  {
    if (qName.startsWith("xmlns:"))
      bindNamespace(qName.substring("xmlns:".length()), value);
    else if (qName.equals("xmlns"))
      bindNamespace(null, value);
    else {
      _xmlWriter.attribute(url, local, qName, value);

      // null namespace binding doesn't add binding
      if (url != null && ! url.equals("") && ! prefix.equals(""))
        bindNamespace(prefix, url);
    }
  }

  /**
   * Sends the attribute to the output
   *
   * @param url the namespace for the attribute name
   * @param prefix the prefix for the attribute name
   * @param local the local attribute name
   * @param qName the full qualified name
   * @param value the attribute's value
   */
  public void attribute(String qName, String value)
    throws IOException, SAXException
  {
    _xmlWriter.attribute(null, null, qName, value);
  }

  public void bindNamespace(String prefix, String url)
    throws IOException, SAXException
  {
    String oldUrl = _namespaces.get(prefix);

    // If the namespace matches, return
    if (oldUrl == null && url.equals("") ||
        oldUrl != null && url.equals(oldUrl))
      return;

    // Send the namespace declaration to the writer
    if (prefix != null) {
      _xmlWriter.startPrefixMapping(prefix, url);
      _xmlWriter.attribute(XMLNS, prefix, "xmlns:" + prefix, url);
      _namespaces.put(prefix, url);
    }
    else {
      _xmlWriter.startPrefixMapping(null, url);
      _xmlWriter.attribute(XMLNS, null, "xmlns", url);
      _namespaces.put(null, url);
    }

    StackItem item = _elementStack.get(_depth - 1);
    item.addNamespace(prefix, oldUrl);
  }

  /**
   * Pop the accumulated text to the DOM.
   */
  public void popText()
    throws IOException, SAXException
  {
    if (_xmlWriter == ATTR_WRITER)
      return;
    
    Text textNode = null;
    
    if (_text.length() == 0)
      return;
    
    if (_filename != null)
      _line = _tailLine;

    if (isCdata)
      _xmlWriter.cdata(_text.getBuffer(), 0, _text.getLength());
    else
      _xmlWriter.text(_text.getBuffer(), 0, _text.getLength());
    
    _text.clear();
  }

  /**
   * Returns the attribute with the given name.
   */
  public Object getProperty(String name)
  {
    return _transformer.getProperty(name);
  }

  /**
   * Sets the attribute with the given name.
   */
  public void setProperty(String name, Object value)
  {
    _transformer.setProperty(name, value);
  }

  /**
   * removes the attribute with the given name.
   */
  public void removeProperty(String name)
  {
  }

  /**
   * Lists the names of all the attributes.
   */
  public Iterator getPropertyNames()
  {
    return null;
  }

  public Object getParameter(String name)
  {
    return _transformer.getParameter(name);
  }

  public Path getPwd()
  {
    return (Path) getProperty("caucho.pwd");
  }

  public OutputStream openWrite(ExprEnvironment env, String href)
    throws IOException
  {
    if (_xmlWriter instanceof XmlPrinter) {
      XmlPrinter printer = (XmlPrinter) _xmlWriter;

      Path path = printer.getPath();

      if (path != null) {
        Path dst = path.getParent().lookup(href);
        dst.getParent().mkdirs();

        return dst.openWrite();
      }
    }
    
    Path stylesheetPath = env.getStylesheetEnv().getPath();
    
    return stylesheetPath.getParent().lookup(href).openWrite();
  }

  public XslWriter openResultDocument(OutputStream os)
    throws IOException, SAXException
  {
    XMLWriter writer = new XmlPrinter(os);
    XslWriter out = new XslWriter(null, _stylesheet, _transformer);
    out.init(writer);

    writer.startDocument();

    return out;
  }

  /**
   * @deprecated
   */
  public javax.servlet.jsp.PageContext getPage()
  {
    return (javax.servlet.jsp.PageContext) getProperty("caucho.page.context");
  }

  private IOException error(String message)
  {
    if (_filename != null)
      return new IOException(_filename + ":" + _line + ": " + message);
    else
      return new IOException(message);
  }
  
  public String getSystemId()
  {
    if (_systemId != null)
      return _systemId;
    else
      return _filename;
  }
    
  public String getFilename()
  {
    if (_filename != null)
      return _filename;
    else
      return _systemId;
  }
    
  public String getPublicId()
  {
    return null;
  }

  public int getLineNumber()
  {
    return _line;
  }

  public int getColumnNumber()
  {
    return 0;
  }

  static class StackItem {
    String _url;
    String _prefix;
    String _local;
    String _qName;
    boolean _isCdata;

    ArrayList<String> _nsPrefixes;
    ArrayList<String> _nsUrls;

    void clear()
    {
    }

    void init(String url, String prefix, String local, String qName,
              boolean isCdata)
    {
      if (_nsPrefixes != null) {
        _nsPrefixes.clear();
        _nsUrls.clear();
      }

      _url = url;
      _prefix = prefix;
      _local = local;
      _qName = qName;

      _isCdata = isCdata;
    }

    String getNamespace()
    {
      return _url;
    }

    String getPrefix()
    {
      return _prefix;
    }

    String getLocalName()
    {
      return _local;
    }

    String getName()
    {
      return _qName;
    }

    boolean getCdata()
    {
      return _isCdata;
    }

    int nsSize()
    {
      return _nsPrefixes == null ? 0 : _nsPrefixes.size();
    }

    String getNSPrefix(int i)
    {
      return _nsPrefixes.get(i);
    }

    String getNSUrl(int i)
    {
      return _nsUrls.get(i);
    }

    void addNamespace(String prefix, String oldUrl)
    {
      if (_nsPrefixes == null) {
        _nsPrefixes = new ArrayList<String>();
        _nsUrls = new ArrayList<String>();
      }

      _nsPrefixes.add(prefix);
      _nsUrls.add(oldUrl);
    }
  }
}
