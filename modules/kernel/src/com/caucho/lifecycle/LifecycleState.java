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

package com.caucho.lifecycle;

/**
 * Lifecycle constants.
 */
public interface LifecycleState {
  public static final int IS_NEW = 0;
  public static final int IS_INITIALIZING = 1;
  public static final int IS_INIT = 2;
  public static final int IS_STARTING = 3;
  public static final int IS_STANDBY = 4;
  public static final int IS_WARMUP = 5;
  public static final int IS_ACTIVE = 6;
  public static final int IS_FAILED = 7;
  public static final int IS_STOPPING = 8;
  public static final int IS_STOPPED = 9;
  public static final int IS_DESTROYING = 10;
  public static final int IS_DESTROYED = 11;

  public int getState();

  public String getStateName();

  public boolean isInitializing();

  public boolean isInit();

  public boolean isBeforeInit();

  public boolean isAfterInit();

  public boolean isStarting();

  public boolean isBeforeActive();

  public boolean isAfterActive();

  public boolean isWarmup();

  public boolean isActive();

  public boolean isError();

  public boolean isStopping();

  public boolean isStopped();

  public boolean isDestroying();

  public boolean isDestroyed();
}
