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


package javax.portlet;

import java.util.Enumeration;

public interface PortletSession
{
  public static final int APPLICATION_SCOPE = 0x01;
  public static final int PORTLET_SCOPE = 0x02;

  public Object getAttribute(String name);

  public Object getAttribute(String name,int scope);

  public Enumeration getAttributeNames();

  public Enumeration getAttributeNames(int scope);

  public long getCreationTime();

  public String getId();

  public long getLastAccessedTime();

  public int getMaxInactiveInterval();

  public void invalidate();

  public boolean isNew();

  public void removeAttribute(String name) ;

  public void removeAttribute(String name, int scope) ;

  public void setAttribute(String name, Object value);

  public void setAttribute(String name, Object value, int scope);

  public void setMaxInactiveInterval(int interval);

  public PortletContext getPortletContext();

}

