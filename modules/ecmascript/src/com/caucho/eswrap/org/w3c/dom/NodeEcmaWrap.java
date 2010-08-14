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
 *
 * $Id: NodeEcmaWrap.java,v 1.2 2004/09/29 00:13:10 cvs Exp $
 */

package com.caucho.eswrap.org.w3c.dom;

import com.caucho.xpath.XPath;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

import java.util.Iterator;

public class NodeEcmaWrap {
  public static Node find(Node node, String pattern)
    throws XPathException
  {
    return XPath.find(pattern, node);
  }

  public static Iterator select(Node node, String pattern)
    throws XPathException
  {
    return XPath.select(pattern, node);
  }

  public static Object evalObject(Node node, String pattern)
    throws XPathException
  {
    return XPath.evalObject(pattern, node);
  }

  public static double evalNumber(Node node, String pattern)
    throws XPathException
  {
    return XPath.evalNumber(pattern, node);
  }

  public static String evalString(Node node, String pattern)
    throws XPathException
  {
    return XPath.evalString(pattern, node);
  }
}
