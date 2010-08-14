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

package javax.servlet.jsp.jstl.tlv;

import java.io.*;
import java.util.*;
import javax.servlet.jsp.tagext.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class PermittedTaglibsTLV extends TagLibraryValidator {
  private HashSet<String> _permitted;
  
  public ValidationMessage []validate(String prefix, String uri, PageData data)
  {
    Map init = getInitParameters();

    String permitted = (String) init.get("permittedTaglibs");

    if (permitted == null || "".equals(permitted))
      return null;

    _permitted = new HashSet<String>();

    //spec required
    _permitted.add("http://java.sun.com/JSP/Page");
    
    //self
    _permitted.add(uri);

    String []values = permitted.split("[ \t\n\r]+");

    for (int i = 0; i < values.length; i++) {
      String permittedUri = values[i].trim();

      if (! "".equals(permittedUri))
        _permitted.add(permittedUri);
    }

    try {
      InputStream is = data.getInputStream();

      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setNamespaceAware(true);
      SAXParser parser = factory.newSAXParser();

      DefaultHandler handler = new Handler();

      parser.parse(is, handler);
    } catch (Exception e) {
      return new ValidationMessage[] {
        new ValidationMessage("", e.getMessage())
      };
    }
    
    return null;
  }

  private class Handler extends DefaultHandler {
    public void startElement(String uri, String localName,
                             String qName,
                             Attributes attributes)
      throws SAXException
    {
      if (uri != null
          && ! "".equals(uri)
          && ! _permitted.contains(uri)) {
        throw new SAXException(uri + " is not a permitted tag library");
      }
    }
  }
}
