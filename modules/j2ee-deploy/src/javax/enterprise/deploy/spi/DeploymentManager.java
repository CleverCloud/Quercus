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

package javax.enterprise.deploy.spi;

import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.shared.DConfigBeanVersionType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.exceptions.DConfigBeanVersionUnsupportedException;
import javax.enterprise.deploy.spi.exceptions.InvalidModuleException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Manager for the deployments.
 */
public interface DeploymentManager {
  /**
   * Returns the targets supported by the manager.
   */
  public Target []getTargets()
    throws IllegalStateException;
  
  /**
   * Returns the current running modules.
   */
  public TargetModuleID []getRunningModules(ModuleType moduleType,
                                            Target []targetList)
    throws TargetException, IllegalStateException;
  
  /**
   * Returns the current non-running modules.
   */
  public TargetModuleID []getNonRunningModules(ModuleType moduleType,
                                               Target []targetList)
    throws TargetException, IllegalStateException;
  
  /**
   * Returns all available modules.
   */
  public TargetModuleID []getAvailableModules(ModuleType moduleType,
                                              Target []targetList)
    throws TargetException, IllegalStateException, IOException;
  
  /**
   * Returns a configuration for the deployable object.
   */
  public DeploymentConfiguration createConfiguration(DeployableObject dObj)
    throws InvalidModuleException;
  
  /**
   * Deploys the object.
   */
  public ProgressObject distribute(Target []targetList,
                                   File archive,
                                   File deploymentPlan)
    throws IllegalStateException;
  
  /**
   * Deploys the object.
   * @Deprecated
   */
  public ProgressObject distribute(Target []targetList,
                                   InputStream archive,
                                   InputStream deploymentPlan)
    throws IllegalStateException;
  
  /**
   * Deploys the object.
   */
  public ProgressObject distribute(Target []targetList,
                                   ModuleType type,
                                   InputStream archive,
                                   InputStream deploymentPlan)
    throws IllegalStateException;
  
  /**
   * Starts the modules.
   */
  public ProgressObject start(TargetModuleID []moduleIDList)
    throws IllegalStateException;
  
  /**
   * Stops the modules.
   */
  public ProgressObject stop(TargetModuleID []moduleIDList)
    throws IllegalStateException;
  
  /**
   * Undeploys the modules.
   */
  public ProgressObject undeploy(TargetModuleID []moduleIDList)
    throws IllegalStateException;

  /**
   * Returns true if the redeploy is supported.
   */
  public boolean isRedeploySupported();
  
  /**
   * Redeploys the object.
   */
  public ProgressObject redeploy(TargetModuleID []targetList,
                                 File archive,
                                 File deploymentPlan)
    throws IllegalStateException;
  
  /**
   * Redeploys the object.
   */
  public ProgressObject redeploy(TargetModuleID []targetList,
                                 InputStream archive,
                                 InputStream deploymentPlan)
    throws IllegalStateException;

  /**
   * Frees any resources.
   */
  public void release();

  /**
   * Returns the default locale.
   */
  public Locale getDefaultLocale();

  /**
   * Returns the current locale.
   */
  public Locale getCurrentLocale();

  /**
   * Sets the default locale.
   */
  public void setLocale(Locale locale);

  /**
   * Returns the supported locales.
   */
  public Locale []getSupportedLocales();

  /**
   * Returns true if the locale is supported.
   */
  public boolean isLocaleSupported(Locale locale);

  /**
   * Returns the bean's J2EE version.
   */
  public DConfigBeanVersionType getDConfigBeanVersion();

  /**
   * Returns true if the given version is supported.
   */
  public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType version);

  /**
   * Sets true if the given version is supported.
   */
  public void setDConfigBeanVersionSupported(DConfigBeanVersionType version)
    throws DConfigBeanVersionUnsupportedException;
}

