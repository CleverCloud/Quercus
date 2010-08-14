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
import com.caucho.config.types.*;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.xml.LooseXml;

import org.w3c.dom.Document;

import javax.annotation.PostConstruct;
import java.util.logging.Logger;

/**
 * Imports values from a separate file.
 */
public class ResinInclude extends ResinControl {
  private static final L10N L = new L10N(ResinInclude.class);
  private static final Logger log 
    = Logger.getLogger(ResinInclude.class.getName());

  private Path _path;
  private boolean _isOptional = true;
  private String _systemId;

  /**
   * Sets the current location.
   */
  public void setConfigSystemId(String systemId)
  {
    _systemId = systemId;
  }
  
  /**
   * Sets the resin:import path.
   */
  public void setHref(String path)
  {
    _path = Vfs.lookup().lookup(FileVar.__FILE__.toString()).getParent().lookup(path);
  }
  
  /**
   * Sets the resin:import path.
   */
  public void setPath(String path)
  {
    setHref(path);
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
    if (_path == null)
      throw new ConfigException(L.l("'href' attribute missing from resin:include."));

    if (_path.canRead() && ! _path.isDirectory()) {
    }
    else {
      throw new ConfigException(L.l("Required file '{0}' can not be read for resin:include.",
                                    _path.getNativePath()));
    }
    
    Object object = getObject();

    String schema = null;

    // Use the relax schema for beans with schema.
    if (object instanceof SchemaBean) {
      schema = ((SchemaBean) object).getSchema();
    }

    log.config(L.l("resin:include '{0}'.\nresin:include is deprecated.  Please use resin:import instead.", _path.getNativePath()));


    LooseXml xml = new LooseXml();

    Document doc = xml.parseDocument(_path);

    new Config().configure(object, doc); // , schema);
  }
}

