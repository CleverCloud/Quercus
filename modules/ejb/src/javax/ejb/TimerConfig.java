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
 * @author Reza Rahman
 */
package javax.ejb;

import java.io.Serializable;

/**
 * Specifies the configuration of a timer.
 * 
 * @author Reza Rahman
 */
public class TimerConfig {

  private Serializable _info;
  private boolean _persistent;

  /**
   * Constructs a new timer configuration.
   */
  public TimerConfig()
  {
    super();
  }

  /**
   * Constructs a new timer configuration.
   * 
   * @param info
   *          Info object associated with the timer.
   * @param persistent
   *          Indicates if a timer should be persisted.
   */
  public TimerConfig(Serializable info, boolean persistent)
  {
    super();

    _info = info;
    _persistent = persistent;
  }

  /**
   * Gets info object associated with timer.
   * 
   * @return Info object associated with timer.
   */
  public Serializable getInfo()
  {
    return _info;
  }

  /**
   * Sets info object associated with timer.
   * 
   * @param info
   *          Info object associated with timer.
   */
  public void setInfo(Serializable info)
  {
    _info = info;
  }

  /**
   * Indicates if timer should be persisted.
   * 
   * @return true if timer should be persisted.
   */
  public boolean isPersistent()
  {
    return _persistent;
  }

  /**
   * Sets if timer should be persisted.
   * 
   * @param persistent
   *          Flag to indicate if timer should be persisted.
   */
  public void sePersistent(boolean persistent)
  {
    _persistent = persistent;
  }
}