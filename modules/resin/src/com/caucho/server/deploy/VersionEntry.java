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

package com.caucho.server.deploy;

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.Period;
import com.caucho.env.repository.AbstractRepository;
import com.caucho.env.repository.RepositoryTagEntry;
import com.caucho.loader.Environment;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.Crc64;
import com.caucho.util.L10N;
import com.caucho.util.WeakAlarm;
import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VersionEntry {
  private final String _name;
  private final String _contextPath;
  private final String _baseContextPath;
  private final String _sha1;
  private final String _root;

  VersionEntry(String name,
               String contextPath,
               String baseContextPath,
               String sha1,
               String root)
  {
    _name = name;
    _contextPath = contextPath;
    _baseContextPath = baseContextPath;
    _sha1 = sha1;
    _root = root;
  }

  public String getName()
  {
    return _name;
  }

  public String getContextPath()
  {
    return _contextPath;
  }

  public String getBaseContextPath()
  {
    return _baseContextPath;
  }

  public String getSha1()
  {
    return _sha1;
  }

  public String getRoot()
  {
    return _root;
  }

  @Override
    public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _contextPath
            + "," + _baseContextPath
            + "," + _root + "]");
  }
}
