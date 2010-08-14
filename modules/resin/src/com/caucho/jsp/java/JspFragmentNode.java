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
import com.caucho.util.CharBuffer;

/**
 * Represents the body for a fragment (jsp:attribute or jsp:body)
 */
abstract public class JspFragmentNode extends JspContainerNode
  implements JspSegmentNode
{
  private int _fragmentCode;
  private String _fragmentName;

  private boolean _isValueFragment;
  private boolean _isJspFragment;

  public JspFragmentNode()
  {
  }
  
  /**
   * Called after all the attributes from the tag.
   */
  @Override
  public void endAttributes()
    throws JspParseException
  {
    _fragmentCode = _gen.uniqueId();
    
    _fragmentName = "_jsp_fragment_" + _fragmentCode;
  }

  /**
   * Returns the fragment name.
   */
  public String getFragmentName()
  {
    return _fragmentName;
  }

  /**
   * Returns the tag name for the current tag.
   */
  public String getCustomTagName()
  {
    return "_jsp_parent_tag";
  }
  
  /**
   * Adds a text node.
   */
  public JspNode addText(String text)
    throws JspParseException
  {
    JspNode node = new StaticText(_gen, text, this);
    
    addChild(node);

    return node;
  }

  /**
   * Returns true if trimming is enabled.
   */
  public boolean isTrim()
  {
    return false;
  }

  /**
   * Returns true if the children are static.
   */
  public boolean isStatic()
  {
    if (_children == null)
      return true;
    
    for (int i = 0; i < _children.size(); i++) {
      if (! _children.get(i).isStatic())
        return false;
    }

    return true;
  }
  
  /**
   * Returns true if the children are static.
   */
  public boolean isValueFragment()
  {
    return _isValueFragment;
  }
  
  /**
   * Set true if the fragment is used as a fragment object.
   */
  public void setJspFragment(boolean isFragment)
  {
    _isJspFragment = isFragment;
  }
  
  /**
   * Set true if the fragment is used as a fragment object.
   */
  public boolean isJspFragment()
  {
    return _isJspFragment;
  }

  /**
   * Generates code for the fragment variables.
   */
  public void generateFragmentPrologue(JspJavaWriter out)
    throws Exception
  {
    if (_isValueFragment)
      return;

    _isJspFragment = true;

    if (isStatic())
      out.println("com.caucho.jsp.StaticJspFragmentSupport " + _fragmentName + " = null;");
    else
      out.println("_CauchoFragment " + _fragmentName + " = null;");
  }

  /**
   * Generates the children.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (hasScriptingElement() && isJspFragment()) {
      JspNode node = findScriptingNode();
      // jsp/104c jsp/104d
      if (node == null)
        throw error(L.l("Fragments may not contain scripting elements"));
      else if (node._filename.equals(_filename))
        throw node.error(L.l("Fragments may not contain scripting elements"));
    }
    
    generateChildren(out);
  }

  /**
   * Generates the code for a fragment.
   */
  protected String generateValue()
    throws Exception
  {
    if (isStatic())
      return '"' + escapeJavaString(getStaticText()) + '"';
    
    _isValueFragment = true;

    if (hasScriptingElement() && isJspFragment()) {
      JspNode node = findScriptingNode();

      if (node == null)
        throw error(L.l("Fragments may not contain scripting elements"));
      else if (node._filename.equals(_filename))
        throw node.error(L.l("Fragments may not contain scripting elements"));
    }

    _gen.addFragment(this);
    
    TagInstance parent = getParent().getTag();

    CharBuffer cb = new CharBuffer();

    // cb.append("_CauchoFragment.");
    cb.append(_fragmentName + "(pageContext, ");

    for (;
         parent != null && parent.isTagFileTag();
         parent = parent.getParent()) {
    }

    if (parent == null || parent.getId() == TagInstance.TOP_TAG)
      cb.append("null");
    else if (parent.getId().startsWith("top_"))
      cb.append("_jsp_parent_tag");
    else if (! hasCustomTag())
      cb.append(parent.getId());
    else if (parent.isSimpleTag())
      cb.append(parent.getId() + "_adapter");
    else
      cb.append(parent.getId());

    if (_gen instanceof JavaTagGenerator) {
      JavaTagGenerator tagGen = (JavaTagGenerator) _gen;

      if (tagGen.isStaticDoTag())
        cb.append(", _jspBody"); //jsp/100f
      else
        cb.append(", getJspBody()"); //jsp/100g
    } else
      cb.append(", null");

    cb.append(", _jsp_state");
    cb.append(", _jsp_pageManager");

    cb.append(")");

    return cb.close();
  }

  /**
   * Generates the code for the fragment method.
   */
  void generateValueMethod(JspJavaWriter out)
    throws Exception
  {
    if (hasScriptingElement() && isJspFragment()) {
      JspNode node = findScriptingNode();

      if (node == null)
        throw error(L.l("Fragments may not contain scripting elements"));
      else if (node._filename.equals(_filename))
        throw node.error(L.l("Fragments may not contain scripting elements"));
    }
    
    out.println();
    // jsp/1c0o - non-static
    
    //if (_isValueFragment)
    // out.print("static ");
    out.println("String " + _fragmentName + "(");
    out.println("  com.caucho.jsp.PageContextImpl pageContext,");
    out.println("  javax.servlet.jsp.tagext.JspTag _jsp_parent_tag,");
    out.println("  javax.servlet.jsp.tagext.JspFragment _jspBody,");
    out.println("  TagState _jsp_state,");
    out.println("  com.caucho.jsp.PageManager _jsp_pageManager)");
    out.println("  throws Throwable");
    out.println("{");
    out.pushDepth();

    out.println("com.caucho.jsp.PageContextImpl _jsp_parentContext = pageContext;");
    out.println("JspWriter out = pageContext.pushBody();");
    out.println("javax.el.ELContext _jsp_env = pageContext.getELContext();");

    if (hasScripting()) {
      out.println("javax.servlet.http.HttpServletRequest request = (javax.servlet.http.HttpServletRequest) pageContext.getRequest();");
      out.println("javax.servlet.http.HttpServletResponse response = (javax.servlet.http.HttpServletResponse) pageContext.getResponse();");
      out.println("javax.servlet.http.HttpSession session = pageContext.getSession();");
      out.println("javax.servlet.ServletContext application = pageContext.getServletContext();");
      out.println("javax.servlet.ServletConfig config = pageContext.getServletConfig();");
    }

    out.println("try {");
    out.pushDepth();

    generatePrologue(out);
      
    generate(out);

    out.print("return ((com.caucho.jsp.BodyContentImpl) out)");
    /*
    if (isTrim())
      out.println(".getTrimString();");
    else
      out.println(".getString();");
    */
    // jsp/18do
    out.println(".getString();");

    out.popDepth();
    out.println("} finally {");
    out.pushDepth();
    out.println("pageContext.popAndReleaseBody();");
    out.popDepth();
    out.println("}");
      
    out.popDepth();
    out.println("}");
  }
}
