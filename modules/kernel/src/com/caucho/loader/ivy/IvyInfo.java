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

package com.caucho.loader.ivy;

import java.util.*;
import java.util.logging.*;

import javax.annotation.*;

import com.caucho.config.program.ConfigProgram;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * IvyInfo configuration
 */
public class IvyInfo {
  private IvyManager _manager;
  
  private String _organisation;
  private String _module;
  private String _revision;

  private String _status = "integration";
  private String _publication;
  private String _branch;

  IvyInfo(IvyManager manager)
  {
    _manager = manager;
  }

  public void setOrganisation(String org)
  {
    _organisation = org;
  }

  public String getOrganisation()
  {
    return _organisation;
  }

  public void setModule(String module)
  {
    _module = module;
  }

  public String getModule()
  {
    return _module;
  }

  public void setRevision(String rev)
  {
    _revision = rev;
  }

  public String getRevision()
  {
    return _revision;
  }

  public void setStatus(String status)
  {
    _status = status;
  }

  public String getStatus()
  {
    return _status;
  }

  /**
   * Ignore unknown properties
   */
  public void addBuilderProgram(ConfigProgram program)
  {
  }

  public boolean isMatch(String org, String module)
  {
    return _organisation.equals(org) && _module.equals(module);
  }

  @Override
  public int hashCode()
  {
    int hash = 37;

    hash = hash * 65521 + _organisation.hashCode();
    hash = hash * 65521 + _module.hashCode();
    hash = hash * 65521 + _revision.hashCode();

    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof IvyInfo))
      return false;

    IvyInfo info = (IvyInfo) o;

    return (_organisation.equals(info._organisation)
            && _module.equals(info._module)
            && _revision.equals(info._revision));
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "["
            + _organisation
            + "," + _module
            + "," + _revision
            + "," + _status + "]");
  }
}
