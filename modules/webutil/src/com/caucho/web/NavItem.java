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
 * $Id: NavItem.java,v 1.2 2004/09/29 00:13:49 cvs Exp $
 */

package com.caucho.web;

import com.caucho.util.Tree;

import java.util.ArrayList;
import java.util.Iterator;

public class NavItem {
  Tree tree;
  String title;
  String link;
  String description;
  String brief;
  String _product;

  NavItem()
  {
  }

  void setTree(Tree tree)
  {
    this.tree = tree;
  }

  /**
   * Returns the underlying tree.
   */
  Tree getTree()
  {
    return tree;
  }

  /**
   * Returns the parent item.
   */
  public NavItem getParent()
  {
    if (tree == null)
      return null;
    
    Tree parent = tree.getParent();

    return parent == null ? null : (NavItem) parent.getData();
  }

  public Iterator children()
  {
    return tree.iterator();
  }

  public String getTitle()
  {
    return title;
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  public String getProduct()
  {
    return _product;
  }

  public void setProduct(String product)
  {
    _product = product;
  }

  public String getBrief()
  {
    return brief;
  }

  public void setBrief(String brief)
  {
    this.brief = brief;
  }

  public String getLink()
  {
    return link;
  }

  public void setLink(String link)
  {
    this.link = link;
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  /**
   * Returns the previous sibling.
   */
  public NavItem getPrevious()
  {
    Tree prevTree = tree.getPrevious();
    
    if (prevTree == null)
      return null;
    else
      return (NavItem) prevTree.getData();
  }

  /**
   * Returns the previous item in a preorder DFS traversal.
   */
  public NavItem getPreviousPreorder()
  {
    Tree prevTree = tree.getPreviousPreorder();
    
    if (prevTree == null)
      return null;
    else
      return (NavItem) prevTree.getData();
  }

  /**
   * Returns the next sibling item.
   */
  public NavItem getNext()
  {
    Tree nextTree = tree.getNext();
    
    if (nextTree == null)
      return null;
    else
      return (NavItem) nextTree.getData();
  }

  /**
   * Returns the next item in a preorder DFS traversal.
   */
  public NavItem getNextPreorder()
  {
    Tree nextTree = tree.getNextPreorder();
    
    if (nextTree == null)
      return null;
    else
      return (NavItem) nextTree.getData();
  }

  /**
   * Returns the specialized family navigation
   */
  public ArrayList familyNavigation()
  {
    ArrayList list = new ArrayList();

    familyNavigation(tree, list);

    return list;
  }

  /**
   * Specialized to get a family navigation.
   */
  private boolean familyNavigation(Tree tree, ArrayList results)
  {
    if (tree == null) {
      return false;
    }

    boolean hasParent = false;
    if (tree.getParent() != null) {
      hasParent = familyNavigation(tree.getParent(), results);
    }

    Iterator iter = tree.iterator();

    boolean hasChild = false;
    while (iter.hasNext()) {
      NavItem child = (NavItem) iter.next();

      if (! hasChild && hasParent)
        results.add(null);
      
      hasChild = true;
      results.add(child);
    }

    return hasChild || hasParent;
  }

  public String toString()
  {
    return "[NavItem title='" + title + "' link='" + link + "']";
  }
}
