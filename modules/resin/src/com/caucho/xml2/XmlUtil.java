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

import com.caucho.util.CharBuffer;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;

/**
 * XML utilities for manipulating names and the DOM.
 */
public class XmlUtil {
  /**
   * Treats the string as an attribute list, splitting it into a HashMap.
   *
   * @param name a string to be interpreted as an attribute list
   * @return a hash map containing the attribute (key, value) pairs.
   */
  public static HashMap<String,String> splitNameList(String name)
    throws IOException
  {
    HashMap<String,String> attrs = new HashMap<String,String>();
    CharBuffer cb = new CharBuffer();

    int length = name.length();
    int i = 0;
    int ch = 0;
    while (i < length) {
      for (; i < length && XmlChar.isWhitespace(ch = name.charAt(i)); i++) {
      }

      if (i < length && ! XmlChar.isNameStart(ch))
        throw new IOException("expected name at " + (char) ch);
      
      cb.clear();
      while (i < length && XmlChar.isNameChar(ch)) {
        cb.append((char) ch);

        ch = name.charAt(++i);
      }
      String key = cb.toString();
      cb.clear();

      for (; i < length && XmlChar.isWhitespace(ch = name.charAt(i)); i++) {
      }

      if (ch != '=') {
        attrs.put(key, "");
        continue;
      }

      while (++i < length && XmlChar.isWhitespace(ch = name.charAt(i))) {
      }

      if (i >= length)
        break;

      cb.clear();
      if (ch == '\'') {
        while (++i < length && (ch = name.charAt(i)) != '\'')
          cb.append((char) ch);
        i++;
      } else if (ch == '"') {
        while (++i < length && (ch = name.charAt(i)) != '\"')
          cb.append((char) ch);
        i++;
      } else if (XmlChar.isNameChar(ch)) {
        cb.append((char) ch);
        while (++i < length && XmlChar.isNameChar(ch = name.charAt(i)))
          cb.append((char) ch);
      } else
        throw new IOException("unexpected");

      attrs.put(key, cb.toString());
    }

    return attrs;
  }

  /**
   * Extracts an attribute from a processing instruction.  Since
   * processing instructions are opaque, the standard DOM has no API
   * for the common case where the PI value is an attribute list.
   *
   * <code><pre>
   *   &lt;?xml-stylesheet href="default.xsl"?&gt;
   * </pre></code>
   *
   * <p>In the above example,
   * <code><pre>getPIAttribute(node.getNodeValue(),&nbsp;"href")</pre></code>
   * would return "default.xsl".
   *
   * @param pi the value of the processing instruction
   * @param key the attribute key
   * @return the value corresponding to the attribute key.
   */
  public static String getPIAttribute(String pi, String key)
  {
    CharBuffer nameBuf = new CharBuffer();
    CharBuffer valueBuf = new CharBuffer();

    int i = 0;
    int length = pi.length();;
    while (i < length) {
      int ch = 0; 
      for (; i < length && XmlChar.isWhitespace(ch = pi.charAt(i)); i++) {
      }

      nameBuf.clear();
      for (; i < length && XmlChar.isNameChar(ch = pi.charAt(i)); i++)
        nameBuf.append((char) ch);

      for (; i < length && XmlChar.isWhitespace(ch = pi.charAt(i)); i++) {
      }
      
      if (i < length && ch != '=') {
        if (nameBuf.length() == 0)
          return null;
        else if (nameBuf.toString().equals(key))
          return nameBuf.toString();
        else
          continue;
      }

      i++;
      for (; i < length && XmlChar.isWhitespace(ch = pi.charAt(i)); i++) {
      }

      // Parse the attribute value: '.*' or ".*" or \w+
      valueBuf.clear();
      if (ch == '\'') {
        i++;
        for (; i < length && (ch = pi.charAt(i)) != '\''; i++)
          valueBuf.append((char) ch);
        i++;
      }
      else if (ch == '\"') {
        i++;
        for (; i < length && (ch = pi.charAt(i)) != '\"'; i++)
          valueBuf.append((char) ch);
        i++;
      }
      else if (XmlChar.isNameChar(ch)) {
        for (; i < length && XmlChar.isNameChar(ch = pi.charAt(i)); i++)
          valueBuf.append((char) ch);
      }
      else
        return null; // XXX: should throw an exception?

      String name = nameBuf.toString();
      if (name.equals(key))
        return valueBuf.toString();
    }

    return null;
  }

