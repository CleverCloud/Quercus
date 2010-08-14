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

package com.caucho.xsl.java;

import com.caucho.java.JavaWriter;
import com.caucho.xsl.XslParseException;

import java.util.ArrayList;

/**
 * Represents the xsl:choose element.
 */
public class XslChoose extends XslNode {
  private ArrayList<XslWhen> _tests = new ArrayList<XslWhen>();
  private XslOtherwise _otherwise;

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:choose";
  }

  /**
   * Adds a child node.
   */
  public void addChild(XslNode node)
    throws XslParseException
  {
    if (node instanceof XslWhen) {
      _tests.add((XslWhen) node);
    }
    else if (node instanceof XslOtherwise) {
      _otherwise = (XslOtherwise) node;
    }
    else
      super.addChild(node);
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    if (_tests.size() == 0) {
      if (_otherwise != null)
        _otherwise.generate(out);
    }
    else {
      for (int i = 0; i < _tests.size(); i++) {
        if (i != 0)
          out.print("else ");

        _tests.get(i).generate(out);
      }

      if (_otherwise != null) {
        out.print("else ");
        _otherwise.generate(out);
      }
    }
  }
}
