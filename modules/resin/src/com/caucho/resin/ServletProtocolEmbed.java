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

package com.caucho.resin;

import com.caucho.config.program.ContainerProgram;
import com.caucho.config.program.PropertyValueProgram;
import com.caucho.server.dispatch.ServletProtocolConfig;

/**
 * Embeddable version of a servlet remoting protocol like hessian or burlap.
 * The service class can be any Java class.  Typically the class will have an
 * interface marked with a @javax.ejb.Remote annotation.
 *
 * <code><pre>
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/www/foo");
 *
 * ServletMappingEmbed myService
 *   = new ServletMappingEmbed("my-service", "/service", "qa.Myservice");
 *
 * myService.setProtocol(new ServletProtocolEmbed("hessian"));
 *
 * webApp.addServletMapping(myService);
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class ServletProtocolEmbed
{
  private String _uri;
  private ContainerProgram _init = new ContainerProgram();

  /**
   * Creates a new embedded servlet protocol
   */
  public ServletProtocolEmbed()
  {
  }

  /**
   * Creates a new servlet protocol
   *
   * @param uri the protocol uri, e.g. "hessian:" or "burlap:"
   */
  public ServletProtocolEmbed(String uri)
  {
    setUri(uri);
  }

  /**
   * Sets the protocol's uri
   */
  public void setUri(String uri)
  {
    _uri = uri;
  }

  /**
   * Adds a property.
   */
  public void addProperty(String name, Object value)
  {
    _init.addProgram(new PropertyValueProgram(name, value));
  }

  ServletProtocolConfig createProtocol()
  {
    ServletProtocolConfig protocol = new ServletProtocolConfig();
    protocol.setUri(_uri);
    protocol.setInit(_init);
    protocol.init();

    return protocol;
  }
}
