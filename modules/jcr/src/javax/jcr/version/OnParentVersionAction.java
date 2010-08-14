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

package javax.jcr.version;

import java.util.HashMap;

public final class OnParentVersionAction {
  public static final int COPY = 1;
  public static final int VERSION = 2;
  public static final int INITIALIZE = 3;
  public static final int COMPUTE = 4;
  public static final int IGNORE = 5;
  public static final int ABORT = 6;

  public static final String ACTIONNAME_COPY = "COPY";
  public static final String ACTIONNAME_VERSION = "VERSION";
  public static final String ACTIONNAME_INITIALIZE = "INITIALIZE";
  public static final String ACTIONNAME_COMPUTE = "COMPUTE";
  public static final String ACTIONNAME_IGNORE = "IGNORE";
  public static final String ACTIONNAME_ABORT = "ABORT";

  private static final HashMap _nameMap = new HashMap();

  private OnParentVersionAction()
  {
  }
  
  public static String nameFromValue(int action)
  {
    switch (action) {
    case COPY:
      return ACTIONNAME_COPY;

    case VERSION:
      return ACTIONNAME_VERSION;

    case INITIALIZE:
      return ACTIONNAME_INITIALIZE;

    case COMPUTE:
      return ACTIONNAME_COMPUTE;

    case IGNORE:
      return ACTIONNAME_IGNORE;

    case ABORT:
      return ACTIONNAME_ABORT;

    default:
      throw new IllegalArgumentException(action + " is an unknown OnParentVersionAction");
    }
  }
  
  public static int valueFromName(String name)
  {
    Integer value = (Integer) _nameMap.get(name);

    if (value != null)
      return value.intValue();
    else
      throw new IllegalArgumentException(name + " is an unknown OnParentVersionAction");
  }

  static {
    _nameMap.put(ACTIONNAME_COPY, new Integer(COPY));
    _nameMap.put(ACTIONNAME_VERSION, new Integer(VERSION));
    _nameMap.put(ACTIONNAME_INITIALIZE, new Integer(INITIALIZE));
    _nameMap.put(ACTIONNAME_COMPUTE, new Integer(COMPUTE));
    _nameMap.put(ACTIONNAME_COMPUTE, new Integer(COMPUTE));
    _nameMap.put(ACTIONNAME_IGNORE, new Integer(IGNORE));
    _nameMap.put(ACTIONNAME_ABORT, new Integer(ABORT));
  }
}
