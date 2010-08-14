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
 * @author Sam
 */

package com.caucho.netbeans;

import com.caucho.netbeans.ide.ResinTarget;

import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;

import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.shared.DConfigBeanVersionType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DConfigBeanVersionUnsupportedException;
import javax.enterprise.deploy.spi.exceptions.InvalidModuleException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.logging.*;

public final class ResinDeploymentManager
  implements DeploymentManager
{
  private static final Logger log
    = Logger.getLogger(ResinDeploymentManager.class.getName());
  
  private static final PluginL10N L = new PluginL10N(ResinDeploymentManager.class);

  private final String _uri;
  private final InstanceProperties _ip;
  private final ResinConfiguration _resinConfiguration;
  private ResinProcess _resinProcess;
  private TargetModuleID []_runningModules = new TargetModuleID[0];

  private ResinPlatformImpl _j2eePlatform;

    
  public ResinDeploymentManager(String uri, InstanceProperties ip) 
    throws DeploymentManagerCreationException
  {
    _uri = uri;
    _ip = ip;
    
    // XXX: what is connected for?
    _resinConfiguration = new ResinConfiguration(ip);
    _resinProcess = new ResinProcess(_uri, _resinConfiguration);
    
  }

  public ResinConfiguration getResinConfiguration()
  {
    return _resinConfiguration;
  }

  public ResinProcess getResinProcess()
  {
    if (_resinProcess == null) {
      _resinProcess = new ResinProcess(_uri, _resinConfiguration);
      _resinProcess.init();
    }
    return _resinProcess;
  }

  public Target[] getTargets()
    throws IllegalStateException
  {
    return new ResinTarget[] {
      new ResinTarget(_uri, _resinConfiguration),
    };
  }

  public TargetModuleID[] getRunningModules(ModuleType moduleType,
                                            Target[] target)
    throws TargetException, IllegalStateException
  {
    return _runningModules;
  }

  public TargetModuleID[] getNonRunningModules(ModuleType moduleType,
                                               Target[] target)
    throws TargetException, IllegalStateException
  {
    return new TargetModuleID[0];
  }

  public TargetModuleID[] getAvailableModules(ModuleType moduleType,
                                              Target[] target)
    throws TargetException, IllegalStateException
  {
    return new TargetModuleID[0];
  }

  public DeploymentConfiguration createConfiguration(DeployableObject deployableObject)
    throws InvalidModuleException
  {
    return null;
    /*
    ModuleType type = deployableObject.getType();

    if (type == ModuleType.WAR)
      throw new UnsupportedOperationException("XXX: unimplemented");
    else if (type == ModuleType.EAR)
      throw new UnsupportedOperationException("XXX: unimplemented");
    else if (type == ModuleType.EJB)
      throw new UnsupportedOperationException("XXX: unimplemented");
    else {
      throw new InvalidModuleException(L.l("Unsupported module type ''{0}''", type));
    }
    */
  }

  public ProgressObject distribute(Target[] target, 
                                   File archive,
                                   File plan)
    throws IllegalStateException
  {
    try {
      String urlString = "http://localhost:" + _resinConfiguration.getPort()
        + "/resin:local-deploy/deploy?action=add-web-app"
        + "&context-path=" + _resinConfiguration.getContextPath()
        + "&war=" + archive.getAbsolutePath();
      
      if (plan != null)
        urlString += "&resin-web=" + plan.getAbsolutePath();
    
      log.fine("Dist: " + urlString);
    
      URL url = new URL(urlString);
      
      StringBuilder sb = new StringBuilder();
      InputStream is = url.openStream();
      int ch;
      while ((ch = is.read()) >= 0) {
        sb.append((char) ch);
      }
      
      is.close();
      
      log.fine("Complete: " + sb);
    
      return new SuccessProgressObject(target);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ProgressObject distribute(Target[] target,
                                   InputStream archive,
                                   InputStream plan)
    throws IllegalStateException
  {
    return new SuccessProgressObject(target);
  }

  public ProgressObject distribute(Target[] target,
				   ModuleType type,
                                   InputStream archive,
                                   InputStream plan)
    throws IllegalStateException
  {
    return null;
  }

  public ProgressObject start(TargetModuleID[] targetModuleIDs)
    throws IllegalStateException
  {
    _runningModules = targetModuleIDs;
    
    return new SuccessProgressObject(targetModuleIDs);
  }

  public ProgressObject stop(TargetModuleID[] targetModuleIDs)
    throws IllegalStateException
  {
    _runningModules = new TargetModuleID[0];
    
    return new SuccessProgressObject();
  }

  public ProgressObject undeploy(TargetModuleID[] targetModuleIDs)
    throws IllegalStateException
  {
    return new SuccessProgressObject();
  }

  public boolean isRedeploySupported()
  {
    return false;
  }

  @Override
  public ProgressObject redeploy(TargetModuleID[] targetModuleID,
                                 File archive,
                                 File plan)
  {
    return null;
  }

  @Override
  public ProgressObject redeploy(TargetModuleID[] targetModuleID,
                                 InputStream archive,
                                 InputStream plan)
  {
    return null;
  }

  public void release()
  {
  }

  public Locale getDefaultLocale()
  {
    return null;
  }

  public Locale getCurrentLocale()
  {
    return null;
  }

  public void setLocale(Locale locale)
    throws UnsupportedOperationException
  {
  }

  public Locale[] getSupportedLocales()
  {
    return null;
  }

  public boolean isLocaleSupported(Locale locale)
  {
    return false;
  }

  public DConfigBeanVersionType getDConfigBeanVersion()
  {
    return null;
  }

  public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType dConfigBeanVersionType)
  {
    return false;
  }

  public void setDConfigBeanVersionSupported(DConfigBeanVersionType version)
    throws DConfigBeanVersionUnsupportedException
  {
  }

  public void setDConfigBeanVersion(DConfigBeanVersionType dConfigBeanVersionType)
    throws DConfigBeanVersionUnsupportedException
  {
  }

  public ResinPlatformImpl getJ2eePlatform()
  {
    /*
    if (_j2eePlatform == null)
      _j2eePlatform = new ResinPlatformImpl(_resinConfiguration);

    return _j2eePlatform;
    */
    return null;
  }

  public String getUri()
  {
    return _uri;
  }
}
