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

package com.caucho.jsp.cfg;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.DependencyBean;
import com.caucho.jsp.JspLineParseException;
import com.caucho.jsp.JspParseException;
import com.caucho.util.L10N;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.PersistentDependency;

import javax.servlet.jsp.tagext.SimpleTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Configuration for the taglib tag in the .tld
 */
public class TldTag implements DependencyBean {
  private final static L10N L = new L10N(TldTag.class);
  private final static Logger log
    = Logger.getLogger(TldTag.class.getName());

  private TldTaglib _taglib;
  
  private String _name;
  private String _tagClassName;
  private String _teiClassName;
  private String _bodyContent;
  private String _displayName;
  private String _info;
  private String _smallIcon;
  private String _largeIcon;
  private String _description;
  private ArrayList<TagVariableInfo> _variableList =
    new ArrayList<TagVariableInfo>();
  private ArrayList<TagAttributeInfo> _attributeList =
    new ArrayList<TagAttributeInfo>();
  private ArrayList<TldFragmentAttribute> _fragmentAttributeList =
    new ArrayList<TldFragmentAttribute>();
  private boolean _dynamicAttributes;
  private String _dynamicAttributeName;
  private String _example;

  private String _configLocation;
  private JspParseException _configException;
  
  private ArrayList<Dependency> _dependencyList
    = new ArrayList<Dependency>();

  private TldTag _baseTag;

  TldTag(TldTaglib taglib)
  {
    _taglib = taglib;
  }

  public TldTag()
  {
  }

  /**
   * Sets the config location.
   */
  public void setConfigLocation(String filename, int line)
  {
    _configLocation = filename + ":" + line + ": ";
  }

  /**
   * Adds a dependency.
   */
  public void addDependency(PersistentDependency dependency)
  {
    _dependencyList.add(dependency);
  }

  /**
   * Returns the dependency.
   */
  public ArrayList<Dependency> getDependencyList()
  {
    return _dependencyList;
  }

  /**
   * Sets the tag name, i.e. the local name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the tag name, i.e. the local name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the icon.
   */
  public void setIcon(com.caucho.config.types.Icon icon)
  {
    if (icon != null) {
      _smallIcon = icon.getSmallIcon();
      _largeIcon = icon.getLargeIcon();
    }
  }

  /**
   * Sets the tag class
   */
  public void setTagClass(String tagClassName)
    throws ConfigException
  {
    _tagClassName = tagClassName;

    Class tagClass = null;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      tagClass = Class.forName(tagClassName, false, loader);

      Config.checkCanInstantiate(tagClass);
    } catch (ConfigException e) {
      throw e;
    } catch (Throwable e) {
      log.warning(_configLocation +  e);

      if (_configException == null)
        _configException = new JspLineParseException(_configLocation + e);

      return;
    }

