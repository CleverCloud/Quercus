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
import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xsl.Sort;
import com.caucho.xsl.XslParseException;

import java.util.ArrayList;

/**
 * Represents any XSL node from the stylesheet.
 */
public class XslApplyTemplates extends XslNode {
  private String _select;
  private String _mode;

  private ArrayList<XslSort> _sorts = new ArrayList<XslSort>();

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:apply-templates";
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("select")) {
      _select = value;
    }
    else if (name.getName().equals("mode")) {
      _mode = value;
    }
    else
      super.addAttribute(name, value);
  }

  /**
   * Ends the attributes.
   */
  public void endAttributes()
    throws XslParseException
  {
  }

  /**
   * Adds a child node.
   */
  public void addChild(XslNode node)
    throws XslParseException
  {
    if (node instanceof XslSort) {
      _sorts.add((XslSort) node);
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
    AbstractPattern selectPattern = null;
    
    if (_select != null) {
      selectPattern = parseSelect(_select);
    }

    Sort []sort = null;

    if (_sorts.size() > 0) {
      sort = new Sort[_sorts.size()];

      for (int i = 0; i < _sorts.size(); i++)
        sort[i] = _sorts.get(i).generateSort();
    }
    

    if (sort != null && selectPattern == null) {
      // selectPattern = parseSelect("*", node);
      selectPattern = parseSelect("*");
    }

    pushCall(out);
    
    generateChildren(out);
    
    printApplyTemplates(out, selectPattern, _mode, sort);
    
    popCall(out);
  }

  /**
   * Prints code for xsl:apply-templates
   *
   * @param select the select pattern
   * @param mode the template mode
   * @param sort the sort expressions
   */
  private void printApplyTemplates(JavaWriter out,
                                   AbstractPattern select,
                                   String mode,
                                   Sort []sort)
    throws Exception
  {
    int min = 0;
    int max = Integer.MAX_VALUE;

    String applyName = "applyNode" + _gen.getModeName(mode);
    String env = "_xsl_arg" + _gen.getCallDepth();

    if (select == null && sort == null) {
      out.println("for (Node _xsl_node = node.getFirstChild();");
      out.println("     _xsl_node != null;");
      out.println("     _xsl_node = _xsl_node.getNextSibling()) {");
      out.println("  " + env + ".setSelect(node, null);");
      out.println("  " + env + ".setCurrentNode(_xsl_node);");
      out.println("  " + applyName + "(out, _xsl_node, " + env + ", " +
                  min + ", " + max + ");");
      out.println("}");
    }
    else if (sort == null) {
      int oldSelectDepth = _gen.getSelectDepth();

      String name = printSelectBegin(out, select, false, null);

      out.println(env + ".setSelect(node, _select_patterns[" +
                  _gen.addSelect(select) + "]);");
      out.println(env + ".setCurrentNode(" + name + ");");

      out.println(applyName + "(out, " + name + ", " + env + ", " + 
                  min + ", " + max + ");");

      for (; _gen.getSelectDepth() > oldSelectDepth; _gen.popSelectDepth()) {
        out.popDepth();
        out.println("}");
      }
    }
    else {
      int sortIndex = _gen.addSort(sort);
      
      out.println("{");
      out.pushDepth();
      out.println("ArrayList _xsl_list = xslSort(node, env" +
                  ", _select_patterns[" + _gen.addSelect(select) + "]" +
                  ", _xsl_sorts[" + sortIndex + "]);");
      out.println(env + ".setContextSize(_xsl_list.size());");
      out.println("for (int _xsl_i = 0; _xsl_i < _xsl_list.size(); _xsl_i++) {");
      out.println("  " + env + ".setContextPosition(_xsl_i + 1);");
      out.println("  " + applyName + "(out, (Node) _xsl_list.get(_xsl_i)" + 
              ", " + env + ", " + min + ", " + max + ");");
      out.println("}");
      out.popDepth();
      out.println("}");
    }
  }

  protected void popScope(JavaWriter out)
    throws Exception
  {
  }
}
