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
import com.caucho.jsp.TagInstance;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import javax.servlet.jsp.tagext.TagInfo;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Special generator for a JSTL c:forEach tag.
 */
public class JstlCoreForEach extends JstlNode {
  private static final QName VAR = new QName("var");
  private static final QName VAR_STATUS = new QName("varStatus");
  
  private static final QName ITEMS = new QName("items");
  private static final QName BEGIN = new QName("begin");
  private static final QName END = new QName("end");
  private static final QName STEP = new QName("step");
  
  private String _var;
  private String _varStatus;

  private String _items;
  private JspAttribute _itemsAttr;

  private String _begin;
  private JspAttribute _beginAttr;

  private String _end;
  private JspAttribute _endAttr;

  private String _step;
  private JspAttribute _stepAttr;

  private boolean _isInteger;
  private int _depth;
  private String _tagVar;

  private TagInstance _tag;
  private boolean _isDeclaration;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (VAR.equals(name))
      _var = value;
    else if (VAR_STATUS.equals(name))
      _varStatus = value;
    else if (ITEMS.equals(name)) {
      _items = value;
      _attributeNames.add(name);
      _attributeValues.add(value);
    }
    else if (BEGIN.equals(name))
      _begin = value;
    else if (END.equals(name))
      _end = value;
    else if (STEP.equals(name))
      _step = value;
    else
      throw error(L.l("'{0}' is an unknown attribute for <{1}>.",
                      name.getName(), getTagName()));
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, JspAttribute value)
    throws JspParseException
  {
    if (ITEMS.equals(name))
      _itemsAttr = value;
    else if (BEGIN.equals(name))
      _beginAttr = value;
    else if (END.equals(name))
      _endAttr = value;
    else if (STEP.equals(name))
      _stepAttr = value;
    else
      throw error(L.l("'{0}' is an unknown jsp:attribute for <{1}>.",
                      name.getName(), getTagName()));
  }

  /**
   * Returns true if the tag has scripting values.
   */
  public boolean hasScripting()
  {
    return (super.hasScripting() ||
            hasScripting(_items) || hasScripting(_itemsAttr) ||
            hasScripting(_begin) || hasScripting(_beginAttr) ||
            hasScripting(_end) || hasScripting(_endAttr) ||
            hasScripting(_step) || hasScripting(_stepAttr));
  }

  /**
   * Returns true for an integer forEach.
   */
  public boolean isInteger()
  {
    return _items == null && _itemsAttr == null;
  }

  public TagInstance getTag()
  {
    return _tag;
  }

  /**
   * Returns the tag name for the current tag.
   */
  public String getCustomTagName()
  {
    if (_tag == null)
      return null;
    else
      return _tag.getId();
  }

  /**
   * Returns true for a simple tag.
   */
  public boolean isSimpleTag()
  {
    return false;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<c:forEach");
      
    if (_itemsAttr != null) {
      os.print(" items=\"");
      _itemsAttr.printXml(os);
      os.print("\"");
    }
    else if (_items != null) {
      os.print(" items=\"");
      printXmlText(os, _items);
      os.print("\"");
    }
      
    if (_beginAttr != null) {
      os.print(" begin=\"");
      _beginAttr.printXml(os);
      os.print("\"");
    }
    else if (_begin != null) {
      os.print(" begin=\"");
      printXmlText(os, _begin);
      os.print("\"");
    }
      
    if (_endAttr != null) {
      os.print(" end=\"");
      _endAttr.printXml(os);
      os.print("\"");
    }
    else if (_end != null) {
      os.print(" end=\"");
      printXmlText(os, _end);
      os.print("\"");
    }
      
    if (_stepAttr != null) {
      os.print(" step=\"");
      _stepAttr.printXml(os);
      os.print("\"");
    }
    else if (_step != null) {
      os.print(" step=\"");
      printXmlText(os, _step);
      os.print("\"");
    }

    os.print(">");

    printXmlChildren(os);

    os.print("</c:forEach>");
  }
  
  /**
   * Generates the prologue for the c:forEach tag.
   */
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    TagInstance parent = getParent().getTag();

    _tag = parent.findTag(getQName(), _attributeNames, false);

    if (_tag != null) {
      _tagVar = _tag.getId();
    }
    else {
      _isDeclaration = true;

      TagInfo tagInfo = _gen.getTag(getQName());

      _tag = parent.addTag(_gen, getQName(), tagInfo, null,
                           _attributeNames, _attributeValues, false);

      String id = "_jsp_loop_" + _gen.uniqueId();

      _tag.setId(id);
      
      _tagVar = _tag.getId();

      if (isInteger())
        out.println("com.caucho.jsp.IntegerLoopSupportTag " + _tagVar + " = null;");
      else
        out.println("com.caucho.jsp.IteratorLoopSupportTag " + _tagVar + " = null;");
    }

    generatePrologueChildren(out);
  }

  private boolean hasDeclaration()
  {
    return (_varStatus != null || hasTag());
  }

  /**
   * Returns the depth of the loop tags.
   */
  private int getDepth()
  {
    JspNode node = this;
    int depth = 0;

    for (; ! (node instanceof JspSegmentNode); node = node.getParent()) {
      if (node instanceof JstlCoreForEach) {
        JstlCoreForEach forEach = (JstlCoreForEach) node;

        if (forEach.isInteger() == isInteger())
          depth++;
      }
    }

    return depth;
  }

  /**
   * Returns true if this is the first declaration for the forEach
   */
  private boolean isFirst()
  {
    JspNode node = this;
    
    for (; ! (node instanceof JspSegmentNode); node = node.getParent()) {
    }
    
    return isFirst(node, getDepth()) == 1;
  }

  /**
   * Returns true if this is the first declaration for the forEach
   */
  private int isFirst(JspNode node, int depth)
  {
    if (node == this)
      return 1;
    else if (node instanceof JstlCoreForEach) {
      JstlCoreForEach forEach = (JstlCoreForEach) node;

      if (forEach.isInteger() == isInteger()
          && forEach.getDepth() == depth
          && forEach.hasDeclaration())
        return 0;
    }
    
    if (node instanceof JspContainerNode) {
      ArrayList<JspNode> children = ((JspContainerNode) node).getChildren();

      if (children == null)
        return -1;

      for (int i = 0; i < children.size(); i++) {
        JspNode child = children.get(i);

        int result = isFirst(child, depth);

        if (result >= 0)
          return result;
      }
    }
    
    return -1;
  }
  
  /**
   * Generates the code for the c:forEach tag.
   */
  @Override
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_items == null && _itemsAttr == null)
      generateIntegerForEach(out);
    else
      generateCollectionForEach(out);
  }
  
  /**
   * Generates the code for the c:forEach tag.
   */
  public void generateIntegerForEach(JspJavaWriter out)
    throws Exception
  {
    if (_begin == null && _beginAttr == null)
      throw error(L.l("required attribute 'begin' missing from <{0}>",
                      getTagName()));

    if (_end == null && _endAttr == null)
      throw error(L.l("required attribute 'end' missing from <{0}>",
                      getTagName()));

    int uniqueId = _gen.uniqueId();

    String oldStatusVar = "_jsp_status_" + uniqueId;

    if (_tagVar != null) {
      out.println(_tagVar + " = _jsp_state.get" + _tagVar
                  + "(pageContext, _jsp_parent_tag);");
    }

    String beginVar = "_jsp_begin_" + uniqueId;
    String endVar = "_jsp_end_" + uniqueId;
    String iVar = "_jsp_i_" + uniqueId;
    
    out.print("int " + beginVar + " = ");
    if (_beginAttr != null)
      out.print(_beginAttr.generateValue(int.class));
    else
      out.print(generateValue(int.class, _begin));
    out.println(";");

    out.print("int " + endVar + " = ");
    if (_endAttr != null)
      out.print(_endAttr.generateValue(int.class));
    else
      out.print(generateValue(int.class, _end));
    out.println(";");
    
    String stepVar = null;
    if (_step != null || _stepAttr != null) {
      stepVar = "_jsp_step_" + uniqueId;
      out.print("int " + stepVar + " = ");
      
      if (_stepAttr != null)
        out.print(_stepAttr.generateValue(int.class));
      else
        out.print(generateValue(int.class, _step));
      
      out.println(";");
    }
    else
      stepVar = "1";

    if (_tagVar != null)
      out.println(_tagVar +
                  ".init(" +
                  beginVar +
                  ", " +
                  endVar +
                  ", " +
                  stepVar +
                  ", " +
                  (_begin != null) +
                  ", " +
                  (_end != null) +
                  ", " +
                  (_step != null) +
                  ");");

    if (_varStatus != null) {
      out.print("Object " + oldStatusVar + " = pageContext.putAttribute(\"");
      out.print(escapeJavaString(_varStatus));
      out.println("\", " + _tagVar + ");");
    }

    out.print("for (int " + iVar + " = " + beginVar + "; ");
    out.print(iVar + " <= " + endVar + "; ");
    out.println(iVar + " += " + stepVar + ") {");
    out.pushDepth();

    if (_var != null) {
      out.print("pageContext.setAttribute(\"" + escapeJavaString(_var) + "\"");
      out.println(", new Integer(" + iVar + "));");
    }

    if (_tagVar != null) {
      out.println(_tagVar + ".setCurrent(" + iVar + ");");
    }

    generateChildren(out);
    
    out.popDepth();
    out.println("}");

    if (_var != null) {
      out.print("pageContext.pageSetOrRemove(\"");
      out.print(escapeJavaString(_var));
      out.println("\", null);");
    }

    if (_varStatus != null) {
      out.print("if (" + oldStatusVar + " instanceof javax.servlet.jsp.jstl.core.LoopTagStatus)");
      out.pushDepth();
      out.print("pageContext.pageSetOrRemove(\"");
      out.print(escapeJavaString(_varStatus));
      out.println("\", "+oldStatusVar+");");
      out.popDepth();
      out.println("else");
      out.pushDepth();
      out.print("pageContext.pageSetOrRemove(\"");
      out.print(escapeJavaString(_varStatus));
      out.println("\", null);");
      out.popDepth();
    }
  }

  
  /**
   * Generates the code for the c:forEach tag.
   */
  public void generateCollectionForEach(JspJavaWriter out)
    throws Exception
  {
    int uniqueId = _gen.uniqueId();

    String oldStatusVar = "_jsp_status_" + uniqueId;

    if (_tagVar != null) {
      out.println(_tagVar + " = _jsp_state.get" + _tagVar
                  + "(pageContext, _jsp_parent_tag);");
    }

    String itemsVar = "_jsp_items_" + uniqueId;

    out.print("java.lang.Object " + itemsVar + " = ");
    if (_itemsAttr != null)
      out.print(_itemsAttr.generateValue(Object.class));
    else
      out.print(generateParameterValue(Object.class,
                                       _items,
                                       true,
                                       _tag.getAttributeInfo("items"),
                                       _parseState.isELIgnored()));
    out.println(";");

    String mapperVar = "_jsp_vm_" + uniqueId;
    String deferredValue = null;

    if (_items != null && _items.contains("#{")) {
      deferredValue = "_caucho_value_expr_" + _gen.addValueExpr(_items, "");
    }

    if (deferredValue != null && _var != null) {
      out.print("javax.el.ValueExpression " + mapperVar
                  + " = _jsp_env.getVariableMapper().resolveVariable(\"");
      out.print(escapeJavaString(_var));
      out.println("\");");
    }
    
    String iterVar = "_jsp_iter_" + uniqueId;
    String iVar = "_jsp_i_" + uniqueId;
    out.println("java.util.Iterator " + iterVar
                + " = com.caucho.jstl.rt.CoreForEachTag.getIterator("
                + itemsVar + ");");

    String beginVar = null;
    if (_beginAttr != null || _begin != null) {
      beginVar = "_jsp_begin_" + uniqueId;
      out.print("int " + beginVar + " = ");
      if (_beginAttr != null)
        out.print(_beginAttr.generateValue(int.class));
      else
        out.print(generateValue(int.class, _begin));
      out.println(";");
    }

    String intVar = "_jsp_int_" + uniqueId;
    if (beginVar != null) {
      out.print("for (int " + intVar + " = " + beginVar + ";");
      out.println(intVar + " > 0; " + intVar + "--)");
      out.println("  if (" + iterVar + ".hasNext()) " + iterVar + ".next();");
    }

    String endVar = null;
    if (_endAttr != null || _end != null) {
      endVar = "_jsp_end_" + uniqueId;
      
      out.print("int " + endVar + " = ");
      if (_endAttr != null)
        out.print(_endAttr.generateValue(int.class));
      else
        out.print(generateValue(int.class, _end));
      out.println(";");
    }
    
    String stepVar = null;
    if (_step != null || _stepAttr != null) {
      stepVar = "_jsp_step_" + uniqueId;
      out.print("int " + stepVar + " = ");
      
      if (_stepAttr != null)
        out.print(_stepAttr.generateValue(int.class));
      else
        out.print(generateValue(int.class, _step));
      
      out.println(";");
    }
    else
      stepVar = "1";

    if (_tagVar != null) {
      out.print(_tagVar + ".init(");
      if (beginVar != null)
        out.print(beginVar + ", ");
      else
        out.print("0, ");
      
      if (endVar != null)
        out.print(endVar + ", ");
      else
        out.print("Integer.MAX_VALUE, ");

      out.print(stepVar + ", ");
      out.print((_begin != null) + ", ");
      out.print((_end != null) + ", ");
      out.println((_step != null) + ");");
    }

    if (_varStatus != null) {
      out.print("Object " + oldStatusVar + " = pageContext.putAttribute(\"");
      out.print(escapeJavaString(_varStatus));
      out.println("\", " + _tagVar + ");");
    }

    if (endVar != null) {
      String begin = beginVar == null ? "0" : beginVar;
      
      out.print("for (int " + intVar + " = " + begin + "; ");
      out.print(intVar + " <= " + endVar);

      out.print(" && " + iterVar + ".hasNext(); ");
      
      out.println(intVar + " += " + stepVar + ") {");
    }
    else
      out.println("while (" + iterVar + ".hasNext()) {");
    
    out.pushDepth();

    out.println("Object " + iVar + " = " + iterVar + ".next();");

    if (_tagVar != null) {
      out.println(_tagVar + ".setCurrent(" + iVar + ", " + iterVar + ".hasNext());");
    }

    if (_var != null) {
      if (deferredValue != null) {
        out.print("_jsp_env.getVariableMapper().setVariable(\"");
        out.print(escapeJavaString(_var));
        out.print("\", ");
        out.print("com.caucho.jstl.rt.CoreForEachTag.getExpr(");
        out.print(deferredValue + ", " + _tagVar + ".getIndex(), " + itemsVar);
        out.println(", \",\"));");
      } else {
        out.print("pageContext.setAttribute(\"" + escapeJavaString(_var) + "\"");
        out.println(", " + iVar + ");");
      }
    }

    generateChildren(out);

    if (! stepVar.equals("1")) {
      String stepI = "_jsp_si_" + uniqueId;

      out.print("for (int " + stepI + " = " + stepVar + "; ");
      out.println(stepI + " > 1; " + stepI + "--)");
      out.println("  if (" + iterVar + ".hasNext()) " + iterVar + ".next();");
      out.println("if (! " + iterVar + ".hasNext())");
      out.println("  break;");
    }
    
    out.popDepth();
    out.println("}");

    if (_var != null) {
      // restore EL variable
      if (deferredValue != null) {
        out.print("_jsp_env.getVariableMapper().setVariable(\"");
        out.print(escapeJavaString(_var));
        out.println("\", " + mapperVar + ");");
      } else {
        out.print("pageContext.pageSetOrRemove(\"");
        out.print(escapeJavaString(_var));
        out.println("\", null);");
      }
    }

    if (_varStatus != null) {
      out.print("if (" + oldStatusVar + " instanceof javax.servlet.jsp.jstl.core.LoopTagStatus)");
      out.pushDepth();
      out.print("pageContext.pageSetOrRemove(\"");
      out.print(escapeJavaString(_varStatus));
      out.println("\", "+oldStatusVar+");");
      out.popDepth();
      out.println("else");
      out.pushDepth();
      out.print("pageContext.pageSetOrRemove(\"");
      out.print(escapeJavaString(_varStatus));
      out.println("\", null);");
      out.popDepth();
    }
  }


  @Override
  public boolean hasCustomTag()
  {
    // uses TagState directly
    return true;
  }

  /**
   * Generates code before the actual JSP.
   */
  @Override
  public void generateTagState(JspJavaWriter out)
    throws Exception
  {
    if (! _isDeclaration) {
      super.generateTagState(out);
      return;
    }
    
    JspNode parentTagNode = getParent().getParentTagNode();
    String tagClass;
      
    if (_items == null && _itemsAttr == null)
      tagClass = "com.caucho.jsp.IntegerLoopSupportTag";
    else
      tagClass = "com.caucho.jsp.IteratorLoopSupportTag";

    out.print("private ");
    out.print(tagClass);
    out.println(" " + _tagVar + ";");

    out.println();
    out.print("final ");
    out.print(tagClass);
    out.println(" get" + _tagVar + "(PageContext pageContext, javax.servlet.jsp.tagext.JspTag _jsp_parent_tag) throws Throwable");
    out.println("{");
    out.pushDepth();
    
    out.println("if (" + _tagVar + " == null) {");
    out.pushDepth();

    out.println(_tagVar + " = new " + tagClass + "();");
      
    if (parentTagNode == null) {
      out.println(_tagVar + ".setParent((javax.servlet.jsp.tagext.Tag) null);");
    }
    else {
      out.println(_tagVar + ".setParent(" + parentTagNode.getCustomTagName() + ");");
    }
    
    out.popDepth();
    out.println("}");
    out.println();
    out.println("return " + _tagVar + ";");
    
    out.popDepth();
    out.println("}");

    super.generateTagState(out);
  }
}

