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

package org.osgi.framework;

import java.util.EventListener;
import java.util.EventObject;

/**
 * A framework events
 */
public class FrameworkEvent extends EventObject
{
  public static final int STARTED = 0x00000001;
  public static final int ERROR = 0x00000002;
  public static final int PACKAGES_REFRESHED = 0x00000004;
  public static final int STARTLEVEL_CHANGED = 0x00000008;
  public static final int WARNING = 0x00000010;
  public static final int INFO = 0x00000020;

  private final int _type;
  private final Throwable _exn;

  /**
   * @deprecated
   */
  public FrameworkEvent(int type, Object source)
  {
    super(source);

    _type = type;
    _exn = null;
  }

  public FrameworkEvent(int type, Bundle bundle, Throwable exn)
  {
    super(bundle);

    _type = type;
    _exn = exn;
  }

  public Throwable getThrowable()
  {
    return _exn;
  }

  public Bundle getBundle()
  {
    return (Bundle) getSource();
  }

  public int getType()
  {
    return _type;
  }
}
