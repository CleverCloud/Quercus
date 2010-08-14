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

public class ScriptFreeTLV extends TagLibraryValidator {
  private boolean _isAllowDeclarations;
  private boolean _isAllowScriptlets;
  private boolean _isAllowExpressions;
  private boolean _isAllowRTExpressions;
  
  public ValidationMessage []validate(String prefix, String uri, PageData data)
  {
    Map init = getInitParameters();

    _isAllowDeclarations = "true".equals(init.get("allowDeclarations"));
    _isAllowScriptlets = "true".equals(init.get("allowScriptlets"));
    _isAllowExpressions = "true".equals(init.get("allowExpressions"));
    _isAllowRTExpressions = "true".equals(init.get("allowRTExpressions"));

    try {
      InputStream is = data.getInputStream();

      SAXParserFactory factory = SAXParserFactory.newInstance();
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
      boolean isValid = true;
      
      if ("jsp:expression".equals(qName))
        isValid = _isAllowExpressions;
      else if ("jsp:declaration".equals(qName))
        isValid = _isAllowDeclarations;
      else if ("jsp:scriptlet".equals(qName))
        isValid = _isAllowScriptlets;

      if (! isValid)
        throw new SAXException(qName + " is not allowed in a script-free JSP page");

      if (! _isAllowRTExpressions && attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          String value = attributes.getValue(i);

          if (value != null && value.indexOf("%=") >= 0)
            throw new SAXException("Runtime expression <" + value + "> is not allowed in a script-free JSP page");
            
        }
      }
    }
  }
}
