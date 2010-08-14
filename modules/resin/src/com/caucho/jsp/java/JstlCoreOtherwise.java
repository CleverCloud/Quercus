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
import com.caucho.vfs.WriteStream;

import java.io.IOException;

/**
 * Special generator for a JSTL c:otherwise tag.
 */
public class JstlCoreOtherwise extends JstlNode {
  /**
   * Called after all the attributes from the tag.
   */
  @Override
  public void endAttributes()
    throws JspParseException
  {
    if (! (getParent() instanceof JstlCoreChoose)) {
      throw error(L.l("c:otherwise must be contained in a c:choose tag."));
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
    os.print("<c:otherwise>");

    printXmlChildren(os);

    os.print("</c:otherwise>");
  }
  
  /**
   * Generates the code for the c:otherwise tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    out.println("{");
    out.pushDepth();
    generateChildren(out);

    out.popDepth();
    out.println("}");
  }
}