  /**
   * Get the next node in a depth first preorder traversal.
   *
   * <ul>
   * <li>If the node has a child, return the child
   * <li>Else if the node has a following sibling, return that sibling
   * <li>Else if the node has a following uncle, return that uncle
   * </ul>
   *
   * @param node the current node
   * @return the next node in the preorder traversal
   */
  public static Node getNext(Node node)
  {
    if (node == null)
      return null;
    
    if (node.getFirstChild() != null)
      return node.getFirstChild();

    for (; node != null; node = node.getParentNode()) {
      if (node.getNextSibling() != null)
        return node.getNextSibling();
    }

    return null;
  } 

  /**
   * Get the previous node in a DFS preorder traversal
   *
   * @param node the current node
   * @return the previous node in the preorder traversal
   */
  public static Node getPrevious(Node node)
  {
    Node previous;

    if (node == null)
      return null;

    if ((previous = node.getPreviousSibling()) != null) {
      for (;
           previous.getLastChild() != null;
           previous = previous.getLastChild()) {
      }

      return previous;
    }

    return node.getParentNode();
  }

  /**
   * Extracts the text value from the node.  Text nodes return their
   * value and elements return the concatenation of the child values.
   */
  public static String textValue(Node node)
  {
    if (node instanceof Element || node instanceof DocumentFragment) {
      String s = null;
      CharBuffer cb = null;
      
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        String value = null;
        
        if (child instanceof Element || child instanceof Document) {
          if (cb == null)
            cb = CharBuffer.allocate();
          if (s != null)
            cb.append(s);
          s = null;

          textValue(cb, child);
        }
        else if ((value = child.getNodeValue()) == null || value == "") {
        }
        else if (cb != null)
          cb.append(value);
        else if (s == null && s != "") {
          s = value;
        }
        else {
          cb = CharBuffer.allocate();

          cb.append(s);
          cb.append(value);
          s = null;
        }
      }

      if (s != null)
        return s;
      else if (cb != null)
        return cb.close();
      else
        return "";
    }
    else {
      String value = node.getNodeValue();

      if (value != null)
        return value;
      else
        return "";
    }
  }

  /**
   * Extracts the text value from the node.  Text nodes return their
   * value and elements return the concatenation of the child values.
   */
  public static void textValue(CharBuffer cb, Node node)
  {
    if (node instanceof Element || node instanceof DocumentFragment) {
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        textValue(cb, child);
      }
    }
    else if (node instanceof Comment || node instanceof ProcessingInstruction) {
    }
    else
      cb.append(node.getNodeValue());
  }

  /**
   * Extracts the text value from the node.  Text nodes return their
   * value and elements return the concatenation of the child values.
   */
  public static boolean isWhitespace(String text)
  {
    for (int i = text.length() - 1; i >= 0; i--)
      if (! XmlChar.isWhitespace(text.charAt(i)))
        return false;

    return true;
  }

  /**
   * Sends data to the helper.
   */
  public static void toSAX(Node node, ContentHandler handler)
    throws SAXException
  {
    for (; node != null; node = node.getNextSibling()) {
      if (node instanceof ProcessingInstruction) {
        ProcessingInstruction pi = (ProcessingInstruction) node;

        handler.processingInstruction(pi.getNodeName(), pi.getData());
      }
      else if (node instanceof DocumentFragment) {
        toSAX(node.getFirstChild(), handler);
      }
    }
  }

  /**
   * Returns the namespace for the given prefix.
   */
  public static String getNamespace(Node node, String prefix)
  {
    for (; node != null; node = node.getParentNode()) {
      if (node instanceof CauchoElement)
        return ((CauchoElement) node).getNamespace(prefix);
    }

    return null;
  }
}
