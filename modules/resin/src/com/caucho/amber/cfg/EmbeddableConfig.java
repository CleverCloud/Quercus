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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import com.caucho.amber.type.*;


/**
 * <embeddable> tag in the orm.xml
 */
public class EmbeddableConfig extends AbstractEnhancedConfig {

  // attributes
  private String _className;
  private boolean _isMetadataComplete;

  // elements
  private String _description;
  private EmbeddableAttributesConfig _attributes;

  private EntityIntrospector _introspector;
  private EmbeddableType _embeddableType;

  EmbeddableConfig(String name)
  {
    super(name);
  }

  EmbeddableConfig(String name,
                   EntityIntrospector introspector,
                   EmbeddableType embeddableType)
  {
    super(name);
    
    _embeddableType = embeddableType;
    _introspector = introspector;
  }

  @Override
  BaseConfigIntrospector getIntrospector()
  {
    return _introspector;
  }


  public String getDescription()
  {
    return _description;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  @Override
  BeanType getType()
  {
    return _embeddableType;
  }

  EntityType getEntityType()
  {
    return null;
  }

  /**
   * Returns the attributes.
   */
  public EmbeddableAttributesConfig getAttributes()
  {
    return _attributes;
  }

  /**
   * Sets the attributes.
   */
  public void setAttributes(EmbeddableAttributesConfig attributes)
  {
    _attributes = attributes;
  }

  /**
   * Returns the class name.
   */
  public String getClassName()
  {
    return _className;
  }

  /**
   * Sets the class name.
   */
  public void addClass(String className)
  {
    _className = className;
  }

  /**
   * Returns true if the metadata is complete.
   */
  public boolean isMetaDataComplete()
  {
    return _isMetadataComplete;
  }

  /**
   * Sets the metadata is complete (true) or not (false).
   */
  public void setMetaDataComplete(boolean isMetaDataComplete)
  {
    _isMetadataComplete = isMetaDataComplete;
  }
}
