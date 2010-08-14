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
import java.util.Locale;
import java.util.Map;

public interface PortletRequest
{
  public static final String USER_INFO = "javax.portlet.userinfo";
  public static final String BASIC_AUTH = "BASIC";
  public static final String FORM_AUTH = "FORM";
  public static final String CLIENT_CERT_AUTH = "CLIENT_CERT";
  public static final String DIGEST_AUTH = "DIGEST";

  public boolean isWindowStateAllowed(WindowState state);

  public boolean isPortletModeAllowed(PortletMode mode);

  public PortletMode getPortletMode();

  public WindowState getWindowState();

  public PortletPreferences getPreferences();

  public PortletSession getPortletSession();

  public PortletSession getPortletSession(boolean create);

  public String getProperty(String name);

  public Enumeration getProperties(String name);

  public Enumeration getPropertyNames();

  public PortalContext getPortalContext();

  public String getAuthType();

  public String getContextPath();

  public String getRemoteUser();

  public java.security.Principal getUserPrincipal();

  public boolean isUserInRole(String role);

  public Object getAttribute(String name);

  public Enumeration getAttributeNames();

  public String getParameter(String name);

  public Enumeration getParameterNames();

  public String[] getParameterValues(String name);

  public Map getParameterMap();

  public boolean isSecure();

  public void setAttribute(String name, Object o);

  public void removeAttribute(String name);

  public String getRequestedSessionId();

  public boolean isRequestedSessionIdValid();

  public String getResponseContentType();

  public Enumeration getResponseContentTypes();

  public Locale getLocale();

  public Enumeration getLocales();

  public String getScheme();

  public String getServerName();

  public int getServerPort();

}
