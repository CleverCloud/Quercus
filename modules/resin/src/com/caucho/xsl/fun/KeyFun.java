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

package com.caucho.xsl.fun;

import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.XPathFun;
import com.caucho.xpath.pattern.AbstractPattern;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The key(...) function.
 */
public class KeyFun extends XPathFun {
  private HashMap<String,Key> _keys;

  public KeyFun()
  {
    _keys = new HashMap<String,Key>();
  }

  /**
   * Add a new key.
   *
   * @param name name of the key
   * @param match the key's match pattern
   * @param use the key's use expression
   */
  public void add(String name, AbstractPattern match, Expr use)
  {
    _keys.put(name, new Key(match, use));
  }

  public HashMap<String,Key> getKeys()
  {
    return _keys;
  }

  /**
   * Evaluate the function.
   *
   * @param pattern The context pattern.
   * @param args The evaluated arguments
   */
  public Object eval(Node node, ExprEnvironment env, 
                     AbstractPattern pattern, ArrayList args)
    throws XPathException
  {
    if (args.size() < 2)
      return null;

    String name = Expr.toString(args.get(0));
    Key key = _keys.get(name);
    Object value = args.get(1);

    if (key == null)
      return null;

    if (value == null)
      return null;

    ArrayList nodes = new ArrayList();
    if (value instanceof NodeList) {
      NodeList list = (NodeList) value;
      for (int i = 0; i < list.getLength(); i++)
        key(node, env, key._match, key._use, Expr.toString(list.item(i)), nodes);
    }
    else if (value instanceof ArrayList) {
      ArrayList list = (ArrayList) value;
      for (int i = 0; i < list.size(); i++)
        key(node, env, key._match, key._use, Expr.toString(list.get(i)), nodes);
    }
    else if (value instanceof Iterator) {
      Iterator iter = (Iterator) value;
      while (iter.hasNext())
        key(node, env, key._match, key._use, Expr.toString(iter.next()), nodes);
    }
    else
      key(node, env, key._match, key._use, Expr.toString(value), nodes);

    return nodes;
  }

  private void key(Node node, ExprEnvironment env, 
                   AbstractPattern match, Expr use, String value,
                   ArrayList nodes)
    throws XPathException
  {
    Iterator iter = match.select(node, env);
    while (iter.hasNext()) {
      Node subnode = (Node) iter.next();
      String nodeValue = use.evalString(subnode, env);

      if (value.equals(nodeValue))
        nodes.add(subnode);
    }
  }

  public static class Key {
    AbstractPattern _match;
    Expr _use;

    Key(AbstractPattern match, Expr use)
    {
      _match = match;
      _use = use;
    }

    public AbstractPattern getMatch()
    {
      return _match;
    }

    public Expr getUse()
    {
      return _use;
    }
  }
}
