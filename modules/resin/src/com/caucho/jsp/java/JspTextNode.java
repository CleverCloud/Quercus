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

import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

import java.io.IOException;

/**
 * Represents static text.
 */
public class JspTextNode extends JspNode {
  private String _text;
  
  public JspTextNode(JavaJspGenerator gen, String text, JspNode parent)
  {
    if (gen == null)
      throw new NullPointerException();
    
    setGenerator(gen);
    setQName(new QName("jsp", "text", JSP_NS));
    setParent(parent);

    _text = text;
  }

  /**
   * Gets the text.
   */
  public String getText()
  {
    return _text;
  }

  /**
   * sets the text.
   */
  public void setText(String text)
  {
    _text = text;
  }
  
  /**
   * Return true if the node only has static text.
   */
  public boolean isStatic()
  {
    return true;
  }

  /**
   * Returns true if whitespace.
   */
  public boolean isWhitespace()
  {
    String text = _text;
      
    for (int i = text.length() - 1; i >= 0; i--) {
      if (! XmlChar.isWhitespace(text.charAt(i)))
        return false ;
    }

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
    printXmlText(os, _text);
  }

  /**
   * Generates the code for the static text
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    out.addText(_text);
  }

  /**
   * Generates the code for the static text
   *
   * @param out the output writer for the generated java.
   */
  public void generateStatic(JspJavaWriter out)
    throws Exception
  {
    out.print(_text);
  }

  /**
   * Generates text from a string.
   *
   * @param out the output writer for the generated java.
   * @param string the text to generate.
   * @param offset the offset into the text.
   * @param length the length of the text.
   */
  private void generateText(JspJavaWriter out, String text,
                            int offset, int length)
    throws IOException
  {
    
    if (length > 32000) {
      generateText(out, text, offset, 16 * 1024);
      generateText(out, text, offset + 16 * 1024, length - 16 * 1024);
      return;
    }

    text = text.substring(offset, offset + length);
    int index = _gen.addString(text);
    
    out.print("out.write(_jsp_string" + index + ", 0, ");
    out.println("_jsp_string" + index + ".length);");
  }
}
