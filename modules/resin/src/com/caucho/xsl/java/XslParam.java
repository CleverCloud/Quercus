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
 * Represents an xsl:param node from the stylesheet.
 */
public class XslParam extends XslNode implements XslTopNode {
  private String _name;
  private String _select;
  private String _as;
  private String _require;

  private boolean _isGlobal;

  /**
   * returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:param";
  }
  
  /**
   * Returns the param name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the parent.
   */
  public void setParent(XslNode parent)
  {
    super.setParent(parent);

    if (parent instanceof XslStylesheet)
      _isGlobal = true;
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
    else if (name.getName().equals("as"))
      _as = value;
    else if (name.getName().equals("require"))
      _require = value;
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
      throw error(L.l("xsl:param needs a 'name' attribute."));

    if (_isGlobal)
      _gen.addGlobalParameter(_name);
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    /*
    print("_xsl_arg" + _callDepth + ".addVar(\"" + name + "\", ");
    generateString(value, '+', elt);
    println(");");
    */
    
    if (_isGlobal) {
      out.println("if (out.getParameter(\"" + _name + "\") != null)");
      out.println("  env.setGlobal(\"" + _name + "\", out.getParameter(\"" + _name + "\"));");
      out.println("else {");
      out.pushDepth();
    }

    if (_select != null) {
      if (_isGlobal) {
        out.print("env.setGlobal(\"" + _name + "\", ");
        out.println("_exprs[" + addExpr(_select) + "].evalObject(node, env));");
      }
      else {
        out.print("_exprs[" + addExpr(_select) + "]");
        out.println(".addParam(env, \"" + _name + "\", " +
                    "node, env);");
      }
    }
    else if (hasChildren()) {
      out.println("if (env.getVar(\"" + _name + "\") == null) {");
      out.pushDepth();
      
      String id = "frag" + _gen.generateId();
      
      out.println("XMLWriter " + id + " = out.pushFragment();");

      generateChildren(out);

      if (_isGlobal)
        out.print("env.setGlobal(\"");
      else
        out.print("env.addVar(\"");
      
      out.printJavaString(_name);
      out.println("\", out.popFragment(" + id + "));");
      
      out.popDepth();
      out.println("}");
    }
    
    if (_isGlobal) {
      out.popDepth();
      out.println("}");
    }
  }
}
