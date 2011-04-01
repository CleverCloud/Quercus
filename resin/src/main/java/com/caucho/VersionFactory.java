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

package com.caucho;

import java.lang.reflect.*;

final public class VersionFactory {
  private static Class<?> _versionClass;
  private static Field _fullVersion;
  private static Field _version;
  private static Field _versionDate;
  private static Field _copyright;

  public static final String COPYRIGHT =
    "Copyright(c) 1998-2010 Caucho Technology.  All rights reserved.";

  public static String getFullVersion()
  {
    try {
      return (String) _fullVersion.get(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String getVersion()
  {
    try {
      return (String) _version.get(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String getVersionDate()
  {
    try {
      return (String) _versionDate.get(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String getCopyright()
  {
    try {
      return (String) _copyright.get(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static {
    Class versionClass = null;

    try {
      versionClass = Class.forName("com.caucho.ProVersion");
    } catch (Exception e) {
    }

    try {
      if (versionClass == null)
        versionClass = Class.forName("com.caucho.Version");

      _fullVersion = versionClass.getField("FULL_VERSION");
      _version = versionClass.getField("VERSION");
      _versionDate = versionClass.getField("VERSION_DATE");
      _copyright = versionClass.getField("COPYRIGHT");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
