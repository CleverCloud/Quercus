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

package com.caucho.xml2;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.util.HashMap;

/**
 * CauchoDocument extends the DOM, providing namespaces
 */
public interface CauchoDocument extends Document, CauchoNode {
  public final static String DEPENDS = "caucho.depends";
  
  /**
   * Creates a namespace-aware element
   */
  public Element createElement(String prefix, String local, String url);
  /**
   * Creates a namespace-aware attribute
   */
  public Attr createAttribute(String prefix, String local, String url);

  public Text createUnescapedTextNode(String text);

  public HashMap getNamespaces();

  public Object getProperty(String name);
  public void setProperty(String name, Object value);
}
