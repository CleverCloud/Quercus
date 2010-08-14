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


package com.caucho.netbeans.nodes;

import com.caucho.netbeans.ResinDeploymentManager;
import com.caucho.netbeans.ide.ResinTargetModuleID;

import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.RequestProcessor;

import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;

/**
 * This class represents one J2EE module, UI representation is ResinModuleNode
 */
public final class ResinModule
  implements Node.Cookie, Comparable, ProgressListener
{

  private final ResinDeploymentManager manager;
  private final ResinTargetModuleID tmID;
  private Node node;
  private final String displayName;

  /**
   * Creates a new instance of ResinModule
   */
  public ResinModule(ResinDeploymentManager manager, ResinTargetModuleID tmID)
  {
    this.manager = manager;
    this.tmID = tmID;
    displayName = computeDisplayName();
  }

  public String getDisplayName()
  {
    return displayName;
  }

  public String getShortDescription()
  {
    return tmID.getModuleID();
  }

  public void setRepresentedNode(Node node)
  {
    this.node = node;
  }

  public String getWebURL()
  {
    return tmID.getWebURL();
  }

  public void undeploy()
  {
    RequestProcessor.getDefault().post(new Runnable()
    {
      public void run()
      {
        // TODO: Use the Progress API to track the undeploy progress
        ProgressObject po = manager.undeploy(new TargetModuleID[]{tmID});
        po.addProgressListener(ResinModule.this);
        DeploymentStatus deploymentStatus = po.getDeploymentStatus();
        // TODO make sure handleDeploymentStatus won't be called twice
        if (deploymentStatus.isCompleted() || deploymentStatus.isFailed()) {
          handleDeploymentStatus(deploymentStatus);
        }
      }
    });
  }

  private String computeDisplayName()
  {
    String result = tmID.getModuleID();
    int slashIdx = result.lastIndexOf('/');
    if (slashIdx == result.length() - 1) {
      // module is deployed as directory, get the second rightmost slash
      result = result.substring(0, result.length() - 1);
      slashIdx = result.lastIndexOf('/');
    }
    // cut the extension if deployed as archive
    int dotIdx = result.lastIndexOf('.');
    int endIdx = (dotIdx > -1 && dotIdx > slashIdx) ? dotIdx : result.length();
    return result.substring(slashIdx + 1, endIdx);
  }

  public void handleProgressEvent(ProgressEvent progressEvent)
  {
    handleDeploymentStatus(progressEvent.getDeploymentStatus());
  }

  private void handleDeploymentStatus(DeploymentStatus deployStatus)
  {
    if (deployStatus.getState() == StateType.COMPLETED) {
      CommandType command = deployStatus.getCommand();
      if (command == CommandType.START || command == CommandType.STOP) {
        StatusDisplayer.getDefault().setStatusText(deployStatus.getMessage());
      }
      else if (command == CommandType.UNDEPLOY) {
        Node parent = node.getParentNode();
        if (parent != null) {
          Children children = parent.getChildren();
          if (children instanceof ResinModuleChildren) {
            ((ResinModuleChildren) children).updateKeys();
            StatusDisplayer.getDefault()
              .setStatusText(deployStatus.getMessage());
          }
        }
      }
    }
    else if (deployStatus.getState() == StateType.FAILED) {
      NotifyDescriptor notDesc
        = new NotifyDescriptor.Message(deployStatus.getMessage(),
                                       NotifyDescriptor.ERROR_MESSAGE);
      DialogDisplayer.getDefault().notify(notDesc);
      StatusDisplayer.getDefault().setStatusText(deployStatus.getMessage());
    }
  }

  public int compareTo(Object o)
  {
    ResinModule other = (ResinModule) o;
    int res = getDisplayName().compareToIgnoreCase(other.getDisplayName());
    if (res == 0) {
      res
        = getShortDescription().compareToIgnoreCase(other.getShortDescription());
    }
    return res;
  }
}
