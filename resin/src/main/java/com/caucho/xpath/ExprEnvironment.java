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

package com.caucho.xpath;

import com.caucho.xpath.expr.Var;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Adds variable environment.
 */
public interface ExprEnvironment {
  /**
   * Returns the value associated with name.  
   *
   * <p><em>name must be interned</em>
   */
  public Var getVar(String name);
  
  /**
   * Returns the value associated with name.  
   *
   * <p><em>name must be interned</em>
   */
  public XPathFun getFunction(String name);
  
  /**
   * Sets the node context.
   */
  public Node setContextNode(Node node);
  /**
   * Returns the StylesheetEnv
   */
  public StylesheetEnv getStylesheetEnv();
  /**
   * Return context node from the XPath expression context.
   */
  Node getContextNode();
  
  /**
   * Returns the context position from the XPath expression context.
   */
  int getContextPosition();
  
  /**
   * Returns the context size from the XPath expression context.
   */
  int getContextSize();
  
  /**
   * Returns the current node from the XSLT context; the same
   * as calling current() function from the XPath expression context.
   */
  Node getCurrentNode();
  
  /**
   * Returns a Document to be used for creating nodes.
   */
  Document getOwnerDocument();
  /**
   * Returns an Object representing the value of the system property
   * whose expanded-name has the specified namespace URI and local part.
   */
  Object systemProperty(String namespaceURI, String localName);
  /**
   * Returns the string-value of the specified Node.
   */
  String stringValue(Node n);
}
