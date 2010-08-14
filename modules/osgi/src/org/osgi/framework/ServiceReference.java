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

package org.osgi.framework;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

/**
 * A reference to a service
 */
public interface ServiceReference extends Comparable
{
  /**
   * Returns the service's property
   */
  public Object getProperty(String key);

  /**
   * Returns all the service's property keys
   */
  public String []getPropertyKeys();

  /**
   * Returns the bundle that registered the service
   */
  public Bundle getBundle();

  /**
   * Returns the bundles using the service
   */
  public Bundle []getUsingBundles();

  /**
   * Checks if the bundled which registered the service uses this class name
   */
  public boolean isAssignableTo(Bundle bundle,
                                String className);

  /**
   * Compares to another reference for ordering
   */
  public int compareTo(Object reference);
}
