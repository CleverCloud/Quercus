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
 * The OSGi bundle
 */
public interface Bundle
{
  public static final int UNINSTALLED = 0x00000001;
  public static final int INSTALLED = 0x00000002;
  public static final int RESOLVED = 0x00000004;
  public static final int STARTING = 0x00000008;
  public static final int STOPPING = 0x00000010;
  public static final int ACTIVE = 0x00000020;
  
  public static final int START_TRANSIENT = 1;
  public static final int START_ACTIVATION_POLICY = 2;
  public static final int STOP_TRANSIENT = 1;

  /**
   * Returns the bundle's current state
   */
  public int getState();

  /**
   * Start the bundle
   */
  public void start(int options)
    throws BundleException;

  /**
   * Start the bundle
   */
  public void start()
    throws BundleException;

  /**
   * Stop the bundle
   */
  public void stop(int options)
    throws BundleException;

  /**
   * Start the bundle
   */
  public void stop()
    throws BundleException;

  /**
   * Updates the bundle
   */
  public void update()
    throws BundleException;

  /**
   * Updates the bundle from an input stream
   */
  public void update(InputStream is)
    throws BundleException;

  /**
   * Uninstall the bundle
   */
  public void uninstall()
    throws BundleException;

  /**
   * Returns the Manifest headers
   */
  public Dictionary getHeaders();

  /**
   * Returns the bundle's unique id
   */
  public long getBundleId();

  /**
   * Returns the location
   */
  public String getLocation();

  /**
   * Returns the bundle's registered services
   */
  public ServiceReference []getRegisteredServices();

  /**
   * Returns the services the bundle is using
   */
  public ServiceReference []getServicesInUse();

  /**
   * Returns true if the bundle has the specified permission
   */
  public boolean hasPermission(Object permission);

  /**
   * Returns the specified resource from the bundle
   */
  public URL getResource(String name);

  /**
   * Returns the localized view of the manifest
   */
  public Dictionary getHeaders(String locale);

  /**
   * Returns the bundle's symbolic name
   */
  public String getSymbolicName();

  /**
   * Loads a class using the bundle's classloader
   */
  public Class loadClass(String name)
    throws ClassNotFoundException;

  /**
   * Returns the resources for the bundle
   */
  public Enumeration getResources(String name)
    throws IOException;

  /**
   * Returns the paths to entries in the bundle
   */
  public Enumeration getEntryPaths(String path);

  /**
   * Returns a URL to the named entry
   */
  public URL getEntry(String path);

  /**
   * Returns the last modified time of the bundle.
   */
  public long getLastModified();

  /**
   * Returns entries matching a pattern.
   */
  public Enumeration findEntries(String path,
                                 String filePattern,
                                 boolean recurse);

  /**
   * Returns the bundle's context
   */
  public BundleContext getBundleContext();
}
