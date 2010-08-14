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

package com.caucho.jca.ra;

import javax.enterprise.inject.spi.Bean;
import javax.resource.spi.ResourceAdapter;

import com.caucho.config.inject.InjectManager;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.StartLifecycleException;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;

/**
 * Controller for a resource-adapter
 */
public class ResourceAdapterController implements EnvironmentListener
{
  private InjectManager _beanManager;
  private final Bean<ResourceAdapter> _comp;
  private final ResourceArchive _raConfig;

  private Lifecycle _lifecycle = new Lifecycle();
  private ResourceAdapter _ra;

  public ResourceAdapterController(Bean<ResourceAdapter> comp,
                                   ResourceArchive raConfig)
  {
    _beanManager = InjectManager.create();

    _comp = comp;
    _raConfig = raConfig;

    Environment.addEnvironmentListener(this);
  }

  public ResourceAdapter getResourceAdapter()
  {
    start();

    return _ra;
  }

  /**
   * Starts the resource adapter
   */
  private void start()
  {
    if (! _lifecycle.toActive())
      return;
    
    if (_ra != null)
      return;

    _ra = (ResourceAdapter) _beanManager.getReference(_comp);

    try {
      _ra.start(ResourceManagerImpl.create());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new StartLifecycleException(e);
    }
  }

  /**
   * Stops the resource adapter
   */
  private void stop()
  {
    if (! _lifecycle.toStop())
      return;

    try {
      ResourceAdapter ra = _ra;

      if (ra != null)
        ra.stop();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new StartLifecycleException(e);
    }
  }

  /**
   * Handles the environment config phase.
   */
  @Override
  public void environmentConfigure(EnvironmentClassLoader loader)
    throws StartLifecycleException
  {
  }

  /**
   * Handles the environment bind phase.
   */
  @Override
  public void environmentBind(EnvironmentClassLoader loader)
    throws StartLifecycleException
  {
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  @Override
  public void environmentStart(EnvironmentClassLoader loader)
    throws StartLifecycleException
  {
    start();
  }


  /**
   * Handles the case where the environment is stopping
   */
  @Override
  public void environmentStop(EnvironmentClassLoader loader)
  {
    stop();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _comp + "]";
  }
}
