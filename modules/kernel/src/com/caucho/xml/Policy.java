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

package com.caucho.xml;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

import java.util.HashMap;

/**
 * The Policy class defines the parsing policy.  It configures the parser
 * between HTML, XML, and loose versions of HTML and XML.
 */
class Policy {
  static L10N L = new L10N(Policy.class);
  
  final static int ERROR = 0;
  final static int IGNORE = ERROR + 1;
  final static int PUSH = IGNORE + 1;
  final static int PUSH_EMPTY = PUSH + 1;
  final static int PUSH_OPT = PUSH_EMPTY + 1;
  final static int PUSH_VERBATIM = PUSH_OPT + 1;
  final static int POP = PUSH_VERBATIM + 1;
  final static int POP_AND_LOOP = POP + 1;

  private NamespaceMap namespaces;
  private HashMap nameCache = new HashMap();
  private HashMap _attrCache = new HashMap();
  protected QName opt;
  protected ReadStream is;

  boolean expandReferences = true;
  boolean optionalTags = true;
  boolean skipWhitespace;
  boolean skipComments;
  boolean strictComments;
  boolean strictAttributes;
  boolean entitiesAsText = false;
  boolean expandEntities = true;
  boolean strictCharacters;
  boolean strictXml;
  boolean singleTopElement;
  boolean normalizeWhitespace = false;
  boolean forgiving;
  boolean _isNamespaceAware = false;

  /**
   * Initialize the policy.
   */
  void init()
  {
    namespaces = null;
    nameCache.clear();
    _attrCache.clear();
    opt = null;
    is = null;
    
    expandReferences = true;
    optionalTags = true;
    skipWhitespace = false;
    skipComments = false;
    strictComments = false;
    strictAttributes = false;
    entitiesAsText = false;
    expandEntities = true;
    strictCharacters = false;
    strictXml = false;
    singleTopElement = false;
    normalizeWhitespace = false;
    forgiving = false;
    _isNamespaceAware = false;
  }

  /**
   * Sets the current read stream.
   */
  void setStream(ReadStream is)
  {
    this.is = is;
  }

  QName getOpt()
  {
    return opt;
  }

  /**
   * Sets the new namespace binding.
   *
   * @param ns the namespace
   */
  void setNamespace(NamespaceMap ns)
  {
    if (namespaces != ns) {
      nameCache.clear();
      _attrCache.clear();
    }

    namespaces = ns;
  }

  /**
   * Set true for namespace aware.
   */
  void setNamespaceAware(boolean isNamespaceAware)
  {
    _isNamespaceAware = isNamespaceAware;
  }

  /**
   * Clears the namespace cache when the namespace changes.
   */
  void clearNamespaceCache()
  {
    namespaces = null;
    nameCache.clear();
    _attrCache.clear();
  }

  QName getAttributeName(CharBuffer eltName, CharBuffer source)
  {
    return getAttributeName(eltName, source, false);
  }
  
  /**
   * Returns the qname for the named attribute.
   *
   * @param eltName the current node
   * @param source the name of the attribute
   *
   * @param the QName including namespace for the attribute name.
   */
  QName getAttributeName(CharBuffer eltName, CharBuffer source, boolean nsNull)
  {
    QName qname = (QName) _attrCache.get(source);
    if (qname != null)
      return qname;

    int i = source.lastIndexOf(':');
    String fullName = source.toString();
    String prefix = null;
    String localName = null;
    String ns = null;

    if (! _isNamespaceAware) {
      localName = fullName;
    }
    else if (i < 0) {
      localName = fullName;
    }
    else {
      prefix = source.substring(0, i);
      
      ns = NamespaceMap.get(namespaces, prefix);

      if (ns != null) {
        localName = source.substring(i + 1);
      }
      else if ("xml".equals(prefix)) {
        ns = XmlParser.XML;
        localName = source.substring(i + 1);
      }
      else {
        prefix = null;
        localName = source.toString();
      }
    }

    qname = new QName(fullName, prefix, localName, ns);

    _attrCache.put(source.clone(), qname);

    return qname;
  }

