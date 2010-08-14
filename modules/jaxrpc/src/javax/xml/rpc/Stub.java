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

package javax.xml.rpc;

import java.util.Iterator;

/**
 * Represents the base interface for stub classes.
 */
public interface Stub {
  public static final String USERNAME_PROPERTY =
    "javax.xml.rpc.security.auth.username";
  public static final String PASSWORD_PROPERTY =
    "javax.xml.rpc.security.auth.password";
  public static final String ENDPOINT_ADDRESS_PROPERTY =
    "javax.xml.rpc.service.endpoint.address";
  public static final String SESSION_MAINTAIN_PROPERTY =
    "javax.xml.rpc.session.maintain";
  
  /**
   * Sets a named configuration property.
   */
  public void _setProperty(String name, Object value)
    throws JAXRPCException;
  
  /**
   * Gets a named configuration property.
   */
  public Object _getProperty(String name)
    throws JAXRPCException;
  
  /**
   * Returns an iterator of the properties for the stub.
   */
  public Iterator _getPropertyNames();
}
