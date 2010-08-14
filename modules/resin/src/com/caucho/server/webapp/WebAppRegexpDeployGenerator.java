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

package com.caucho.server.webapp;

import com.caucho.config.types.PathBuilder;
import com.caucho.server.deploy.DeployContainer;
import com.caucho.server.deploy.DeployGenerator;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The generator for the web-app deploy
 */
public class WebAppRegexpDeployGenerator
  extends DeployGenerator<WebAppController> {
  private static final Logger log
    = Logger.getLogger(WebAppSingleDeployGenerator.class.getName());

  private WebAppContainer _container;
  
  private WebAppController _parent;

  private WebAppConfig _config;
  
  private ArrayList<WebAppConfig> _webAppDefaults =
    new ArrayList<WebAppConfig>();

  private ArrayList<WebAppController> _entries =
    new ArrayList<WebAppController>();

  /**
   * Creates the new host deploy.
   */
  public WebAppRegexpDeployGenerator(DeployContainer<WebAppController> deployContainer)
  {
    super(deployContainer);
  }

  /**
   * Creates the new host deploy.
   */
  public WebAppRegexpDeployGenerator(DeployContainer<WebAppController> deployContainer,
                            WebAppContainer container,
                            WebAppConfig config)
  {
    super(deployContainer);
    
    setContainer(container);

    _config = config;
  }

  /**
   * Gets the webApp container.
   */
  public WebAppContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the webApp container.
   */
  public void setContainer(WebAppContainer container)
  {
    _container = container;
  }
  /**
   * Sets the parent webApp.
   */
  public void setParent(WebAppController parent)
  {
    _parent = parent;
  }
  
  /**
   * Returns the current array of webApp entries.
   */
  public WebAppController generateController(String name)
  {
    Pattern regexp = _config.getURLRegexp();
    Matcher matcher = regexp.matcher(name);

    if (! matcher.find() || matcher.start() != 0) {
      return null;
    }

    int length = matcher.end() - matcher.start();

    String contextPath = matcher.group();
        
    ArrayList<String> vars = new ArrayList<String>();

    //WebAppDeployControlleryController entry = new WebAppDeployControlleryController(this, contextPath);
    HashMap<String,Object> varMap = new HashMap<String,Object>();
    // entry.getVariableMap();
        
    for (int j = 0; j <= matcher.groupCount(); j++) {
      vars.add(matcher.group(j));
      varMap.put("app" + j, matcher.group(j));
    }

    varMap.put("regexp", vars);

    Path appDir = null;
    
    try {
      String appDirPath = _config.getDocumentDirectory();

      if (appDirPath == null)
        appDirPath = "./" + matcher.group(0);
      
      appDir = PathBuilder.lookupPath(appDirPath, varMap);

      if (! appDir.isDirectory() || ! appDir.canRead()) {
        return null;
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return null;
    }

    WebAppController controller = null;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());
      
      synchronized (_entries) {
        for (int i = 0; i < _entries.size(); i++) {
          controller = _entries.get(i);

          if (appDir.equals(controller.getRootDirectory()))
            return controller;
        }

        controller = new WebAppController(name, name, appDir, _container);

        // XXX: not dynamic-deploy in the sense that the mappings are known
        //controller.setDynamicDeploy(true);
        controller.getVariableMap().putAll(varMap);
        controller.setRegexpValues(vars);
        controller.setConfig(_config);
        // _controller.setJarPath(_archivePath);

        for (int i = 0; i < _webAppDefaults.size(); i++)
          controller.addConfigDefault(_webAppDefaults.get(i));
      
        _entries.add(controller);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    controller.setSourceType("regexp");
    
    //controller.deploy();

    return controller;
  }

  public String toString()
  {
    if (_config == null)
      return "WebAppRegexpDeployGenerator[]";
    else
      return "WebAppRegexpDeployGenerator[" + _config.getURLRegexp().pattern() + "]";
  }
}
