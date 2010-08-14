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
 * Represents the xsl:for-each element.
 */
public class XslForEach extends XslNode {
  private String _select;

  private ArrayList<XslSort> _sorts = new ArrayList<XslSort>();

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("select")) {
      _select = value;
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
    /*
    if (_select == null)
      throw error(L.l("<xsl:for-each> requires a 'select' attribute."));
    */
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
    if (_sorts.size() != 0) {
      printForEachSort(out);
      return;
    }
    
    out.println("{");
    out.pushDepth();

    AbstractPattern select = parseSelect(_select);
    
    boolean hasExprEnv = ! allowJavaSelect(select);

    int id = _gen.generateId();
    
    String sel = "_xsl_sel" + id;
    String oldCxt = "_xsl_cxt" + id;
    String oldCur = "_xsl_cur" + id;
    String oldSel = "_xsl_old_sel" + id;
    String oldEnv = "_xsl_env" + id;
    String oldSize = "_xsl_old_size" + id;

    out.println("com.caucho.xpath.pattern.AbstractPattern " + sel + ";");
    out.print(sel + " = _select_patterns[");
    out.print(_gen.addSelect(select));
    out.println("];");
    out.println("Node " + oldCxt + " = env.getContextNode();");
    out.println("Node " + oldCur + " = env.getCurrentNode();");
    
    if (! hasExprEnv) {
      out.println("AbstractPattern " + oldSel + " = env.setSelect(node, " + sel + ");");
      out.println("int " + oldSize + " = env.setContextSize(0);");
    }
    
    
    // String pos = "_xsl_pos" + unique++;
    String iter = "_xsl_iter" + _gen.generateId();

    int oldSelectDepth = _gen.getSelectDepth();
    
    // println("int " + pos + " = 0;");

    boolean hasEnv = false;
    
    if (allowJavaSelect(select)) {
      out.println("ExprEnvironment " + oldEnv + " = env.setExprEnv(null);");
      
      String ptr = printSelectBegin(out, select, true, null);

      _gen.pushLoop();
      out.println("Node " + _gen.getElement() + " = node;");
      out.println("node = " + ptr + ";");
    }
    else {
      out.print("NodeIterator " + iter + " = " + sel);
      out.println(".select(node, " + getEnv() + ");");
      out.println("ExprEnvironment " + oldEnv + " = env.setExprEnv(" + iter + ");");
      out.println("while (" + iter + ".hasNext()) {");
      out.pushDepth();
      _gen.setSelectDepth(_gen.getSelectDepth() + 1);
      
      _gen.pushLoop();
      
      out.println("Node " + _gen.getElement() + " = node;");
      out.println("node = " + iter + ".nextNode();");
      
    }
    out.println("env.setCurrentNode(node);");
    
    // println(pos + "++;");

    // String oldPos = currentPos;
    // currentPos = pos;

    AbstractPattern oldNodeListContext = _gen.getNodeListContext();
    _gen.setNodeListContext(parseMatch(_select));

    generateChildren(out);

    _gen.setNodeListContext(oldNodeListContext);
    
    // currentPos = oldPos;
    
    out.println("node = " + _gen.getElement() + ";");
    out.println("env.setCurrentNode(" + oldCur + ");");

    int selectDepth = _gen.getSelectDepth();
    
    for (; selectDepth > oldSelectDepth; selectDepth--) {
      out.popDepth();
      out.println("}");
    }
    _gen.setSelectDepth(oldSelectDepth);
    
    out.println("env.setExprEnv(" + oldEnv + ");");
    
    if (! hasExprEnv) {
      out.println("env.setSelect(" + oldCxt + ", " + oldSel + ");");
      out.println("env.setContextSize(" + oldSize + ");");
    //println("env.setCurrentNode(node);");
    }
    
    out.popDepth();
    out.println("}");

    _gen.popLoop();
  }
  
  /*
  public void generate(JavaWriter out)
    throws Exception
  {
    Sort []sort = new Sort[_sorts.size()];

    for (int i = 0; i < _sorts.size(); i++)
      sort[i] = _sorts.get(i).generateSort();
    
    out.println("{");
    out.pushDepth();

    AbstractPattern select = parseSelect(_select);
    
    boolean hasExprEnv = ! allowJavaSelect(select);

    int id = _gen.generateId();
    
    String sel = "_xsl_sel" + id;
    String oldCxt = "_xsl_cxt" + id;
    String oldCur = "_xsl_cur" + id;
    String oldSel = "_xsl_old_sel" + id;
    String oldEnv = "_xsl_env" + id;
    
    out.println("env.setCurrentNode(node);");
    String pos = "_xsl_pos" + _gen.generateId();
    String list = "_xsl_list" + _gen.generateId();

    int sortIndex = _gen.addSort(sort);
    
    println("Node " + oldCxt + " = env.getContextNode();");
    println("Node " + oldCur + " = env.getCurrentNode();");
    
    out.println("ArrayList " + list +
            " = xslSort(node, env" +
            ", _select_patterns[" + _gen.addSelect(select) + "]" +
            ", _xsl_sorts[" + sortIndex + "]);");
    out.println("env.setContextSize(" + list + ".size());");
    out.println("for (int " + pos + " = 1; " + pos +
            " <= " + list + ".size(); " + pos + "++) {");
    _gen.pushLoop();
    out.pushDepth();
    out.println("Node " + _gen.getElement() + " = node;");
    out.println("node = (Node) " + list + ".get(" + pos + " - 1);");

    String oldPos = _gen.getCurrentPosition();
    _gen.setCurrentPosition(pos);
    
    out.println("env.setPosition(" + pos + ");");
    
    AbstractPattern oldNodeListContext = _gen.getNodeListContext();
    _gen.setNodeListContext(parseMatch(_select));

    generateChildren(out);

    _gen.setCurrentPosition(oldPos);

    _gen.setNodeListContext(oldNodeListContext);
    
    out.println("node = " + _gen.getElement() + ";");
    
    out.popDepth();
    out.println("}");
    _gen.popLoop();
    out.popDepth();
    out.println("}");
  }
  */

  public void printForEachSort(JavaWriter out)
    throws Exception
  {
    Sort []sort = new Sort[_sorts.size()];

    for (int i = 0; i < _sorts.size(); i++)
      sort[i] = _sorts.get(i).generateSort();
    
    out.println("{");
    out.pushDepth();

    AbstractPattern select = parseSelect(_select);
    
    boolean hasExprEnv = ! allowJavaSelect(select);

    int id = _gen.generateId();
    
    String sel = "_xsl_sel" + id;
    String oldCxt = "_xsl_cxt" + id;
    String oldCur = "_xsl_cur" + id;
    String oldSel = "_xsl_old_sel" + id;
    String oldEnv = "_xsl_env" + id;
    
    out.println("env.setCurrentNode(node);");
    String pos = "_xsl_pos" + _gen.generateId();
    String list = "_xsl_list" + _gen.generateId();

    int sortIndex = _gen.addSort(sort);
    
    out.println("Node " + oldCxt + " = env.getContextNode();");
    out.println("Node " + oldCur + " = env.getCurrentNode();");
    
    out.println("ArrayList " + list +
            " = xslSort(node, env" +
            ", _select_patterns[" + _gen.addSelect(select) + "]" +
            ", _xsl_sorts[" + sortIndex + "]);");
    out.println("env.setContextSize(" + list + ".size());");
    out.println("for (int " + pos + " = 1; " + pos +
            " <= " + list + ".size(); " + pos + "++) {");
    _gen.pushLoop();
    out.pushDepth();
    out.println("Node " + _gen.getElement() + " = node;");
    out.println("node = (Node) " + list + ".get(" + pos + " - 1);");

    String oldPos = _gen.getCurrentPosition();
    _gen.setCurrentPosition(pos);
    
    out.println("env.setPosition(" + pos + ");");
    
    AbstractPattern oldNodeListContext = _gen.getNodeListContext();
    _gen.setNodeListContext(parseMatch(_select));

    generateChildren(out);

    _gen.setCurrentPosition(oldPos);

    _gen.setNodeListContext(oldNodeListContext);
    
    out.println("node = " + _gen.getElement() + ";");
    
    out.popDepth();
    out.println("}");
    _gen.popLoop();
    out.popDepth();
    out.println("}");
  }
}
