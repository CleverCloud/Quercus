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
import com.caucho.jsp.JspParseException;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.PersistentDependency;

import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.faces.component.UIComponent;
import java.util.ArrayList;

/**
 * Configuration for the taglib tag in the .tld
 */
public class JsfTag extends TldTag {
  private Class _componentClass;
  
  public void setComponentClass(Class cl)
  {
    Config.validate(cl, UIComponent.class);
    
    _componentClass = cl;
  }
  
  public Class getComponentClass()
  {
    return _componentClass;
  }

  /**
   * Returns the dependency.
   */
  public ArrayList<Dependency> getDependencyList()
  {
    return getBaseTag().getDependencyList();
  }

  /**
   * Gets the tag class
   */
  @Override
  public Class getTagClass()
  {
    return getBaseTag().getTagClass();
  }

  /**
   * Gets the tag class
   */
  @Override
  public String getTagClassName()
  {
    return getBaseTag().getTagClassName();
  }

  /**
   * Gets the tei class
   */
  @Override
  public String getTeiClassName()
  {
    return getBaseTag().getTeiClassName();
  }

  /**
   * Gets the tei object
   */
  @Override
  public TagExtraInfo getTagExtraInfo()
  {
    return getBaseTag().getTagExtraInfo();
  }

  /**
   * Gets the body-content
   */
  @Override
  public String getBodyContent()
  {
    return getBaseTag().getBodyContent();
  }

  /**
   * Gets the display-name
   */
  @Override
  public String getDisplayName()
  {
    return getBaseTag().getDisplayName();
  }

  /**
   * Gets the info
   */
  @Override
  public String getInfo()
  {
    return getBaseTag().getInfo();
  }

  /**
   * Gets the description
   */
  @Override
  public String getDescription()
  {
    return getBaseTag().getDescription();
  }

  /**
   * Gets the variables
   */
  public ArrayList<TagVariableInfo> getVariableList()
  {
    return getBaseTag().getVariableList();
  }

  /**
   * Returns the variables.
   */
  public TagVariableInfo []getVariables()
  {
    return getBaseTag().getVariables();
  }

  /**
   * Gets the attributes
   */
  public ArrayList getAttributeList()
  {
    return getBaseTag().getAttributeList();
  }

  /**
   * Returns the attributes.
   */
  public TagAttributeInfo []getAttributes()
  {
    return getBaseTag().getAttributes();
  }

  /**
   * Returns the attributes.
   */
  public ArrayList getFragmentAttributes()
  {
    return getBaseTag().getFragmentAttributes();
  }

  /**
   * Gets the dynamic-attributes
   */
  @Override
  public boolean getDynamicAttributes()
  {
    return getBaseTag().getDynamicAttributes();
  }

  /**
   * Gets the dynamic-attrisavesavebutes
   */
  @Override
  public String getDynamicAttributeName()
  {
    return getBaseTag().getDynamicAttributeName();
  }
  
  /**
   * validates.
   */
  public void validate()
    throws JspParseException
  {
    getBaseTag().validate();
  }
  
  public String toString()
  {
    return "JsfTag[" + getName() + "]";
  }
}
