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

package com.caucho.xml;

/**
 * Loose XML parser interface.  The parser can parse directly into
 * the DOM, or it can be used as a SAX parser.
 *
 * <p>Loose XML is forgiving for some common lazy cases, e.g. the following
 * is allowed in LooseXml, but not XML
 * <pre><code>
 *   &lt;elt attr=1/>
 * </code></pre>
 *
 * <p>Also, Loose XML adds a convenient shortcut that's standard SGML but
 * not XML.
 * <code>&lt;foo/any text/></code> is equivalent to
 * <code>&lt;foo>any text&lt;/foo></code>
 *
 * <p>To parse a file into a DOM Document use
 * <pre><code>
 * Document doc = new LooseXml().parseDocument("foo.xml");
 * </code></pre>
 *
 * <p>To parse a string into a DOM Document use
 * <pre><code>
 * String xml = "&lt;top>small test&lt;/top>";
 * Document doc = new LooseXml().parseDocumentString(xml);
 * </code></pre>
 *
 * <p>To parse a file using the SAX API use
 * <pre><code>
 * LooseXml xml = new LooseXml();
 * xml.setContentHandler(myContentHandler);
 * xml.parse("foo.xml");
 * </code></pre>
 */
public class LooseXml extends XmlParser {
  /**
   * Create a new LooseXml parser.
   */
  public LooseXml()
  {
    super(new Policy(), null);
  }
}
