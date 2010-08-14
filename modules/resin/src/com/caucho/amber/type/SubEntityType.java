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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.type;

import com.caucho.amber.field.Id;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberColumn;
import com.caucho.util.L10N;

/**
 * Represents an application persistent bean type
 */
public class SubEntityType extends EntityType {
  private static final L10N L = new L10N(SubEntityType.class);

  private EntityType _root;
  private EntityType _parent;

  public SubEntityType(AmberPersistenceUnit amberPersistenceUnit,
                       EntityType parent)
  {
    super(amberPersistenceUnit);

    _parent = parent;
    _root = parent.getRootType();

    if (_root == null) {
      // jpa/0ge2: parent is MappedSuperclassType.
      _root = this;
    }

    _loadGroupIndex = -1;
    _defaultLoadGroupIndex = -1;
    _dirtyIndex = -1;
  }

  /**
   * Returns the id.
   */
  @Override
  public Id getId()
  {
    if (super.getId() != null)
      return super.getId();
    else
      return _parent.getId();
  }

  /**
   * Returns the root type.
   */
  public EntityType getRootType()
  {
    return _root;
  }

  /**
   * Returns the parent class.
   */
  public EntityType getParentType()
  {
    return _parent;
  }

  /**
   * Returns true if the superclass is a MappedSuperclass.
   */
  public boolean isParentMappedSuperclass()
  {
    if (_parent instanceof MappedSuperclassType)
      return true;

    return false;
  }

  /**
   * Returns the discriminator.
   */
  @Override
  public AmberColumn getDiscriminator()
  {
    if (getRootType() == this) // jpa/0ge2
      return super.getDiscriminator();

    return getRootType().getDiscriminator();
  }

  /**
   * Returns the load group index, overriding the parent.
   */
  @Override
  public int getLoadGroupIndex()
  {
    if (_loadGroupIndex < 0) {
      _loadGroupIndex = _parent.getLoadGroupIndex();

      // jpa/0ge2: MappedSuperclassType and
      // jpa/0gg0: abstract parent
      if (_parent.isEntity()) {
        _loadGroupIndex++;

        _defaultLoadGroupIndex = _loadGroupIndex;
      }
    }

    return _loadGroupIndex;
  }

  /**
   * Returns the current load group.
   */
  public int getDefaultLoadGroupIndex()
  {
    if (_defaultLoadGroupIndex < 0) {
      // initialized by getLoadGroupIndex()
      if (getParentType().isEntity())
        getLoadGroupIndex();
      else
        _defaultLoadGroupIndex = _parent.getDefaultLoadGroupIndex();
    }

    return _defaultLoadGroupIndex;
  }

  /**
   * Returns the dirty index, overriding the parent.
   */
  public int getDirtyIndex()
  {
    if (_dirtyIndex < 0) {
      _dirtyIndex = _parent.getDirtyIndex();
      _minDirtyIndex = _dirtyIndex;
    }

    return _dirtyIndex;
  }


  /**
   * Printable version of the entity.
   */
  public String toString()
  {
    return "SubEntityType[" + getBeanClass().getName() + "]";
  }
}
