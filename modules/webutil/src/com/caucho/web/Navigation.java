/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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
 *
 * $Id: Navigation.java,v 1.2 2004/09/29 00:13:49 cvs Exp $
 */

package com.caucho.web;

import com.caucho.util.Tree;
import com.caucho.vfs.Path;
import com.caucho.xml.LooseXml;
import com.caucho.xpath.Env;
import com.caucho.xpath.Expr;
import com.caucho.xpath.XPath;
import com.caucho.xpath.XPathFun;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Iterator;

public class Navigation {
  private Element root;
  private String base;
  private Tree tree;

  public Navigation()
  {
  }

  public Navigation(Path path, String base)
    throws Exception
  {
    Document doc = new LooseXml().parseDocument(path);

    init(doc.getDocumentElement(), base);
  }

  public Navigation(Env env, Path path, String base)
    throws Exception
  {
    Document doc = new LooseXml().parseDocument(path);

    init(env, doc.getDocumentElement(), base);
  }

  /**
   * Create a new navigation structure.
   *
   * @param root the top of the navigation
   */
  public Navigation(Env env, Element root, String base)
    throws Exception
  {
    init(env, root, base);
  }

  public Navigation(Element root, String base)
    throws Exception
  {
    init(root, base);
  }

  public void init(Element root, String base)
    throws Exception
  {
    init(null,root,base);
  }

  public void init(Env env, Element root, String base)
    throws Exception
  {
    tree = new Tree(null);

    this.root = root;
    if (base == null || base == "")
      base = "/";

    this.base = base;
    
    if (root != null)
      fillChildren(env, tree, root.getFirstChild(), base);
  }

  public static Navigation createNested(Path pwd, String base)
    throws Exception
  {
    return createNested(null,pwd,base);
  }

  public static Navigation createNested(Env env, Path pwd, String base)
    throws Exception
  {
    Navigation baseNav = null;
    Navigation subNav = null;

    if (base.startsWith("/"))
      base = base.substring(1);
    
    String dir = base;
    while (true) {
      Path path = pwd.lookup(dir).lookup("toc.xml");

      Navigation nav = null;

      if (path.exists())
        nav = new Navigation(env, path, dir);

      if (baseNav == null)
        baseNav = nav;
      else if (nav != null)
        baseNav.linkParent(nav);

      if (dir.equals(""))
        break;

      int p;
      if (dir.endsWith("/")) {
        p = dir.lastIndexOf('/', dir.length() - 2);
      }
      else
        p = dir.lastIndexOf('/');

      if (p <= 0)
        dir = "";
      else
        dir = dir.substring(0, p + 1);
    }

    return baseNav;
  }

  public static Navigation createNested(ArrayList paths, String base)
    throws Exception
  {
    return createNested(null,paths,base);
  }

  public static Navigation createNested(Env env, ArrayList paths, String base)
    throws Exception
  {
    Navigation baseNav = null;
    Navigation subNav = null;

    if (base.startsWith("/"))
      base = base.substring(1);
    
    String dir = base;
    for (int i = 0; i < paths.size(); i++) {
      Path path = ((Path) paths.get(i)).lookup("toc.xml");

      Navigation nav = null;

      if (path.exists())
        nav = new Navigation(env, path, dir);

      if (baseNav == null)
        baseNav = nav;
      else if (nav != null)
        baseNav.linkParent(nav);

      if (dir.equals(""))
        break;

      int p;
      if (dir.endsWith("/")) {
        p = dir.lastIndexOf('/', dir.length() - 2);
      }
      else
        p = dir.lastIndexOf('/');

      if (p <= 0)
        dir = "";
      else
        dir = dir.substring(0, p + 1);
    }
    
    return baseNav;
  }

  public Navigation linkParent(Navigation parent)
  {
    if (tree == null) {
      tree = parent.tree;
      return this;
    }

    if (tree.getFirst() == null)
      return this;

    NavItem test = (NavItem) tree.getFirst().getData();
    NavItem link = parent.findURL(test.getLink());

    if (link == null)
      return null;

    Tree parentTree = link.getTree();
    linkTree(link.getTree(), tree.getFirst());
    this.tree = parent.tree;

    return this;
  }

