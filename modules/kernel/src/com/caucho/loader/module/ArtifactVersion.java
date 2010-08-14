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

package com.caucho.loader.module;

import com.caucho.util.L10N;

/**
 * Artifact version major.minor.micro-qualifier
 */
public class ArtifactVersion
{
  private static final L10N L = new L10N(ArtifactVersion.class);

  private final int _major;
  private final int _minor;
  private final int _micro;

  private final String _qualifier;

  public ArtifactVersion(int major, int minor, int micro, String qualifier)
  {
    _major = major;
    _minor = minor;
    _micro = micro;
    _qualifier = qualifier;

    if ("".equals(qualifier))
      throw new IllegalArgumentException(L.l("qualifier may not be the empty string"));
  }

  public static ArtifactVersion create(String value)
  {
    int major = 0;
    int minor = 0;
    int micro = 0;
    String qualifier = null;
    
    int length = value.length();
    int i = 0;

    int ch = 0;

    for (; i < length && '0' <= (ch = value.charAt(i)) && ch <= '9'; i++) {
      major = 10 * major + ch - '0';
    }

    if (i < length && ch == '.')
      i++;

    for (; i < length && '0' <= (ch = value.charAt(i)) && ch <= '9'; i++) {
      minor = 10 * minor + ch - '0';
    }

    if (i < length && ch == '.')
      i++;

    for (; i < length && '0' <= (ch = value.charAt(i)) && ch <= '9'; i++) {
      micro = 10 * micro + ch - '0';
    }

    if (i < length && (ch == '-' || ch == '_' || ch == '.'))
      i++;

    if (i < length)
      qualifier = value.substring(i);

    return new ArtifactVersion(major, minor, micro, qualifier);
  }

  public int compareTo(ArtifactVersion version)
  {
    if (_major < version._major)
      return -1;
    else if (version._major < _major)
      return 1;
    
    if (_minor < version._minor)
      return -1;
    else if (version._minor < _minor)
      return 1;
    
    if (_micro < version._micro)
      return -1;
    else if (version._micro < _micro)
      return 1;

    if (_qualifier == null && version._qualifier != null)
      return -1;
    else if (_qualifier != null && version._qualifier == null)
      return 1;
    else if (_qualifier == null && version._qualifier == null)
      return 0;
    else
      return _qualifier.compareTo(version._qualifier);
  }

  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(_major);
    sb.append(".").append(_minor);
    sb.append(".").append(_micro);

    if (_qualifier != null)
      sb.append("-").append(_qualifier);

    return sb.toString();
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[").append(_major);
    sb.append(".").append(_minor);
    sb.append(".").append(_micro);

    if (_qualifier != null)
      sb.append("-").append(_qualifier);
    
    sb.append("]");

    return sb.toString();
  }
}
