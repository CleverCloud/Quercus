/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.boot;

import java.io.Serializable;

/**
 * Queries the Resin instance for its pid.
 */
@SuppressWarnings("serial")
public class PidQuery implements Serializable {
  private final int _pid;

  public PidQuery()
  {
    _pid = 0;
  }

  public PidQuery(int pid)
  {
    _pid = pid;
  }
  
  /**
   * Returns the query's pid
   */
  public int getPid()
  {
    return _pid;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _pid + "]";
  }
}
