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
 * <entity-listener> tag in the orm.xml
 */
public class EntityListenerConfig {

  // attributes
  private String _className;

  // elements
  private PrePersistConfig _prePersist;
  private PostPersistConfig _postPersist;
  private PreRemoveConfig _preRemove;
  private PostRemoveConfig _postRemove;
  private PreUpdateConfig _preUpdate;
  private PostUpdateConfig _postUpdate;
  private PostLoadConfig _postLoad;

  private AbstractListenerConfig _listener;

  /**
   * Returns the method name.
   */
  public String getMethodName()
  {
    return _listener.getMethodName();
  }

  /**
   * Returns the listener.
   */
  public AbstractListenerConfig getListener()
  {
    return _listener;
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

  public PrePersistConfig getPrePersist()
  {
    return _prePersist;
  }

  public void setPrePersist(PrePersistConfig prePersist)
  {
    _listener = prePersist;
    _prePersist = prePersist;
  }

  public PostPersistConfig getPostPersist()
  {
    return _postPersist;
  }

  public void setPostPersist(PostPersistConfig postPersist)
  {
    _listener = postPersist;
    _postPersist = postPersist;
  }

  public PreRemoveConfig getPreRemove()
  {
    return _preRemove;
  }

  public void setPreRemove(PreRemoveConfig preRemove)
  {
    _listener = preRemove;
    _preRemove = preRemove;
  }

  public PostRemoveConfig getPostRemove()
  {
    return _postRemove;
  }

  public void setPostRemove(PostRemoveConfig postRemove)
  {
    _listener = postRemove;
    _postRemove = postRemove;
  }

  public PreUpdateConfig getPreUpdate()
  {
    return _preUpdate;
  }

  public void setPreUpdate(PreUpdateConfig preUpdate)
  {
    _listener = preUpdate;
    _preUpdate = preUpdate;
  }

  public PostUpdateConfig getPostUpdate()
  {
    return _postUpdate;
  }

  public void setPostUpdate(PostUpdateConfig postUpdate)
  {
    _listener = postUpdate;
    _postUpdate = postUpdate;
  }

  public PostLoadConfig getPostLoad()
  {
    return _postLoad;
  }

  public void setPostLoad(PostLoadConfig postLoad)
  {
    _listener = postLoad;
    _postLoad = postLoad;
  }
}
