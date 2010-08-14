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
import com.caucho.vfs.WriteStream;

import javax.servlet.jsp.tagext.VariableInfo;
import java.io.IOException;

/**
 * Represents the body for a SimpleTag
 */
public class JspBody extends JspFragmentNode {
  private TagInstance _tag;
  
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
   * Returns the tag name for the current tag.
   */
  public String getCustomTagName()
  {
    if (isJspFragment())
      return "_jsp_parent_tag";
    else
      return getParent().getCustomTagName();
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
   * Returns the TagInstance of the enclosing parent.
   */
  public TagInstance getTag()
  {
    JspNode parent = getParent();

    if (parent == null)
      return _gen.getRootTag();
    else if (parent instanceof CustomSimpleTag ||
             parent instanceof TagFileTag) {
      if (_tag == null)
        _tag = new TagInstance(_gen.getTagManager());

      return _tag;
    }
    else
      return parent.getTag();
  }

  /**
   * Called after all the attributes from the tag.
   */
  public void endAttributes()
    throws JspParseException
  {
    super.endAttributes();
    
    JspNode parent = getParent();

    if (parent == null
        //        parent instanceof JspRoot ||
        || parent instanceof JspTop) {
      throw error(L.l("jsp:body must be contained in a valid tag."));
    }
    else if (parent instanceof JspBody
             || parent instanceof JspAttribute) {
      throw error(L.l("jsp:body is not allowed in <{0}>",
                      parent.getTagName()));
    }

    if (parent instanceof GenericTag) {
      GenericTag tag = (GenericTag) parent;

      if ("tagdependent".equals(tag.getBodyContent())
          && _gen.getJspBuilder() != null) {
        _gen.getJspBuilder().startTagDependent();
      }
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
    os.print("<jsp:body");
    os.print(" jsp:id=\"" + _gen.generateJspId() + "\">");
    printXmlChildren(os);
    os.print("</jsp:body>");
  }

  /**
   * Generates the prologue.
   */
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    JspNode parent = getParent();
    
    if (! isJspFragment()) {
      generatePrologueChildren(out);
      return;
    }
    
    super.generatePrologue(out);

    TagInstance parentTag = getParent().getTag();
    boolean isSimple = false;

    if (parentTag == null || parentTag.isTop()) {
    }
    else if (! getTag().isTop()) {
    }
    else if (parentTag.isSimpleTag()) {
      getTag().setId(TagInstance.FRAGMENT_WITH_SIMPLE_PARENT);
    }
    else
      getTag().setId(TagInstance.FRAGMENT_WITH_TAG_PARENT);
  }

  /**
   * Generates the prologue as a child, i.e. in the fragment
   * definition itself.
   */
  public void generatePrologueChildren(JspJavaWriter out)
    throws Exception
  {
    super.generatePrologueChildren(out);
    
    JspNode parent = getParent();

    if (parent instanceof GenericTag) {
      GenericTag tag = (GenericTag) parent;

      tag.printVarDeclaration(out, VariableInfo.AT_BEGIN);
      tag.printVarDeclaration(out, VariableInfo.NESTED);
    }
  }

  /**
   * Direct generation of the body is forbidden.
   */
  /*
  public void generate(JspJavaWriter out)
    throws Exception
  {
    throw error(L.l("jsp:body must be contained in a valid tag."));
  }
  */
}
