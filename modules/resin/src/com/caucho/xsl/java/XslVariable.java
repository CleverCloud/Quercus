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
import com.caucho.xml.QName;
import com.caucho.xsl.XslParseException;

/**
 * Sets an xsl variable.
 */
public class XslVariable extends XslNode implements XslTopNode {
  private String _name;
  private String _select;

  private boolean _isGlobal;

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:variable";
  }

  /**
   * Set true for global param.
   */
  public void setGlobal(boolean isGlobal)
  {
    _isGlobal = true;
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("name"))
      _name = value;
    else if (name.getName().equals("select"))
      _select = value;
    else
      super.addAttribute(name, value);
  }

  /**
   * Ends the attributes.
   */
  public void endAttributes()
    throws XslParseException
  {
    if (_name == null)
      throw error(L.l("'name' is a required attribute of <xsl:variable>"));
  }

  /**
   * Ends the element.
   */
  public void endElement()
  {
    addVariableCount();
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    if (_select != null) {
      int index = addExpr(_select);

      if (_isGlobal) {
        out.print("env.setGlobal(\"" + _name + "\", ");
        out.print("_exprs[" + index + "].evalObject(node, env));");
      }
      else {
        out.print("_exprs[" + index + "]");
      
        out.println(".addVar(env, \"" + _name + "\", node, env);");
      }
    }
    else {
      String id = "frag" + _gen.generateId();
      
      out.println("XMLWriter " + id + " = out.pushFragment();");

      generateChildren(out);
      
      if (_isGlobal)
        out.print("env.setGlobal(\"");
      else
        out.print("env.addVar(\"");
      
      out.printJavaString(_name);
      out.println("\", out.popFragment(" + id + "));");
    }
    /*
    boolean isDisabled = disable.equals("yes");
    if (isDisabled)
      startDisableEscaping();
    */

    // printStringExpr(out, _select);

    /*
    if (isDisabled)
      endDisableEscaping();
    */
  }

  protected void printPopScope(JavaWriter out)
    throws Exception
  {
  }
}
