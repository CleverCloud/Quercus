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
 * @author Sam
 */

package com.caucho.config.types;

import com.caucho.vfs.*;

/**
 * Configuration for the ejb-local-ref
 */
public class EjbLocalRef
  extends EjbRef
{
  private Class<?> _local;

  public EjbLocalRef()
  {
  }

  public EjbLocalRef(Path path)
  {
    super(path);
  }

  public EjbLocalRef(Path path, String sourceEjbName)
  {
    super(path, sourceEjbName);
  }

  public boolean isEjbLocalRef()
  {
    return true;
  }

  protected String getTagName()
  {
    return "<ejb-local-ref>";
  }

  public void setLocalHome(Class<?> home)
  {
    // XXX: should distinguish
    setHome(home);
  }

  @Override
  public Class<?> getLocal()
  {
    return _local;
  }

  public void setLocal(Class<?> local)
  {
    //setRemote(local);

    _local = local;
  }

  /**
   * Merges duplicated information in application-client.xml / resin-application-client.xml
   */
  public void mergeFrom(EjbRef otherRef)
  {
    super.mergeFrom(otherRef);

    EjbLocalRef other = (EjbLocalRef) otherRef;

    if (_local == null)
      _local = other._local;
  }
}