  /**
   * Attaches the child tree in its proper location in the dest tree
   *
   * @param destTree parent tree 
   * @param subTree child tree
   */
  private void linkTree(Tree destTree, Tree subTree)
  {
    for (Tree child = subTree.getFirst();
         child != null;
         child = child.getNext()) {
      NavItem item = (NavItem) child.getData();
      Tree childTree = destTree.append(item);
      item.setTree(childTree);
      linkTree(childTree, child);
    }
  }

  /**
   * Returns an attribute from the top-level navigation element.
   *
   * @param name The name of the attribute.
   */
  public String getAttribute(String name)
  {
    if (root == null)
      return "";
    
    return root.getAttribute(name);
  }

  /**
   * @param url the url to match
   */
  public NavItem findURL(String url)
  {
    if (tree == null)
      return null;
    
    url = normalizeURL(url);
    
    Iterator iter = tree.dfs();
    while (iter.hasNext()) {
      Tree tree = (Tree) iter.next();
      NavItem item = (NavItem) tree.getData();

      if (item.getLink().equals(url)) {
        return item;
      }
    }

    return null;
  }
  
  /**
   */
  private void fillChildren(Env env, Tree tree, Node childNode, String base)
    throws Exception
  {
    XPathFun docShouldDisplay = env == null ? null : env.getFunction("doc-should-display");

    for (; childNode != null; childNode = childNode.getNextSibling()) {
      if (! childNode.getNodeName().equals("item"))
        continue;

      if (docShouldDisplay != null && !Expr.toBoolean(docShouldDisplay.eval(childNode,env,null,null)))
        continue;

      Element elt = (Element) childNode;

      NavItem item = new NavItem();
      String href = linkPattern.evalString(elt);

      item.setLink(resolveURL(href, childNode, base));
      item.setTitle(titlePattern.evalString(elt));

      String desc;
      desc = descPattern.evalString(elt);
      item.setDescription(desc);

      item.setProduct(_productPattern.evalString(elt));

      Tree childTree = tree.append(item);
      item.setTree(childTree);

      fillChildren(env, childTree, childNode.getFirstChild(), base);
    }
  }

  private String resolveURL(String url, Node node, String base)
  {
    if (url.length() == 0)
      return "/";
    
    if (url.startsWith("http:") || url.charAt(0) == '/')
      return url; //  normalizeURL(url);

    for (; node instanceof Element; node = node.getParentNode()) {
      Element elt = (Element) node;
    
      String nodeBase = elt.getAttribute("xml:base");

      if (nodeBase.equals(""))
        continue;
      
      if (! nodeBase.endsWith("/"))
        return resolveURL(nodeBase + "/" + url, elt.getParentNode(), base);
      else
        return resolveURL(nodeBase + url, elt.getParentNode(), base);
    }
    
    if (! base.endsWith("/"))
      return normalizeURL(base + "/" + url);
    else
      return normalizeURL(base + url);
  }
  
  private String normalizeURL(String url)
  {
    if (url.startsWith("/"))
      return url;
    else if (url.startsWith("http://"))
      return url;
    else
      return "/" + url;

    /*
    int i;
    for (i = "http://".length(); i < url.length(); i++) {
      if (url.charAt(i) == '/') {
        return url.substring(i);
      }
    }
    
    return "/";
    */
  }

  static Expr linkPattern;
  static Expr titlePattern;
  static Expr descPattern;
  static Expr _productPattern;
  static {
    try {
      linkPattern = XPath.parseExpr("if(@link,@link,link)");
      titlePattern = XPath.parseExpr("if(@title,@title,title)");
      descPattern = XPath.parseExpr("if(@description,@description,description)");
      _productPattern = XPath.parseExpr("if(@product,@product,product)");
    } catch (Exception e) {
    }
  }
}
