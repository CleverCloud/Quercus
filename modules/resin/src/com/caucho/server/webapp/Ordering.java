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
 * @author Alex Rojkov
 */

package com.caucho.server.webapp;

import com.caucho.config.Configurable;

import java.util.List;
import java.util.ArrayList;

public class Ordering {
  private Ordering _before;

  private Ordering _after;

  private Ordering _others;
  
  private List _order = new ArrayList();

  @Configurable
  public void addName(String name) {
    if (! _order.contains(name))
      _order.add(name);
  }

  public List getOrder() {
    return _order;
  }

  @Configurable
  public Ordering createBefore() {
    if (_before != null)
      throw new IllegalStateException();

    _before = new Ordering();

    return _before;
  }

  public Ordering getBefore() {
    return _before;
  }

  @Configurable
  public Ordering createAfter() {
    if (_after != null)
      throw new IllegalStateException();

    _after = new Ordering();
    
    return _after;
  }

  public Ordering getAfter() {
    return _after;
  }

  @Configurable
  public void addOthers(Ordering others) {
    if (_others != null)
      throw new IllegalStateException();
    
    _others = others;
    _order.add(others);
  }

  public boolean hasOthers() {
    return _others != null;
  }

  public boolean isOthers(Object ordering) {
    return ordering == _others;
  }
}
