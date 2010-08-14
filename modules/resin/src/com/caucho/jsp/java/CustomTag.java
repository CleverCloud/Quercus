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

import java.util.ArrayList;

import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.JspIdConsumer;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TryCatchFinally;
import javax.servlet.jsp.tagext.VariableInfo;

import com.caucho.jsp.AnalyzedTag;
import com.caucho.xml.QName;

/**
 * Represents a custom tag.
 */
public class CustomTag extends GenericTag
{
  /**
   * Generates code before the actual JSP.
   */
  @Override
  public void generateTagState(JspJavaWriter out)
    throws Exception
  {
    if (isDeclaringInstance()) {
      out.print("private ");
      out.printClass(_tagClass);
      out.println(" " + _tag.getId() + ";");
                                           
      out.println();
      out.print("final ");
      out.printClass(_tagClass);
      out.println(" get" + _tag.getId() + "(PageContext pageContext, javax.servlet.jsp.tagext.JspTag _jsp_parent_tag) throws Throwable");
      out.println("{");
      out.pushDepth();
    
      out.println("if (" + _tag.getId() + " == null) {");
      out.pushDepth();

      generateTagInit(out);
    
      out.popDepth();
      out.println("}");
      out.println();
      out.println("return " + _tag.getId() + ";");
    
      out.popDepth();
      out.println("}");
    }

    super.generateTagState(out);
  }
  
  /**
   * Generates code before the actual JSP.
   */
  @Override
  public void generateTagRelease(JspJavaWriter out)
    throws Exception
  {
    if (isDeclaringInstance()) {
      out.println("if (" + _tag.getId() + " != null)");
      out.println("  " + _tag.getId() + ".release();");
    }
    
    super.generateTagRelease(out);
  }
  
  /**
   * Generates the code for a custom tag.
   *
   * @param out the output writer for the generated java.
   */
  @Override
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String name = _tag.getId();
    String tagHackVar = "_jsp_endTagHack" + _gen.uniqueId();
    Class<?> cl = _tagClass;

    AnalyzedTag analyzedTag = _tag.getAnalyzedTag();

    boolean isIterator = (IterationTag.class.isAssignableFrom(cl)
                          || BodyTag.class.isAssignableFrom(cl));
    boolean isBodyTag = BodyTag.class.isAssignableFrom(cl);
    boolean isCatch = TryCatchFinally.class.isAssignableFrom(cl);

    boolean isEmpty = isEmpty();
    boolean usesTagBody = (isBodyTag && ! isEmpty
                           && analyzedTag.getStartReturnsBuffered());
    boolean hasEndTag = analyzedTag.getDoEnd();
    
    if ("empty".equalsIgnoreCase(getBodyContent())) {
      if (! isEmpty)
        throw error(L.l("<{0}> expects an empty body", getTagName()));
    }
    if (usesTagBody && hasEndTag)
      out.println("com.caucho.jsp.BodyContentImpl " + tagHackVar + " = null;");
    else
      tagHackVar = "out";

    if (! isDeclared()) {
      out.println(name + " = _jsp_state.get" + name + "(pageContext, _jsp_parent_tag);");
    }

    if (JspIdConsumer.class.isAssignableFrom(_tag.getTagClass())) {
      out.println(name + ".setJspId(\"jsp" + _gen.generateJspId() + "\");");
      
      /*
      String shortName = className;
      int p = shortName.lastIndexOf('.');
      if (p >= 0)
        shortName = shortName.substring(p + 1);

      out.println(name + ".setJspId(\"" + shortName + "-" + _gen.generateJspId() + "\");");
      */
    }

    fillAttributes(out, name);

    printVarDeclare(out, VariableInfo.AT_BEGIN);

    String oldTag = "_jsp_writer" + _gen.uniqueId();
    
    if (analyzedTag.getDoCatch())
      out.println("javax.servlet.jsp.JspWriter " + oldTag + " = out;");
    
    if (analyzedTag.getDoCatch() || analyzedTag.getDoFinally()) {
      out.println("try {");
      out.pushDepth();
    }

    boolean hasStartTag = analyzedTag.getDoStart();
    int startCount = ((analyzedTag.getStartReturnsSkip() ? 1 : 0)
                      + (analyzedTag.getStartReturnsInclude() ? 1 : 0)
                      + (analyzedTag.getStartReturnsBuffered() ? 1 : 0));
    
    int thisId = _gen.uniqueId();
    if (! hasStartTag) {
    }
    else if (startCount == 1) {
      out.println(name + ".doStartTag();");
    }
    else {
      out.println("int _jspEval" + thisId + " = " + name + ".doStartTag();");
    }
    printVarAssign(out, VariableInfo.AT_BEGIN);

