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


/**
 * <mapped-superclass> tag in the orm.xml
 */
public class MappedSuperclassConfig extends AbstractEnhancedConfig {

  // attributes
  private String _className;
  private boolean _isMetadataComplete;

  // elements
  private String _description;
  private IdClassConfig _idClass;
  private boolean _excludeDefaultListeners;
  private boolean _excludeSuperclassListeners;
  private EntityListenersConfig _entityListeners;
  private PrePersistConfig _prePersist;
  private PostPersistConfig _postPersist;
  private PreRemoveConfig _preRemove;
  private PostRemoveConfig _postRemove;
  private PreUpdateConfig _preUpdate;
  private PostUpdateConfig _postUpdate;
  private PostLoadConfig _postLoad;
  private AttributesConfig _attributes;

  public MappedSuperclassConfig()
  {
  }

  MappedSuperclassConfig(String name)
  {
    super(name);
  }

  /**
   * Returns the attributes.
   */
  public AttributesConfig getAttributes()
  {
    return _attributes;
  }

  /**
   * Returns the class name.
   */
  public String getClassName()
  {
    return _className;
  }

  /**
   * Returns the class name.
   */
  public String getSimpleClassName()
  {
    int p = _className.lastIndexOf('.');

    if (p > 0)
      return _className.substring(p + 1);
    else
      return _className;
  }

  /**
   * Returns true if the metadata is complete.
   */
  public boolean isMetaDataComplete()
  {
    return _isMetadataComplete;
  }

  /**
   * Sets the attributes.
   */
  public void setAttributes(AttributesConfig attributes)
  {
    _attributes = attributes;
  }

  /**
   * Sets the class name.
   */
  public void addClass(String className)
  {
    _className = className;
  }

  /**
   * Sets the metadata is complete (true) or not (false).
   */
  public void setMetaDataComplete(boolean isMetaDataComplete)
  {
    _isMetadataComplete = isMetaDataComplete;
  }

  public String getDescription()
  {
    return _description;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  public IdClassConfig getIdClass()
  {
    return _idClass;
  }

  public void setIdClass(IdClassConfig idClass)
  {
    _idClass = idClass;
  }

  public boolean getExcludeDefaultListeners()
  {
    return _excludeDefaultListeners;
  }

  public void setExcludeDefaultListeners(boolean excludeDefaultListeners)
  {
    _excludeDefaultListeners = excludeDefaultListeners;
  }

  public boolean getExcludeSuperclassListeners()
  {
    return _excludeSuperclassListeners;
  }

  public void setExcludeSuperclassListeners(boolean excludeSuperclassListeners)
  {
    _excludeSuperclassListeners = excludeSuperclassListeners;
  }

  public EntityListenersConfig getEntityListeners()
  {
    return _entityListeners;
  }

  public void setEntityListeners(EntityListenersConfig entityListeners)
  {
    _entityListeners = entityListeners;
  }

  public PrePersistConfig getPrePersist()
  {
    return _prePersist;
  }

  public void setPrePersist(PrePersistConfig prePersist)
  {
    _prePersist = prePersist;
  }

  public PostPersistConfig getPostPersist()
  {
    return _postPersist;
  }

  public void setPostPersist(PostPersistConfig postPersist)
  {
    _postPersist = postPersist;
  }

  public PreRemoveConfig getPreRemove()
  {
    return _preRemove;
  }

  public void setPreRemove(PreRemoveConfig preRemove)
  {
    _preRemove = preRemove;
  }

  public PostRemoveConfig getPostRemove()
  {
    return _postRemove;
  }

  public void setPostRemove(PostRemoveConfig postRemove)
  {
    _postRemove = postRemove;
  }

  public PreUpdateConfig getPreUpdate()
  {
    return _preUpdate;
  }

  public void setPreUpdate(PreUpdateConfig preUpdate)
  {
    _preUpdate = preUpdate;
  }

  public PostUpdateConfig getPostUpdate()
  {
    return _postUpdate;
  }

  public void setPostUpdate(PostUpdateConfig postUpdate)
  {
    _postUpdate = postUpdate;
  }

  public PostLoadConfig getPostLoad()
  {
    return _postLoad;
  }

  public void setPostLoad(PostLoadConfig postLoad)
  {
    _postLoad = postLoad;
  }

  public String toString()
  {
    return "MappedSuperclassConfig[" + _className + "]";
  }
}
