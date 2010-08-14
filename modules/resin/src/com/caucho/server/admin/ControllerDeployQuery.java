/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

package com.caucho.server.admin;

import java.io.Serializable;

/**
 * Deploys a controller, i.e. expanding the .git into the filesystem.
 * The controller is in the stopped state, i.e. deployment does not
 * also start the controller.
 *
 * The controller is identified by the tag, which
 * looks like "war/foo.com/my-war" or "ear/foo.com/my-ear".  The first
 * component is the deployment type, the second is the virtual host and
 * the third is the specific name.
 */
public class ControllerDeployQuery implements Serializable {
  private String _tag;

  private ControllerDeployQuery()
  {
  }

  public ControllerDeployQuery(String tag)
  {
    _tag = tag;
  }

  public String getTag()
  {
    return _tag;
  }

  /**
   * Parses the tag type for convenience, "ear", "war", etc
   */
  public String getTagType()
  {
    int p = _tag.indexOf('/');

    return _tag.substring(0, p);
  }

  /**
   * Parses the tag host for convenience
   */
  public String getTagHost()
  {
    int p = _tag.indexOf('/');
    int q = _tag.indexOf('/', p + 1);

    return _tag.substring(p, q);
  }

  /**
   * Parses the tag name for convenience
   */
  public String getTagName()
  {
    int p = _tag.lastIndexOf('/');

    return _tag.substring(p + 1);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _tag + "]";
  }
}
