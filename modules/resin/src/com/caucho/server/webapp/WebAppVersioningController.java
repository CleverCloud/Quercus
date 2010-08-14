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

package com.caucho.server.webapp;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.server.deploy.VersionEntry;

import java.util.*;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A configuration entry for a versioning web-app.
 */
public class WebAppVersioningController extends WebAppController {
  private static final L10N L = new L10N(WebAppVersioningController.class);
  private static final Logger log
    = Logger.getLogger(WebAppController.class.getName());

  private static final long EXPIRE_PERIOD = 3600 * 1000L;

  private long _versionRolloverTime = EXPIRE_PERIOD;

  private ArrayList<WebAppController> _controllerList
    = new ArrayList<WebAppController>();

  private final WebAppExpandDeployGenerator _generator;

  // if the versioning has a controller matching the un-versioned path
  private WebAppController _baseController;
  
  private long _restartTime;
  
  private WebAppController _primaryController;
  private boolean _isModified = true;
  private AtomicBoolean _isUpdating = new AtomicBoolean();

  public WebAppVersioningController(String versionContextPath,
                                    String baseContextPath,
                                    WebAppExpandDeployGenerator generator,
                                    WebAppContainer container)
  {
    super(versionContextPath, baseContextPath, null, container);

    _generator = generator;
  }

  void setModified(boolean isModified)
  {
    _isModified = isModified;
  }

  void setBaseController(WebAppController baseController)
  {
    _baseController = baseController;
  }

  public boolean isVersioning()
  {
    return true;
  }

  @Override
  public String getVersion()
  {
    if (_primaryController != null)
      return _primaryController.getVersion();
    else
      return "";
  }

  /**
   * Returns the instance for a top-level request
   *
   * @return the request object or null for none.
   */
  @Override
  public WebApp instantiateDeployInstance()
  {
    WebAppController controller = _primaryController;

    if (controller != null)
      return controller.getDeployInstance();
    else
      return null;
  }

  /**
   * Starts the entry.
   */
  @Override
  protected WebApp startImpl()
  {
    updateVersionImpl();

    WebAppController controller = _primaryController;

    if (controller != null)
      return controller.request();
    else
      return null;
  }

  /**
   * Initialize the controller.
   */
  @Override
  protected void initBegin()
  {
    /*
    super.initBegin();
    */
  }

  public boolean isModified()
  {
    if (_isModified)
      return true;

    VersionEntry entry = _generator.getVersionEntry(getId());

    if (entry == null)
      return false;


    return false;
  }

  public void updateVersion()
  {
    _isModified = true;

    updateVersionImpl();
  }
  
  private void updateVersionImpl()
  {
    if (! _isUpdating.compareAndSet(false, true))
      return;

    try {
      synchronized (this) {
        WebAppController oldPrimaryController = _primaryController;
        
        WebAppController newPrimaryController = null;
        
        String versionName = _generator.getPrimaryVersion(getId());

        if (versionName != null) {
          newPrimaryController
            = _container.getWebAppGenerator().findController(versionName);
        } else if (_baseController != null
                   && _controllerList.size() == 0) {
          // server/1h52
          newPrimaryController = _baseController;
        }

        if (newPrimaryController == null) {
          throw new ConfigException(L.l(this + " does not have an implementing version"));
        }

        if (newPrimaryController == oldPrimaryController)
          return;

        log.fine(this + " updating primary to " + newPrimaryController);

        if (oldPrimaryController != null
            && oldPrimaryController != newPrimaryController) {
          _controllerList.add(oldPrimaryController);
        }

        newPrimaryController.setVersionAlias(true);
        _primaryController = newPrimaryController;

        _controllerList.remove(newPrimaryController);

        int size = _controllerList.size();
        if (size > 0) {
          WebAppController oldController = _controllerList.get(size - 1);

          long expireTime = Alarm.getCurrentTime() + _versionRolloverTime;

          _primaryController.setOldWebApp(oldController, expireTime);
        }

        _restartTime = Alarm.getCurrentTime();
      }
    } finally {
      _isUpdating.set(false);
    }
  }
  
  /**
   * Returns a printable view.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[" + getId() + "]";
  }

  static class VersionNameComparator implements Comparator<String>
  {
    public int compare(String versionA, String versionB)
    {
      /*
      String versionA = a.getVersion();
      String versionB = b.getVersion();
      */

      int lengthA = versionA.length();
      int lengthB = versionB.length();

      int indexA = 0;
      int indexB = 0;

      while (indexA < lengthA && indexB < lengthB) {
        int valueA = 0;
        int valueB = 0;
        char chA;
        char chB;

        for (;
             indexA < lengthA
               && '0' <= (chA = versionA.charAt(indexA)) && chA <= '9';
             indexA++) {
          valueA = 10 * valueA + chA - '0';
        }

        for (;
             indexB < lengthB
               && '0' <= (chB = versionB.charAt(indexB)) && chB <= '9';
             indexB++) {
          valueB = 10 * valueB + chB - '0';
        }

        if (valueA < valueB)
          return 1;
        else if (valueB < valueA)
          return -1;

        while (indexA < lengthA && indexB < lengthB
               && ! ('0' <= (chA = versionA.charAt(indexA)) && chA <= '9')
               && ! ('0' <= (chB = versionB.charAt(indexB)) && chB <= '9')) {

          if (chA < chB)
            return 1;
          else if (chB < chA)
            return -1;

          indexA++;
          indexB++;
        }

        if (indexA < lengthA
            && ! ('0' <= (chA = versionA.charAt(indexA)) && chA <= '9'))
          return 1;
        else if (indexB < lengthB
                 && ! ('0' <= (chB = versionB.charAt(indexB)) && chB <= '9'))
          return -1;
      }

      if (indexA != lengthA)
        return 1;
      else if (indexB != lengthB)
        return -1;
      else
        return 0;
    }
  }
}
