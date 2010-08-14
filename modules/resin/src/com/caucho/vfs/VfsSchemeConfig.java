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
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

/**
 * Configures a vfs scheme.
 */
public class VfsSchemeConfig {
  private static L10N L = new L10N(VfsSchemeConfig.class);
  
  private String _name;
  private Path _path;

  /**
   * Sets the scheme name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the scheme path.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Initialize the scheme.
   */
  public void init()
    throws ConfigException
  {
    if (_name == null)
      throw new ConfigException(L.l("vfs-scheme requires a name attribute"));

    if (_path != null) {
      Vfs.getLocalScheme().put(_name, _path);
    }
  }
}
