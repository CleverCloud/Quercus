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
import java.util.Enumeration;
import java.util.ResourceBundle;

public abstract class GenericPortlet implements Portlet, PortletConfig
{
  private transient PortletConfig _config;

  public GenericPortlet()
  {
  }

  public void init(PortletConfig config) throws PortletException
  {
    _config = config;
    init();
  }

  public void init() throws PortletException
  {
  }

  public void processAction(ActionRequest request, ActionResponse response)
    throws PortletException, IOException {
    throw new PortletException("processAction method not implemented");
  }

  public void render(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    response.setTitle(getTitle(request));
    doDispatch(request, response);
  }

  protected String getTitle(RenderRequest request) {
    return _config.getResourceBundle(request.getLocale()).getString("javax.portlet.title");
  }

  protected void doDispatch(RenderRequest request, RenderResponse response)
    throws PortletException,IOException
  {
    WindowState state = request.getWindowState();

    if (!state.equals(WindowState.MINIMIZED)) {
      PortletMode mode = request.getPortletMode();

      if (mode.equals(PortletMode.VIEW)) {
        doView(request, response);
      }
      else if (mode.equals(PortletMode.EDIT)) {
        doEdit(request, response);
      }
      else if (mode.equals(PortletMode.HELP)) {
        doHelp(request, response);
      }
      else {
        throw new PortletException("unknown portlet mode: " + mode);
      }
    }

  }

  protected void doView(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    throw new PortletException("doView method not implemented");
  }

  protected void doEdit(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    throw new PortletException("doEdit method not implemented");
  }

  protected void doHelp(RenderRequest request, RenderResponse response)
    throws PortletException, IOException
  {
    throw new PortletException("doHelp method not implemented");

  }

  public PortletConfig getPortletConfig()
  {
    return _config;
  }

  public void destroy()
  {
  }

  public String getPortletName()
  {
    return _config.getPortletName();
  }

  public PortletContext getPortletContext()
  {
    return _config.getPortletContext();
  }

  public ResourceBundle getResourceBundle(java.util.Locale locale)
  {
    return _config.getResourceBundle(locale);
  }

  public String getInitParameter(String name)
  {
    return _config.getInitParameter(name);
  }

  public Enumeration getInitParameterNames()
  {
    return _config.getInitParameterNames();
  }
}
