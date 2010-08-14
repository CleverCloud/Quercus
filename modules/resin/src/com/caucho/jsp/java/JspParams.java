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
import java.util.ArrayList;

/**
 * Represents a Java scriptlet.
 */
public class JspParams extends JspNode {
  private ArrayList<JspParam> _params = new ArrayList<JspParam>();

  /**
   * Returns the param name.
   */
  public ArrayList<JspParam> getParams()
  {
    return _params;
  }

  /**
   * Adds a child.
   */
  public void addChild(JspNode node)
    throws JspParseException
  {
    if (node instanceof JspParam)
      _params.add((JspParam) node);
    else if (node instanceof JspBody) {
    }
    else
      super.addChild(node);
  }

  /**
   * Adds a child after initialization.
   */
  public void addChildEnd(JspNode node)
    throws JspParseException
  {
    if (node instanceof JspBody) {
      JspBody body = (JspBody) node;

      for (JspNode child : body.getChildren()) {
        if (child instanceof JspParam)
          _params.add((JspParam) child);
        else
          super.addChild(child);
      }
    }
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    throw new IOException(L.l("<jsp:params> does not generate code directly."));
  }

  /**
   * Generates the code for the jsp:params
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    throw error(L.l("<jsp:params> does not generate code directly."));
  }
}
