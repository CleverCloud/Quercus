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
 * @author Scott Ferguson
 */

package javax.resource.spi.endpoint;

import javax.resource.ResourceException;
import javax.resource.NotSupportedException;
import java.lang.reflect.Method;

/**
 * Implemented by the application server, called by the resource adapter.
 */
public interface MessageEndpoint {
  /**
   * Called by a resource adapter before a message is delivered.
   */
  public void beforeDelivery(Method method)
    throws NoSuchMethodException, ResourceException;

  /**
   * Called by a resource adapter after a message is delivered.
   */
  public void afterDelivery()
    throws ResourceException;

  /**
   * Called when the resource adapter doesn't need a proxy endpoint.
   */
  public void release();
}
