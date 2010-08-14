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

import java.io.IOException;
import java.util.Map;

/**
 * The <code>ActionResponse</code> is the request interface available to the
 * portlet in the portlet's preocessAction method.
 * 
 * @see ActionRequest
 * @see PortletResponse
 */
public interface ActionResponse extends PortletResponse
{
  public void setWindowState(WindowState state) 
    throws WindowStateException;

  public void setPortletMode(PortletMode portletMode)
    throws PortletModeException;

  /**
   * Send a response to the client that redirects the client to another
   * location. 
   *
   * <code>location</code> must be an absolute URL ("http://myserver/...")  or
   * a URI with a full path ("/myapp/mypath").  This method may encode the URL
   * before the redirect is sent to the client.
   *
   * <code>path</code> may also be a relative path ("images/myimage.gif"), in
   * which case it is a url to a resource in the current "portal", typically a
   * path relative to the current webapp.  Allowing a relative path is an
   * extension of the behaviour defined by the portlet specification.
   *
   * An IllegalStateException is thrown if any of the following methods have 
   * been called:
   *
   * <ul>
   * <li>setPortletMode 
   * <li>setWindowState 
   * <li>setRenderParameter 
   * <li>setRenderParameters 
   * </ul>
   *
   * @see javax.portlet.ActionResponse#sendRedirect 
   */ 
  public void sendRedirect(String location)
    throws IOException; 

  public void setRenderParameters(Map parameters);

  public void setRenderParameter(String key, String value);

  public void setRenderParameter(String key, String[] values);
}


