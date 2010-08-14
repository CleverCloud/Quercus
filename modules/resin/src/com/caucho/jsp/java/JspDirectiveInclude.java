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

package com.caucho.jsp.java;

import com.caucho.jsp.JspContentHandler;
import com.caucho.jsp.JspParseException;
import com.caucho.jsp.Namespace;
import com.caucho.jsp.ParseState;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;
import com.caucho.xml.Xml;

import org.xml.sax.SAXException;

import java.io.IOException;

public class JspDirectiveInclude extends JspNode {
  static L10N L = new L10N(JspDirectiveInclude.class);

  static private final QName FILE = new QName("file");

  private String _file;
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (FILE.equals(name))
      _file = value;
    else {
      throw error(L.l("'{0}' is an unknown JSP include directive attributes.  Compile-time includes need a 'file' attribute.",
                      name.getName()));
    }
  }

  /**
   * When the element completes.
   */
  public void endElement()
    throws JspParseException
  {
    if (_file == null)
      throw error(L.l("<{0}> needs a 'file' attribute.",
                      getTagName()));

    try {
      ParseState parseState = _gen.getParseState();
      
      if (parseState.isXml()) {
        Xml xml = new Xml();
        xml.setContentHandler(new JspContentHandler(parseState.getBuilder()));
        Path path = resolvePath(_file, parseState);

        path.setUserPath(_file);
        xml.setNamespaceAware(true);

        for (Namespace ns = parseState.getNamespaces();
             ns != null;
             ns = ns.getNext()) {
          xml.pushNamespace(ns.getPrefix(), ns.getURI());
        }

        xml.parse(path);
      }
      else
        _gen.getJspParser().pushInclude(_file);
    } catch (SAXException e) {
      throw error(e);
    } catch (IOException e) {
      throw error(e);
    }
  }

  private Path resolvePath(String value, ParseState parseState)
  {
    Path include;
    if (value.length() > 0 && value.charAt(0) == '/')
      include = parseState.resolvePath(value);
    else
      include = parseState.resolvePath(parseState.getUriPwd() + value);

    return include;
    /*
    String newUrl = _uriPwd;

    if (value.startsWith("/"))
      newUrl = value;
    else
      newUrl = _uriPwd + value;

    include.set

    return newUrl;
    */
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    // jsp/0354
    // os.print("<jsp:directive.include file=\"" + _file + "\"/>");
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
