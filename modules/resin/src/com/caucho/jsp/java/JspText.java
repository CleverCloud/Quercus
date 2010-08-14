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
import com.caucho.util.CharBuffer;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents static text.
 */
public class JspText extends JspNode {
  private ArrayList<JspNode> _children = new ArrayList<JspNode>();
  
  public JspText()
  {
  }

  /**
   * Adds text to the scriptlet.
   */
  public JspNode addText(String text)
  {
    JspNode node = new StaticText(_gen, text, this);
    
    _children.add(node);

    return node;
  }

  /**
   * Adds a child node.
   */
  public void addChild(JspNode node)
    throws JspParseException
  {
    if (node.getTagName().equals("resin-c:out"))
      _children.add(node);
    else
      super.addChild(node);
  }

  /**
   * Gets the text.
   */
  /*
  public String getText()
  {
    throw newreturn (String) _children.get(0);
  }
  */

  /**
   * sets the text.
   */
  /*
  public void setText(String text)
  {
    addText(text);
  }
  */
  
  /**
   * Return true if the node only has static text.
   */
  public boolean isStatic()
  {
    for (int i = 0; i < _children.size(); i++)
      if (! _children.get(i).isStatic())
        return false;

    
    return true;
  }

  /**
   * Returns the static text.
   */
  public void getStaticText(CharBuffer cb)
  {
    for (int i = 0; i < _children.size(); i++)
      _children.get(i).getStaticText(cb);
  }

  /**
   * Returns true if whitespace.
   */
  public boolean isWhitespace()
  {
    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);
      
      if (! (child instanceof StaticText))
        return false;

      if (! ((StaticText) child).isWhitespace())
        return false;
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
    os.print("<jsp:text");
    printJspId(os);
    os.print(">");

    for (int i = 0; i < _children.size(); i++)
      _children.get(i).printXml(os);

    os.print("</jsp:text>");
  }

  /**
   * Generates the start location.
   */
  public void generateStartLocation(JspJavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the code for the static text
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    for (int i = 0; i < _children.size(); i++)
      _children.get(i).generate(out);
  }

  //jsp/0416
  @Override
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    if (_children == null)
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      child.generatePrologue(out);
    }
  }

  //jsp/0416
  @Override
  public void generateTagStateChildren(JspJavaWriter out)
    throws Exception
  {
    if (_children != null) {
      for (int i = 0; i < _children.size(); i++) {
        JspNode child = _children.get(i);

        child.generateTagState(out);
      }
    }
  }

  //jsp/0416
  @Override
  public boolean hasCustomTag()
  {
    for (int i = 0; _children != null && i < _children.size(); i++) {
      JspNode child = _children.get(i);

      if (child instanceof CustomTag)
        return true;

      if (child.hasCustomTag())
        return true;
    }
    return false;
  }

  /**
   * Generates the code for the static text
   *
   * @param out the output writer for the generated java.
   */
  public void generateStatic(JspJavaWriter out)
    throws Exception
  {
    for (int i = 0; i < _children.size(); i++)
      _children.get(i).generateStatic(out);
  }
}
