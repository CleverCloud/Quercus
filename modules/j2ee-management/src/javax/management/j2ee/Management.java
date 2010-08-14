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
 * @author Sam
 */


package javax.management.j2ee;

import javax.ejb.EJBObject;
import javax.management.*;
import javax.management.j2ee.ListenerRegistration;
import java.rmi.RemoteException;
import java.util.Set;

/**
 * EJB service interface for management.
 */
public interface Management extends EJBObject {
  /**
   * Returns the value of the named attribute.
   */
  public Object getAttribute(ObjectName name, String attribute)
    throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, RemoteException;

  /**
   * Returns the values of the named attributes.
   */
  public AttributeList getAttributes(ObjectName name, String []attributes)
    throws InstanceNotFoundException, ReflectionException, RemoteException;

  /**
   * Returns the value of the default domain.
   */
  public String getDefaultDomain()
    throws RemoteException;

  /**
   * Returns the total number of management beans.
   */
  public Integer getMBeanCount()
    throws RemoteException;

  /**
   * Returns the {@link MBeanInfo} for a management beans.
   */
  public MBeanInfo getMBeanInfo(ObjectName objectName)
    throws IntrospectionException,
           InstanceNotFoundException,
           ReflectionException,
           RemoteException;

  /**
   * Invokes an operation on a management bean and returns the result.
   */
  public Object invoke(ObjectName objectName,
                       String operationName,
                       Object []parameters,
                       String []signature)
    throws MBeanException, InstanceNotFoundException, ReflectionException, RemoteException;

  /**
   * Returns true if the management bean with the specified name is registered.
   */
  public boolean isRegistered(ObjectName objectName)
    throws RemoteException;

  /**
   * Returns a Set of {@link ObjectName} that match the query.
   */
  public Set queryNames(ObjectName objectName, QueryExp queryExp)
    throws RemoteException;

  public void setAttribute(ObjectName objectName, Attribute attribute)
    throws InstanceNotFoundException,
           AttributeNotFoundException,
           InvalidAttributeValueException,
           MBeanException,
           ReflectionException,
           RemoteException;

  /**
   * Sets the values of the specified attributes.
   */
  public AttributeList setAttributes(ObjectName objectName, AttributeList attributes)
    throws InstanceNotFoundException,
           ReflectionException,
           RemoteException;

  /**
   * Returns the listener registry.  The listener registry is used
   * to register a listener that receives notifications.
   */
  public ListenerRegistration getListenerRegistry()
    throws RemoteException;

}
