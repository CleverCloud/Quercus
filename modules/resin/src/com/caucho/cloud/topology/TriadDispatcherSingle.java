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

package com.caucho.cloud.topology;

/**
 * Selects one of the triad members, given the triad owner.
 */
public class TriadDispatcherSingle<X> extends TriadDispatcher<X> {
  private final X _valueA;
  
  public TriadDispatcherSingle(X valueA)
  {
    _valueA = valueA;
    
    if (valueA == null)
      throw new NullPointerException();
  }
  
  /**
   * Returns the member A.
   */
  @Override
  public X getA()
  {
    return _valueA;
  }
  
  /**
   * Returns the primary server.
   */
  @Override
  public X primary(TriadOwner owner)
  {
    return _valueA;
  }
  
  /**
   * Returns the secondary server.
   */
  @Override
  public X secondary(TriadOwner owner)
  {
    return null;
  }
  
  /**
   * Returns the tertiary server.
   */
  @Override
  public X tertiary(TriadOwner owner)
  {
    return null;
  }
}
