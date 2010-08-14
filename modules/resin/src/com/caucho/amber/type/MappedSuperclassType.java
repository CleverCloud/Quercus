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

package com.caucho.amber.type;

import com.caucho.amber.entity.MappedSuperclass;
import com.caucho.amber.gen.*;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.*;
import com.caucho.java.gen.ClassComponent;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Represents a non-persistent class with abstract O/R mapping information.
 */
public class MappedSuperclassType extends EntityType {
  private static final Logger log = Logger.getLogger(MappedSuperclassType.class.getName());
  private static final L10N L = new L10N(MappedSuperclassType.class);

  public MappedSuperclassType(AmberPersistenceUnit amberPersistenceUnit)
  {
    super(amberPersistenceUnit);
  }

  /**
   * returns false since the mapped superclass can't be loaded
   */
  @Override
  public boolean isEntity()
  {
    return false;
  }
  
  /**
   * Gets the instance class.
   */
  @Override
  public Class getInstanceClass()
  {
    return getInstanceClass(MappedSuperclass.class);
  }

  /**
   * Returns the component interface name.
   */
  @Override
  public String getComponentInterfaceName()
  {
    return "com.caucho.amber.entity.MappedSuperclass";
  }

  /**
   * Gets a component generator.
   */
  @Override
  public ClassComponent getComponentGenerator()
  {
    return new MappedSuperclassComponent();
  }

  @Override
  public AmberTable getTable()
  {
    return null;
  }

  /**
   * id is not initialized for mapped superclass
   */
  @Override
  protected void initId()
  {
  }
}