    if (analyzedTag.getStartReturnsSkip()
        && ! analyzedTag.getStartReturnsInclude()
        && ! analyzedTag.getStartReturnsBuffered()) {
      // jsp/18cp
      generateChildrenEmpty();
    }
    else if (isEmpty) {
      // jsp/18kc
      /*
      if (isBodyTag)
        out.println("  " + name + ".setBodyContent((javax.servlet.jsp.tagext.BodyContent) null);");
      */
    }
    else {
      if (startCount > 1 && analyzedTag.getStartReturnsSkip()) {
        out.println("if (_jspEval" + thisId + " != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {");
        out.pushDepth();
      }
      else if ((hasVarDeclaration(VariableInfo.NESTED)
               || childHasScriptlet())
               && ! (analyzedTag.getDoCatch()
                     || analyzedTag.getDoFinally()
                     || (analyzedTag.getDoAfter()
                         && analyzedTag.getAfterReturnsAgain()))) {
        out.println("{");
        out.pushDepth();
      }

      if (usesTagBody) {
        if (analyzedTag.getStartReturnsBuffered()
            && analyzedTag.getStartReturnsInclude()) {
          out.println("if (_jspEval" + thisId + " == javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_BUFFERED) {");
          out.pushDepth();
        }
        
        out.println("out = pageContext.pushBody();");

        if (hasEndTag) {
          out.println(tagHackVar + " = (com.caucho.jsp.BodyContentImpl) out;");
          out.println(name + ".setBodyContent(" + tagHackVar + ");");
        }
        else
          out.println(name + ".setBodyContent((javax.servlet.jsp.tagext.BodyContent) " + tagHackVar + ");");
        
        if (analyzedTag.getDoInit())
          out.println(name + ".doInitBody();");
        
        if (analyzedTag.getStartReturnsBuffered()
            && analyzedTag.getStartReturnsInclude()) {
          out.popDepth();
          out.println("}");

          // jsp/18kf - req by JSP TCK
          /*
          if (_tag.getBodyContent()) {
            out.println("else");
            out.println("  " + name + ".setBodyContent((javax.servlet.jsp.tagext.BodyContent) null);");
          }
          */
        }
      }
      else if (isBodyTag && _tag.getBodyContent())
        out.println(name + ".setBodyContent((javax.servlet.jsp.tagext.BodyContent) null);");

      if (analyzedTag.getDoAfter() && analyzedTag.getAfterReturnsAgain()) {
        out.println("do {");
        out.pushDepth();
      }

      out.setLocation(getFilename(), getStartLine());

      if (_children != null)
        printVarDeclaration(out, VariableInfo.NESTED);
      
      out.setLocation(getFilename(), getStartLine());
      
      generateChildren(out);

      out.setLocation(getFilename(), getEndLine());

      if (analyzedTag.getDoAfter() && analyzedTag.getAfterReturnsAgain()) {
        out.popDepth();
        out.println("} while (" + name + ".doAfterBody() == javax.servlet.jsp.tagext.IterationTag.EVAL_BODY_AGAIN);");
      }
      else if (analyzedTag.getDoAfter()) {
        out.println(name + ".doAfterBody();");
      }

      if (usesTagBody) {
        if (analyzedTag.getStartReturnsBuffered()
            && analyzedTag.getStartReturnsInclude()) {
          out.println("if (_jspEval" + thisId + " == javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_BUFFERED)");

          if (hasEndTag)
            out.println("  out = pageContext.popBody();");
          else
            out.println("  out = pageContext.popAndReleaseBody();");
        }
        else if (analyzedTag.getStartReturnsBuffered()) {
          if (hasEndTag)
            out.println("out = pageContext.popBody();");
          else
            out.println("out = pageContext.popAndReleaseBody();");
        }
      }

      if (startCount > 1 && analyzedTag.getStartReturnsSkip()) {
        out.popDepth();
        out.println("}");
      }
      else if (isEmpty) {
      }
      else if ((hasVarDeclaration(VariableInfo.NESTED)
               || childHasScriptlet())
               && ! (analyzedTag.getDoCatch()
                     || analyzedTag.getDoFinally()
                     || (analyzedTag.getDoAfter()
                         && analyzedTag.getAfterReturnsAgain()))) {
        out.popDepth();
        out.println("}");
      }
    }

    out.setLocation(getFilename(), getEndLine());
    
    int endCount = ((analyzedTag.getEndReturnsSkip() ? 1 : 0)
                    + (analyzedTag.getEndReturnsEval() ? 1 : 0));
    
    String endVar = "_jsp_end_" + _gen.uniqueId();

    if (! hasEndTag) {
    }
    else if (endCount > 1)
      out.println("int " + endVar + " = " + name + ".doEndTag();");
    else
      out.println(name + ".doEndTag();");

