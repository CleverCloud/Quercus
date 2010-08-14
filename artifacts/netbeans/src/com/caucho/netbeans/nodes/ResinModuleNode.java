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

import com.caucho.netbeans.nodes.actions.ModuleBrowseAction;
import com.caucho.netbeans.nodes.actions.ModuleUndeployAction;

import org.netbeans.modules.j2ee.deployment.plugins.api.UISupport;
import org.netbeans.modules.j2ee.deployment.plugins.api.UISupport.ServerIcon;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.actions.SystemAction;

import javax.enterprise.deploy.shared.ModuleType;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Node that represents a J2EE module which is deployed on the server
 */
public final class ResinModuleNode
  extends AbstractNode
{

  private final ResinModule module;
  private final ModuleType moduleType;

  /**
   * Creates a new instance of ResinModuleNode
   */
  public ResinModuleNode(ResinModule module, ModuleType moduleType)
  {
    super(Children.LEAF);
    this.module = module;
    this.moduleType = moduleType;
    getCookieSet().add(module);
  }

  public Image getIcon(int type)
  {
    if (ModuleType.WAR.equals(moduleType)) {
      return UISupport.getIcon(ServerIcon.WAR_ARCHIVE);
    }
    else if (ModuleType.EJB.equals(moduleType)) {
      return UISupport.getIcon(ServerIcon.EJB_ARCHIVE);
    }
    else {
      return UISupport.getIcon(ServerIcon.EAR_ARCHIVE);
    }
  }

  public Image getOpenedIcon(int type)
  {
    return getIcon(type);
  }

  public String getDisplayName()
  {
    return module.getDisplayName();
  }

  public String getShortDescription()
  {
    return module.getShortDescription();
  }

  public Action[] getActions(boolean context)
  {
    List actions = new ArrayList();
    if (module.getWebURL() != null) {
      actions.add(SystemAction.get(ModuleBrowseAction.class));
    }
    actions.add(SystemAction.get(ModuleUndeployAction.class));
    return (SystemAction[]) actions.toArray(new SystemAction[actions.size()]);
  }
}
