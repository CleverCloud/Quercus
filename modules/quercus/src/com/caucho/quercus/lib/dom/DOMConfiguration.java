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

package com.caucho.quercus.lib.dom;

public class DOMConfiguration
  extends DOMWrapper<org.w3c.dom.DOMConfiguration>
{
  DOMConfiguration(
      DOMImplementation impl, org.w3c.dom.DOMConfiguration delegate)
  {
    super(impl, delegate);
  }

  public boolean canSetParameter(String name, Object value)
  {
    return _delegate.canSetParameter(name, value);
  }

  public Object getParameter(String name)
    throws DOMException
  {
    try {
      return wrap(_delegate.getParameter(name));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMStringList getParameterNames()
  {
    return wrap(_delegate.getParameterNames());
  }

  public void setParameter(String name, Object value)
    throws DOMException
  {
    try {
      _delegate.setParameter(name, value);
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }
}
