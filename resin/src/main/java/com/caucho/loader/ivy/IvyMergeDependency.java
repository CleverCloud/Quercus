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

package com.caucho.loader.ivy;

import java.util.*;

import com.caucho.config.program.ConfigProgram;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * IvyMergeDependency configuration
 */
public class IvyMergeDependency extends IvyDependency {
  private ArrayList<IvyDependency> _dependencyList
    = new ArrayList<IvyDependency>();

  IvyMergeDependency(IvyDependency a, IvyDependency b)
  {
    setOrg(a.getOrg());
    setName(a.getName());

    _dependencyList.add(a);
    _dependencyList.add(b);
  }

  public IvyDependency merge(IvyDependency dep)
  {
    _dependencyList.add(dep);

    return this;
  }

  public Path resolve(IvyCache cache)
  {
    IvyDependency min = _dependencyList.get(0);

    for (int i = 1; i < _dependencyList.size(); i++) {
      IvyDependency dep = _dependencyList.get(i);

      IvyRevision rev1 = new IvyRevision(min.getRev());
      IvyRevision rev2 = new IvyRevision(dep.getRev());

      int cmp = rev1.compareTo(rev2);
      
      if (cmp < 0)
        min = dep;
    }
    
    return cache.resolve(min);
  }

  @Override
  public int hashCode()
  {
    int hash = 37;

    hash = hash * 65521 + getOrg().hashCode();
    hash = hash * 65521 + getName().hashCode();

    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof IvyMergeDependency))
      return false;

    IvyMergeDependency dep = (IvyMergeDependency) o;

    return (getOrg().equals(dep.getOrg())
            && getName().equals(dep.getName()));
  }
}
