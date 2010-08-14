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

package com.caucho.server.e_app;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.ArrayList;

/**
 * Configuration for the application.xml file.
 */
public class ApplicationConfig {
  private static final L10N L = new L10N(ApplicationConfig.class);
  
  private String _displayName;
  private String _description;
  
  private ArrayList<WebModule> _webModules = new ArrayList<WebModule>();
  private ArrayList<Path> _ejbModules = new ArrayList<Path>();
  private ArrayList<Path> _javaModules = new ArrayList<Path>();
  private ArrayList<String> _connectorModules = new ArrayList<String>();
  private ArrayList<String> _altDDModules = new ArrayList<String>();

  /**
   * Sets the root directory.
   */
  public ApplicationConfig()
  {
  }
  
  /**
   * Sets the id
   */
  public void setId(String id)
  {
  }
  
  /**
   * Sets the application version.
   */
  public void setVersion(String version)
  {
  }
  
  /**
   * Sets the schema location
   */
  public void setSchemaLocation(String schema)
  {
  }

  /**
   * Sets the display name.
   */
  public void setDisplayName(String name)
  {
    _displayName = name;
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Sets the icon.
   */
  public void setIcon(Icon icon)
  {
  }
  
  /**
   * Adds a module.
   */
  public Module createModule()
  {
    return new Module();
  }

  /**
   * Adds a security role.
   */
  public void addSecurityRole(SecurityRole role)
  {
  }

  /**
   * Returns the web modules.
   */
  ArrayList<WebModule> getWebModules()
  {
    return _webModules;
  }

  /**
   * Returns the ejb modules.
   */
  ArrayList<Path> getEjbModules()
  {
    return _ejbModules;
  }

  /**
   * Returns the application client module.
   */
  ArrayList<Path> getJavaModules()
  {
    return _javaModules;
  }

  public class Module {
    private String _id;

    /**
     * Sets the module id.
     */
    public void setId(String id)
    {
      _id = id;
    }
    
    /**
     * Creates a new web module.
     */
    public void addWeb(WebModule web)
    {
      _webModules.add(web);

    }
    
    /**
     * Adds a new ejb module.
     */
    public void addEjb(Path path)
    {
      _ejbModules.add(path);
    }
    
    /**
     * Adds a new java module.
     */
    public void addJava(Path path)
      throws ConfigException
    {
      if (! path.canRead())
        throw new ConfigException(L.l("<java> module {0} must be a valid path.",
                                      path));
      
      _javaModules.add(path);
    }
    
    /**
     * Adds a new connector
     */
    public void addConnector(String path)
    {
      _connectorModules.add(path);
    }
    
    /**
     * Adds a new alt-dd module.
     */
    public void addAltDD(String path)
    {
      _altDDModules.add(path);
    }
  }
}
