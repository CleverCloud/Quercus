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

package com.caucho.management.server;

import com.caucho.jmx.Description;

/**
 * Management interface for the memory
 *
 * <pre>
 * resin:type=Memory
 * </pre>
 */
@Description("Memory returns the JDK's memory statistics")
public interface MemoryMXBean extends ManagedObjectMXBean {
  @Description("Current allocated CodeCache memory")
  public long getCodeCacheCommitted();
  
  @Description("Max allocated CodeCache memory")
  public long getCodeCacheMax();
  
  @Description("Current used CodeCache memory")
  public long getCodeCacheUsed();
  
  @Description("Current free CodeCache memory")
  public long getCodeCacheFree();

  @Description("Current allocated Eden memory")
  public long getEdenCommitted();
  
  @Description("Max allocated Eden memory")
  public long getEdenMax();
  
  @Description("Current used Eden memory")
  public long getEdenUsed();
  
  @Description("Current free Eden memory")
  public long getEdenFree();
  
  @Description("Current allocated PermGen memory")
  public long getPermGenCommitted();
  
  @Description("Max allocated PermGen memory")
  public long getPermGenMax();
  
  @Description("Current used PermGen memory")
  public long getPermGenUsed();
  
  @Description("Current free PermGen memory")
  public long getPermGenFree();
  
  @Description("Current allocated Survivor memory")
  public long getSurvivorCommitted();
  
  @Description("Max allocated Survivor memory")
  public long getSurvivorMax();
  
  @Description("Current used Survivor memory")
  public long getSurvivorUsed();
  
  @Description("Current free Survivor memory")
  public long getSurvivorFree();
  
  @Description("Current allocated Tenured memory")
  public long getTenuredCommitted();
  
  @Description("Max allocated Tenured memory")
  public long getTenuredMax();
  
  @Description("Current used Tenured memory")
  public long getTenuredUsed();
  
  @Description("Current free Tenured memory")
  public long getTenuredFree();
}
