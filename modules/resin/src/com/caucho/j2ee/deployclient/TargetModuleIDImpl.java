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

package com.caucho.j2ee.deployclient;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

/**
 * Represents a deployed module.
 */
public class TargetModuleIDImpl implements TargetModuleID, java.io.Serializable {
  private TargetImpl _target;
  private String _moduleID;

  private TargetModuleID _parent;

  TargetModuleIDImpl()
  {
  }

  public TargetModuleIDImpl(TargetImpl target, String moduleID)
  {
    _target = target;
    _moduleID = moduleID;
  }
  
  /**
   * Returns the target for the module.
   */
  public Target getTarget()
  {
    return _target;
  }
    
  /**
   * Returns the id.
   */
  public String getModuleID()
  {
    return _moduleID;
  }
    
  /**
   * Returns the web url
   */
  public String getWebURL()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the identifier of the parent object.
   */
  public TargetModuleID getParentTargetModuleID()
  {
    return _parent;
  }

  /**
   * Sets the parent.
   */
  public void setParentTargetModuleID(TargetModuleID parent)
  {
    _parent = parent;
  }

  /**
   * Returns the children of the object.
   */
  public TargetModuleID []getChildTargetModuleID()
  {
    return new TargetModuleID[0];
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof TargetModuleIDImpl))
      return false;

    TargetModuleIDImpl id = (TargetModuleIDImpl) o;

    return (_target.equals(id.getTarget())
            && _moduleID.equals(id.getModuleID()));
  }

  public String toString()
  {
    return "TargetModuleIDImpl[" + getModuleID() + "," + getTarget() + "]";
  }
}

