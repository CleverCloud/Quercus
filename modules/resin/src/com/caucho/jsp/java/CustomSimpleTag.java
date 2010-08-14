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
import com.caucho.jsp.TagInstance;
import com.caucho.xml.QName;

import javax.servlet.jsp.tagext.JspIdConsumer;
import javax.servlet.jsp.tagext.VariableInfo;
import java.util.ArrayList;

/**
 * Represents a custom tag.
 */
public class CustomSimpleTag extends GenericTag
{
  JspBody _body;
  private boolean _oldLocalScriptingInvalid;

  /**
   * Simple tags can't be reused.
   */
  public boolean isReuse()
  {
    return false;
  }

  @Override
  protected void addTagDepend()
  {
    // jsp/10i0 - #3578
  }
  
  /**
   * Called when the attributes end.
   */
  public void endAttributes()
    throws JspParseException
  {
    super.endAttributes();
    
    _oldLocalScriptingInvalid = _parseState.isLocalScriptingInvalid();
    // jsp/18dj
    // _parseState.setScriptingInvalid(true);
  }
  
  /**
   * Adds a child node.
   */
  public void endElement()
    throws Exception
  {
    super.endElement();
    
    _parseState.setLocalScriptingInvalid(_oldLocalScriptingInvalid);
    
    if (isEmpty())
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode node = (JspNode) _children.get(i);

      if (node instanceof JspBody) {
        if (_body != null)
          throw error(L.l("Only one <jsp:body> is allowed as a child of a tag."));

        _body = (JspBody) node;
        _children.remove(i);
        return;
      }
    }

    _body = new JspBody();
    _body.setParent(this);
    _body.setStartLocation(_sourcePath, _filename, _startLine);
    _body.setGenerator(_gen);
    _body.endAttributes();
    _body.setEndLocation(_filename, _startLine);
    
    for (int i = 0; i < _children.size(); i++) {
      JspNode node = _children.get(i);

      if (! (node instanceof JspAttribute))
        _body.addChild(node);
      
      _body.setEndLocation(node.getFilename(), node.getEndLine());
    }
    _body.endElement();
    _children = null;
  }
  
  /**
   * Set true if the node contains a child tag.
   */
  public boolean hasCustomTag()
  {
    if (_body != null && _body.hasCustomTag())
      return true;
    else
      return super.hasCustomTag();
  }
  
  /**
   * Generates code before the actual JSP.
   */
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    super.generatePrologue(out);

    if (_body != null) {
      _body.setJspFragment(true);
      _body.generateFragmentPrologue(out);
    }
    
    if (hasCustomTag()) {
      // jsp/18ei, jsp/18e8
      if (_tag.generateAdapterDeclaration()) {
        out.println("javax.servlet.jsp.tagext.Tag " + _tag.getId() + "_adapter = null;");
      }
    }
  }
  
  /**
   * Generates the code for a custom tag.
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String name = _tag.getId();
    String className = _tagInfo.getTagClassName();
    Class cl = _tagClass;

    if (! isReuse()) {
      generateTagInit(out);
    }
    else if (! isDeclared()) {
      out.println("if (" + name + " == null) {");
      out.pushDepth();
      generateTagInit(out);
      out.popDepth();
      out.println("}");
      out.println();
    }

    fillAttributes(out, name);

    if (_body != null) {
      out.print(name + ".setJspBody(");
      generateFragment(out, _body, "pageContext", false);
      out.println(");");
    }

    out.println(name + ".doTag();");

    printVarDeclaration(out, VariableInfo.AT_END);
  }

  /**
   * Generates the initialization code for the tag.
   *
   * @param out the output stream
   */
  private void generateTagInit(JspJavaWriter out)
    throws Exception
  {
    TagInstance parent = _tag.getParent();

    String var = _tag.getId();
    String className = _tag.getTagClass().getName();
    
    if (_tag.getAnalyzedTag().getHasInjection()) {
      // out.println("_jsp_inject_" + _tag.getId() + ".configure(" + var + ");");
      out.print(var + " = com.caucho.config.inject.InjectManager.create().createTransientObject(");
      out.printClass(_tag.getTagClass());
      out.println(".class);");
    }
    else {
      out.print(var + " = new ");
      out.printClass(_tag.getTagClass());
      out.println("();");
    }
/*      
    out.print(var + " = new ");
    out.printClass(_tag.getTagClass());
    out.println("();");
    */

    if (JspIdConsumer.class.isAssignableFrom(_tag.getTagClass())) {
      String shortName = className;
      int p = shortName.lastIndexOf('.');
      if (p >= 0)
        shortName = shortName.substring(p + 1);

      out.print(var + ".setJspId(\"" + shortName + "-" + _gen.generateJspId() + "\");");      
    }

    out.println(var + ".setJspContext(pageContext);");
    JspNode parentNode = getParent().getParentTagNode();
    if (parentNode != null) {
      out.println(var + ".setParent(" + parentNode.getCustomTagName() + ");");
    }

    /*
    if (_tag.getAnalyzedTag() != null
        && _tag.getAnalyzedTag().getHasInjection()) {
      out.println("_jsp_inject_" + _tag.getId() + ".configure(" + var + ");");
    }
    */

    if (hasCustomTag()) {
      out.println(var + "_adapter = new javax.servlet.jsp.tagext.TagAdapter(" + var + ");");
    }

    ArrayList<QName> names = _tag.getAttributeNames();
    for (int i = 0; i < names.size(); i++) {
      QName name = names.get(i);

      String value = _tag.getAttribute(name);
      if (value == null)
        continue;

      generateSetAttribute(out, var, name, value, false, false,
                           _tag.getAttributeInfo(name.getLocalName()));
    }
  }
}
