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

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;

/**
 * The bundle's execution context.
 */
public interface BundleContext
{
  /**
   * Returns the bundle property.  Search the Framework properties,
   * then the system properties
   */
  public String getProperty(String key);

  /**
   * Returns the Bundle for this context
   */
  public Bundle getBundle();

  /**
   * Installs a bundle from the location string
   */
  public Bundle installBundle(String location)
    throws BundleException;

  /**
   * Installs a bundle from an input stream.
   */
  public Bundle installBundle(String location,
                              InputStream is)
    throws BundleException;

  /**
   * Returns the bundle with the given id.
   */
  public Bundle getBundle(long id);

  /**
   * Returns all the installed bundles
   */
  public Bundle []getBundles();

  /**
   * Adds a listener for service events
   */
  public void addServiceListener(ServiceListener listener,
                                 String filter)
    throws InvalidSyntaxException;

  /**
   * Adds a listener for service events
   */
  public void addServiceListener(ServiceListener listener);

  /**
   * Removes a listener for service events
   */
  public void removeServiceListener(ServiceListener listener);

  /**
   * Adds a listener for bundle events
   */
  public void addBundleListener(BundleListener listener);

  /**
   * Removes a listener for bundle events
   */
  public void removeBundleListener(BundleListener listener);

  /**
   * Adds a listener for framework events
   */
  public void addFrameworkListener(FrameworkListener listener);

  /**
   * Removes a listener for framework events
   */
  public void removeFrameworkListener(FrameworkListener listener);

  /**
   * Registers a service
   */
  public ServiceRegistration registerService(String []classNames,
                                             Object service,
                                             Dictionary properties);

  /**
   * Registers a service
   */
  public ServiceRegistration registerService(String className,
                                             Object service,
                                             Dictionary properties);

  /**
   * Returns matching services.
   */
  public ServiceReference []getServiceReferences(String className,
                                                 String filter)
    throws InvalidSyntaxException;

  /**
   * Returns all matching services.
   */
  public ServiceReference []getAllServiceReferences(String className,
                                                    String filter)
    throws InvalidSyntaxException;

  /**
   * Returns a service reference
   */
  public ServiceReference getServiceReference(String className);

  /**
   * Returns the service object for the service
   */
  public Object getService(ServiceReference reference);

  /**
   * Release the service object for the service
   */
  public boolean ungetService(ServiceReference reference);

  /**
   * Returns a data storage area for the bundle
   */
  public File getDataFile(String fileName);

  /**
   * Creates a filter
   */
  public Filter createFilter(String filter)
    throws InvalidSyntaxException;
}