  /**
   * Returns the fully qualified name, including namespaces, for the
   * new qname.
   *
   * @param node the current parent node
   * @param source the qname string needing resolving.
   *
   * @return the QName including namespace for the source.
   */
  QName getName(CharBuffer source)
  {
    QName qname = (QName) nameCache.get(source);
    if (qname != null)
      return qname;

    int i = source.lastIndexOf(':');
    String fullName = source.toString();
    String prefix = null;
    String localName = null;
    String ns = null;;

    if (! _isNamespaceAware) {
    }
    else if (i < 0) {
      ns = NamespaceMap.get(namespaces, "");
      localName = source.toString();
    }
    else {
      prefix = source.substring(0, i);
      
      ns = NamespaceMap.get(namespaces, prefix);

      if (ns != null) {
        localName = source.substring(i + 1);
      }
      else {
        prefix = null;
        localName = source.toString();
      }
    }

    qname = new QName(fullName, prefix, localName, ns);

    nameCache.put(source.clone(), qname);

    return qname;
  }

  /**
   * Returns the fully qualified name, including namespaces, for the
   * new qname.
   *
   * @param source the qname string needing resolving.
   *
   * @return the QName including namespace for the source.
   */
  QName getNamespaceName(CharBuffer source)
  {
    QName qname = (QName) nameCache.get(source);
    if (qname != null)
      return qname;

    int i = source.lastIndexOf(':');
    String prefix;
    String localName;

    // xml/01ek
    if (true) {
      prefix = null;
      localName = source.toString();
    }
    else if (i < 0) {
      prefix = null;
      localName = source.toString();
    }
    else {
      prefix = source.substring(0, i);
      localName = source.substring(i + 1);
    }

    // xml/01ek vs xml/01eg
    qname = new QName(prefix, localName, null); // XmlParser.XMLNS

    nameCache.put(source.clone(), qname);

    return qname;
  }

  /**
   * Returns true if the string contains only whitespace.
   *
   * @param s string to test
   * @return true if the string is completely whitespace
   */
  boolean isWhitespaceOnly(String s)
  {
    for (int i = s.length() - 1; i >= 0; i--)
      if (! XmlChar.isWhitespace(s.charAt(i)))
        return false;

    return true;
  }

  /**
   * Returns the action to be performed with the next node on an open
   * tag.  In general, for XML, the next node is just pushed into the tree.
   *
   * @param parser the current XML parser
   * @param node the current node
   * @param next the node that needs an action
   *
   * @return the action code for the next node
   */
  int openAction(XmlParser parser, QName node, QName next)
    throws XmlParseException
  {
    String nodeName = node.getName();
    /*
    if (nodeName.equals("#document")) {
      QDocument document = (QDocument) node;

      switch (next.getNodeType()) {
      case Node.TEXT_NODE:
        if (isWhitespaceOnly(next.getNodeValue()))
          return PUSH; // XXX: ignore
        break;

      case Node.COMMENT_NODE:
      case Node.PROCESSING_INSTRUCTION_NODE:
        return PUSH;
      }

      if (document.getDocumentElement() == null &&
          next.getNodeType() == Node.ELEMENT_NODE) {
        document.setDocumentElement((Element) next);
        return PUSH;
      }

      Element elt = document.getDocumentElement();
      return PUSH;
    } else
      return PUSH;
    */
    return PUSH;
  }

  /**
   * Returns the action to be performed with the next node on a close
   * tag.  In general, for XML, the current node is changed to its parent
   *
   * @param parser the current XML parser
   * @param node the current node
   * @param tagEnd the name of the close tag
   *
   * @return the action code for the next node
   */
  int elementCloseAction(XmlParser parser, QName node, String tagEnd)
    throws XmlParseException
  {
    String nodeName = node.getName();

    if (nodeName.equals("#document") && tagEnd.equals(""))
      return POP;
    else if (nodeName.equals(tagEnd))
      return POP;
    else {
      String expect = nodeName;
      if (expect.equals("#document"))
        expect = L.l("end of document");
      else
        expect = "`</" + expect + ">'";
      if (tagEnd.equals(""))
        tagEnd = L.l("end of file");
      else
        tagEnd = "`</" + tagEnd + ">'";

      throw parser.error(L.l("expected {0} at {1}", expect, tagEnd));
    }
  }
}
