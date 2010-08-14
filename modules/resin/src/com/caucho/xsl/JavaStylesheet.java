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

import com.caucho.xpath.Env;
import com.caucho.xpath.Expr;
import com.caucho.xpath.NamespaceContext;
import com.caucho.xpath.Pattern;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

import java.util.HashMap;

/**
 * Implementation class for JavaScript stylesheets.  It is made public only
 * because generated Java classes need to access these
 * routines.
 */
public class JavaStylesheet extends StylesheetImpl {
  protected Pattern []patterns;
  protected Expr []exprs;
  protected Sort [][]_xsl_sorts;
  protected NamespaceContext []_namespaces;
  protected XslNumberFormat []_xsl_formats;

  protected int getTemplateId(HashMap templates,
                              Node node, Env env, int min, int max)
    throws XPathException
  {
    Template template = null;

    Template []templateList = (Template []) templates.get(node.getNodeName());
    if (templateList == null)
      templateList = (Template []) templates.get("*");

    int funId = 0;

    int oldPosition = env.setContextPosition(0);
    int oldSize = env.setContextSize(0);
    Node oldNode = env.setContextNode(null);
      
    for (int i = 0; templateList != null && i < templateList.length; i++) {
      Template subtemplate = templateList[i];

      if (min <= subtemplate.maxImportance &&
          subtemplate.maxImportance <= max &&
          subtemplate.pattern.match(node, env)) {
        funId = subtemplate.funId;
        break;
      }
    }
    
    env.setContextPosition(oldPosition);
    env.setContextSize(oldSize);
    env.setContextNode(oldNode);

    return funId;
  }
}
