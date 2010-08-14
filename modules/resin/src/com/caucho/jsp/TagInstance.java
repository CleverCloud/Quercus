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

package com.caucho.jsp;

import com.caucho.jsp.java.JspTagFileSupport;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import javax.servlet.jsp.tagext.SimpleTag;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.VariableInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Describes a single tag instance.
 */
public class TagInstance {
  static final L10N L = new L10N(TagInstance.class);
  private static final Logger log
    = Logger.getLogger(TagInstance.class.getName());

  // Special object to note that the attribute varies
  private static final Object VARIES = new Varies();

  public static final String TOP_TAG = null;
  public static final String FRAGMENT_WITH_TAG_PARENT = "top_tag_fragment";
  public static final String FRAGMENT_WITH_SIMPLE_PARENT = "top_simple_fragment";

  private ParseTagManager _manager;

  private TagInstance _top;
  private TagInfo _tagInfo;
  private int _maxId;
  
  private TagInstance _parent;
  private ArrayList<TagInstance> _children = new ArrayList<TagInstance>();
  private ArrayList<TagInstance> _tags = new ArrayList<TagInstance>();

  private JspGenerator _gen;
  private String _tagId = null;
  private QName _qname;
  private Class _cl;
  private VariableInfo []_varInfo;
  private boolean _needsAdapter;
  private boolean _hasAdapterDeclaration;

  private AnalyzedTag _analyzedTag;

  private ArrayList<QName> _attributeNames = new ArrayList<QName>();
  private ArrayList<Object> _attributeValues = new ArrayList<Object>();

  private boolean _hasBodyContent;
  
  public TagInstance(ParseTagManager manager)
  {
    _manager = manager;
    
    _top = this;
    _tagId = null;
  }
  
  public TagInstance(ParseTagManager manager,
                     String id)
  {
    _manager = manager;
    _top = this;
    _tagId = id;
  }
  
  TagInstance(JspGenerator gen,
              TagInstance parent,
              TagInfo tagInfo,
              QName qname,
              Class cl)
  {
    _gen = gen;
    _qname = qname;
    _parent = parent;
    _manager = parent._manager;
    _tagInfo = tagInfo;
    _cl = cl;
    
    _top = parent._top;
    parent._children.add(this);
    _top._tags.add(this);

    if (tagInfo != null) {
      String className = tagInfo.getTagClassName();
      int p = className.lastIndexOf('.');
      if (p >= 0)
        className = className.substring(p + 1);
      _tagId = "_jsp_" + className + "_" + _gen.uniqueId();
    }
    
    _analyzedTag = _manager.analyzeTag(cl);
  }

  /**
   * Returns the tag name
   */
  public QName getQName()
  {
    return _qname;
  }

  /**
   * Returns the tag name
   */
  public String getName()
  {
    return _qname.getName();
  }
  
  public String getId()
  {
    return _tagId;
  }

  public void setId(String id)
  {
    _tagId = id;
  }

  public boolean generateAdapterDeclaration()
  {
    if (_hasAdapterDeclaration) {
      return false;
    }
    else {
      _hasAdapterDeclaration = true;
      
      return true;
    }
  }

  public TagInstance getParent()
  {
    return _parent;
  }

  /**
   * Returns true for a top tag.
   */
  public boolean isTop()
  {
    return _tagId == null || _tagId.startsWith("top_");
  }

  /**
   * Returns the tag class.
   */
  public Class getTagClass()
  {
    return _cl;
  }

  /**
   * Returns true if it's a simple tag.
   */
  public boolean isSimpleTag()
  {
    return _cl != null && SimpleTag.class.isAssignableFrom(_cl);
  }

  /**
   * Returns true if it's a simple tag.
   */
  public boolean isTagFileTag()
  {
    return _cl != null && JspTagFileSupport.class.isAssignableFrom(_cl);
  }
  
  public TagInfo getTagInfo()
  {
    return _tagInfo;
  }

  /**
   * Returns the analyzed tag.
   */
  public AnalyzedTag getAnalyzedTag()
  {
    return _analyzedTag;
  }

  void setVarInfo(VariableInfo []varInfo)
  {
    _varInfo = varInfo;
  }

  public VariableInfo []getVarInfo()
  {
    return _varInfo;
  }

  public int size()
  {
    return _top._maxId;
  }

  /**
   * Returns true if there are children.
   */
  public boolean hasChildren()
  {
    return _children != null && _children.size() > 0;
  }

  /**
   * Set true if needs an adapter.
   */
  public boolean getNeedsAdapter()
  {
    return _needsAdapter || isSimpleTag() && hasChildren();
  }

  /**
   * if needs an adapter.
   */
  public void setNeedsAdapter(boolean needsAdapter)
  {
    _needsAdapter = needsAdapter;
  }

  /**
   * Iterates through the children.
   */
  public Iterator<TagInstance> iterator()
  {
    return _children.iterator();
  }

  /**
   * Iterates through the children.
   */
  public TagInstance get(int i)
  {
    return _top._tags.get(i);
  }

  /**
   * Returns the tag's attribute names.
   */
  public ArrayList<QName> getAttributeNames()
  {
    return _attributeNames;
  }

