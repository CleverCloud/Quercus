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

import com.caucho.jsp.JspParseException;
import com.caucho.vfs.WriteStream;

import java.io.IOException;

/**
 * Represents a Java scriptlet.
 */
public class JspExpression extends JspNode {
  private String _text;

  /**
   * Adds text to the expression.
   */
  public JspNode addText(String text)
  {
    _text = text;

    return null;
  }

  /**
   * Completes the expression.
   */
  public void endElement()
    throws JspParseException
  {
    if (_parseState.isScriptingInvalid() ||
        _parseState.isLocalScriptingInvalid())
      throw error(L.l("Script expressions are forbidden here.  Scripting has been disabled either:\n1) disabled by the web.xml scripting-invalid\n2) disabled in a tag's descriptor\n3) forbidden in <jsp:attribute> or <jsp:body> tags."));
  }
  
  /**
   * True if the node has scripting
   */
  public boolean hasScripting()
  {
    return true;
  }
  
  /**
   * True if the node has scripting element (i.e. not counting rtexpr values)
   */
  @Override
  public boolean hasScriptingElement()
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
    os.print("<jsp:expression");
    printJspId(os);
    os.print(">");

    printXmlText(os, _text);
    os.print("</jsp:expression>");
  }

  /**
   * Generates the code for the scriptlet
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    int length = _text.length();

    out.print("out.print((");

    // when an expression ends with '// comment', the code needs a
    // newline so following code doesn't become part of the comment
    boolean hasSlashes = false;

    for (int i = 0; i < length; i++) {
      char ch = _text.charAt(i);

      if (ch == '/' && i + 1 < length && _text.charAt(i + 1) == '/')
        hasSlashes = true;

      // destLine needs incrementing for newlines
      if (ch == '\n') {
        hasSlashes = false;
        out.println();
      }
      else 
        out.print(ch);
    }
    if (hasSlashes)
      out.println();
    
    // jsp/150l
    out.setLocation(_filename, _endLine);
    //  _endLine = -1;
    
    out.println("));");
  }
}
