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
 * HTML parser interface.  The parser can parse directly into the DOM, or it
 * can be used as a SAX parser.
 *
 * <p>To parse a file into a DOM Document use
 * <pre><code>
 * Document doc = new Html().parseDocument("foo.html");
 * </code></pre>
 *
 * <p>To parse a string into a DOM Document use
 * <pre><code>
 * String html = "&lt;h1>small test&lt;/h1>";
 * Document doc = new Html().parseDocumentString(html);
 * </code></pre>
 *
 * <p>To parse a file using the SAX API use
 * <pre><code>
 * Html html = new Html();
 * html.setContentHandler(myContentHandler);
 * html.parse("foo.html");
 * </code></pre>
 */
public class Html extends XmlParser {
  /**
   * Create a new HTML parser
   */
  public Html()
  {
    super(new HtmlPolicy(), null);
    _forgiving = true;
    
    init();
  }
}
