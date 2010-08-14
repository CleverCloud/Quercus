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

import com.caucho.inject.Module;
import com.caucho.jsp.JspParseException;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a Java scriptlet.
 */
@Module
public class JspForward extends JspNode {
  private static final QName PAGE = new QName("page");
  
  private String _page;
  
  private ArrayList<JspParam> _params;
  
  /**
   * Adds an attribute.
   */
  @Override
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (PAGE.equals(name))
      _page = value;
    else
      throw error(L.l("`{0}' is an invalid attribute in <jsp:forward>",
                      name.getName()));
  }

  /**
   * Adds a parameter.
   */
  @Override
  public void addChild(JspNode node)
    throws JspParseException
  {
    if (node instanceof JspParam) {
      JspParam param = (JspParam) node;

      if (_params == null)
        _params = new ArrayList<JspParam>();

      _params.add(param);
    }
    else {
      super.addChild(node);
    }
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  @Override
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:forward");

    printXmlAttribute(os, "page", _page);

    os.print("></jsp:forward>");
  }

  /**
   * Generates the code for the scriptlet
   *
   * @param out the output writer for the generated java.
   */
  @Override
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_page == null)
      throw error(L.l("<jsp:forward> expects a `page' attribute.  `page' specifies the path to forward."));

    out.print("pageContext.forward(");
      
    generateIncludeUrl(out, _page, _params);
      
    out.print(");");

    if (_gen.isTag() || isInFragment())
      out.println("if (true) throw new SkipPageException();");
    else
      out.println("if (true) return;");
  }
  
}
