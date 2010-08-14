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
import javax.ejb.SessionBean;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Abstract base class for a 3.0 session object
 */
abstract public class AbstractSessionObject
    implements Serializable
{
  // ejb/0ff0
  protected Class _businessInterface;

  public SessionBean _getObject()
  {
    throw new UnsupportedOperationException("_getObject is not implemented");
  }

  /**
   * Returns the server.
   */
  abstract public AbstractEjbBeanManager getServer();

  /**
   * Returns the server.
   */
  public AbstractEjbBeanManager __caucho_getServer()
  {
    return getServer();
  }

  /**
   * Returns the business interface.
   */
  public Class __caucho_getBusinessInterface()
  {
    return _businessInterface;
  }

  /**
   * Sets the business interface.
   */
  public void __caucho_setBusinessInterface(Class businessInterface)
  {
    _businessInterface = businessInterface;
  }

  /**
   * Serialize the HomeSkeletonWrapper in place of this object.
   *
   * @return the matching skeleton wrapper.
   */
  public Object writeReplace() throws ObjectStreamException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
