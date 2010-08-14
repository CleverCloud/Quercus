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
import com.caucho.xml.XmlChar;

import java.io.IOException;

/**
 * Represents a jsp:attribute node
 */
public class JspAttribute extends JspFragmentNode {
  private static final QName NAME = new QName("name");
  private static final QName TRIM = new QName("trim");
  
  private QName _name;
  private boolean _trim = true;
  private TagInstance _tag;

  private boolean _oldLocalScriptingInvalid;

  public JspAttribute()
  {
  }

  /**
   * Returns the attribute name.
   */
  public QName getName()
  {
    return _name;
  }

  /**
   * Returns true if trimming is enabled.
   */
  public boolean isTrim()
  {
    return _trim;
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (NAME.equals(name))
      _name = _gen.getParseState().getQName(value);
    else if (TRIM.equals(name))
      _trim = value.equals("true");
    else
      throw error(L.l("`{0}' is an unknown attribute for jsp:attribute.",
                      name));
  }
  
  /**
   * Called when the attributes end.
   */
  public void endAttributes()
    throws JspParseException
  {
    _oldLocalScriptingInvalid = _parseState.isLocalScriptingInvalid();
    // jsp/18di
    // _parseState.setScriptingInvalid(true);
    
    super.endAttributes();
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
   * Adds an attribute.
   */
  public void endElement()
    throws JspParseException
  {
    _parseState.setLocalScriptingInvalid(_oldLocalScriptingInvalid);
    
    if (_name == null)
      throw error(L.l("jsp:attribute needs a `name' attribute."));

    if (_trim) {
      prefix_loop:
      while (_children.size() > 0) {
        JspNode node = (JspNode) _children.get(0);
        
        if (! (node instanceof StaticText))
          break;

        StaticText textNode = (StaticText) node;
        
        String text = textNode.getText();

        for (int i = 0; i < text.length(); i++) {
          if (! XmlChar.isWhitespace(text.charAt(i))) {
            textNode.setText(text.substring(i));
            break prefix_loop;
          }
        }

        _children.remove(0);
      }

      suffix_loop:
      while (_children.size() > 0) {
        JspNode node = _children.get(_children.size() - 1);
        
        if (! (node instanceof StaticText))
          break;

        StaticText textNode = (StaticText) node;
        
        String text = textNode.getText();

        for (int i = text.length() - 1; i >= 0; i--) {
          if (! XmlChar.isWhitespace(text.charAt(i))) {
            textNode.setText(text.substring(0, i + 1));
            break suffix_loop;
          }
        }

        _children.remove(_children.size() - 1);
      }
    }
  }

  /**
   * Returns the root tag instance of the root.
   */
  public TagInstance getTag()
  {
    if (_tag == null)
      _tag = new TagInstance(_gen.getTagManager());

    return _tag;
  }

  /**
   * Returns true if the children are static.
   */
  public boolean isStatic()
  {
    return isChildrenStatic();
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:attribute name=\"" + _name + "\">");
    printXmlChildren(os);
    os.print("</jsp:attribute>");
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
    
    TagInstance parentTag = getParent().getTag();

    if (parentTag == null || parentTag.getId() == null) {
    }
    else if (parentTag.isSimpleTag())
      getTag().setId(TagInstance.FRAGMENT_WITH_SIMPLE_PARENT);
    else
      getTag().setId(TagInstance.FRAGMENT_WITH_TAG_PARENT);

    super.generatePrologue(out);
  }

  /**
   * Generates the fragment as a value.
   */
  String generateValue(Class type)
    throws Exception
  {
    if (isStatic()) {
      String text = getStaticText();

      if (_trim)
        text = text.trim();

      return stringToValue(type, '"' + escapeJavaString(text) + '"');
    }
    else {
      return stringToValue(type, generateValue());
    }
  }

  /**
   * Generates the code for a fragment.
   */
  protected String generateValue()
    throws Exception
  {
    if (! isStatic())
      return super.generateValue();
    else if (_trim)
      return '"' + escapeJavaString(getStaticText().trim()) + '"';
    else
      return '"' + escapeJavaString(getStaticText()) + '"';
  }
}
