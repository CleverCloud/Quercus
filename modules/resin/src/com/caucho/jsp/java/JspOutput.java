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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp.java;

import com.caucho.jsp.JspParseException;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;

public class JspOutput extends JspNode {
  static final L10N L = new L10N(JspOutput.class);

  static final private QName OMIT_XML_DECLARATION =
    new QName("omit-xml-declaration");
  static final private QName DOCTYPE_SYSTEM =
    new QName("doctype-system");
  static final private QName DOCTYPE_PUBLIC =
    new QName("doctype-public");
  static final private QName DOCTYPE_ROOT_ELEMENT =
    new QName("doctype-root-element");

  private boolean _omitXmlDeclaration;

  private String _doctypeSystem;
  private String _doctypePublic;
  private String _doctypeRootElement;
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (! _gen.isXml())
      throw error(L.l("jsp:output is only allowed in jspx files."));
    
    if (OMIT_XML_DECLARATION.equals(name)) {
      _gen.setOmitXmlDeclaration(attributeToBoolean(name.getName(), value));
    }
    else if (DOCTYPE_SYSTEM.equals(name)) {
      String oldValue = _gen.getDoctypeSystem();

      if (oldValue != null && ! oldValue.equals(value)) {
        throw error(L.l("jsp:output doctype-system '{0}' conflicts with previous value '{1}'.  The doctype-system attribute may only be specified once.",
                        value, oldValue));
      }

      _gen.setDoctypeSystem(value);
      _doctypeSystem = value;
    }
    else if (DOCTYPE_PUBLIC.equals(name)) {
      String oldValue = _gen.getDoctypePublic();

      if (oldValue != null && ! oldValue.equals(value)) {
        throw error(L.l("jsp:output doctype-public '{0}' conflicts with previous value '{1}'.  The doctype-public attribute may only be specified once.",
                        value, oldValue));
      }

      _gen.setDoctypePublic(value);
      _doctypePublic = value;
    }
    else if (DOCTYPE_ROOT_ELEMENT.equals(name)) {
      String oldValue = _gen.getDoctypeRootElement();

      if (oldValue != null && ! oldValue.equals(value)) {
        throw error(L.l("jsp:output doctype-root-element '{0}' conflicts with previous value '{1}'.  The doctype-root-element attribute may only be specified once.",
                        value, oldValue));
      }

      _gen.setDoctypeRootElement(value);
      _doctypeRootElement = value;
    }
    else {
      throw error(L.l("'{0}' is an unknown jsp:output attribute.  Value attributes are: doctype-public, doctype-system, doctype-root-element.",
                      name.getName()));
    }
  }

  /**
   * When the element completes.
   */
  public void endElement()
    throws JspParseException
  {
    if (_doctypeSystem != null && _doctypeRootElement == null) {
      throw error(L.l("<jsp:output> with a 'doctype-system' attribute requires a 'doctype-root-element' attribute."));
    }
    
    if (_doctypePublic != null && _doctypeSystem == null) {
      throw error(L.l("<jsp:output> with a 'doctype-public' attribute requires a 'doctype-system' attribute."));
    }
    
    if (_doctypeRootElement != null && _doctypeSystem == null) {
      throw error(L.l("<jsp:output> with a 'doctype-root-element' attribute requires a 'doctype-system' attribute."));
    }
    
    _gen.setDoctypeSystem(_doctypeSystem);
    _gen.setDoctypePublic(_doctypePublic);
    _gen.setDoctypeRootElement(_doctypeRootElement);
  }
  
  /**
   * Return true if the node only has static text.
   */
  public boolean isStatic()
  {
    return true;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
  }
}
