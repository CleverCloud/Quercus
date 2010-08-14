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

import com.caucho.netbeans.nodes.actions.ContainerRefreshAction;

import org.netbeans.modules.j2ee.deployment.plugins.api.UISupport;
import org.netbeans.modules.j2ee.deployment.plugins.api.UISupport.ServerIcon;
import org.openide.nodes.AbstractNode;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;

import javax.enterprise.deploy.shared.ModuleType;
import java.awt.*;
import org.openide.util.lookup.Lookups;

/**
 * Node that represents a container, holder of J2EE modules of the same type
 */
public final class ResinModuleContainerNode
  extends AbstractNode
{
  private ModuleType _moduleType;

  /**
   * Creates a new instance of ResinModuleContainerNode
   */
  public ResinModuleContainerNode(Lookup lookup, ModuleType moduleType)
  {
    this(new ResinModuleChildren(lookup, moduleType));
    
    _moduleType = moduleType;
  }

  private ResinModuleContainerNode(ResinModuleChildren resinWebModuleChildren)
  {
    super(resinWebModuleChildren);
    
    getCookieSet().add(resinWebModuleChildren);
  }

  public Image getIcon(int type)
  {
    if (ModuleType.WAR.equals(_moduleType)) {
      return UISupport.getIcon(ServerIcon.WAR_FOLDER);
    }
    else if (ModuleType.EJB.equals(_moduleType)) {
      return UISupport.getIcon(ServerIcon.EJB_FOLDER);
    }
    else {
      return UISupport.getIcon(ServerIcon.EAR_FOLDER);
    }
  }

  public Image getOpenedIcon(int type)
  {
    if (ModuleType.WAR.equals(_moduleType)) {
      return UISupport.getIcon(ServerIcon.WAR_OPENED_FOLDER);
    }
    else if (ModuleType.EJB.equals(_moduleType)) {
      return UISupport.getIcon(ServerIcon.EJB_OPENED_FOLDER);
    }
    else {
      return UISupport.getIcon(ServerIcon.EAR_OPENED_FOLDER);
    }
  }

  public String getDisplayName()
  {
    if (ModuleType.WAR.equals(_moduleType)) {
      return NbBundle.getMessage(ResinModuleContainerNode.class,
                                 "LBL_WebContainer");
    }
    else if (ModuleType.EJB.equals(_moduleType)) {
      return NbBundle.getMessage(ResinModuleContainerNode.class,
                                 "LBL_EJBContainer");
    }
    else {
      return NbBundle.getMessage(ResinModuleContainerNode.class,
                                 "LBL_EARContainer");
    }
  }

  public javax.swing.Action[] getActions(boolean context)
  {
    return new SystemAction[]{SystemAction.get(ContainerRefreshAction.class)};
  }
}