  /**
   * Set true if the tag has a body content.
   */
  public void setBodyContent(boolean hasBodyContent)
  {
    _hasBodyContent = hasBodyContent;
  }

  /**
   * Get true if the tag has a body content.
   */
  public boolean getBodyContent()
  {
    return _hasBodyContent;
  }

  /**
   * Adds a child tag.  If the tag exists, just reuse it.
   *
   * @param tagName the JSP name of the tag
   * @param tagInfo the TagInfo structure for the tag
   * @param cl the tag's implementation class
   * @param names the array of attribute names
   * @param values the array of attribute values
   */
  public TagInstance addTag(JspGenerator gen,
                            QName tagName,
                            TagInfo tagInfo,
                            Class cl,
                            ArrayList<QName> names,
                            ArrayList<Object> values,
                            boolean hasBodyContent)
  {
    TagInstance child = null;//findTag(tagName, names);
    if (child == null)
      child = new TagInstance(gen, this, tagInfo, tagName, cl);

    child.setBodyContent(hasBodyContent);

    for (int i = 0; i < names.size(); i++) {
      QName name = names.get(i);
      Object value = values.get(i);
      
      if (value instanceof String) {
        String strValue = (String) value;

        // runtime and EL expressions can't have shared values
        if (strValue.startsWith("<%=") || strValue.startsWith("%="))
          value = null;
        else if (strValue.indexOf("${") >= 0)
          value = null;
        else if (strValue.indexOf("#{") >= 0) {
          // jsp/1cn1 - the expression can depend on the context
          value = null;
        }

        child.addAttribute(name, value);
      } else {
        child.addAttribute(name, null);
      }
    }

    return child;
  }

  /**
   * Adds a new tag.  Always create a new tag.
   */
  public TagInstance addNewTag(JspGenerator gen,
                               QName tagName,
                               TagInfo tagInfo,
                               Class cl,
                               ArrayList<QName> names,
                               ArrayList<String> values,
                               boolean hasBodyContent)
  {
    TagInstance child = null;
    if (child == null)
      child = new TagInstance(gen, this, tagInfo, tagName, cl);

    child.setBodyContent(hasBodyContent);

    for (int i = 0; i < names.size(); i++) {
      QName name = names.get(i);
      String value = values.get(i);

      if (value.startsWith("<%=") || value.startsWith("%="))
        value = null;

      child.addAttribute(name, value);
    }

    return child;
  }

  /**
   * Sets the attribute.  Null values can't be pre-cached.
   */
  public void addAttribute(QName name, Object value)
  {
    for (int i = 0; i < _attributeNames.size(); i++) {
      QName attrName = _attributeNames.get(i);
      
      if (attrName.equals(name)) {
        Object oldValue = _attributeValues.get(i);
        if (value == null || oldValue == null || ! value.equals(oldValue))
          _attributeValues.set(i, null);

        return;
      }
    }

    if (name == null)
      throw new NullPointerException();
    
    _attributeNames.add(name);
    _attributeValues.add(value);
  }

  public String getAttribute(QName name)
  {
    for (int i = 0; i < _attributeNames.size(); i++) {
      if (name.equals(_attributeNames.get(i)))
        return (String) _attributeValues.get(i);
    }

    return null;
  }

  boolean canBeRequestTime(String name)
  {
    TagAttributeInfo attrs[] = _tagInfo.getAttributes();

    if (attrs == null)
      return true;

    for (int i = 0; i < attrs.length; i++) {
      if (name.equals(attrs[i].getName()))
        return attrs[i].canBeRequestTime();
    }

    return false;
  }

  public TagAttributeInfo getAttributeInfo(String name)
  {
    if (_tagInfo == null)
      return null;

    TagAttributeInfo attrs[] = _tagInfo.getAttributes();

    if (attrs == null)
      return null;

    for (int i = 0; i < attrs.length; i++) {
      if (name.equals(attrs[i].getName()))
        return attrs[i];
    }

    return null;
  }

  /**
   * Finds the matching tag.
   */
  public TagInstance findTag(QName tagName,
                             ArrayList<QName> names,
                             boolean hasBodyContent)
  {
    for (int i = 0; i < _children.size(); i++) {
      TagInstance child = _children.get(i);

      if (child.match(tagName, names, hasBodyContent))
        return child;
    }

    return null;
  }
  
  /**
   * Returns true for matching instances.
   */
  boolean match(QName tagName, ArrayList<QName> names, boolean hasBodyContent)
  {
    if (! _qname.equals(tagName))
      return false;

    if (_attributeNames.size() != names.size())
      return false;

    if (_hasBodyContent != hasBodyContent)
      return false;
    
    for (int i = 0; i < _attributeNames.size(); i++) {
      QName attrName = _attributeNames.get(i);

      if (names.indexOf(attrName) < 0)
        return false;
    }

    for (int i = 0; i < names.size(); i++) {
      QName name = names.get(i);

      if (_attributeNames.indexOf(name) < 0)
        return false;
    }

    return true;
  }

  static class Varies {
    public String toString()
    {
      return "Value-Varies[]";
    }
  }
}
