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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import javax.persistence.CascadeType;
import java.util.HashSet;


/**
 * <cascade> tag in orm.xml
 */
public class CascadeConfig {

  // elements
  private HashSet<CascadeType> _cascadeSet
    = new HashSet<CascadeType>();

  protected CascadeType[] getCascadeTypes() 
  {
    CascadeType cascade[] = new CascadeType[_cascadeSet.size()];
    return _cascadeSet.toArray(cascade);
  }

  public boolean getCascadeAll()
  {
    return _cascadeSet.contains(CascadeType.ALL);
  }

  public void setCascadeAll(boolean cascadeAll)
  {
    if (cascadeAll)
      _cascadeSet.add(CascadeType.ALL);
  }

  public boolean getCascadePersist()
  {
    return _cascadeSet.contains(CascadeType.PERSIST);
  }

  public void setCascadePersist(boolean cascadePersist)
  {
    if (cascadePersist)
      _cascadeSet.add(CascadeType.PERSIST);
  }

  public boolean getCascadeMerge()
  {
    return _cascadeSet.contains(CascadeType.MERGE);
  }

  public void setCascadeMerge(boolean cascadeMerge)
  {
    if (cascadeMerge)
      _cascadeSet.add(CascadeType.MERGE);
  }

  public boolean getCascadeRemove()
  {
    return _cascadeSet.contains(CascadeType.REMOVE);
  }

  public void setCascadeRemove(boolean cascadeRemove)
  {
    if (cascadeRemove)
      _cascadeSet.add(CascadeType.REMOVE);
  }

  public boolean getCascadeRefresh()
  {
    return _cascadeSet.contains(CascadeType.REFRESH);
  }

  public void setCascadeRefresh(boolean cascadeRefresh)
  {
    if (cascadeRefresh)
      _cascadeSet.add(CascadeType.REFRESH);
  }
}