    if (! hasEndTag || ! usesTagBody) {
    }
    else if (hasStartTag
             && (analyzedTag.getStartReturnsSkip()
                 || analyzedTag.getStartReturnsInclude())) {
      out.println("if (" + tagHackVar + " != null) {");
      out.println("  pageContext.releaseBody(" + tagHackVar + ");");
      out.println("  " + tagHackVar + " = null;");
      out.println("}");
    }
    else {
      out.println("pageContext.releaseBody(" + tagHackVar + ");");
    }

    if (analyzedTag.getEndReturnsSkip()) {
      if (hasEndTag && endCount > 1)
        out.println("if (" + endVar + " == javax.servlet.jsp.tagext.Tag.SKIP_PAGE)");
      else
        out.println("if (true)");

      if (_gen.isTag() || isInFragment())
        out.println("  throw new SkipPageException();");
      else
        out.println("  return;");
    }
    
    if (analyzedTag.getDoCatch()) {
      String t = "_jsp_exn_" + _gen.uniqueId();
      
      out.popDepth();
      out.println("} catch (Throwable " + t + ") {");
      out.println("  pageContext.setWriter(" + oldTag + ");");
      out.println("  out = " + oldTag + ";");
      out.println("  " + name + ".doCatch(" + t + ");");
      out.pushDepth();
    }
    
    if (analyzedTag.getDoFinally()) {
      out.popDepth();
      out.println("} finally {");
      out.println("  " + name + ".doFinally();");
      out.pushDepth();
    }

    if (analyzedTag.getDoCatch() || analyzedTag.getDoFinally()) {
      out.popDepth();
      out.println("}");
    }
    
    printVarDeclaration(out, VariableInfo.AT_END);
    
    out.setLocation(getFilename(), getEndLine());
  }

  /**
   * Generates the initialization code for the tag.
   *
   * @param out the output stream
   */
  private void generateTagInit(JspJavaWriter out)
    throws Exception
  {
    String var = _tag.getId();
    
    if (_tag.getAnalyzedTag().getHasInjection()) {
      out.print(var + " = (");
      out.printClass(_tag.getTagClass());
      out.println(") _jsp_inject_" + _tag.getId() + ".create();");
    }
    else {
      out.print(var + " = new ");
      out.printClass(_tag.getTagClass());
      out.println("();");
    }

    JspNode parentTagNode = getParent().getParentTagNode();

    out.println(var + ".setPageContext(pageContext);");
    if (parentTagNode == null) {
      // jsp/100h
      out.println("if (_jsp_parent_tag instanceof javax.servlet.jsp.tagext.Tag)");
      out.println("  " + var + ".setParent((javax.servlet.jsp.tagext.Tag) _jsp_parent_tag);");
      out.println("else if (_jsp_parent_tag instanceof javax.servlet.jsp.tagext.SimpleTag)");
      out.println("  " + var + ".setParent(new javax.servlet.jsp.tagext.TagAdapter((javax.servlet.jsp.tagext.SimpleTag) _jsp_parent_tag));");
      out.println("else");
      out.println("  " + var + ".setParent((javax.servlet.jsp.tagext.Tag) null);");
    }
    else if (parentTagNode.isSimpleTag()) {
      String parentName = parentTagNode.getCustomTagName();
      
      out.println("if (" + parentName + "_adapter == null)");
      out.println("  " + parentName + "_adapter = new javax.servlet.jsp.tagext.TagAdapter(" + parentName + ");");
      out.println(var + ".setParent(" + parentName + "_adapter);");
    }
    else {
      String parentName = parentTagNode.getCustomTagName();
      
      out.println(var + ".setParent((javax.servlet.jsp.tagext.Tag) " + parentName + ");");
    }

    ArrayList<QName> names = _tag.getAttributeNames();
    for (int i = 0; i < names.size(); i++) {
      QName name = names.get(i);

      String value = _tag.getAttribute(name);
      if (value == null)
        continue;
      
      TagAttributeInfo attrInfo = _tag.getAttributeInfo(name.getLocalName());

      boolean isRequestTime = true;

      if (attrInfo != null)
        isRequestTime = attrInfo.canBeRequestTime();

      generateSetAttribute(out, var, name, value,
                           isRequestTime,
                           false, attrInfo);
    }
  }

  /**
   * Returns true if the node or one of its children is a scriptlet
   */
  private boolean childHasScriptlet()
  {
    ArrayList<JspNode> children = getChildren();

    if (children == null)
      return false;

    for (int i = 0; i < children.size(); i++) {
      JspNode child = children.get(i);

      if (child instanceof JspScriptlet || child instanceof JspExpression)
        return true;
    }

    return false;
  }
}
