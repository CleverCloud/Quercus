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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

public interface PortletContext
{
  public String getServerInfo();

  public PortletRequestDispatcher getRequestDispatcher(String path);

  public PortletRequestDispatcher getNamedDispatcher(String name);

  public InputStream getResourceAsStream(String path);

  public int getMajorVersion();

  public int getMinorVersion();

  public String getMimeType(String file);

  public String getRealPath(String path);

  public Set getResourcePaths(String path);

  public URL getResource(String path) throws MalformedURLException;

  public Object getAttribute(String name);

  public Enumeration getAttributeNames();

  public String getInitParameter(String name);

  public Enumeration getInitParameterNames();

  public void log(String msg);

  public void log(String msg, Throwable throwable);

  public void removeAttribute(String name);

  public void setAttribute(String name, Object object);

  public String getPortletContextName();
}
