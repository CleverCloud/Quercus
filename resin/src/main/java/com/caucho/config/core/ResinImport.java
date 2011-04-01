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

package com.caucho.config.core;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.SchemaBean;
import com.caucho.config.type.FlowBean;
import com.caucho.config.types.FileSetType;
import com.caucho.loader.Environment;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Imports values from a separate file.
 */
// XXX: FlowBean is from ioc/04c1 and server/1ac2
public class ResinImport extends ResinControl implements FlowBean
{
  private static final L10N L = new L10N(ResinImport.class);
  private static final Logger log
    = Logger.getLogger(ResinImport.class.getName());

  private Path _path;
  private FileSetType _fileSet;
  private boolean _isOptional;

  /**
   * Sets the resin:import path.
   */
  public void setPath(Path path)
  {
    if (path == null)
      throw new NullPointerException(L.l("'path' may not be null for resin:import"));
    
    _path = path;
  }

  /**
   * Sets the resin:import fileset.
   */
  public void setFileset(FileSetType fileSet)
  {
    _fileSet = fileSet;
  }
  
  /**
   * Sets true if the path is optional.
   */
  public void setOptional(boolean optional)
  {
    _isOptional = optional;
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    if (_path == null) {
      if (_fileSet == null)
        throw new ConfigException(L.l("'path' attribute missing from resin:import."));
    }
    else if (_path.canRead() && ! _path.isDirectory()) {
    }
    else if (_isOptional && ! _path.exists()) {
      log.finer(L.l("resin:import '{0}' is not readable.", _path));

      Environment.addDependency(new Depend(_path));
      return;
    }
    else {
      throw new ConfigException(L.l("Required file '{0}' can not be read for resin:import.",
                                    _path.getNativePath()));
    }
    
    Object object = getObject();

    String schema = null;
    // Use the relax schema for beans with schema.
    if (object instanceof SchemaBean) {
      schema = ((SchemaBean) object).getSchema();
    }

    ArrayList<Path> paths;

    if (_fileSet != null)
      paths = _fileSet.getPaths();
    else {
      paths = new ArrayList<Path>();
      paths.add(_path);
    }

    for (int i = 0; i < paths.size(); i++) {
      Path path = paths.get(i);

      log.config(L.l("resin:import '{0}'", path.getNativePath()));

      Environment.addDependency(new Depend(path));

      Config config = new Config();
      // server/10hc
      // config.setResinInclude(true);

      config.configureBean(object, path, schema);
    }
  }
}

