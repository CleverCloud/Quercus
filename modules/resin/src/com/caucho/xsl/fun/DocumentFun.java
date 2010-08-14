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

import com.caucho.vfs.Path;
import com.caucho.xml.CauchoNode;
import com.caucho.xml.LooseHtml;
import com.caucho.xml.QDocument;
import com.caucho.xml.Xml;
import com.caucho.xpath.Env;
import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.XPathFun;
import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xsl.TransformerImpl;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The document(...) function.
 */
public class DocumentFun extends XPathFun {
  private static final Logger log
    = Logger.getLogger(DocumentFun.class.getName());

  TransformerImpl _transformer;
  boolean _isHtml;

  public DocumentFun(TransformerImpl transformer)
  {
    _transformer = transformer;
  }

  public void setHtml(boolean isHtml)
  {
    _isHtml = isHtml;
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
    if (args.size() < 1)
      return null;

    Node basenode;
    String name = Expr.toString(args.get(0));

    if (args.size() > 1)
      basenode = Expr.toNode(args.get(1));
    else
      basenode = Expr.toNode(args.get(0));

    Path stylesheetPath = env.getStylesheetEnv().getPath();
    URIResolver resolver = _transformer.getURIResolver();

    Path path;

    if (name == null || name.equals(""))
      name = stylesheetPath.getTail();

    String systemId = null;

    DocumentType dtd = null;
    Document owner = null;

    if (basenode == null) {
    }
    else if (basenode.getOwnerDocument() != null) {
      owner = basenode.getOwnerDocument();
      dtd = owner.getDoctype();
    }
    else if (basenode instanceof Document) {
      owner = (Document) basenode;
      dtd = owner.getDoctype();
    }
    
    if (basenode instanceof CauchoNode)
      systemId = ((CauchoNode) basenode).getBaseURI();

    Path pwd = stylesheetPath.getParent();

    if (systemId == null && owner instanceof QDocument)
      systemId = ((QDocument) owner).getSystemId();

    if (systemId == null && dtd != null)
      systemId = dtd.getSystemId();

    if (systemId == null)
      systemId = stylesheetPath.getURL();

    Node doc = null;
    Source source = null;
    if (resolver != null) {
      try {
        source = resolver.resolve(name, systemId);
      } catch (TransformerException e) {
        throw new XPathException(e);
      }
    }

    if (source != null) {
      systemId = source.getSystemId();
      path = pwd.lookup(systemId);
    }
    else if (systemId != null) {
      pwd = pwd.lookup(systemId).getParent();

      path = pwd.lookup(name);
    }
    else
      path = pwd.lookup(name);

    _transformer.addCacheDepend(path);

    if (env instanceof Env)
      doc = (Node) ((Env) env).getCache(path);
    if (doc != null)
      return doc;

    try {
      if (_isHtml)
        doc = new LooseHtml().parseDocument(path);
      else
        doc = new Xml().parseDocument(path);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      //XXX:throw new XPathException(e);
    }

    if (env instanceof Env && source == null)
      ((Env) env).setCache(path, doc);

    return doc;
  }
}
