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
import com.caucho.netbeans.ide.ResinTargetModuleID;
import java.util.logging.Logger;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;

public class SuccessProgressObject implements ProgressObject
{
  private static final Logger log
    = Logger.getLogger(SuccessProgressObject.class.getName());
  
  private Target []_target;
  private TargetModuleID []_targetModuleIDs = new TargetModuleID[0];
  
  public SuccessProgressObject(Target []target)
  {
    _target = target;
    _targetModuleIDs = new TargetModuleID[] {
      new ResinTargetModuleID((ResinTarget) target[0]),
    };
  }

  public SuccessProgressObject()
  {
  }

  SuccessProgressObject(TargetModuleID[] targetModuleIDs)
  {
    _targetModuleIDs = targetModuleIDs;
  }

  public DeploymentStatus getDeploymentStatus()
  {
    return SuccessStatus.SUCCESS;
  }

  public TargetModuleID[] getResultTargetModuleIDs()
  {
    return _targetModuleIDs;
  }

  public ClientConfiguration getClientConfiguration(TargetModuleID arg0)
  {
    return null;
  }

  public boolean isCancelSupported()
  {
    return false;
  }

  public void cancel() throws OperationUnsupportedException
  {
  }

  public boolean isStopSupported()
  {
    return false;
  }

  public void stop() throws OperationUnsupportedException
  {
  }

  public void addProgressListener(ProgressListener arg0)
  {
  }

  public void removeProgressListener(ProgressListener arg0)
  {
  }
}
