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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.dom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.util.LruCache;
import com.caucho.xpath.Expr;

public class DOMXPath
{
  private DOMNamespaceContext _context;
  private DOMDocument _document;
  
  public static DOMXPath __construct(Env env, DOMDocument document)
  {
    return new DOMXPath(env, document);
  }
  
  private DOMXPath(Env env, DOMDocument document)
  {
    _document = document;
  }
  
  public Object evaluate(Env env,
                         String expression)
  {
    Node node = _document.getDelegate();
    
    NodeList nodeList = (NodeList) query(env, expression, node);

    if (nodeList.getLength() == 1) {
      return _document.wrap(nodeList.item(0));
    }
    else
      return _document.wrap(nodeList);
      
  }
  
  public DOMNodeList query(Env env,
                           String expression,
                           @Optional DOMNode<Node> contextNode)
  {
    Node node;
    
    if (contextNode != null)
      node = contextNode.getDelegate();
    else
      node = _document.getDelegate();
    
    NodeList nodeList = (NodeList) query(env, expression, node);

    return _document.wrap(nodeList);
  }

  private NodeList query(Env env, String pattern, Node node)
  {
    // the JDKs xpath is extremely inefficient, causing benchmark
    // problems with mediawiki
    
    try {
      Expr expr = com.caucho.xpath.XPath.parseExpr(pattern);

      return (NodeList) expr.evalObject(node);
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /*
  private NodeList query(Env env, String pattern, Node node)
  {
    try {
      if (_context == null) {
        Quercus quercus = env.getQuercus();

        ExpressionCache cache
          = (ExpressionCache) quercus.getSpecial("caucho.domxpath.cache");

        if (cache == null) {
          cache = new ExpressionCache();
          quercus.setSpecial("caucho.domxpath.cache", cache);
        }

        XPathExpression expr = cache.compile(pattern);

        NodeList nodeList
          = (NodeList) expr.evaluate(node, XPathConstants.NODESET);

        cache.free(pattern, expr);

        return nodeList;
      }
      else {
        XPath xpath = (XPath) env.getSpecialValue("caucho.domxpath.xpath");

        if (xpath == null) {
          XPathFactory factory = XPathFactory.newInstance();
          xpath = factory.newXPath();
          env.setSpecialValue("caucho.domxpath.xpath", xpath);
        }

        xpath.setNamespaceContext(_context);

        XPathExpression expr = xpath.compile(pattern);

        NodeList nodeList
          = (NodeList) expr.evaluate(node, XPathConstants.NODESET);

        return nodeList;
      }
    }
    catch (XPathExpressionException e) {
      throw new QuercusModuleException(e);
    }
  }
  */
  
  public boolean registerNamespace(String prefix, String namespaceURI)
  {
    if (_context == null)
      _context = new DOMNamespaceContext();
    
    _context.addNamespace(prefix, namespaceURI);

    return true;
  }
  
  public class DOMNamespaceContext
    implements NamespaceContext
  {
    private HashMap<String, LinkedHashSet<String>> _namespaceMap
      = new HashMap<String, LinkedHashSet<String>>();
    
    protected void addNamespace(String prefix, String namespaceURI)
    {
      LinkedHashSet<String> list = _namespaceMap.get(namespaceURI);
      
      if (list == null) {
        list = new LinkedHashSet<String>();
        
        _namespaceMap.put(namespaceURI, list);
      }

      list.add(prefix);
    }
    
    public String getNamespaceURI(String prefix)
    {
      for (Map.Entry<String, LinkedHashSet<String>> entry
           : _namespaceMap.entrySet()) {
        if (entry.getValue().contains(prefix))
          return entry.getKey();
      }

      return null;
    }
    
    public String getPrefix(String namespaceURI)
    {
      Iterator<String> iter = getPrefixes(namespaceURI);
      
      if (iter != null)
        return iter.next();
      else
        return null;
    }
    
    public Iterator<String> getPrefixes(String namespaceURI)
    {
      LinkedHashSet<String> prefixList = _namespaceMap.get(namespaceURI);
      
      if (prefixList != null)
        return prefixList.iterator();
      else
        return null;
    }
  }

  static class ExpressionCache {
    private final XPathFactory _factory = XPathFactory.newInstance();
    
    private final LruCache<String,ExpressionEntry> _xpathCache
      = new LruCache<String,ExpressionEntry>(1024);

    XPathExpression compile(String pattern)
      throws XPathExpressionException
    {
      ExpressionEntry entry = _xpathCache.get(pattern);
      XPathExpression expr = null;

      if (entry != null)
        expr = entry.allocate();

      if (expr == null) {
        XPath xpath = _factory.newXPath();
        expr = xpath.compile(pattern);
      }

      return expr;
    }

    void free(String pattern, XPathExpression expr)
    {
      ExpressionEntry entry = _xpathCache.get(pattern);

      if (entry == null) {
        entry = new ExpressionEntry();
        _xpathCache.put(pattern, entry);
      }

      entry.free(expr);
    }
  }

  static class ExpressionEntry {
    private XPathExpression _expr;

    XPathExpression allocate()
    {
      synchronized (this) {
        XPathExpression expr = _expr;
        _expr = null;

        return expr;
      }
    }

    void free(XPathExpression expr)
    {
      synchronized (this) {
        _expr = expr;
      }
    }
  }
}
