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


package com.caucho.tools.profiler;

import java.util.Comparator;

abstract class ProfilerNodeComparator
  implements Comparator<ProfilerPoint>
{
  private boolean _isDescendingName;
  private boolean _isDescending;

  public void setDescendingName(boolean isDescendingName)
  {
    _isDescendingName = isDescendingName;
  }

  public void setDescending(boolean isDescending)
  {
    _isDescending = isDescending;
  }

  abstract protected int compareImpl(ProfilerPoint o1, ProfilerPoint o2);

  protected int compareLong(long l1, long l2)
  {
    if (l1 < l2)
      return -1;
    else if (l1 == l2)
      return 0;
    else
      return 1;
  }

  public int compare(ProfilerPoint o1, ProfilerPoint o2)
  {
    int cmp;

    if (_isDescending)
      cmp = compareImpl(o2, o1);
    else
      cmp = compareImpl(o1, o2);

    if (cmp == 0) {
      if (_isDescendingName)
        cmp = o2.getName().compareTo(o1.getName());
      else
        cmp = o1.getName().compareTo(o2.getName());
    }

    if (cmp == 0) {
      if (!o1.equals(o2))
        cmp = -1;
    }

    return cmp;
  }
}
