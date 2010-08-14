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

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

public final class ResinTargetModuleID
  implements TargetModuleID
{

  private final Target _target;
  private final String _moduleID;
  private final String _webURL;
  private final TargetModuleID[] _childTargetModuleID;
  private final String _path;

  private TargetModuleID _parentTargetModuleID;

  public ResinTargetModuleID(Target target,
                             String moduleID,
                             String webURL,
                             TargetModuleID parentTargetModuleID,
                             TargetModuleID[] childTargetModuleID,
                             String path)
  {
    _target = target;
    _moduleID = moduleID;
    _webURL = webURL;
    _parentTargetModuleID = parentTargetModuleID; // XXX: may cause problems?
    _childTargetModuleID = childTargetModuleID;
    _path = path;
  }
  
  public ResinTargetModuleID(ResinTarget target)
  {
    _target = target;
    _moduleID = "test-module";
    _webURL = "/";
    _parentTargetModuleID = null;
    _childTargetModuleID = new TargetModuleID[0];
    _path = "path";
  }

  public Target getTarget()
  {
    return _target;
  }

  public String getModuleID()
  {
    return _moduleID;
  }

  public String getWebURL()
  {
    return _webURL;
  }

  public TargetModuleID getParentTargetModuleID()
  {
    return _parentTargetModuleID;
  }

  public TargetModuleID[] getChildTargetModuleID()
  {
    return _childTargetModuleID;
  }

  public String getPath()
  {
    return _path;
  }

  public String toString()
  {
    return _moduleID;
  }
}
