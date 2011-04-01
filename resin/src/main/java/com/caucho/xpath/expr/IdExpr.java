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

package com.caucho.xpath.expr;

import com.caucho.util.CharBuffer;
import com.caucho.xml.QDocumentType;
import com.caucho.xml.XmlChar;
import com.caucho.xml.XmlUtil;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.pattern.NodeIterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Iterator;

public class IdExpr extends Expr {
  private Expr _expr;

  private ExprEnvironment _lastEnv;
  private int _lastUseCount;
  private Node _lastContext;
  private ArrayList _lastList;

  public IdExpr(ArrayList<Expr> args)
  {
    if (args.size() > 0)
      _expr = args.get(0);
  }

  public boolean isNodeSet()
  {
    return true;
  }

  /**
   * Evaluates the expression as a number
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return the number representation of id
   */
  public double evalNumber(Node node, ExprEnvironment env)
    throws XPathException
  {
    String string = evalString(node, env);

    return stringToNumber(string);
  }

  /**
   * Evaluates the expression as a boolean
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return true if the node exists
   */
  public boolean evalBoolean(Node node, ExprEnvironment env)
    throws XPathException
  {
    return id(node, env).size() > 0;
  }

  /**
   * The string value of the id expression is just the text value of the
   * first node.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return true if the node exists
   */
  public String evalString(Node node, ExprEnvironment env)
    throws XPathException
  {
    NodeIterator iter = evalNodeSet(node, env);

    if (! iter.hasNext())
      return "";

    Node qNode = (Node) iter.next();
    return XmlUtil.textValue(qNode);
  }

  /**
   * The string value of the id expression is just the list of nodes.
   *
   * @param node the current node
   * @param env the variable environment.
   *
   * @return true if the node exists
   */
  public Object evalObject(Node node, ExprEnvironment env)
    throws XPathException
  {
    ArrayList<Element> list = id(node, env);

    return list;
  }

  /**
   * Returns the list of string ids.
   */
  private ArrayList<Element> id(Node context, ExprEnvironment env)
    throws XPathException
  {
    ArrayList idList = getIdList(context, env);
    ArrayList<Element> list = new ArrayList<Element>();

    if (idList == null || idList.size() == 0)
      return list;
    
    Node ptr;

    if (context instanceof Document)
      ptr = context;
    else
      ptr = context.getOwnerDocument();
    
    while ((ptr = XmlUtil.getNext(ptr)) != null) {
      if (ptr instanceof Element) {
        Element elt = (Element) ptr;

        QDocumentType dtd;
        dtd = (QDocumentType) elt.getOwnerDocument().getDoctype();
        String id = null;
        if (dtd != null)
          id = (String) dtd.getElementId(elt.getNodeName());

        if (id != null) {
          String idValue = elt.getAttribute(id);
          if (idList.contains(idValue) && ! list.contains(elt))
            list.add(elt);
        }
      }
    }

    return list;
  }

  /**
   * Evaluates the id expression, returning a list of strings.
   *
   * @param env the XPath environment
   * @param node the context node.
   *
   * @return a list of string values.
   */
  private ArrayList<String> getIdList(Node node, ExprEnvironment env)
    throws XPathException
  {
    ArrayList<String> idList = new ArrayList<String>();

    Object obj = _expr.evalObject(node, env);
    if (obj instanceof NodeList) {
      NodeList list = (NodeList) obj;

      int length = list.getLength();
      for (int i = 0; i < length; i++) {
        Node value = list.item(i);

        addText(idList, XmlUtil.textValue(value));
      }
    }
    else if (obj instanceof ArrayList) {
      ArrayList list = (ArrayList) obj;

      for (int i = 0; i < list.size(); i++) {
        Node value = (Node) list.get(i);

        addText(idList, XmlUtil.textValue(value));
      }
    }
    else if (obj instanceof Iterator) {
      Iterator iter = (Iterator) obj;

      while (iter.hasNext()) {
        Node value = (Node) iter.next();

        addText(idList, XmlUtil.textValue(value));
      }
    }
    else
      addText(idList, toString(obj));

    return idList;
  }

  private void addText(ArrayList<String> idList, String text)
  {
    int len = text.length();
    CharBuffer cb = new CharBuffer();
    int i = 0;
    int ch = 0;
    for (; i < len && XmlChar.isWhitespace(text.charAt(i)); i++) {
    }
    
    if (i == len)
      return;
    
    while (i < len) {
      cb.clear();
      for (; i < len && ! XmlChar.isWhitespace(text.charAt(i)); i++)
        cb.append(text.charAt(i));

      idList.add(cb.toString());

      for (; i < len && XmlChar.isWhitespace(text.charAt(i)); i++) {
      }
    }
  }

  public String toString()
  {
    if (_expr != null)
      return "id(" + _expr + ")";
    else
      return "id()";
  }
}
