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

package com.caucho.ejb.session;

import com.caucho.ejb.protocol.ObjectSkeletonWrapper;
import com.caucho.ejb.server.AbstractEjbBeanManager;

import javax.ejb.Handle;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.rmi.RemoteException;
import javax.ejb.*;

/**
 * Abstract base class for a stateful session object
 */
abstract public class StatefulObject
  implements Serializable
{
  private String _primaryKey;
  
  /**
   * Returns the server.
   */
  public AbstractEjbBeanManager getServer()
  {
    return getStatefulServer();
  }

  /**
   * Returns the server which owns this bean.
   */
  public AbstractEjbBeanManager __caucho_getServer()
  {
    return getStatefulServer();
  }

  public abstract StatefulManager getStatefulServer();

  public String __caucho_getId()
  {
    if (_primaryKey == null)
      _primaryKey = getStatefulServer().createSessionKey(this);
    
    return _primaryKey;
  }

  //
  // EJB 2.1 methods
  //

  /**
   * Returns the primary key
   */
  public Object getPrimaryKey()
  {
    return __caucho_getId();
  }

  /**
   * Serialize the HomeSkeletonWrapper in place of this object.
   *
   * @return the matching skeleton wrapper.
   */
  public Object writeReplace() throws ObjectStreamException
  {
    throw new UnsupportedOperationException(getClass().getName());
    // return new ObjectSkeletonWrapper(getHandle());
  }
}
