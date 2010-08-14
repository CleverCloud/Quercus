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


package com.caucho.netbeans.ide;

import com.caucho.netbeans.ResinDeploymentManager;

import org.netbeans.modules.j2ee.deployment.plugins.spi.TargetModuleIDResolver;
import org.openide.ErrorManager;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import java.util.ArrayList;
import java.util.Map;


public final class ResinTargetModuleIDResolver
  extends TargetModuleIDResolver
{
  private final ResinDeploymentManager _manager;

  public ResinTargetModuleIDResolver(DeploymentManager manager)
  {
    this._manager = (ResinDeploymentManager) manager;
  }

  public TargetModuleID[] lookupTargetModuleID(Map targetModuleInfo,
                                               Target[] targets)
  {
    String contextRoot = (String) targetModuleInfo.get(KEY_CONTEXT_ROOT);

    if (contextRoot == null)
      return EMPTY_TMID_ARRAY;

    if ("".equals(contextRoot)) {
      contextRoot = "/"; // NOI18N
    }
    else {
      contextRoot = contextRoot.substring(1);
    }

    ArrayList<TargetModuleID> result = new ArrayList<TargetModuleID>();

    try {
      TargetModuleID[] targetModuleIDs
        = _manager.getAvailableModules(ModuleType.WAR, targets);

      for (TargetModuleID targetModuleID1 : targetModuleIDs) {

        ResinTargetModuleID targetModuleID
          = (ResinTargetModuleID) targetModuleID1;

        if (contextRoot.equals(targetModuleID.getPath())) {
          result.add(targetModuleID);
        }
      }
    }
    catch (Exception ex) {
      ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
    }

    return result.toArray(new TargetModuleID[result.size()]);
  }
}
