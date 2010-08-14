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

import java.util.EventListener;

/**
 * Represents a version
 */
public class Version implements Comparable
{
  public static final Version emptyVersion = new Version(0, 0, 0);

  private final int _major;
  private final int _minor;
  private final int _micro;
  private final String _qualifier;

  public Version(int major, int minor, int micro)
  {
    _major = major;
    _minor = minor;
    _micro = micro;
    _qualifier = null;
  }

  public Version(int major, int minor, int micro, String qualifier)
  {
    _major = major;
    _minor = minor;
    _micro = micro;
    _qualifier = qualifier;
  }

  public Version(String version)
  {
    throw new UnsupportedOperationException();
  }

  public static Version parseVersion(String version)
  {
    throw new UnsupportedOperationException();
  }

  public int getMajor()
  {
    return _major;
  }

  public int getMinor()
  {
    return _minor;
  }

  public int getMicro()
  {
    return _micro;
  }

  public String getQualifier()
  {
    return _qualifier;
  }
  
  public int compareTo(Object obj)
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    if (_qualifier == null || "".equals(_qualifier))
      return "" + _major + "." + _minor + "." + _micro;
    else
      return "" + _major + "." + _minor + "." + _micro + "." + _qualifier;
  }
}