    if (! Tag.class.isAssignableFrom(tagClass)
        && ! SimpleTag.class.isAssignableFrom(tagClass))
      throw new ConfigException(L.l("{0} must either implement Tag or SimpleTag.",
                                    tagClass.getName()));
  }

  /**
   * Sets the tei class
   */
  public void setTagclass(String tagClassName)
    throws ConfigException, InstantiationException, IllegalAccessException
  {
    setTagClass(tagClassName);
  }

  /**
   * Gets the tag class
   */
  public Class getTagClass()
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      return Class.forName(_tagClassName, false, loader);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the tag class
   */
  public String getTagClassName()
  {
    return _tagClassName;
  }

  /**
   * Sets the tei class
   */
  public void setTeiClass(String teiClassName)
    throws ConfigException
  {
    _teiClassName = teiClassName;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class teiClass = Class.forName(teiClassName, false, loader);

      Config.validate(teiClass, TagExtraInfo.class);
    } catch (ConfigException e) {
      throw e;
    } catch (Throwable e) {
      log.warning(_configLocation +  e);

      if (_configException == null)
        _configException = new JspParseException(_configLocation + e);

      return;
    }
  }
  
  /**
   * Old-style setting of the tei class
   */
  public void setTeiclass(String teiClassName)
    throws ConfigException
  {
    setTeiClass(teiClassName);
  }

  /**
   * Gets the tei class
   */
  public String getTeiClassName()
  {
    return _teiClassName;
  }

  /**
   * Gets the tei object
   */
  public TagExtraInfo getTagExtraInfo()
  {
    try {
      if (_teiClassName == null)
        return null;
      else {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
        Class teiClass = Class.forName(_teiClassName, false, loader);

        return (TagExtraInfo) teiClass.newInstance();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the body-content
   */
  public void setBodyContent(String bodyContent)
  {
    _bodyContent = bodyContent;
  }

  /**
   * Sets the bodycontent (backwards compat)
   */
  public void setBodycontent(String bodyContent)
  {
    setBodyContent(bodyContent);
  }

  /**
   * Gets the body-content
   */
  public String getBodyContent()
  {
    return _bodyContent;
  }

  /**
   * Sets the display-name
   */
  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  /**
   * Gets the display-name
   */
  public String getDisplayName()
  {
    return _displayName;
  }

  /**
   * Sets the info
   */
  public void setInfo(String info)
  {
    _info = info;
  }

  /**
   * Gets the info
   */
  public String getInfo()
  {
    return _info;
  }

  /**
   * Sets the small-icon
   */
  public void setSmallIcon(String smallIcon)
  {
    _smallIcon = smallIcon;
  }

  /**
   * Gets the small-icon
   */
  public String getSmallIcon()
  {
    return _smallIcon;
  }

  /**
   * Sets the large-icon
   */
  public void setLargeIcon(String largeIcon)
  {
    _largeIcon = largeIcon;
  }

  /**
   * Gets the large-icon
   */
  public String getLargeIcon()
  {
    return _largeIcon;
  }

  /**
   * Sets the description
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Gets the description
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * Adds a variable.
   */
  public void addVariable(TldVariable variable)
    throws ConfigException
  {
    TagVariableInfo varInfo;

    String scopeName = variable.getScope();
    int scope;

    if (scopeName == null)
      scope = VariableInfo.NESTED;
    else if (scopeName.equals("NESTED"))
      scope = VariableInfo.NESTED;
    else if (scopeName.equals("AT_BEGIN"))
      scope = VariableInfo.AT_BEGIN;
    else if (scopeName.equals("AT_END"))
      scope = VariableInfo.AT_END;
    else
      throw new ConfigException(L.l("{0} expects a valid scope at `{1}'",
                                    variable.getNameGiven(), scopeName));

    varInfo = new TagVariableInfo(variable.getNameGiven(),
                                  variable.getNameFromAttribute(),
                                  variable.getVariableClass(),
                                  variable.getDeclare(),
                                  scope);

    _variableList.add(varInfo);
  }

  /**
   * Gets the variables
   */
  public ArrayList<TagVariableInfo> getVariableList()
  {
    return _variableList;
  }

  /**
   * Returns the variables.
   */
  public TagVariableInfo []getVariables()
  {
    TagVariableInfo []variables;

    variables = new TagVariableInfo[_variableList.size()];

    return _variableList.toArray(variables);
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(TldAttribute attribute)
  {
    TagAttributeInfo attrInfo;

    if (attribute.getDeferredValue() != null
        && _taglib != null
        && _taglib.getJspVersion() != null
        && _taglib.getJspVersion().compareTo("2.1") < 0) {
      // jsp/18u9
      throw new ConfigException(L.l("<deferred-value> for tag '{0}' requires a taglib with jsp-version 2.1 or later",
                                    getName()));
    }

    Class type = attribute.getType();
    attrInfo = new TagAttributeInfo(attribute.getName(),
                                    attribute.getRequired(),
                                    type == null ? null : type.getName(),
                                    attribute.getRtexprvalue(),
                                    attribute.isFragment(),
                                    attribute.getDescription(),
                                    attribute.getDeferredValue() != null,
                                    attribute.getDeferredMethod() != null,
                                    attribute.getExpectedType(),
                                    attribute.getDeferredMethodSignature());
    
    _attributeList.add(attrInfo);
  }

  /**
   * Gets the attributes
   */
  public ArrayList getAttributeList()
  {
    return _attributeList;
  }

  /**
   * Returns the attributes.
   */
  public TagAttributeInfo []getAttributes()
  {
    TagAttributeInfo []attributes;

    attributes = new TagAttributeInfo[_attributeList.size()];
    
    return (TagAttributeInfo []) _attributeList.toArray(attributes);
  }

  /**
   * Adds an fragmentAttribute.
   */
  public void addFragmentAttribute(TldFragmentAttribute attribute)
  {
    _fragmentAttributeList.add(attribute);
  }

  /**
   * Returns the attributes.
   */
  public ArrayList getFragmentAttributes()
  {
    return _fragmentAttributeList;
  }

  /**
   * Sets the example
   */
  public void setExample(String example)
  {
    _example = example;
  }

  /**
   * Gets the dynamic-attributes
   */
  public boolean getDynamicAttributes()
  {
    return _dynamicAttributes;
  }

  /**
   * Sets the dynamic-attributes
   */
  public void setDynamicAttributes(boolean dynamicAttributes)
  {
    _dynamicAttributes = dynamicAttributes;
  }

  /**
   * Gets the dynamic-attrisavesavebutes
   */
  public String getDynamicAttributeName()
  {
    return _dynamicAttributeName;
  }

  /**
   * Sets the dynamic-attributes
   */
  public void setDynamicAttributeName(String name)
  {
    _dynamicAttributeName = name;
  }

  /**
   * Gets the example
   */
  public String getExample()
  {
    return _example;
  }
  
  /**
   * validates.
   */
  public void validate()
    throws JspParseException
  {
    if (_configException != null)
      throw _configException;
  }

  public int hashCode()
  {
    return _name.hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof TldTag))
      return false;

    TldTag tag = (TldTag) o;

    return _name.equals(tag._name);
  }
  
  public void setBaseTag(TldTag tag)
  {
    if (tag == this)
      throw new IllegalStateException();
    
    _baseTag = tag;
  }
  
  public TldTag getBaseTag()
  {
    return _baseTag;
  }

  public String toString()
  {
    return getClass().getName() + "[" + getName() + "]";
  }
}
