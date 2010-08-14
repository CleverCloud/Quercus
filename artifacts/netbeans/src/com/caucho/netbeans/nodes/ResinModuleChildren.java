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

import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Mode children
 */
public class ResinModuleChildren
  extends Children.Keys
  implements Node.Cookie
{

  private static final String WAIT_NODE = "wait_node"; // NOI18N
  private final Lookup lookup;
  private final ModuleType moduleType;

  ResinModuleChildren(Lookup lookup, ModuleType moduleType)
  {
    this.lookup = lookup;
    this.moduleType = moduleType;
  }

  public void updateKeys()
  {
    ArrayList ts = new ArrayList();
    ts.add(WAIT_NODE);
    setKeys(ts);
    RequestProcessor.getDefault().post(new Runnable()
    {
      public void run()
      {
        ResinDeploymentManager manager = (ResinDeploymentManager) lookup.lookup(
          ResinDeploymentManager.class);
        Target target = (Target) lookup.lookup(Target.class);
        ArrayList<ResinModule> list = new ArrayList();
        if (manager != null && target != null) {
          // TODO: add a check whether the server is not in suspended state
          try {
            TargetModuleID[] modules = manager.getRunningModules(moduleType,
                                                                 new Target[]{
                                                                   target});
            for (TargetModuleID tmID : modules) {
              list.add(new ResinModule(manager, (ResinTargetModuleID) tmID));
            }
            Collections.sort(list);
          }
          catch (TargetException e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
          }
          catch (IllegalStateException e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
          }
        }
        setKeys(list);
      }
    });
  }

  protected void addNotify()
  {
    updateKeys();
  }

  protected void removeNotify()
  {
    setKeys(java.util.Collections.EMPTY_SET);
  }

  protected org.openide.nodes.Node[] createNodes(Object key)
  {
    if (key instanceof ResinModule) {
      ResinModule module = (ResinModule) key;
      ResinModuleNode node = new ResinModuleNode(module, moduleType);
      module.setRepresentedNode(node);
      return new Node[]{node};
    }
    else if (key instanceof String && key.equals(WAIT_NODE)) {
      return new Node[]{createWaitNode()};
    }
    return null;
  }

  private Node createWaitNode()
  {
    AbstractNode n = new AbstractNode(Children.LEAF);
    n.setName(NbBundle.getMessage(ResinModuleChildren.class,
                                  "LBL_WaitNode_DisplayName"));
    n.setIconBaseWithExtension("org/openide/src/resources/wait.gif"); // NOI18N
    return n;
  }
}
