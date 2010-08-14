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

import com.caucho.util.L10N;
import com.caucho.xml.DOMBuilder;
import com.caucho.xml.QDocument;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.TemplatesHandler;

public class TemplatesHandlerImpl extends DOMBuilder
  implements TemplatesHandler {
  protected static L10N L = new L10N(TemplatesHandlerImpl.class);

  Templates _templates;
  AbstractStylesheetFactory _factory;
  
  TemplatesHandlerImpl(AbstractStylesheetFactory factory)
  {
    _factory = factory;

    init(new QDocument());
  }

  public javax.xml.transform.Templates getTemplates()
  {
    if (_templates == null) {
      try {
        endDocument();
      } catch (Exception e) {
      }
    }
    
    return _templates;
  }

  public void endDocument()
    throws SAXException
  {
    super.endDocument();

    Node node = getNode();

    try {
      _templates = _factory.newTemplates(node);
    } catch (TransformerException e) {
      e.printStackTrace();
      throw new SAXException(e);
    }
  }
}
