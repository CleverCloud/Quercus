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

/**
 * Artifact version major.minor.micro-qualifier
 */
public class ArtifactVersionRange
{
  private final ArtifactVersion _min;
  private final boolean _isMinInclusive;
  
  private final ArtifactVersion _max;
  private final boolean _isMaxInclusive;

  private final boolean _isStrict;
  
  public ArtifactVersionRange(ArtifactVersion min,
                              boolean isMinInclusive,
                              ArtifactVersion max,
                              boolean isMaxInclusive)
  {
    _min = min;
    _isMinInclusive = isMinInclusive;
    _max = max;
    _isMaxInclusive = isMaxInclusive;
    
    _isStrict = true;
  }
  
  public ArtifactVersionRange(ArtifactVersion version)
  {
    _min = version;
    _isMinInclusive = true;
    _max = version;
    _isMaxInclusive = true;
    
    _isStrict = false;
  }

  public static ArtifactVersionRange create(String value)
  {
    ArtifactVersion min = null;
    ArtifactVersion max = null;
    boolean isMinInclusive = true;
    boolean isMaxInclusive = true;
    boolean isStrict = false;

    if (value.startsWith("[")) {
      isMinInclusive = true;
      isStrict = true;
      value = value.substring(1);
    }
    else if (value.startsWith("(")) {
      isMinInclusive = false;
      isStrict = true;
      value = value.substring(1);
    }

    if (value.endsWith("]")) {
      isMaxInclusive = true;
      isStrict = true;
      value = value.substring(0, value.length() - 1);
    }
    else if (value.endsWith(")")) {
      isMaxInclusive = false;
      isStrict = true;
      value = value.substring(0, value.length() - 1);
    }

    value = value.trim();

    if (value.indexOf(',') >= 0) {
      isStrict = true;
      
      String []values = value.split("[, ]+");

      String minValue = values[0].trim();
      String maxValue = values[1].trim();

      if (minValue.length() > 0)
        min = ArtifactVersion.create(minValue);
      
      if (maxValue.length() > 0)
        max = ArtifactVersion.create(maxValue);
    }
    else {
      min = max = ArtifactVersion.create(value);
    }

    if (isStrict) {
      return new ArtifactVersionRange(min, isMinInclusive,
                                      max, isMaxInclusive);
    }
    else
      return new ArtifactVersionRange(min);
  }

  public boolean isMatch(ArtifactVersion version)
  {
    if (! _isStrict)
      return true;

    if (_min != null) {
      int cmp = version.compareTo(_min);

      if (cmp < 0 || cmp == 0 && ! _isMinInclusive)
        return false;
    }

    if (_max != null) {
      int cmp = version.compareTo(_max);

      if (cmp > 0 || cmp == 0 && ! _isMaxInclusive)
        return false;
    }
    
    return true;
  }

  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    if (! _isStrict) {
    }
    else if (_isMinInclusive)
      sb.append("[");
    else
      sb.append("(");

    if (_min != null)
      sb.append(_min.toDebugString());

    if (_min != _max) {
      sb.append(",");

      if (_max != null)
        sb.append(_max.toDebugString());
    }

    if (! _isStrict) {
    }
    else if (_isMaxInclusive)
      sb.append("]");
    else
      sb.append(")");

    return sb.toString();
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());

    if (! _isStrict)
      sb.append("{");
    else if (_isMinInclusive)
      sb.append("[");
    else
      sb.append("(");

    if (_min != null)
      sb.append(_min.toDebugString());

    if (_min != _max) {
      sb.append(",");

      if (_max != null)
        sb.append(_max.toDebugString());
    }

    if (! _isStrict)
      sb.append("}");
    else if (_isMaxInclusive)
      sb.append("]");
    else
      sb.append(")");

    return sb.toString();
  }
}
