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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.j2ee.deployclient;

import javax.enterprise.deploy.spi.Target;

/**
 * Target for a deployment.
 */
public class TargetImpl
  implements Target, Comparable<TargetImpl>, java.io.Serializable
{
  private String _name;
  private String _description;

  private String _clientRefs;

  /**
   * Null constructor.
   */
  public TargetImpl()
  {
  }

  /**
   * Constructor
   */
  public TargetImpl(String name, String description)
  {
    _name = name;
    _description = description;
  }

  /**
   * Returns the target name.
   */
  public String getName()
  {
    return _name;
  }

  public String getClientRefs()
  {
    return _clientRefs;
  }

  public void setClientRefs(String refs)
  {
    _clientRefs = refs;
  }

  /**
   * Sorts according to name.
   */
  public int compareTo(TargetImpl o)
  {
    if (o == null)
      return 1;

    if (_name == null)
      return o._name == null ? 0 : 1;

    if (o._name == null)
      return 1;

    return _name.compareTo(o._name);
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof TargetImpl))
      return false;

    TargetImpl target = (TargetImpl) o;

    return _name.equals(target.getName());
  }

  /**
   * Returns the target description.
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * Prints the string.
   */
  public String toString()
  {
    return "TargetImpl[" + _name + "]";
  }

}

