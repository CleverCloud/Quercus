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

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;

/**
 * The unparsed-entity(...) function.
 */
public class UnparsedEntityFun extends XPathFun {
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
    if (args.size() < 1)
      return "";

    String name = Expr.toString(args.get(0));
    Document doc = node.getOwnerDocument();
    DocumentType dtd = doc.getDoctype();
    NamedNodeMap map = dtd.getEntities();
    Entity entity = (Entity) map.getNamedItem(name);
    
    if (entity == null)
      return "";
    else if (entity.getSystemId() != null && ! entity.getSystemId().equals(""))
      return entity.getSystemId();
    else
      return entity.getPublicId();
  }
}
