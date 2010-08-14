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

package com.caucho.jsp.cfg;

import com.caucho.config.program.ConfigProgram;
import com.caucho.server.webapp.WebApp;
import com.caucho.vfs.Path;

import java.util.ArrayList;

/**
 * Configuration for the taglib in the .tld
 */
public class TldPreload {
  private boolean _isInit;

  private String _uri;
  private String _location;

  private ArrayList<TldListener> _listeners = new ArrayList<TldListener>();

  private Path _path;
  private Throwable _configException;

  /**
   * Sets the uri
   */
  public void setURI(String uri)
  {
    _uri = uri;
  }

  /**
   * Gets the uri
   */
  public String getURI()
  {
    return _uri;
  }

  /**
   * Sets the location
   */
  public void setLocation(String location)
  {
    _location = location;
  }

  /**
   * Gets the location
   */
  public String getLocation()
  {
    return _uri;
  }

  /**
   * Adds a listener
   */
  public void addListener(TldListener listener)
  {
    _listeners.add(listener);
  }

  /**
   * Sets the path to the tld.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Gets the path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets any configuration exception
   */
  public void setConfigException(Throwable e)
  {
    _configException = e;
  }

  /**
   * Gets any configuration exception
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Ignores unknown options.
   */
  public void addBuilderProgram(ConfigProgram program)
  {
  }


  /**
   * Applies the listeners.
   */
  public void initListeners(WebApp app)
    throws InstantiationException, IllegalAccessException
  {
    if (app == null)
      return;

    for (int i = 0; i < _listeners.size(); i++) {
      TldListener listener = _listeners.get(i);

      listener.register(app);
    }
  }

  public boolean isJsf()
  {
    return false;
  }

  public String toString()
  {
    return "TldPreload[" + _path + "]";
  }
}
