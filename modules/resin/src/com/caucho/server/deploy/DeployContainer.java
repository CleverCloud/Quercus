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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.make.CachedDependency;
import com.caucho.vfs.Dependency;

/**
 * A container of deploy objects.
 */
public class DeployContainer<C extends DeployController<?>>
  extends CachedDependency
  implements Dependency
{
  private final DeployListGenerator<C> _deployListGenerator
    = new DeployListGenerator<C>(this);

  private final ArrayList<C> _controllerList = new ArrayList<C>();

  private final Lifecycle _lifecycle = new Lifecycle();

  /**
   * Creates the deploy container.
   */
  public DeployContainer()
  {
    setCheckInterval(Environment.getDependencyCheckInterval());
  }
  
  /**
   * Adds a deploy generator.
   */
  public void add(DeployGenerator<C> generator)
  {
    Set<String> names = new TreeSet<String>();
    generator.fillDeployedKeys(names);

    _deployListGenerator.add(generator);

    if (_lifecycle.isActive())
      update(names);
  }
  
  /**
   * Removes a deploy.
   */
  public void remove(DeployGenerator<C> generator)
  {
    Set<String> names = new TreeSet<String>();
    generator.fillDeployedKeys(names);

    _deployListGenerator.remove(generator);

    if (_lifecycle.isActive())
      update(names);
  }

  /**
   * Returns true if the deployment has modified.
   */
  public boolean isModifiedImpl()
  {
    return _deployListGenerator.isModified();
  }

  /**
   * Logs the reason for modification.
   */
  public boolean logModified(Logger log)
  {
    return _deployListGenerator.logModified(log);
  }

  /**
   * Forces updates.
   */
  public void update()
  {
    _deployListGenerator.update();
  }

  /**
   * Initialize the container.
   */
  @PostConstruct
  public void init()
  {
    if (! _lifecycle.toInit())
      return;
  }

  /**
   * Start the container.
   */
  public void start()
  {
    init();

    if (! _lifecycle.toActive())
      return;

    _deployListGenerator.start();

    HashSet<String> keys = new LinkedHashSet<String>();

    _deployListGenerator.fillDeployedKeys(keys);
    for (String key : keys) {
      startImpl(key);
    }

    ArrayList<C> controllerList;

    synchronized (_controllerList) {
      controllerList = new ArrayList<C>(_controllerList);

      Collections.sort(controllerList, new StartupPriorityComparator());
    }

    for (int i = 0; i < controllerList.size(); i++) {
      C controller = controllerList.get(i);

      controller.startOnInit();
    }
  }

  /**
   * Returns the matching entry.
   */
  public C findController(String name)
  {
    C controller = findDeployedController(name);

    if (controller != null)
      return controller;

    controller = generateController(name);

    if (controller == null)
      return null;
    // server/10tm
    else if (controller.isNameMatch(name)) {
      return controller;
    }
    else {
      return null;
    }
  }

  /**
   * Returns the deployed entries.
   */
  public ArrayList<C> getControllers()
  {
    ArrayList<C> list = new ArrayList<C>();

    synchronized (_controllerList) {
      list.addAll(_controllerList);
    }

    return list;
  }

  /**
   * Updates all the names.
   */
  private void update(Set<String> names)
  {
    Iterator<String> iter = names.iterator();
    while (iter.hasNext()) {
      String name = iter.next();

      update(name);
    }
  }

  /**
   * Callback from the DeployGenerator when the deployment changes.
   * <code>update</code> is only called when a deployment is added
   * or removed, e.g. with a new .war file.
   *
   * The entry handles its own internal changes, e.g. a modification to
   * a web.xml file.
   */
  public C update(String name)
  {
    C newController = updateImpl(name);
    
    if (_lifecycle.isActive() && newController != null)
      newController.startOnInit();

    return newController;
  }

  /**
   * Callback from the DeployGenerator when the deployment changes.
   * <code>update</code> is only called when a deployment is added
   * or removed, e.g. with a new .war file.
   *
   * The entry handles its own internal changes, e.g. a modification to
   * a web.xml file.
   */
  public C updateNoStart(String name)
  {
    C newController = updateImpl(name);

    return newController;
  }

  /**
   * Callback from the DeployGenerator when the deployment changes.
   * <code>update</code> is only called when a deployment is added
   * or removed, e.g. with a new .war file.
   *
   * The entry handles its own internal changes, e.g. a modification to
   * a web.xml file.
   */
  C updateImpl(String name)
  {
    C oldController = null;

    synchronized (_controllerList) {
      oldController = findDeployedController(name);

      if (oldController != null)
        _controllerList.remove(oldController);
    }
    
    if (oldController != null)
      oldController.destroy();

    // destroy must be before generate because of JMX unregistration
      
    C newController = generateController(name);

    return newController;
  }

  /**
   * Starts a particular controller.
   *
   * @param name the domain-specified name matching the controller, e.g. the
   *  hostname or the context-path.
   */
  private C startImpl(String name)
  {
    C oldController = null;

    synchronized (_controllerList) {
      oldController = findDeployedController(name);
    }

    if (oldController != null)
      return oldController;
      
    return generateController(name);
  }

  /**
   * Called to explicitly remove an entry from the cache.
   */
  public void remove(String name)
  {
    C oldController = null;

    synchronized (_controllerList) {
      oldController = findDeployedController(name);

      if (oldController != null)
        _controllerList.remove(oldController);
    }

    if (oldController != null)
      oldController.destroy();
  }

  /**
   * Generates the controller.
   */
  private C generateController(String name)
  {
    // XXX: required for watchdog
    /*
    if (! _lifecycle.isActive())
      return null;
    */
    
    C newController = _deployListGenerator.generateController(name);

    // server/1h00,13g4
    // generated controller might match the name, e.g.
    // when webapps deploy has an overriding explicit <web-app>
    if (newController == null)
      return null;

    // the new entry might already be generated by another thread
    synchronized (_controllerList) {
      C controller = findDeployedController(newController.getId());
      
      if (controller != null) {
        if (controller.isVersioning())
          controller.updateVersion();

        return controller;
      }

      _controllerList.add(newController);
    }
    
    init(newController);

    return newController;
  }
  

  private void init(C controller)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(controller.getParentClassLoader());

      controller.init();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns an already deployed entry.
   */
  private C findDeployedController(String name)
  {
    synchronized (_controllerList) {
      for (int i = 0; i < _controllerList.size(); i++) {
        C controller = _controllerList.get(i);

        if (controller.isNameMatch(name)) {
          return controller;
        }
      }
    }

    return null;
  }
  
  /**
   * Closes the stop.
   */
  public void stop()
  {
    if (! _lifecycle.toStop())
      return;

    ArrayList<C> controllers;

    synchronized (_controllerList) {
      controllers = new ArrayList<C>(_controllerList);

      Collections.sort(controllers, new StartupPriorityComparator());
    }

    for (int i = controllers.size() - 1; i >= 0; i--)
      controllers.get(i).stop();
  }
  
  /**
   * Closes the deploys.
   */
  public void destroy()
  {
    stop();
    
    if (! _lifecycle.toDestroy())
      return;
    
    _deployListGenerator.destroy();

    ArrayList<C> controllerList;

    synchronized (_controllerList) {
      controllerList = new ArrayList<C>(_controllerList);
      _controllerList.clear();

      Collections.sort(controllerList, new StartupPriorityComparator());
    }

    for (int i = controllerList.size() - 1; i >= 0; i--) {
      C controller = controllerList.get(i);

      controller.destroy();
    }
  }

  public String toString()
  {
    return "DeployContainer$" + System.identityHashCode(this) + "[]";
  }

  public class StartupPriorityComparator
    implements Comparator<C>
  {
    public int compare(C a, C b)
    {
      if (a.getStartupPriority() == b.getStartupPriority())
        return 0;
      else if (a.getStartupPriority() < b.getStartupPriority())
        return -1;
      else
        return 1;
    }
  }
}
