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
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.LineCompileException;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A node which can contain other nodes.
 */
public abstract class JspContainerNode extends JspNode {
  static final L10N L = new L10N(JspContainerNode.class);
  
  protected ArrayList<QName> _attributeNames = new ArrayList<QName>();
  protected ArrayList<Object> _attributeValues = new ArrayList<Object>();
  
  protected ArrayList<JspNode> _children = new ArrayList<JspNode>();

  private ArrayList<JspAttribute> _attrChildren;

  protected boolean _hasJspAttribute;
  
  /**
   * Adds an attribute.
   *
   * @param name the name of the attribute.
   * @param value the value of the attribute.
   */
  @Override
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (name == null)
      throw new NullPointerException();

    if (_attributeNames.indexOf(name) >= 0)
      throw error(L.l("'{0}' is a duplicate attribute name.  Attributes must occur only once in a tag.", name.getName()));
    
    _attributeNames.add(name);
    _attributeValues.add(value);
  }

  /**
   * Adds a JspAttribute attribute.
   *
   * @param name the name of the attribute.
   * @param value the value of the attribute.
   */
  @Override
  public void addAttribute(QName name, JspAttribute value)
    throws JspParseException
  {
    if (name == null)
      throw new NullPointerException();
    
    if (_attributeNames.indexOf(name) >= 0)
      throw error(L.l("'{0}' is a duplicate attribute name.  Attributes must occur only once in a tag.", name.getName()));
    
    _attributeNames.add(name);
    _attributeValues.add(value);

    _gen.addFragment(value);
  }

  /**
   * Returns the named attribute.
   */
  public Object getAttribute(String name)
    throws JspParseException
  {
    for (int i = 0; i < _attributeNames.size(); i++) {
      if (name.equals(_attributeNames.get(i).getName()))
        return _attributeValues.get(i);
    }

    return null;
  }
  
  /**
   * Adds a child node.
   */
  @Override
  public void addChild(JspNode node)
    throws JspParseException
  {
    node.setParent(this);
    
    if (node instanceof JspAttribute) {
      JspAttribute attr = (JspAttribute) node;

      QName name = attr.getName();

      addAttribute(name, attr);

      _hasJspAttribute = true;

      int i = 0;
      while (_children != null && i < _children.size()) {
        JspNode child = _children.get(i);

        if (child instanceof StaticText) {
          String text = ((StaticText) child).getText();

          if (isWhitespace(text))
            _children.remove(i);
          else
            throw child.error(L.l("tags using jsp:attribute must put body content in a jsp:body tag."));
        }
        else if (child instanceof JspBody)
          i++;
        else if (child instanceof JspAttribute)
          i++;
        else {
          throw child.error(L.l("tags using jsp:attribute must put body content in a jsp:body tag"));
        }
      }
    }
    else if (_hasJspAttribute && ! (node instanceof JspBody)) {
      throw node.error(L.l("tags using jsp:attribute must put body content in a jsp:body tag"));
    }
    else if (node instanceof StaticText) {
      StaticText text = (StaticText) node;
      String data = text.getText();

      boolean isXml = _gen.isXml();
      for (JspNode ptr = this; ptr != null; ptr = ptr.getParent()) {
        if (ptr instanceof JspRoot)
          isXml = true;
      }

      if (_children == null)
        _children = new ArrayList<JspNode>();
      
      for (int i = 0;
           i < data.length();
           i++) {
        if (! XmlChar.isWhitespace(data.charAt(i))) {
          _children.add(node);
          return;
        }
      }

      if (! isXml)
        _children.add(node);
    }
    else if (node instanceof JspBody && hasJspBody()) {
      throw node.error(L.l("tags may only have a single jsp:body tag"));
    }
    else {
      if (_children == null)
        _children = new ArrayList<JspNode>();

      _children.add(node);
    }
  }

  protected void addAttributeChild(JspAttribute attr)
  {
    if (_attrChildren == null)
      _attrChildren = new ArrayList<JspAttribute>();

    _attrChildren.add(attr);
  }

  private boolean hasJspBody()
  {
    if (_children == null)
      return false;

    for (int i = _children.size() - 1; i >= 0; i--) {
      if (_children.get(i) instanceof JspBody)
        return true;
    }

    return false;
  }
  
  /**
   * Adds a child node after its done initializing.
   */
  @Override
  public void addChildEnd(JspNode node)
    throws JspParseException
  {
  }

  /**
   * True if the jsf-parent setting is required.
   */
  @Override
  public boolean isJsfParentRequired()
  {
    if (_children == null)
      return false;

    for (int i = _children.size() - 1; i >= 0; i--) {
      if (_children.get(i).isJsfParentRequired())
        return true;
    }

    return false;
  }

  /**
   * Returns true if the node is empty
   */
  public boolean isEmpty()
  {
    if (_children == null || _children.size() == 0)
      return true;

    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      if (child instanceof JspBody) {
        JspBody body = (JspBody) child;

        return body.isEmpty();
      }
      else if (child instanceof StaticText) {
        StaticText text = (StaticText) child;

        if (! text.isWhitespace())
          return false;
      }
      else
        return false;
    }

    return false;
  }

  /**
   * Returns the static text.
   */
  @Override
  public void getStaticText(CharBuffer cb)
  {
    if (_children == null)
      return;
    
    for (int i = 0; i < _children.size(); i++)
      _children.get(i).getStaticText(cb);
  }
  
  /**
   * Set true if the node contains a child tag.
   */
  @Override
  public boolean hasCustomTag()
  {
    for (int i = 0; _children != null && i < _children.size(); i++) {
      JspNode child = _children.get(i);

      if (child instanceof CustomTag)
        return true;

      if (child.hasCustomTag())
        return true;
    }
    
    for (int i = 0;
         _attributeValues != null && i < _attributeValues.size();
         i++) {
      Object value = _attributeValues.get(i);

      if (value instanceof CustomTag)
        return true;
      else if (value instanceof JspNode &&
               ((JspNode) value).hasCustomTag())
        return true;
    }

    return false;
  }
  
  /**
   * Set true if the node contains a child tag.
   */
  public boolean hasTag()
  {
    for (int i = 0; _children != null && i < _children.size(); i++) {
      JspNode child = _children.get(i);

      if (child instanceof CustomTag ||
          child instanceof CustomSimpleTag)
        return true;

      if (child.hasTag())
        return true;
    }
    
    for (int i = 0;
         _attributeValues != null && i < _attributeValues.size();
         i++) {
      Object value = _attributeValues.get(i);

      if (value instanceof CustomTag || value instanceof CustomSimpleTag)
        return true;

      else if (value instanceof JspNode &&
               ((JspNode) value).hasTag())
        return true;
    }

    return false;
  }
  
  /**
   * Adds a text node.
   */
  @Override
  public JspNode addText(String text)
    throws JspParseException
  {
    if (! _hasJspAttribute) {
      JspNode node = new StaticText(_gen, text, this);
      
      addChild(node);

      return node;
    }
    else if (! isWhitespace(text)) {
      throw error(L.l("tags using jsp:attribute must put body content in a jsp:body tag"));
    }

    return null;
  }

  protected boolean isWhitespace(String text)
  {
    for (int i = 0; i < text.length(); i++) {
      if (! Character.isWhitespace(text.charAt(i)))
        return false;
    }

    return true;
  }

  /**
   * Returns the children.
   */
  public ArrayList<JspNode> getChildren()
  {
    return _children;
  }

  /**
   * Has children.
   */
  public boolean hasChildren()
  {
    return ! isEmpty();
  }
  
  /**
   * True if the node has scripting
   */
  public boolean hasScripting()
  {
    for (int i = 0; _children != null && i < _children.size(); i++) {
      if (_children.get(i).hasScripting()) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * True if the node has scripting element (i.e. not counting rtexpr values)
   */
  @Override
  public boolean hasScriptingElement()
  {
    for (int i = 0; _children != null && i < _children.size(); i++) {
      if (_children.get(i).hasScriptingElement()) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Finds the first scripting node
   */
  @Override
  public JspNode findScriptingNode()
  {
    for (int i = 0; _children != null && i < _children.size(); i++) {
      JspNode node = _children.get(i).findScriptingNode();

      if (node != null)
        return node;
    }

    if (hasScripting())
      return this;
    else
      return null;
  }

  /**
   * Returns true if the children are static.
   */
  public boolean isChildrenStatic()
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
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXmlChildren(WriteStream os)
    throws IOException
  {
    if (_children == null)
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      child.printXml(os);
    }
  }

  /**
   * generates data for prologue children.
   */
  public void generatePrologueChildren(JspJavaWriter out)
    throws Exception
  {
    if (_children == null)
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      child.generatePrologue(out);
    }
  }

  /**
   * generates data for declaration children.
   */
  public void generateDeclarationChildren(JspJavaWriter out)
    throws IOException
  {
    if (_children == null)
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      child.generateDeclaration(out);
    }
  }

  /**
   * generates data for tag state children.
   */
  public void generateTagStateChildren(JspJavaWriter out)
    throws Exception
  {
    if (_children != null) {
      for (int i = 0; i < _children.size(); i++) {
        JspNode child = _children.get(i);

        child.generateTagState(out);
      }
    }
    
    if (_attrChildren != null) {
      for (JspNode child : _attrChildren) {
        child.generateTagState(out);
      }
    }
  }

  /**
   * generates data for tag state children.
   */
  public void generateTagReleaseChildren(JspJavaWriter out)
    throws Exception
  {
    if (_children != null) {
      for (int i = 0; i < _children.size(); i++) {
        JspNode child = _children.get(i);

        child.generateTagRelease(out);
      }
    }
    
    if (_attrChildren != null) {
      for (JspNode child : _attrChildren) {
        child.generateTagRelease(out);
      }
    }
  }

  /**
   * Generates the code for the children.
   *
   * @param out the output writer for the generated java.
   */
  public void generateChildren(JspJavaWriter out)
    throws Exception
  {
    if (_children == null)
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      child.generateStartLocation(out);
      try {
        child.generate(out);
      } catch (Exception e) {
        if (e instanceof LineCompileException)
          throw e;
        else
          throw child.error(e);
      }
      child.generateEndLocation(out);
    }
  }

  /**
   * Generates the code for the children.
   */
  public void generateChildrenEmpty()
    throws Exception
  {
    if (_children == null)
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      child.generateEmpty();
    }
  }

  /**
   * Generates static text.
   *
   * @param out the output writer for the generated java.
   */
  public void generateStatic(JspJavaWriter out)
    throws Exception
  {
    if (_children == null)
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      out.setLocation(child.getFilename(), child.getStartLine());
      child.generateStatic(out);
      // out.setLocation(child.getFilename(), child.getEndLine());
    }
  }

  /**
   * generates data for tag state children.
   */
  @Override
  public void generateClassEpilogueChildren(JspJavaWriter out)
    throws IOException
  {
    if (_children != null) {
      for (int i = 0; i < _children.size(); i++) {
        JspNode child = _children.get(i);

        child.generateClassEpilogue(out);
      }
    }
    
    if (_attrChildren != null) {
      for (JspNode child : _attrChildren) {
        child.generateClassEpilogue(out);
      }
    }
  }
}

